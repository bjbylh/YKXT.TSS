package srv.task;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.LightEnum;
import common.def.MissionType;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import javafx.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

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
        energyCalc.calcEnergy(Instant.now().minusSeconds(60 * 60 * 24 * 2), Instant.now(), 0.0, false);
        energyCalc.close();
    }

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    private Map<Range<Date>, MissionType> rangesMissions = Maps.newHashMap();

    private Map<Range<Date>, Map<Range<Date>, LightEnum>> rangesLight = Maps.newHashMap();


    private MongoCollection<Document> satellite_energy = null;

    private MongoCollection<Document> orbit_attitude = null;

    private MongoCollection<Document> range_sunlight = null;


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

        satellite_energy = mongoDatabase.getCollection("satellite_energy");

        range_sunlight = mongoDatabase.getCollection("range_sunlight");

        orbit_attitude = mongoDatabase.getCollection("orbit_attitude");

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

        initRange(pool);

        ArrayList<Document> datas = new ArrayList<>();

        for (Instant t = start; t.toEpochMilli() < end.toEpochMilli(); ) {
            LightEnum lightEnum = getLightEnum(t);

            if (lightEnum == LightEnum.SUNLIGHT) {
                MissionType missionType = getMissionType(t);

                if (missionType == MissionType.HF) {
                    initValue += fetchInclination(t) * (sailboard_current - hf_power_playback) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JL) {
                    initValue += fetchInclination(t) * (sailboard_current - jl_power_image) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JLHF) {
                    initValue += fetchInclination(t) * (sailboard_current - record_play_power) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SC) {
                    initValue += fetchInclination(t) * (sailboard_current - sc_power_image) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SCHF) {
                    initValue += fetchInclination(t) * (sailboard_current - schf_power_image) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else {
                    initValue += fetchInclination(t) * (sailboard_current - average_power_standby) * power_charge * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                }
            } else {
                MissionType missionType = getMissionType(t);

                if (missionType == MissionType.HF) {
                    initValue -= hf_power_playback / power_efficiency * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JL) {
                    initValue -= jl_power_image / power_efficiency * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.JLHF) {
                    initValue -= record_play_power / power_efficiency * 60.0 / 3600.0 / voltage;

                    if (initValue > power_capacity)
                        initValue = power_capacity;
                } else if (missionType == MissionType.SC) {
                    initValue -= sc_power_image / power_efficiency * 60.0 / 3600.0 / voltage;

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

            Document d = new Document();
            d.append("time_point", Date.from(t));
            d.append("energy", initValue);
            datas.add(d);
            t = t.plusSeconds(60);
        }

        satellite_energy.insertMany(datas);

    }

    public void energyReset(double value) {
        Document d = new Document();
        d.append("time_point", Date.from(Instant.now()));
        d.append("energy", value);
        satellite_energy.insertOne(d);
    }

    private void initRange(TreeMap<Date, Pair<Boolean, Document>> pool) {
        for (Date t : pool.keySet()) {
            if (pool.get(t).getKey()) {
                ArrayList<Document> image_windows = (ArrayList<Document>) pool.get(t).getValue().get("image_window");
                Document window = image_windows.get(0);

                Range r = Range.closedOpen(window.getDate("start_time"), window.getDate("end_time"));

                String workmode = pool.get(t).getValue().getString("work_mode");

                if (workmode.trim().equals("记录"))
                    rangesMissions.put(r, MissionType.JL);
                else if (workmode.trim().equals("实传")) {
                    rangesMissions.put(r, MissionType.SC);
                } else if (workmode.trim().equals("记录+回放"))
                    rangesMissions.put(r, MissionType.JLHF);
                else if (workmode.trim().equals("实传+回放"))
                    rangesMissions.put(r, MissionType.SCHF);
                else
                    rangesMissions.put(r, MissionType.STANDBY);
            } else {
                ArrayList<Document> transmission_window = (ArrayList<Document>) pool.get(t).getValue().get("transmission_window");

                for (Document window : transmission_window) {
                    Range r = Range.closedOpen(window.getDate("start_time"), window.getDate("end_time"));
                    rangesMissions.put(r, MissionType.HF);
                }
            }
        }

        FindIterable<Document> documents = range_sunlight.find();

        for (Document d : documents) {
            Date from = d.getDate("time_point");
            Date to = Date.from(from.toInstant().plusSeconds(3600 * 24 * 7L));

            Range<Date> dateRange = Range.closedOpen(from, to);

            if (!d.containsKey("range_window")) continue;

            ArrayList<Document> range_window = (ArrayList<Document>) d.get("range_window");

            if (range_window.size() > 0) {

                Map<Range<Date>, LightEnum> lm = Maps.newHashMap();

                for (Document window : range_window) {
                    Date start = window.getDate("start_time");
                    Date end = window.getDate("end_time");

                    Range<Date> dateRange1 = Range.closedOpen(start, end);

                    lm.put(dateRange1, LightEnum.SUNLIGHT);

                }
                rangesLight.put(dateRange, lm);
            }
        }
    }

    private void clearDB(Instant start, Instant end) {
        Bson queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));
        satellite_energy.deleteMany(queryBson);
    }

    public void clearAllDB() {
        clearDB(Instant.now().minusSeconds(3600 * 24 * 365 * 10L), Instant.now().plusSeconds(3600 * 24 * 365 * 10L));
    }

    public double fetchEnergy(Instant time) {

        try {
            Document query = new Document();
            query.append("time_point", new Document().append("$lte", Date.from(time)));

            Document sort = new Document();
            sort.append("time_point", -1.0);

            int limit = 1;

            Document first = satellite_energy.find(query).sort(sort).limit(limit).first();

            if (first == null)
                return power_capacity;

            return first.getDouble("energy");

        } catch (Exception e) {
            e.printStackTrace();
            return power_capacity;
        }
    }

    public double fetchInclination(Instant time) {

        try {
            Document query = new Document();
            query.append("time_point", new Document().append("$lte", Date.from(time)));

            Document sort = new Document();
            sort.append("time_point", -1.0);

            int limit = 1;

            Document first = orbit_attitude.find(query).sort(sort).limit(limit).first();

            if (first == null)
                return Math.cos(Math.PI);

            return Math.abs(Math.cos(first.getDouble("solar_panel_Angle")));

        } catch (Exception e) {
            e.printStackTrace();
            return Math.cos(Math.PI);
        }
    }

    private MissionType getMissionType(Instant time) {

        for (Range<Date> r : rangesMissions.keySet()) {
            if (r.contains(Date.from(time)))
                return rangesMissions.get(r);
        }
        return MissionType.STANDBY;
    }

    private LightEnum getLightEnum(Instant time) {
        Range<Date> initDate = null;

        for (Range<Date> r : rangesLight.keySet()) {
            Date from = Date.from(time);
            if (r.contains(from)) {
                if (initDate == null)
                    initDate = r;
                else {
                    if (r.upperEndpoint().after(initDate.upperEndpoint()))
                        initDate = r;
                }
            }
        }

        if (initDate == null)
            return LightEnum.SHADOW;
        else {
            Map<Range<Date>, LightEnum> rangeLightEnumMap = rangesLight.get(initDate);

            for (Range<Date> r : rangeLightEnumMap.keySet()) {
                if (r.contains(Date.from(time)))
                    return LightEnum.SUNLIGHT;
            }
        }

        return LightEnum.SHADOW;
    }
}
