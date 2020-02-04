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

    public long storage_capacity = 0L;

    public long v_record = 0L;

    public long v_playback = 0L;

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    public static InsAndFlashMontor getInstance() {
        return ourInstance;
    }

    private InsAndFlashMontor() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

        Document first = sate_res.find().first();
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");

        for (Document document : properties) {

            if (document.getString("key").equals("storage_capacity")) {
                storage_capacity = Long.parseLong(document.getString("value")) * 1024 * 1024L;
            } else if (document.getString("key").equals("v_record")) {
                v_record = Long.parseLong(document.getString("value"));
            } else if (document.getString("key").equals("v_playback")) {
                v_playback = Long.parseLong(document.getString("value"));
            } else {
            }
        }
//        mongoClient.close();
    }

    public void startup() throws IOException, InterruptedException {
        InsAndFlashMontor.DoWork doWork = new InsAndFlashMontor.DoWork();
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

        private void check() throws IOException {

            ArrayList<Document> pool_inss_image = new ArrayList<>();
            ArrayList<Document> pool_files_image = new ArrayList<>();

            ArrayList<Document> pool_inss_trans = new ArrayList<>();
            ArrayList<Document> pool_files_trans = new ArrayList<>();

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> image_missions = image_mission.find();

            for (Document document : image_missions) {
                if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {
                    if (!document.containsKey("instruction_info"))
                        continue;
                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");
                    if (instruction_info != null && instruction_info.size() > 0) {
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
                    if (instruction_info != null && instruction_info.size() > 0) {
                        pool_inss_trans.add(document);
                        pool_files_trans.add(document);
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

            insertInsData(pool_inss_image, insPool, now);

            insertInsData(pool_inss_trans, insPool, now);

            MongoCollection<Document> pool_instruction = mongoDatabase.getCollection("pool_instruction");
//            for (Document d : pool_instruction.find()) {
//                pool_instruction.deleteOne(d);
//            }

            ArrayList<Document> data = new ArrayList<>();
            for (Document d : insPool.values()) {
                data.add(d);
            }

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

        private void insertInsData(ArrayList<Document> pool_inss, Map<Date, Document> insPool, Date now) {
            for (Document d : pool_inss) {
                //System.out.println(d.toJson());
                ArrayList<Document> instruction_info = (ArrayList<Document>) d.get("instruction_info");
                for (Document ins : instruction_info) {

                    Document newIns = Document.parse(ins.toJson());
                    if (newIns.getBoolean("valid")) {
                        newIns.remove("valid");
                        newIns.append("mission_number", d.getString("mission_number"));
                        Date t = newIns.getDate("execution_time");

                        if (t.before(now))
                            continue;

//                        if (!insPool.containsKey(t))
//                            insPool.put(t, newIns);

                        int i = 1;
                        while(insPool.containsKey(t)){
                            t.setTime(t.getTime() + i);
                            i++;
                        }
                        insPool.put(t, newIns);

                    } else {
                        //TODO 删除文件
                        String filename = FilePathUtil.getRealFilePath(ConfigManager.getInstance().fetchInsFilePath() + d.getString("mission_number"));
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

        private Date getExecTime(Document d) {
            ArrayList<Document> instruction_info = (ArrayList<Document>) d.get("instruction_info");
            if (!instruction_info.get(instruction_info.size() - 1).getBoolean("valid"))
                return null;

            return instruction_info.get(instruction_info.size() - 1).getDate("execution_time");
        }

        private void procFilePool(ArrayList<Document> pool_files_image, ArrayList<Document> pool_files_trans, boolean isRT) {
            Instant instant = Instant.now();

            Date now = Date.from(instant);

            Date zeroTime = Date.from(instant.minusSeconds(60 * 60 * 24 * 365 * 10L));//时间零点初始化为十年前

            Date stopTime = isRT ? Date.from(instant) : Date.from(instant.plusSeconds(60 * 60 * 24 * 365 * 10L));
            for (Document d : pool_files_image) {
                if (d.getString("work_mode").contains("擦除") && d.getBoolean("clear_all")) {
                    Date execution_time = getExecTime(d);

                    if (execution_time == null)
                        continue;

                    if (execution_time.after(zeroTime))
                        zeroTime = execution_time;
                }
            }

            Map<Integer, Pair<Boolean, Boolean>> fileStatus = Maps.newLinkedHashMap();//第一个布尔值表示vaild，第二个表示replayed
            Map<Integer, Date> fileRecordTime = Maps.newLinkedHashMap();
            Map<Integer, Pair<Date, Date>> fileWindows = Maps.newLinkedHashMap();

            TreeMap<Date, Pair<Boolean, Document>> all_pool = new TreeMap<>();

            for (Document d : pool_files_image) {
                Date execution_time = getExecTime(d);

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
                Date execution_time = getExecTime(d);

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

            double totalSize = 0L;//单位
            ArrayList<Document> stepRcd = new ArrayList<>();

            for (Date date : all_pool.keySet()) {
                Document d = all_pool.get(date).getValue();
                Date execution_time = getExecTime(d);
                if (all_pool.get(date).getKey()) {
                    try {

                        if (d.getString("work_mode").contains("擦除")) {
                            ArrayList<String> filenos = (ArrayList<String>) d.get("clear_filenos");

                            for (String file_no : filenos) {
                                if (fileStatus.containsKey(Integer.parseInt(file_no))) {

                                    boolean isReplayed = fileStatus.get(Integer.parseInt(file_no)).getValue();//判断是否被计算过

                                    if (isReplayed) {
                                        ArrayList<Document> image_windows = (ArrayList<Document>) d.get("image_window");
                                        Document window = image_windows.get(0);
                                        Date start_time = window.getDate("start_time");
                                        Date end_time = window.getDate("end_time");
                                        totalSize -= calcPBSize(start_time, end_time);
                                    }

                                    fileStatus.remove(Integer.parseInt(file_no));
                                    fileRecordTime.remove(Integer.parseInt(file_no));
                                    fileWindows.remove(Integer.parseInt(file_no));
                                }
                            }

                        } else {
                            int file_no = Integer.parseInt(d.getString("record_file_no"));

                            ArrayList<Document> image_windows = (ArrayList<Document>) d.get("image_window");

                            Document window = image_windows.get(0);

                            if (fileStatus.containsKey(file_no)) {
                                fileStatus.remove(file_no);
                                fileRecordTime.remove(file_no);
                                fileWindows.remove(file_no);
                            }

                            fileStatus.put(file_no, new Pair<>(false, false));
                            fileRecordTime.put(file_no, execution_time);
                            fileWindows.put(file_no, new Pair<>(window.getDate("start_time"), window.getDate("end_time")));
                        }

                    } catch (Exception e) {
                    }
                } else {

                    try {
                        ArrayList<Document> image_windows = (ArrayList<Document>) d.get("image_window");

                        //Document window = image_windows.get(0);

                        if (d.getString("mode").contains("sequential")) {
                            for (Document window : image_windows) {
                                Date start_time = window.getDate("start_time");
                                Date end_time = window.getDate("end_time");

                                totalSize += calcPBSize(start_time, end_time);
                            }

                        } else if (d.getString("mode").contains("file")) {
                            boolean needCalc = false;
                            int file_no = Integer.parseInt(d.getString("record_file_no"));
                            if (fileStatus.containsKey(file_no)) {
                                //fileWindows.remove(file_no);
                                needCalc = !fileStatus.get(file_no).getValue();//判断是否需要计算容量
                                fileStatus.remove(file_no);
                                fileRecordTime.remove(file_no);
                            }

                            fileStatus.put(file_no, new Pair<>(false, true));
                            fileRecordTime.put(file_no, execution_time);
                            //fileWindows.put(file_no, new Pair<>(window.getDate("start_time"), window.getDate("end_time")));


                            if (needCalc) {
                                for (Document window : image_windows) {
                                    Date start_time = window.getDate("start_time");
                                    Date end_time = window.getDate("end_time");
                                    totalSize += calcPBSize(start_time, end_time);
                                }
                            }
                        } else {
                        }
                    } catch (Exception e) {
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
                        double size = 0.0;
                        if (fileWindows.containsKey(i)) {
                            size = calcRDSize(fileWindows.get(i).getKey(), fileWindows.get(i).getValue());
                        }
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
                doc.append("replayed_size", totalSize);

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

            save.append("data", stepRcd);

            if (pool_files.count(queryBson) <= 0)
                pool_files.insertOne(save);
            else {
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

        private double calcRDSize(Date start_time, Date end_time) {
            long spanMills = end_time.toInstant().toEpochMilli() - start_time.toInstant().toEpochMilli();

            double spanSec = spanMills / 1000.0;

            return spanSec * v_record;
        }
    }
}
