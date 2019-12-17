package srv.task;

import com.google.common.collect.Maps;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import javafx.util.Pair;
import org.bson.Document;

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

    public static InsAndFlashMontor getInstance() {
        return ourInstance;
    }

    private InsAndFlashMontor() {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");

        Document first = sate_res.find().first();
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");

        for (Document document : properties) {

            if (document.getString("key").equals("storage_capacity")) {
                storage_capacity = Long.parseLong(document.getString("value")) * 2014 * 1024L;
            } else if (document.getString("key").equals("v_record")) {
                v_record = Long.parseLong(document.getString("value"));
            } else if (document.getString("key").equals("v_playback")) {
                v_playback = Long.parseLong(document.getString("value"));
            } else {
            }
        }
        mongoClient.close();
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
                    Thread.sleep(1000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void check() throws IOException {
            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

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
                    if (instruction_info.size() > 0) {
                        pool_inss_image.add(document);

            if (document.getString("work_mode").contains("记录") || document.getString("work_mode").contains("擦除")) {
                pool_files_image.add(document);
            }
        }
    }
}

            MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
            FindIterable<Document> transmission_missions = transmission_mission.find();

            for (Document document : transmission_missions) {
                if (!document.containsKey("document") || document.getString("fail_reason").equals("")) {
                    if (!document.containsKey("instruction_info"))
                        continue;
                    ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");
                    if (instruction_info.size() > 0) {
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

            mongoClient.close();
        }

        private void procInsPool(ArrayList<Document> pool_inss_image, ArrayList<Document> pool_inss_trans) {
            Map<Date, Document> insPool = new TreeMap<>();

            insertInsData(pool_inss_image, insPool);

            insertInsData(pool_inss_trans, insPool);

            Date now = Date.from(Instant.now());

            for (Date date : insPool.keySet()) {
                if (date.before(now))
                    insPool.remove(date);
            }

            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> pool_instruction = mongoDatabase.getCollection("pool_instruction");
            for (Document d : pool_instruction.find()) {
                pool_instruction.deleteOne(d);
            }

            ArrayList<Document> data = new ArrayList<>();
            for (Document d : insPool.values()) {
                data.add(d);
            }

            Document insertD = new Document();
            insertD.append("sequences", data);

            pool_instruction.insertOne(insertD);
            mongoClient.close();
        }

        private void insertInsData(ArrayList<Document> pool_inss, Map<Date, Document> insPool) {
            for (Document d : pool_inss) {
                ArrayList<Document> instruction_info = (ArrayList<Document>) d.get("instruction_info");
                for (Document ins : instruction_info) {

                    Document newIns = Document.parse(ins.toJson());
                    if (newIns.getBoolean("valid")) {
                        newIns.remove("valid");
                        newIns.append("mission_number", d.getString("mission_number"));
                        Date t = newIns.getDate("execution_time");

                        insPool.put(t, newIns);
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
            Date zeroTime = Date.from(Instant.now().minusSeconds(60 * 60 * 24 * 365 * 10L));//时间零点初始化为十年前

            Date stopTime = isRT ? Date.from(Instant.now()) : Date.from(Instant.now().plusSeconds(60 * 60 * 24 * 365 * 10L));
            for (Document d : pool_files_image) {
                if (d.getString("work_mode").contains("擦除")) {
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

                if (execution_time.before(zeroTime) || execution_time.after(stopTime))
                    continue;

                int i = 1;
                while (all_pool.containsKey(execution_time)) {
                    execution_time = Date.from(execution_time.toInstant().plusSeconds(i));
                    i++;
                }

                all_pool.put(execution_time, new Pair(true, d));
            }


            for (Document d : pool_files_trans) {
                Date execution_time = getExecTime(d);

                if (execution_time == null)
                    continue;

                if (execution_time.before(zeroTime) || execution_time.after(stopTime))
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
                if (all_pool.get(date).getKey()) {
                    try {
                        int file_no = Integer.parseInt(d.getString("record_file_no"));

                        Date execution_time = getExecTime(d);

                        if (execution_time == null)
                            continue;

                        if (execution_time.before(zeroTime) || execution_time.after(stopTime))
                            continue;

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


                    } catch (Exception e) {
                    }
                } else {
                    try {

                        Date execution_time = getExecTime(d);

                        if (execution_time == null)
                            continue;

                        if (execution_time.before(zeroTime))
                            continue;

                        ArrayList<Document> image_windows = (ArrayList<Document>) d.get("image_window");

                        Document window = image_windows.get(0);

                        if (d.getString("mode").contains("sequential")) {
                            Date start_time = window.getDate("start_time");
                            Date end_time = window.getDate("end_time");

                            totalSize += calcPBSize(start_time, end_time);

                        } else if (d.getString("mode").contains("file")) {
                            boolean repeat = false;
                            int file_no = Integer.parseInt(d.getString("record_file_no"));
                            if (fileStatus.containsKey(file_no)) {
                                fileStatus.remove(file_no);
                                fileRecordTime.remove(file_no);
                                //fileWindows.remove(file_no);
                                repeat = true;
                            }

                            fileStatus.put(file_no, new Pair<>(false, true));
                            fileRecordTime.put(file_no, execution_time);
                            //fileWindows.put(file_no, new Pair<>(window.getDate("start_time"), window.getDate("end_time")));

                            if (!repeat) {
                                Date start_time = window.getDate("start_time");
                                Date end_time = window.getDate("end_time");

                                totalSize += calcPBSize(start_time, end_time);
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
                doc.append("space_used", totalSize);

                stepRcd.add(doc);
            }

            Document save = new Document();

            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> pool_files = mongoDatabase.getCollection("pool_files");
            if (isRT) {
                save.append("type", "REALTIME");
            } else {
                save.append("type", "FORECAST");
            }

            save.append("data", stepRcd);
            pool_files.insertOne(save);
            mongoClient.close();
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
