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

    public static InsAndFlashMontor getInstance() {
        return ourInstance;
    }

    private InsAndFlashMontor() {
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

            if (pool_files_image.size() > 0 || pool_files_trans.size() > 0)
                procFilePool(pool_files_image, pool_files_trans);

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

        private void procFilePool(ArrayList<Document> pool_files_image, ArrayList<Document> pool_files_trans) {
            Date zeroTime = Date.from(Instant.now().minusSeconds(60 * 60 * 24 * 365 * 10L));//时间零点初始化为十年前

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

            for (Document d : pool_files_image) {
                try {
                    int file_no = Integer.parseInt(d.getString("record_file_no"));

                    Date execution_time = getExecTime(d);

                    if (execution_time == null)
                        continue;

                    if (execution_time.before(zeroTime))
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
                    fileWindows.put(file_no,new Pair<>(window.getDate("start_time"),window.getDate("end_time")));


                } catch (Exception e) {
                }
            }
        }
    }
}
