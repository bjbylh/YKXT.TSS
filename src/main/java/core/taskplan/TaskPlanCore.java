package core.taskplan;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.ConfigManager;
import common.def.MainTaskStatus;
import common.def.SubTaskStatus;
import common.def.TaskType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static common.mongo.CommUtils.*;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    private String taskId;
    private TaskType taskType;

    public TaskPlanCore(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
    }

    public void startup() {
        Thread t = new Thread(new TaskPlanCore.proc(taskId));
        t.start();
    }

    private class proc implements Runnable {
        private String id;
        private String[] subList;
        private ArrayList<String> mission_numbners;
        private ArrayList<String> station_mission_numbers = new ArrayList<>();
        private String Transmission_number = "";

        private MongoClient mongoClient;
        private MongoDatabase mongoDatabase;
        private Document Satllitejson;
        private FindIterable<Document> D_orbitjson;
        private long count;
        private ArrayList<Document> GroundStationjson;
        private Instant now = Instant.now();
        private Date startTime = Date.from(Instant.now().plusSeconds(24 * 60 * 60 * 10000));
        private Date endTime = Date.from(Instant.now().minusSeconds(24 * 60 * 60 * 10000));
        private String msgid = "";

        ArrayList<String> params2 = new ArrayList<>();

        private ArrayList<String> orderList;

        public proc(String taskId) {
            this.id = taskId;
            this.subList = getSubList(id);

            //连接数据库
            mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            //卫星资源表
            MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
            Satllitejson = Data_Satllitejson.find().first();

            //地面站资源表
            MongoCollection<Document> Data_GroundStationjson = mongoDatabase.getCollection("groundstation_resource");
            FindIterable<Document> D_GroundStationjson = Data_GroundStationjson.find();
            GroundStationjson = new ArrayList<>();
            for (Document document : D_GroundStationjson) {
                GroundStationjson.add(document);
            }

        }

        @Override
        public void run() {
            for (String aSubList : subList) {
                updateSubStatus(aSubList, SubTaskStatus.TODO);
            }

            RedisPublish.dbRefresh(id);

            System.out.println("[" + Instant.now().toString() + "] " + "需求统筹");
            //需求统筹
            if (MOD_ORDER_OVERALL_PLAN(subList[0])) return;

            System.out.println("[" + Instant.now().toString() + "] " + "可见性分析");
            //可见性分析
            if (MOD_VISIBILITY_CALC(subList[1])) return;

            System.out.println("[" + Instant.now().toString() + "] " + "时间分配");
            //时间分配
            if (MOD_MISSION_PLANNING(subList[2])) return;

            System.out.println("[" + Instant.now().toString() + "] " + "姿态角计算");
            //姿态角计算
            if (MOD_ATTITUDE_CALCULATION(subList[3])) return;

            System.out.println("[" + Instant.now().toString() + "] " + "资源平衡");
            //资源平衡
            if (MOD_ENERGY_CALCULATION(subList[4])) return;

            try {
                if (ConfigManager.getInstance().fetchDebug()) {
                    MongoCollection<Document> Data_Missionjson = mongoDatabase.getCollection("image_mission");
                    FindIterable<Document> D_Missionjson = Data_Missionjson.find();
                    ArrayList<Document> Missionjson = new ArrayList<>();
                    for (Document document : D_Missionjson) {
                        if (mission_numbners.contains(document.getString("mission_number")))
                            Missionjson.add(document);
                    }
                    InstructionGeneration.InstructionGenerationII(Missionjson, null, null, ConfigManager.getInstance().fetchInsFilePath());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println("[" + Instant.now().toString() + "] " + "Finished");
            if (TaskType.CRONTAB == taskType)
                updateMainStatus(id, MainTaskStatus.SUSPEND);
            else
                updateMainStatus(id, MainTaskStatus.FINISHED);
            RedisPublish.dbRefresh(id);

            RedisPublish.taskPlanFinished(msgid, orderList);

            mongoClient.close();
        }


        private void initOrbit() {
            //轨道数据表
            MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");

            Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

            D_orbitjson = Data_Orbitjson.find(Filters.and(queryBson));
            count = Data_Orbitjson.count(Filters.and(queryBson));
        }

        private boolean MOD_ORDER_OVERALL_PLAN(String subid) {
            updateSubStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            orderList = getOrderList(subid);

            ArrayList<Document> orders = new ArrayList<>();

            MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
            FindIterable<Document> documents = image_order.find();
            for (Document order : documents) {
                if (orderList.contains(order.getString("order_number"))) {
                    Date expected_start_time = order.getDate("expected_start_time");
                    if (expected_start_time.before(startTime))
                        startTime = expected_start_time;

                    Date expected_end_time = order.getDate("expected_end_time");
                    if (expected_end_time.after(endTime))
                        endTime = expected_end_time;

                    orders.add(order);

                    if (order.getString("mission_number") != null) {
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        Document filter = new Document();
                        filter.append("mission_number", order.getString("mission_number"));
                        image_mission.deleteOne(filter);
                    }
                }
            }

            if (taskType == TaskType.REALTIME) {
                Document condtion = new Document();
                condtion.append("_id", new ObjectId(subid));
                MongoCollection<Document> sub_task = mongoDatabase.getCollection("sub_task");

                Document first = sub_task.find(condtion).first();

                params2 = (ArrayList<String>) first.get("param2");
                msgid = first.getString("param3");
            }

            this.mission_numbners = OrderOverall.OrderOverallII(orders);

            for (Document order : documents) {
                if (orderList.contains(order.getString("order_number"))) {
                    if (order.containsKey("_id"))
                        order.remove("_id");
                    order.append("order_state", "待规划");
                    Document modifiers = new Document();
                    modifiers.append("$set", order);
                    image_order.updateOne(new Document("order_number", order.getString("order_number")), modifiers, new UpdateOptions().upsert(true));
                }
            }

            initOrbit();

            updateSubStatus(subid, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
            return checkIfStopped(id, 0);
        }

        private boolean MOD_VISIBILITY_CALC(String subid) {
            updateSubStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            //任务表
            MongoCollection<Document> Data_Missionjson = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> D_Missionjson = Data_Missionjson.find();
            ArrayList<Document> Missionjson = new ArrayList<>();
            for (Document document : D_Missionjson) {
                if (mission_numbners.contains(document.getString("mission_number")))
                    Missionjson.add(document);
            }

            MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");

            ArrayList<Document> station_missions = new ArrayList<>();

            if (taskType == TaskType.CRONTAB) {
                Date s = Date.from(now.plusSeconds(60 * 60 * 24));
                Date e = Date.from(now.plusSeconds(60 * 60 * 24 * 2));

                BasicDBObject query2 = new BasicDBObject();
                query2.put("expected_start_time", new BasicDBObject("$lte", e));
                query2.put("expected_end_time", new BasicDBObject("$gte", s));

                FindIterable<Document> documents2 = station_mission.find(query2);

                for (Document d : documents2) {
                    station_missions.add(d);
                }
            } else {

                FindIterable<Document> documents = station_mission.find();

                for (Document sn : documents) {
                    if (params2.contains(sn.getString("mission_number")))
                        station_missions.add(sn);
                }
            }

            for (Document d : station_missions) {
                if (d.getString("transmission_number") != null) {
                    MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
                    Document filter = new Document();
                    filter.append("transmission_number", d.getString("transmission_number"));
                    transmission_mission.deleteOne(filter);
                }
                station_mission_numbers.add(d.getString("mission_number"));
            }

            try {
                Document Transmissionjson = VisibilityCalculation.VisibilityCalculationII(Satllitejson, D_orbitjson, count, GroundStationjson, Missionjson, station_missions);

                if (Transmissionjson != null)
                    Transmission_number = Transmissionjson.getString("transmission_number");
            } catch (ParseException e) {
                e.printStackTrace();
            }

            updateSubStatus(subid, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
            return checkIfStopped(id, 0);
        }

        private boolean MOD_MISSION_PLANNING(String subid) {
            updateSubStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");

            ArrayList<Document> station_missions = new ArrayList<>();

            for (Document d : station_mission.find()) {
                if (station_mission_numbers.contains(d.getString("mission_number")))
                    station_missions.add(d);
            }


            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            ArrayList<Document> Missionjson = new ArrayList<>();
            for (Document mission : image_mission.find()) {
                if (mission_numbners.contains(mission.getString("mission_number"))) {
                    Missionjson.add(mission);
                }
            }

            MongoCollection<Document> transmission_misison = mongoDatabase.getCollection("transmission_mission");

            Document Transmissionjson = null;

            if (Transmission_number != "") {
                for (Document mission : transmission_misison.find()) {
                    if (Transmission_number.equals(mission.getString("transmission_number"))) {
                        Transmissionjson = mission;
                        break;
                    }

                }
            }

            MissionPlanning.MissionPlanningII(this.Satllitejson, this.GroundStationjson, this.D_orbitjson, this.count, Missionjson, Transmissionjson, station_missions);

            updateSubStatus(subid, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
            return checkIfStopped(id, 0);

        }

        private boolean MOD_ATTITUDE_CALCULATION(String subid) {
            updateSubStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            ArrayList<Document> Missionjson = new ArrayList<>();
            for (Document mission : image_mission.find()) {
                if (mission_numbners.contains(mission.getString("mission_number"))) {
                    Missionjson.add(mission);
                }
            }

            MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");

            Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));
            normal_attitude.deleteMany(queryBson);

            AttitudeCalculation.AttitudeCalculationII(this.Satllitejson, this.D_orbitjson, this.count, Missionjson);

//            MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
//            FindIterable<Document> documents = image_order.find();
//            for (Document order : documents) {
//                if (orderList.contains(order.getString("order_number"))) {
//                    if(order.containsKey("_id"))
//                        order.remove("_id");
//                    order.append("order_state", "待执行");
//                    Document modifiers = new Document();
//                    modifiers.append("$set", order);
//                    image_order.updateOne(new Document("order_number", order.getString("order_number")), modifiers, new UpdateOptions().upsert(true));
//                }
//            }
//
//            for (Document mission : image_mission.find()) {
//                if (mission_numbners.contains(mission.getString("mission_number"))) {
//                    if(mission.containsKey("_id"))
//                        mission.remove("_id");
//                    mission.append("mission_state", "待执行");
//                    Document modifiers = new Document();
//                    modifiers.append("$set", mission);
//                    image_mission.updateOne(new Document("mission_number", mission.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
//                }
//            }

            updateSubStatus(subid, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
            return checkIfStopped(id, 0);
        }

        private boolean MOD_ENERGY_CALCULATION(String subid) {
            updateSubStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");

            Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

            FindIterable<Document> documents = normal_attitude.find(Filters.and(queryBson));
            long dcount = normal_attitude.count(Filters.and(queryBson));

            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            ArrayList<Document> Missionjson = new ArrayList<>();
            for (Document mission : image_mission.find()) {
                if (mission_numbners.contains(mission.getString("mission_number"))) {
                    Missionjson.add(mission);
                }
            }


            MongoCollection<Document> transmission_misison = mongoDatabase.getCollection("transmission_mission");

            Document Transmissionjson = null;

            for (Document mission : transmission_misison.find()) {
                if (Transmission_number.equals(mission.getString("transmission_number"))) {
                    Transmissionjson = mission;
                    break;
                }

            }

            ReviewReset.ReviewResetII(Satllitejson, this.D_orbitjson, this.count, documents, dcount, Missionjson, Transmissionjson);


            updateSubStatus(subid, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
            return checkIfStopped(id, 0);
        }

        private ArrayList<String> getOrderList(String subid) {
            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> sub_task = mongoDatabase.getCollection("sub_task");

            ArrayList<String> params;
            if (taskType == TaskType.CRONTAB) {
                MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");

                Date s = Date.from(now.plusSeconds(60 * 60 * 24));
                Date e = Date.from(now.plusSeconds(60 * 60 * 24 * 2));

                BasicDBObject query = new BasicDBObject();
                query.put("expected_start_time", new BasicDBObject("$lte", e));
                query.put("expected_end_time", new BasicDBObject("$gte", s));

                FindIterable<Document> documents = image_order.find(query);
                params = new ArrayList<>();
                for (Document d : documents) {
                    params.add(d.getObjectId("_id").toString());
                }
            } else {
                Document condtion = new Document();
                condtion.append("_id", new ObjectId(subid));

                Document first = sub_task.find(condtion).first();

                params = (ArrayList<String>) first.get("param");
            }
            mongoClient.close();
            return params;
        }

        private boolean checkIfStopped(String id, int ser) {
            MainTaskStatus mainTaskStatus = checkMainTaskStatus(id);
            if (mainTaskStatus == MainTaskStatus.DELETE) {
                for (int i = ser + 1; i < subList.length; i++) {
                    updateSubStatus(subList[i], SubTaskStatus.DELETE);
                }
                RedisPublish.dbRefresh(id);
                return true;
            } else if (mainTaskStatus == MainTaskStatus.SUSPEND) {
                for (int i = ser + 1; i < subList.length; i++) {
                    updateSubStatus(subList[i], SubTaskStatus.SUSPEND);
                }
                RedisPublish.dbRefresh(id);
                return true;
            } else {
            }
            return false;
        }
    }
}
