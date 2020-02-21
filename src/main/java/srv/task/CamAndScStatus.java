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

    public Document getSatus(Instant now, Boolean isRealtime) {
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

            ArrayList<Document> pool_inss_image = new ArrayList<>();
            ArrayList<Document> pool_inss_trans = new ArrayList<>();

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> image_missions = image_mission.find();

            for (Document document : image_missions) {
                if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {

                    if (!document.containsKey("instruction_info"))
                        continue;

                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                    if (!checkInstructionInfo(instruction_info))
                        continue;

                    if (instruction_info.size() > 0) {
                        pool_inss_image.add(document);
                    }
                }
            }

            MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
            FindIterable<Document> transmission_missions = transmission_mission.find();

            for (Document document : transmission_missions) {
                if (!document.containsKey("fail_reason") || document.getString("fail_reason").equals("")) {
                    if (!document.containsKey("instruction_info") || !document.getString("fail_reason").equals(""))
                        continue;

                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                    if (!checkInstructionInfo(instruction_info))
                        continue;

                    if (instruction_info.size() > 0) {
                        pool_inss_trans.add(document);
                    }
                }
            }

            if (pool_inss_image.size() > 0 || pool_inss_trans.size() > 0) {
                Map<Date, Document> insPool = new TreeMap<>();

                insertInsData(pool_inss_image, insPool, Date.from(now), true);

                insertInsData(pool_inss_trans, insPool, Date.from(now), false);

                for (Date t : insPool.keySet()){

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

    private void insertInsData(ArrayList<Document> pool_inss, Map<Date, Document> insPool, Date now, Boolean isImage) {
        for (Document d : pool_inss) {
            //System.out.println(d.toJson());
            ArrayList<Document> instruction_info = (ArrayList<Document>) d.get("instruction_info");
            for (Document ins : instruction_info) {

                Document newIns = Document.parse(ins.toJson());

                if (newIns.getBoolean("valid")) {
                    newIns.remove("valid");

                    Date t = newIns.getDate("execution_time");

                    if (t.after(now))
                        continue;

                    int i = 1;
                    while (insPool.containsKey(t)) {
                        t.setTime(t.getTime() + i);
                        i++;
                    }
                    insPool.put(t, newIns);
                }
            }
        }
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
}
