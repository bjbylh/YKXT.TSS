package srv.task;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.def.LightEnum;
import common.def.MissionType;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import javafx.util.Pair;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by lihan on 2020/3/1.
 */
public class EnergyCalc {
    public static void main(String[] args) {
        EnergyCalc energyCalc = new EnergyCalc();
        energyCalc.calcEnergy(Instant.now().minusSeconds(60 * 60 * 24 * 365), Instant.now(), 0.0, false);
        energyCalc.close();
    }

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    private Map<Range<Date>, MissionType> ranges = Maps.newHashMap();

    private double
            voltage = 42,//电压
            average_power_standby = 1849.78,//待机平均功率
            schf_power_image = 2410.91,//实传加回放功率
            sc_power_image = 2000.0,//实传功率
            jl_power_image = 2000.0,//记录功率
            hf_power_playback = 2246.27,//回放平均功率
            record_play_power = 2417.33,//边记边放功率
            sailboard_current = 4489.0,//太阳帆板最大输出功率
            power_efficiency = 0.94,//放电效率
            power_capacity = 125,//蓄电池容量
            power_charge = 0.93;//充电效率

    public EnergyCalc() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        loadConfig();
    }

    private void loadConfig() {
        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

        Document first = sate_res.find().first();
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");

        for (Document document : properties) {

            if (document.getString("key").equals("average_power_standby")) {
                average_power_standby = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("schf_power_image")) {
                schf_power_image = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("sc_power_image")) {
                sc_power_image = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("jl_power_image")) {
                jl_power_image = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("hf_power_playback")) {
                hf_power_playback = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("record_play_power")) {
                record_play_power = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("sailboard_current")) {
                sailboard_current = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("power_efficiency")) {
                power_efficiency = Double.parseDouble(document.getString("value")) / 100.0;
            } else if (document.getString("key").equals("power_capacity")) {
                power_capacity = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("power_charge")) {
                power_charge = Double.parseDouble(document.getString("value")) / 100.0;
            } else {
            }
        }
    }

    public void close() {
        if (mongoClient != null)
            mongoClient.close();
    }

    public void calcEnergy(Instant start, Instant end, double initValue, boolean useInitValue) {

//        Map<Instant, Document>
        TreeMap<Date, Pair<Boolean, Document>> pool = Maps.newTreeMap();

        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
        FindIterable<Document> image_missions = image_mission.find();

        for (Document document : image_missions) {
            if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {

                if (!document.containsKey("work_mode")) continue;

                if (document.containsKey("work_mode") && document.getString("work_mode").contains("擦除"))
                    continue;

                if (!document.containsKey("instruction_info"))
                    continue;

                ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                if (!CommUtils.checkInstructionInfo(instruction_info))
                    continue;

                if (!CommUtils.checkInstructionInfoValid(instruction_info))
                    continue;

                if (!document.containsKey("image_window"))
                    continue;

                ArrayList<Document> image_window = (ArrayList<Document>) document.get("image_window");

                if (instruction_info.size() > 0 && image_window.size() > 0) {
                    Date execTimeLast = CommUtils.getExecTimeLast(document);
                    Date execTimeFirst = CommUtils.getExecTimeFirst(document);

                    if (execTimeFirst.before(Date.from(start)))
                        continue;

                    if (execTimeFirst.after(Date.from(end)))
                        continue;

                    int i = 1;
                    while (pool.containsKey(execTimeLast)) {
                        execTimeLast = Date.from(execTimeLast.toInstant().plusMillis(i));
                        i++;
                    }

                    pool.put(execTimeLast, new Pair<>(true, document));
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

                if (!CommUtils.checkInstructionInfo(instruction_info))
                    continue;

                if (!CommUtils.checkInstructionInfoValid(instruction_info))
                    continue;

                if (!document.containsKey("transmission_window"))
                    continue;

                ArrayList<Document> transmission_window = (ArrayList<Document>) document.get("transmission_window");

                if (instruction_info.size() > 0 && transmission_window.size() > 0) {
                    Date execTimeLast = CommUtils.getExecTimeLast(document);
                    Date execTimeFirst = CommUtils.getExecTimeFirst(document);

                    if (execTimeFirst.before(Date.from(start)))
                        continue;

                    if (execTimeFirst.after(Date.from(end)))
                        continue;

                    int i = 1;
                    while (pool.containsKey(execTimeLast)) {
                        execTimeLast = Date.from(execTimeLast.toInstant().plusMillis(i));
                        i++;
                    }

                    pool.put(execTimeLast, new Pair<>(false, document));
                }
            }
        }

        clearDB(start, end);

        if (!useInitValue)
            initValue = fetchEnergy(start);


        for (Instant t = start; t.toEpochMilli() < end.toEpochMilli(); t.plusSeconds(10)) {
            LightEnum lightEnum = getLightEnum(t);

            if (lightEnum == LightEnum.SUNLIGHT) {
                MissionType missionType = getMissionType(t);

                if (missionType == MissionType.HF) {
                    initValue += (sailboard_current - hf_power_playback) * power_charge * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JL) {
                    initValue += (sailboard_current - jl_power_image) * power_charge * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JLHF) {
                    initValue += (sailboard_current - record_play_power) * power_charge * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SC) {
                    initValue += (sailboard_current - sc_power_image) * power_charge * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SCHF) {
                    initValue += (sailboard_current - schf_power_image) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else {
                    initValue += (sailboard_current - average_power_standby) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                }
            } else {
                MissionType missionType = getMissionType(t);

                if (missionType == MissionType.HF) {
                    initValue -= hf_power_playback / power_efficiency * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JL) {
                    initValue -= jl_power_image / power_efficiency * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JLHF) {
                    initValue -= record_play_power / power_efficiency * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SC) {
                    initValue -= sc_power_image / power_efficiency * 10.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SCHF) {
                    initValue -= schf_power_image / power_efficiency * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else {
                    initValue -= average_power_standby / power_efficiency * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                }
            }

            output(initValue);
        }
    }

    private void initRange(TreeMap<Date, Pair<Boolean, Document>> pool) {
        for (Date t : pool.keySet()) {
            if (pool.get(t).getKey()) {
                ArrayList<Document> image_windows = (ArrayList<Document>) pool.get(t).getValue().get("image_window");
                Document window = image_windows.get(0);

                Range r = Range.closedOpen(window.getDate("start_time"), window.getDate("end_time"));

                String workmode = pool.get(t).getValue().getString("work_mode");

               // if()
            } else {
                ArrayList<Document> transmission_window = (ArrayList<Document>) pool.get(t).getValue().get("transmission_window");

                for (Document window : transmission_window) {
                    Range r = Range.closedOpen(window.getDate("start_time"), window.getDate("end_time"));
                    ranges.put(r, MissionType.HF);
                }
            }
        }
    }

    private void output(double value) {

    }

    private void clearDB(Instant start, Instant end) {

    }

    public double fetchEnergy(Instant time) {
        return 0.0;
    }

    private MissionType getMissionType(Instant time) {
        return MissionType.HF;
    }

    private LightEnum getLightEnum(Instant time) {
        return LightEnum.SHADOW;
    }
}
