package srv.task;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.time.Instant;
import java.util.*;

/**
 * Created by lihan on 2020/2/20.
 */
public class CamAndScStatus {
    private static CamAndScStatus ourInstance = new CamAndScStatus();

    public static CamAndScStatus getInstance() {
        return ourInstance;
    }

    private CamAndScStatus() {
    }

    public Document getStatus(Instant now, Boolean isRealtime) {
        Document ret = new Document();

        String CamGFAStatus = "ON";
        String CamGFBStatus = "ON";
        String CamDGAStatus = "ON";
        String CamDGBStatus = "ON";

        String SCDevStatus = "ON";

        if (isRealtime) {

            ret
                    .append("CamGFAStatus", CamGFAStatus)
                    .append("CamGFBStatus", CamGFBStatus)
                    .append("CamDGAStatus", CamDGAStatus)
                    .append("CamDGBStatus", CamDGBStatus)
                    .append("SCDevStatus", SCDevStatus);

            return ret;


        } else {

            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            Map<Date, Document> insPool = new TreeMap<>();
//
//            ArrayList<Document> pool_inss_image = new ArrayList<>();
//            ArrayList<Document> pool_inss_trans = new ArrayList<>();

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> image_missions = image_mission.find();

            for (Document document : image_missions) {
                if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {

                    if (!document.containsKey("instruction_info"))
                        continue;

                    if (!document.containsKey("mission_params") || document.get("mission_params") == null)
                        continue;

                    if (!document.containsKey("default_mission_params") || document.get("default_mission_params") == null)
                        continue;

                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                    if (!checkInstructionInfo(instruction_info))
                        continue;

                    if (instruction_info.size() > 0) {

                        Document document1 = instruction_info.get(instruction_info.size() - 1);

                        Date time = document1.getDate("execution_time");

                        if (!insPool.containsKey(time))
                            insPool.put(time, document);
                    }
                }
            }

            MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
            FindIterable<Document> transmission_missions = transmission_mission.find();

            for (Document document : transmission_missions) {
                System.out.println(document);
                if (!document.containsKey("fail_reason") || document.getString("fail_reason").equals("")) {
                    if (!document.containsKey("instruction_info") || !document.getString("fail_reason").equals(""))
                        continue;

                    if (!document.containsKey("mission_params") || document.get("mission_params") == null)
                        continue;

                    if (!document.containsKey("default_mission_params") || document.get("default_mission_params") == null)
                        continue;

                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                    ArrayList<Document> mission_params = (ArrayList<Document>) document.get("mission_params");

                    ArrayList<Document> default_mission_params = (ArrayList<Document>) document.get("default_mission_params");

                    if (!checkInstructionInfo(instruction_info))
                        continue;

                    if (instruction_info.size() > 0 && mission_params.size() > 0 && default_mission_params.size() > 0) {

                        Document document1 = instruction_info.get(instruction_info.size() - 1);

                        Date time = document1.getDate("execution_time");

                        if (!insPool.containsKey(time))
                            insPool.put(time, document);
                    }
                }
            }

            if (insPool.size() > 0) {
                for (Date t : insPool.keySet()) {
                    DevStatusEnum camGFAStatusEnum = getCamGFAStatus(insPool.get(t));
                    DevStatusEnum camGFBStatusEnum = getCamGFBStatus(insPool.get(t));
                    DevStatusEnum camDGAStatusEnum = getCamDGAStatus(insPool.get(t));
                    DevStatusEnum camDGBStatusEnum = getCamDGBStatus(insPool.get(t));
                    DevStatusEnum SCDevStatusEnum = getSCDevStatus(insPool.get(t));

                    if (camGFAStatusEnum.name().equals(DevStatusEnum.OFF)) {
                        CamGFAStatus = "OFF";
                    } else if (camGFAStatusEnum.name().equals(DevStatusEnum.ON)) {
                        CamGFAStatus = "ON";
                    } else {
                    }

                    if (camGFBStatusEnum.name().equals(DevStatusEnum.OFF)) {
                        CamGFBStatus = "OFF";
                    } else if (camGFBStatusEnum.name().equals(DevStatusEnum.ON)) {
                        CamGFBStatus = "ON";
                    } else {
                    }

                    if (camDGAStatusEnum.name().equals(DevStatusEnum.OFF)) {
                        CamDGAStatus = "OFF";
                    } else if (camDGAStatusEnum.name().equals(DevStatusEnum.ON)) {
                        CamDGAStatus = "ON";
                    } else {
                    }

                    if (camDGBStatusEnum.name().equals(DevStatusEnum.OFF)) {
                        CamDGBStatus = "OFF";
                    } else if (camDGBStatusEnum.name().equals(DevStatusEnum.ON)) {
                        CamDGBStatus = "ON";
                    } else {
                    }

                    if (SCDevStatusEnum.name().equals(DevStatusEnum.OFF)) {
                        SCDevStatus = "OFF";
                    } else if (SCDevStatusEnum.name().equals(DevStatusEnum.ON)) {
                        SCDevStatus = "ON";
                    } else {
                    }
                }
            }

            ret
                    .append("CamGFAStatus", CamGFAStatus)
                    .append("CamGFBStatus", CamGFBStatus)
                    .append("CamDGAStatus", CamDGAStatus)
                    .append("CamDGBStatus", CamDGBStatus)
                    .append("SCDevStatus", SCDevStatus);

            return ret;
        }
    }

