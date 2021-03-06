package srv.task;

import com.google.common.collect.Maps;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.ConfigManager;
import common.FilePathUtil;
import common.def.FileType;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import javafx.util.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by lihan on 2019/11/12.
 */
public class InsAndFlashMontor {
    private static InsAndFlashMontor ourInstance = new InsAndFlashMontor();

    public double storage_capacity = 0.0;

    public Double[] v_records = new Double[4];

    public long v_playback = 0L;

    private String[] sensorCodes = new String[4];

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    public static InsAndFlashMontor getInstance() {
        return ourInstance;
    }

    @SuppressWarnings("unchecked")
    private InsAndFlashMontor() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

        Document first = sate_res.find().first();
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");

        for (Document document : properties) {

            if (document.getString("key").equals("storage_capacity")) {
                storage_capacity = Double.parseDouble(document.getString("value")) * 1024 * 1024L;
            } else if (document.getString("key").equals("v_playback")) {
                v_playback = Long.parseLong(document.getString("value"));
            } else {
            }
        }

        for (Document document : properties) {

            if (document.getString("key").equals("v_record_1") && document.getString("group").equals("payload1")) {
                v_records[0] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_2") && document.getString("group").equals("payload2")) {
                v_records[1] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_3") && document.getString("group").equals("payload3")) {
                v_records[2] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_4") && document.getString("group").equals("payload4")) {
                v_records[3] = Double.parseDouble(document.getString("value"));
            } else {
            }
        }

        for (Document document : properties) {

            if (document.getString("key").equals("code") && document.getString("group").equals("payload1")) {
                sensorCodes[0] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload2")) {
                sensorCodes[1] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload3")) {
                sensorCodes[2] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload4")) {
                sensorCodes[3] = document.getString("value");
            } else {
            }
        }

    }

    public void startup() throws IOException, InterruptedException {
        DoWork doWork = new DoWork();
        doWork.start();
    }

    class DoWork extends Thread {
        public void run() {
            while (true) {
                try {
                    check();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    if (mongoClient == null) {
                        mongoClient = MangoDBConnector.getClient();
                        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
                    }
                    e.printStackTrace();
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void check() throws IOException {

            ArrayList<Document> pool_inss_image = new ArrayList<>();
            ArrayList<Document> pool_files_image = new ArrayList<>();

            ArrayList<Document> pool_inss_trans = new ArrayList<>();
            ArrayList<Document> pool_files_trans = new ArrayList<>();

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> image_missions = image_mission.find();

            for (Document document : image_missions) {
                if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {
                    if (document.getString("mission_state").equals("待执行")) {
                        if (document.containsKey("image_window")) {
                            ArrayList<Document> image_windows = (ArrayList<Document>) document.get("image_window");

                            if (image_windows.size() > 0) {
                                Document window = image_windows.get(0);
                                Date end_time = window.getDate("end_time");
                                Date now = Date.from(Instant.now());

                                if (end_time.before(now)) {
                                    document.append("mission_state", "已执行");
                                    Document modifiers = new Document();
                                    modifiers.append("$set", document);
                                    if (document.containsKey("_id"))
                                        document.remove("_id");
                                    image_mission.updateOne(new Document("mission_number", document.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));

                                    if (document.containsKey("order_numbers")) {
                                        try {
                                            ArrayList<String> OrderNumbers = (ArrayList<String>) document.get("order_numbers");
                                            MongoCollection<Document> Data_ImageOrderjson = mongoDatabase.getCollection("image_order");
                                            FindIterable<Document> D_ImageOrderjson = Data_ImageOrderjson.find();
                                            for (Document doc : D_ImageOrderjson) {
                                                if (OrderNumbers.contains(doc.getString("order_number"))) {
                                                    doc.append("order_state", "已执行");
                                                    if (doc.containsKey("_id"))
                                                        doc.remove("_id");
                                                    Document modifiers_mid = new Document();
                                                    modifiers_mid.append("$set", doc);
                                                    Data_ImageOrderjson.updateOne(new Document("order_number", doc.getString("order_number")), modifiers_mid, new UpdateOptions().upsert(true));
                                                }
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                            }

                        } else {

                            if (document.containsKey("expected_start_time") && document.getDate("expected_start_time") != null) {
                                Date end_time = document.getDate("expected_start_time");
                                Date now = Date.from(Instant.ofEpochMilli(Instant.now().toEpochMilli() + 2000L));

                                if (end_time.before(now)) {
                                    document.append("mission_state", "已执行");
                                    Document modifiers = new Document();
                                    modifiers.append("$set", document);
                                    if (document.containsKey("_id"))
                                        document.remove("_id");
                                    image_mission.updateOne(new Document("mission_number", document.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));

                                    if (document.containsKey("order_numbers")) {
                                        try {
                                            ArrayList<String> OrderNumbers = (ArrayList<String>) document.get("order_numbers");
                                            MongoCollection<Document> Data_ImageOrderjson = mongoDatabase.getCollection("image_order");
                                            FindIterable<Document> D_ImageOrderjson = Data_ImageOrderjson.find();
                                            for (Document doc : D_ImageOrderjson) {
                                                if (OrderNumbers.contains(doc.getString("order_number"))) {
                                                    doc.append("order_state", "已执行");
                                                    if (doc.containsKey("_id"))
                                                        doc.remove("_id");
                                                    Document modifiers_mid = new Document();
                                                    modifiers_mid.append("$set", doc);
                                                    Data_ImageOrderjson.updateOne(new Document("order_number", doc.getString("order_number")), modifiers_mid, new UpdateOptions().upsert(true));
                                                }
                                            }
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!document.containsKey("instruction_info"))
                        continue;

                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                    if (!CommUtils.checkInstructionInfo(instruction_info))
                        continue;

                    if (instruction_info.size() > 0) {
                        pool_inss_image.add(document);

                        if (document.containsKey("work_mode") && (document.getString("work_mode").contains("记录") || document.getString("work_mode").contains("擦除"))) {
                            pool_files_image.add(document);
                        }
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

                    if (instruction_info != null && instruction_info.size() > 0) {
                        pool_inss_trans.add(document);
                        pool_files_trans.add(document);
                    }
                }
            }

            MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
            FindIterable<Document> station_missions = station_mission.find();

            for (Document document : station_missions) {
                if (document.containsKey("tag") && document.getString("tag").contains("待执行")) {
                    if (document.containsKey("transmission_number") && !document.getString("transmission_number").equals("")) {
                        String transmission_number = document.getString("transmission_number");

                        for (Document tm : transmission_missions) {

                            if (tm.getString("transmission_number").equals(transmission_number)) {
                                Date t0 = null;
                                if (tm.containsKey("transmission_window")) {
                                    ArrayList<Document> transmission_window = (ArrayList<Document>) tm.get("transmission_window");

                                    if (transmission_window.size() > 0) {

                                        for (Document im : transmission_window) {
                                            Date end_time = im.getDate("end_time");

                                            if (t0 == null)
                                                t0 = end_time;
                                            else {
                                                if (end_time.after(t0))
                                                    t0 = end_time;
                                            }
                                        }
                                    }
                                }

                                if (t0 != null && t0.before(Date.from(Instant.now()))) {
                                    document.append("tag", "已执行");
                                    Document modifiers = new Document();
                                    modifiers.append("$set", document);
                                    station_mission.updateOne(new Document("mission_number", document.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                                }

                                break;
                            }
                        }
                    }
                }
            }

            if (pool_inss_image.size() > 0 || pool_inss_trans.size() > 0)
                procInsPool(pool_inss_image, pool_inss_trans);

            if (pool_files_image.size() > 0 || pool_files_trans.size() > 0) {
                procFilePool(pool_files_image, pool_files_trans, true);
                procFilePool(pool_files_image, pool_files_trans, false);
            }

//            mongoClient.close();
        }

        private void procInsPool(ArrayList<Document> pool_inss_image, ArrayList<Document> pool_inss_trans) {
            Map<Date, Document> insPool = new TreeMap<>();

            Date now = Date.from(Instant.now()/*.minusSeconds(3600 * 24 * 365 * 10L)*/);

            insertInsData(pool_inss_image, insPool, now, true);

            insertInsData(pool_inss_trans, insPool, now, false);

            MongoCollection<Document> pool_instruction = mongoDatabase.getCollection("pool_instruction");

            ArrayList<Document> data = new ArrayList<>();

            data.addAll(insPool.values());

            Document insertD = new Document();
            insertD.append("sequences", data);

            if (pool_instruction.count() <= 0)
                pool_instruction.insertOne(insertD);
            else {
                Document first = pool_instruction.find().first();
                Document modifiers = new Document();
                modifiers.append("$set", insertD);
                pool_instruction.updateOne(new Document("_id", first.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
            }
//            mongoClient.close();
        }

        @SuppressWarnings("unchecked")
        private void insertInsData(ArrayList<Document> pool_inss, Map<Date, Document> insPool, Date now, Boolean isImage) {
            for (Document d : pool_inss) {
                //System.out.println(d.toJson());
                ArrayList<Document> instruction_info = (ArrayList<Document>) d.get("instruction_info");

                int source = 0;

                if (d.containsKey("image_mode") && d.getString("image_mode").equals("人在回路"))
                    source = 1;

                if (d.containsKey("image_mode") && d.getString("image_mode").equals("删除"))
                    source = 2;

                for (Document ins : instruction_info) {

                    Document newIns = Document.parse(ins.toJson());

                    String number = isImage ? d.getString("mission_number") : d.getString("transmission_number");

                    if (newIns.getBoolean("valid")) {
                        newIns.remove("valid");

                        newIns.append("mission_number", number);

                        newIns.append("source", source);

                        Date t = newIns.getDate("execution_time");

                        if (
                                (d.containsKey("image_mode") && d.getString("image_mode").equals("人在回路"))
                                        &&
                                        (d.containsKey("output_type") && d.getString("output_type").equals("META"))) {
                            if (Date.from(Instant.ofEpochMilli(t.toInstant().toEpochMilli() + 24 * 3600 * 1000L)).before(now))
                                continue;
                        } else if (d.containsKey("image_mode") && d.getString("image_mode").equals("删除")) {
                            if (Date.from(Instant.ofEpochMilli(t.toInstant().toEpochMilli() + 24 * 3600 * 1000L)).before(now))
                                continue;
                        } else {
                            if (t.before(now))
                                continue;
                        }

                        int i = 1;
                        while (insPool.containsKey(t)) {
                            t.setTime(t.getTime() + i);
                            i++;
                        }
                        insPool.put(t, newIns);

                    } else {
                        //TODO 删除文件
                        String filename = FilePathUtil.getRealFilePath(ConfigManager.getInstance().fetchInsFilePath() + number);
                        //+ newIns.getString("sequence_code"));

                        File f = new File(filename);

                        if (f.exists() && f.isDirectory()) {
                            File[] files = f.listFiles();
                            for (File file : files) {
                                if (file.getName().contains(newIns.getString("sequence_code")) && !file.getName().contains("delete")) {
                                    String newfilename = FilePathUtil.getRealFilePath(filename + "\\" + file.getName() + ".delete");
                                    file.renameTo(new File(newfilename));
                                }
                            }
                        }
                    }
                }
            }
        }


        @SuppressWarnings("unchecked")
        private void procFilePool(ArrayList<Document> pool_files_image, ArrayList<Document> pool_files_trans, boolean isRT) {
            Instant instant = Instant.now();

            Date now = Date.from(instant);

            Date zeroTime = Date.from(instant.minusSeconds(60 * 60 * 24 * 365 * 10L));//时间零点初始化为十年前

            Date stopTime = isRT ? Date.from(instant) : Date.from(instant.plusSeconds(60 * 60 * 24 * 365 * 10L));

            for (Document d : pool_files_image) {
                if (d.getString("work_mode").contains("擦除") && d.getBoolean("clear_all")) {
                    Date execution_time = CommUtils.getExecTimeCachu(d);

                    if (execution_time == null)
                        continue;

                    if (execution_time.after(zeroTime) && execution_time.before(now))
                        zeroTime = execution_time;
                }
            }

            Map<Integer, Pair<Boolean, Boolean>> fileStatus = Maps.newLinkedHashMap();//第一个布尔值表示vaild，第二个表示replayed
            Map<Integer, Date> fileRecordTime = Maps.newLinkedHashMap();
            Map<Integer, Pair<Date, Date>> fileWindows = Maps.newLinkedHashMap();
            Map<Integer, Document> fileImageMission = Maps.newLinkedHashMap();
            Map<Integer, Double> fileRecordSize = Maps.newLinkedHashMap();
            Map<Integer, Double> filePlayBackSize = Maps.newLinkedHashMap();
            Map<Integer, FileType> filePlayBackStatus = Maps.newLinkedHashMap();

            for (int i = 0; i < 64; i++) {
                fileStatus.put(i, new Pair<>(true, false));
                fileRecordTime.put(i, zeroTime);
                fileWindows.put(i, new Pair<>(zeroTime, zeroTime));
                fileImageMission.put(i, new Document());
                fileRecordSize.put(i, 0.0);
                filePlayBackSize.put(i, 0.0);
                filePlayBackStatus.put(i, FileType.INIT);
            }

            ArrayList<Integer> SortedFileNo = new ArrayList<>();
            int PlayBackFileNoNow = -1;

            TreeMap<Date, Pair<Boolean, Document>> all_pool = new TreeMap<>();

            for (Document d : pool_files_image) {
                Date execution_time = null;

                if (d.getString("work_mode").contains("擦除"))
                    execution_time = CommUtils.getExecTimeCachu(d);
                else
                    execution_time = CommUtils.getExecTimeLast(d);

                if (execution_time == null)
                    continue;

                if (execution_time.equals(zeroTime) || execution_time.before(zeroTime) || execution_time.after(stopTime))
                    continue;

                int i = 1;
                while (all_pool.containsKey(execution_time)) {
                    execution_time = Date.from(execution_time.toInstant().plusMillis(i));
                    i++;
                }

                all_pool.put(execution_time, new Pair(true, d));
            }


            for (Document d : pool_files_trans) {
                Date execution_time = CommUtils.getExecTimeLast(d);

                if (execution_time == null)
                    continue;

                if (execution_time.equals(zeroTime) || execution_time.before(zeroTime) || execution_time.after(stopTime))
                    continue;

                if (all_pool.containsKey(execution_time))
                    execution_time = Date.from(execution_time.toInstant().plusSeconds(1));

                int i = 1;
                while (all_pool.containsKey(execution_time)) {
                    execution_time = Date.from(execution_time.toInstant().plusSeconds(i));
                    i++;
                }

                all_pool.put(execution_time, new Pair(false, d));
            }

//            double totalSize = 0L;//单位
            ArrayList<Document> stepRcd = new ArrayList<>();

            for (Date date : all_pool.keySet()) {
                Document d = all_pool.get(date).getValue();
                Date execution_time = CommUtils.getExecTimeLast(d);

                if (execution_time == null)
                    continue;
                if (all_pool.get(date).getKey()) {//记录任务
                    try {

                        if (d.getString("work_mode").contains("擦除")) {
                            ArrayList<String> filenos = new ArrayList<>();
                            if (d.getBoolean("clear_all")) {
                                for (int i = 0; i < 64; i++) {
                                    filenos.add(String.valueOf(i));
                                }
                            } else {
                                filenos = (ArrayList<String>) d.get("clear_filenos");
                            }


                            for (String file_no : filenos) {
                                if (file_no.equals(""))
                                    continue;

                                fileStatus.remove(Integer.parseInt(file_no));
                                fileStatus.put(Integer.parseInt(file_no), new Pair<>(true, false));

                                fileRecordTime.remove(Integer.parseInt(file_no));
                                fileRecordTime.put(Integer.parseInt(file_no), zeroTime);

                                fileWindows.remove(Integer.parseInt(file_no));
                                fileWindows.put(Integer.parseInt(file_no), new Pair<>(zeroTime, zeroTime));

                                fileImageMission.remove(Integer.parseInt(file_no));
                                fileImageMission.put(Integer.parseInt(file_no), new Document());

                                fileRecordSize.remove(Integer.parseInt(file_no));
                                fileRecordSize.put(Integer.parseInt(file_no), 0.0);

                                filePlayBackSize.remove(Integer.parseInt(file_no));
                                filePlayBackSize.put(Integer.parseInt(file_no), 0.0);

                                filePlayBackStatus.remove(Integer.parseInt(file_no));
                                filePlayBackStatus.put(Integer.parseInt(file_no), FileType.INIT);
                            }
                        } else {

                            if (!d.containsKey("image_window")) continue;

                            String file_no = d.getString("record_file_no");

                            ArrayList<Document> image_windows = (ArrayList<Document>) d.get("image_window");

                            Document window = image_windows.get(0);

                            fileStatus.remove(Integer.parseInt(file_no));
                            fileStatus.put(Integer.parseInt(file_no), new Pair<>(false, false));

                            fileRecordTime.remove(Integer.parseInt(file_no));
                            fileRecordTime.put(Integer.parseInt(file_no), execution_time);

                            fileWindows.remove(Integer.parseInt(file_no));
                            fileWindows.put(Integer.parseInt(file_no), new Pair<>(window.getDate("start_time"), window.getDate("end_time")));

                            fileImageMission.remove(Integer.parseInt(file_no));
                            fileImageMission.put(Integer.parseInt(file_no), d);

                            double size = calcRDSize(fileWindows.get(Integer.parseInt(file_no)).getKey(), fileWindows.get(Integer.parseInt(file_no)).getValue(), d);
                            fileRecordSize.remove(Integer.parseInt(file_no));
                            fileRecordSize.put(Integer.parseInt(file_no), size);

                            filePlayBackSize.remove(Integer.parseInt(file_no));
                            filePlayBackSize.put(Integer.parseInt(file_no), 0.0);

                            filePlayBackStatus.remove(Integer.parseInt(file_no));
                            filePlayBackStatus.put(Integer.parseInt(file_no), FileType.INIT);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {//回放模式

                    try {
                        ArrayList<Document> image_windows = (ArrayList<Document>) d.get("transmission_window");
                        double filemaxsize = 0.0;

                        if (d.getString("mode").contains("sequential")) {
                            //todo
                        } else if (d.getString("mode").contains("file")) {

                            int file_no = Integer.parseInt(d.getString("record_file_no"));

                            double playbacksize = 0.0;

                            for (Document window : image_windows) {
                                Date start_time = window.getDate("start_time");
                                Date end_time = window.getDate("end_time");

                                playbacksize += calcPBSize(start_time, end_time);
                            }

                            if (playbacksize > fileRecordSize.get(file_no)) {
                                Boolean key = fileStatus.get(file_no).getKey();
                                fileStatus.remove(file_no);
                                fileStatus.put(file_no, new Pair<>(key, true));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                long pool_size = 64;
                double flash_usage;
                double allsize = 0.0;

                Document doc = new Document();
                doc.append("time_stamp", execution_time);
                doc.append("pool_size", pool_size);
                for (int i = 0; i < 64; i++) {
                    Document data = new Document();
                    if (fileStatus.containsKey(i)) {
                        data.append("valid", fileStatus.get(i).getKey());
                        data.append("replayed", fileStatus.get(i).getValue());
                        double size = fileRecordSize.get(i);
                        allsize += size;
                        data.append("size", size);
                        data.append("window_start_time", fileWindows.get(i).getKey());
                        data.append("window_end_time", fileWindows.get(i).getValue());
                    } else {
                        data.append("size", 0.0);
                        data.append("valid", true);
                        data.append("replayed", false);
                    }

                    doc.append("file_" + i, data);
                }
                flash_usage = allsize / storage_capacity;
                doc.append("flash_usage", flash_usage);

                doc.append("replayed_size", 0.0);

                stepRcd.add(doc);

            }

            Document save = new Document();

            MongoCollection<Document> pool_files = mongoDatabase.getCollection("pool_files");
            Bson queryBson;

            if (isRT) {
                save.append("type", "REALTIME");
                queryBson = Filters.eq("type", "REALTIME");
            } else {
                save.append("type", "FORECAST");
                queryBson = Filters.eq("type", "FORECAST");
            }

            if (stepRcd.size() == 0) {
                Document doc = new Document();
                doc.append("time_stamp", Date.from(Instant.now()));
                doc.append("pool_size", 64);
                for (int i = 0; i < 64; i++) {
                    Document data = new Document();
                    data.append("valid", true);
                    data.append("replayed", false);
                    data.append("size", 0.0);


                    doc.append("file_" + i, data);
                }
                doc.append("flash_usage", 0.0);

                doc.append("replayed_size", 0.0);

                stepRcd.add(doc);
            }

            save.append("data", stepRcd);

            if (pool_files.count(queryBson) <= 0)
                pool_files.insertOne(save);
            else

            {
                Document first = pool_files.find(queryBson).first();
                Document modifiers = new Document();
                modifiers.append("$set", save);
                pool_files.updateOne(new Document("_id", first.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
            }
        }

        private double calcPBSize(Date start_time, Date end_time) {
            long spanMills = end_time.toInstant().toEpochMilli() - start_time.toInstant().toEpochMilli();

            double spanSec = spanMills / 1000.0;

            return spanSec * v_playback;
        }

        private double calcRDSize(Date start_time, Date end_time, Document imageMisson) {
            long spanMills = end_time.toInstant().toEpochMilli() - start_time.toInstant().toEpochMilli();

            double spanSec = spanMills / 1000.0;

            double size = getSizePerSec(imageMisson);

            return spanSec * size;
        }

        private double getSizePerSec(Document imageMisson) {
            try {
                double ret = 0.0;
                Boolean[] camEnables = new Boolean[4];
                Double[] frameP = new Double[4];

                for (int i = 0; i < camEnables.length; i++) {//初始化四个相机的状态，开始时为关机
                    camEnables[i] = false;
                }

                ArrayList<Document> expected_cam = (ArrayList<Document>) imageMisson.get("expected_cam");

                if (expected_cam == null)
                    return ret;

                if (expected_cam.size() == 0)
                    return ret;

                Document sat = expected_cam.get(0);

                ArrayList<Document> sensors = (ArrayList<Document>) sat.get("sensors");

                for (Document sensor : sensors) {
                    String code = sensor.getString("code");
                    int i = 0;
                    for (String sensorCode : sensorCodes) {
                        if (code.equals(sensorCode)) {
                            camEnables[i] = true;
                            break;
                        }
                        i++;
                    }
                }

                //计算压缩比
                double compressRate = 1.5;//无压缩则按1.5处理
                String P02 = "";

                ArrayList<Document> mission_params = (ArrayList<Document>) imageMisson.get("mission_params");

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P02")) {
                        P02 = mission_param.getString("value");
                    }
                }

                if (P02.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P02")) {
                            P02 = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (!P02.equals("")) {
                    if (P02.equals("1"))
                        compressRate = 2.0;
                }
                //压缩比计算完成

                //计算帧频
                if (camEnables[0] && camEnables[1]) {
                    String P08_AB = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P08_AB")) {
                            P08_AB = mission_param.getString("value");
                        }
                    }

                    if (P08_AB.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P08_AB")) {
                                P08_AB = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P08_AB.equals("06")) {
                        frameP[0] = 0.5;
                        frameP[1] = 0.5;
                    } else {
                        frameP[0] = 1.0;
                        frameP[1] = 1.0;
                    }
                } else if (camEnables[0] && !camEnables[1]) {
                    String P08_A = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P08_A")) {
                            P08_A = mission_param.getString("value");
                        }
                    }

                    if (P08_A.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P08_A")) {
                                P08_A = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P08_A.equals("06")) {
                        frameP[0] = 0.5;
                    } else if (P08_A.equals("07")) {
                        frameP[0] = 0.2;
                    } else if (P08_A.equals("08")) {
                        frameP[0] = 0.1;
                    } else {
                        frameP[0] = 1.0;
                    }

                } else if (!camEnables[0] && camEnables[1]) {
                    String P08_B = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P08_B")) {
                            P08_B = mission_param.getString("value");
                        }
                    }

                    if (P08_B.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P08_B")) {
                                P08_B = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P08_B.equals("02")) {
                        frameP[1] = 2.0;
                    } else if (P08_B.equals("03")) {
                        frameP[1] = 3.0;
                    } else if (P08_B.equals("04")) {
                        frameP[1] = 4.0;
                    } else if (P08_B.equals("05")) {
                        frameP[1] = 5.0;
                    } else if (P08_B.equals("06")) {
                        frameP[1] = 0.5;
                    } else if (P08_B.equals("07")) {
                        frameP[1] = 0.2;
                    } else if (P08_B.equals("08")) {
                        frameP[1] = 0.1;
                    } else {
                        frameP[1] = 1.0;
                    }
                } else {
                }

                if (camEnables[2] && camEnables[3]) {
                    String P09_AB = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P09_AB")) {
                            P09_AB = mission_param.getString("value");
                        }
                    }

                    if (P09_AB.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P08_AB")) {
                                P09_AB = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P09_AB.equals("02")) {
                        frameP[2] = 0.5;
                        frameP[3] = 0.5;
                    } else {
                        frameP[2] = 1.0;
                        frameP[3] = 1.0;
                    }
                } else if (camEnables[2] && !camEnables[3]) {
                    String P09_A = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P09_A")) {
                            P09_A = mission_param.getString("value");
                        }
                    }

                    if (P09_A.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P09_A")) {
                                P09_A = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P09_A.equals("02")) {
                        frameP[2] = 0.5;
                    } else if (P09_A.equals("03")) {
                        frameP[2] = 0.2;
                    } else if (P09_A.equals("04")) {
                        frameP[2] = 0.1;
                    } else {
                        frameP[2] = 1.0;
                    }

                } else if (!camEnables[2] && camEnables[3]) {
                    String P09_B = "";

                    for (Document mission_param : mission_params) {
                        if (mission_param.getString("code").equals("P09_B")) {
                            P09_B = mission_param.getString("value");
                        }
                    }

                    if (P09_B.equals("")) {
                        ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                        for (Document default_mission_param : default_mission_params) {
                            if (default_mission_param.getString("code").equals("P09_B")) {
                                P09_B = default_mission_param.getString("default_value");
                            }
                        }
                    }

                    if (P09_B.equals("02")) {
                        frameP[3] = 0.5;
                    } else if (P09_B.equals("03")) {
                        frameP[3] = 0.2;
                    } else if (P09_B.equals("04")) {
                        frameP[3] = 0.1;
                    } else {
                        frameP[3] = 1.0;
                    }
                } else {
                }
                //帧频计算完成

                for (int i = 0; i < 4; i++) {
                    if (camEnables[i]) {
                        ret += v_records[i] * frameP[i] / compressRate;
                    }
                }
                return ret;
            } catch (Exception e) {
                return 0.0;
            }
        }
    }
}