    private DevStatusEnum getCamGFAStatus(Document mission) {
        DevStatusEnum ret = DevStatusEnum.NONE;

        ArrayList<Document> instruction_info = (ArrayList<Document>) mission.get("instruction_info");

        ArrayList<Document> mission_params = (ArrayList<Document>) mission.get("mission_params");

        ArrayList<Document> default_mission_params = (ArrayList<Document>) mission.get("default_mission_params");

        for (Document meta : instruction_info) {
            if (meta.getString("sequence_code").equals("TCGFG01") && meta.getBoolean("valid")) {
                ArrayList<String> P07 = new ArrayList<>();

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P07")) {
                        P07 = (ArrayList<String>) mission_param.get("value");
                    }
                }

                if (P07 == null || P07.size() == 0) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P07")) {
                            P07 = (ArrayList<String>) default_mission_param.get("default_value");
                        }
                    }
                }

                if (P07 != null && P07.contains("0")) {
                    ret = DevStatusEnum.ON;
                }
            } else if (meta.getString("sequence_code").equals("TCGFG03") && meta.getBoolean("valid")) {
                String P165 = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P165")) {
                        P165 = mission_param.getString("value");
                    }
                }

                if (P165.equals("")) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P165")) {
                            P165 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P165.equals("1") || P165.equals("3")) {
                    ret = DevStatusEnum.OFF;
                }
            }
        }

        return ret;
    }

    private DevStatusEnum getCamGFBStatus(Document mission) {
        DevStatusEnum ret = DevStatusEnum.NONE;

        ArrayList<Document> instruction_info = (ArrayList<Document>) mission.get("instruction_info");

        ArrayList<Document> mission_params = (ArrayList<Document>) mission.get("mission_params");

        ArrayList<Document> default_mission_params = (ArrayList<Document>) mission.get("default_mission_params");

        for (Document meta : instruction_info) {
            if (meta.getString("sequence_code").equals("TCGFG01") && meta.getBoolean("valid")) {
                ArrayList<String> P07 = new ArrayList<>();

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P07")) {
                        P07 = (ArrayList<String>) mission_param.get("value");
                    }
                }

                if (P07 == null || P07.size() == 0) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P07")) {
                            P07 = (ArrayList<String>) default_mission_param.get("default_value");
                        }
                    }
                }

                if (P07 != null && P07.contains("1")) {
                    ret = DevStatusEnum.ON;
                }
            } else if (meta.getString("sequence_code").equals("TCGFG03") && meta.getBoolean("valid")) {
                String P165 = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P165")) {
                        P165 = mission_param.getString("value");
                    }
                }

                if (P165.equals("")) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P165")) {
                            P165 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P165.equals("2") || P165.equals("3")) {
                    ret = DevStatusEnum.OFF;
                }
            }
        }

        return ret;
    }

    private DevStatusEnum getCamDGAStatus(Document mission) {
        DevStatusEnum ret = DevStatusEnum.NONE;

        ArrayList<Document> instruction_info = (ArrayList<Document>) mission.get("instruction_info");

        ArrayList<Document> mission_params = (ArrayList<Document>) mission.get("mission_params");

        ArrayList<Document> default_mission_params = (ArrayList<Document>) mission.get("default_mission_params");

        for (Document meta : instruction_info) {
            if (meta.getString("sequence_code").equals("TCDGG01") && meta.getBoolean("valid")) {
                ArrayList<String> P07 = new ArrayList<>();

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P07")) {
                        P07 = (ArrayList<String>) mission_param.get("value");
                    }
                }

                if (P07 == null || P07.size() == 0) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P07")) {
                            P07 = (ArrayList<String>) default_mission_param.get("default_value");
                        }
                    }
                }

                if (P07 != null && P07.contains("2")) {
                    ret = DevStatusEnum.ON;
                }
            } else if (meta.getString("sequence_code").equals("TCDGG03") && meta.getBoolean("valid")) {
                String P167 = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P167")) {
                        P167 = mission_param.getString("value");
                    }
                }

                if (P167.equals("")) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P167")) {
                            P167 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P167.equals("1") || P167.equals("3")) {
                    ret = DevStatusEnum.OFF;
                }
            }
        }

        return ret;
    }

    private DevStatusEnum getCamDGBStatus(Document mission) {
        DevStatusEnum ret = DevStatusEnum.NONE;

        ArrayList<Document> instruction_info = (ArrayList<Document>) mission.get("instruction_info");

        ArrayList<Document> mission_params = (ArrayList<Document>) mission.get("mission_params");

        ArrayList<Document> default_mission_params = (ArrayList<Document>) mission.get("default_mission_params");

        for (Document meta : instruction_info) {
            if (meta.getString("sequence_code").equals("TCDGG01") && meta.getBoolean("valid")) {
                ArrayList<String> P07 = new ArrayList<>();

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P07")) {
                        P07 = (ArrayList<String>) mission_param.get("value");
                    }
                }

                if (P07 == null || P07.size() == 0) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P07")) {
                            P07 = (ArrayList<String>) default_mission_param.get("default_value");
                        }
                    }
                }

                if (P07 != null && P07.contains("3")) {
                    ret = DevStatusEnum.ON;
                }
            } else if (meta.getString("sequence_code").equals("TCDGG03") && meta.getBoolean("valid")) {
                String P167 = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P167")) {
                        P167 = mission_param.getString("value");
                    }
                }

                if (P167.equals("")) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P167")) {
                            P167 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P167.equals("2") || P167.equals("3")) {
                    ret = DevStatusEnum.OFF;
                }
            }
        }
        return ret;
    }

    private DevStatusEnum getSCDevStatus(Document mission) {
        DevStatusEnum ret = DevStatusEnum.NONE;

        ArrayList<Document> instruction_info = (ArrayList<Document>) mission.get("instruction_info");

        ArrayList<Document> mission_params = (ArrayList<Document>) mission.get("mission_params");

        ArrayList<Document> default_mission_params = (ArrayList<Document>) mission.get("default_mission_params");

        for (Document meta : instruction_info) {
            if (meta.getString("sequence_code").equals("TCKG02") && meta.getBoolean("valid")) {
                String P170 = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P170")) {
                        P170 = mission_param.getString("value");
                    }
                }

                if (P170.equals("")) {
                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P170")) {
                            P170 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P170 == "0") {
                    ret = DevStatusEnum.ON;
                }
            } else if (meta.getString("sequence_code").equals("TCS297") && meta.getBoolean("valid")) {
                ret = DevStatusEnum.ON;
            } else if
                    (
                    (meta.getString("sequence_code").equals("TCAG01") && meta.getBoolean("valid"))
                            ||
                            (meta.getString("sequence_code").equals("TCAG02") && meta.getBoolean("valid"))
                            ||
                            (meta.getString("sequence_code").equals("TCAG03") && meta.getBoolean("valid"))
                            ||
                            (meta.getString("sequence_code").equals("TCAG04") && meta.getBoolean("valid"))
                            ||
                            (meta.getString("sequence_code").equals("TCAG05") && meta.getBoolean("valid"))
                            ||
                            (meta.getString("sequence_code").equals("TCAG06") && meta.getBoolean("valid"))

                    ) {
                ret = DevStatusEnum.OFF;
            }
        }
        return ret;
    }

    private boolean checkInstructionInfo(ArrayList<Document> instruction_info) {
        if (instruction_info == null)
            return false;

        for (Document document : instruction_info) {

            if (!document.containsKey("valid") || !Objects.equals(document.get("valid").getClass().getName(), "java.lang.Boolean"))
                return false;

            if (!document.containsKey("sequence_code") || !Objects.equals(document.get("sequence_code").getClass().getName(), "java.lang.String"))
                return false;


//                System.out.println(document.get("execution_time").getClass().getName());
            if (!document.containsKey("execution_time") || !Objects.equals(document.get("execution_time").getClass().getName(), "java.util.Date"))
                return false;
        }

        return true;
    }

    public static void main(String[] args) {
        Document status = CamAndScStatus.getInstance().getStatus(Instant.now(), false);
        System.out.println(status.toJson());
    }
}
