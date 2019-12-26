package srv.task;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import core.orbit.OrbitCore;
import core.taskplan.TaskPlanCore;
import org.bson.Document;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Created by lihan on 2018/11/13.
 */
public class TaskMonitor {
    private static TaskMonitor ourInstance;

    static {
        ourInstance = new TaskMonitor();
    }

    public static TaskMonitor getInstance() {
        return ourInstance;
    }

    private TaskMonitor() {

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
                    Thread.sleep(1000 * 30);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void check() throws IOException {
            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

            BasicDBObject query = new BasicDBObject();
            BasicDBList saleChannel = new BasicDBList();
            saleChannel.add(MainTaskStatus.NEW.name());
            saleChannel.add(MainTaskStatus.SUSPEND.name());
            query.put("status", new BasicDBObject("$in", saleChannel));


            FindIterable<Document> main_task = tasks.find(query);

//            for(Document d : main_task){
//                System.out.println(d.toString());
//            }

            for (Document document : main_task) {
                if (document.getString("status").equals(MainTaskStatus.NEW.name())) {
                    if (document.getString("type").equals(TaskType.REALTIME.name())) {

                        System.out.println("Found a new REALTIME TASK, Task Info:" + document.toString());

                        Instant instant_ft = Instant.now();
                        Date nt_str = Date.from(instant_ft);

                        tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));
                        tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("rt_core.start_time", nt_str)));

                        String templet = document.getString("templet");

                        if (templet.equals(TempletType.ORBIT_FORECAST.name())) {
                            OrbitCore orbitCore = new OrbitCore(document.get("_id").toString(), TaskType.REALTIME);
                            orbitCore.startup();
                        } else if (templet.equals(TempletType.TASK_PLAN.name())) {
                            TaskPlanCore taskPlanCore = new TaskPlanCore(document.get("_id").toString(), TaskType.REALTIME);
                            taskPlanCore.startup();
                        } else {
                            tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.ERROR.name())));
                        }

                    } else if (document.getString("type").equals(TaskType.CRONTAB.name())) {
                        System.out.println("Found a new CRONTAB TASK, Task Info:" + document.toString());
                        JsonParser parse = new JsonParser();  //创建json解析器
                        JsonObject json = (JsonObject) parse.parse(document.toJson());
                        Date date = ((Document) document.get("cron_core")).getDate("first_time");

                        Instant instant_ft = date.toInstant();
                        long nt = instant_ft.toEpochMilli() + 1000 * Integer.parseInt(json.get("cron_core").getAsJsonObject().get("cycle").getAsString());

                        if (Instant.now().isAfter(instant_ft)) {
                            System.out.println("Found a new CRONTAB TASK, Task Info:");
                            System.out.println(document.toString());
                            tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));
                            tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("cron_core.first_time", Date.from(Instant.ofEpochMilli(nt)))));

                            String templet = document.getString("templet");

                            if (templet.equals(TempletType.ORBIT_FORECAST.name())) {
                                OrbitCore orbitCore = new OrbitCore(document.get("_id").toString(), TaskType.CRONTAB);
                                orbitCore.startup();
                            } else if (templet.equals(TempletType.TASK_PLAN.name())) {
                                TaskPlanCore taskPlanCore = new TaskPlanCore(document.get("_id").toString(), TaskType.CRONTAB);
                                taskPlanCore.startup();
                            } else {
                                tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.ERROR.name())));
                            }
                        }
                    } else {
                        System.out.println("Found a new PERMANENT TASK, Task Info:");
                        System.out.println(document.toString());
                    }
                } else if (document.getString("status").equals(MainTaskStatus.SUSPEND.name())) {
                    if (document.getString("type").equals(TaskType.CRONTAB.name())) {

                        JsonParser parse = new JsonParser();  //创建json解析器
                        JsonObject json = (JsonObject) parse.parse(document.toJson());
                        Date date = ((Document) document.get("cron_core")).getDate("first_time");

                        Instant instant_ft = date.toInstant();
                        long nt = instant_ft.toEpochMilli() + 1000 * Integer.parseInt(json.get("cron_core").getAsJsonObject().get("cycle").getAsString());
                        if (Instant.now().isAfter(instant_ft)) {
                            System.out.println("Found a SUSPEND CRONTAB TASK, Task Info:" + document.toString());

                            tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));
                            tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("cron_core.first_time", Date.from(Instant.ofEpochMilli(nt)))));

                            String templet = document.getString("templet");

                            if (templet.equals(TempletType.ORBIT_FORECAST.name())) {
                                OrbitCore orbitCore = new OrbitCore(document.get("_id").toString(), TaskType.CRONTAB);
                                orbitCore.startup();
                            } else if (templet.equals(TempletType.TASK_PLAN.name())) {
                                TaskPlanCore taskPlanCore = new TaskPlanCore(document.get("_id").toString(), TaskType.CRONTAB);
                                taskPlanCore.startup();
                            } else {
                                tasks.updateOne(Filters.eq("_id", document.get("_id")), new Document("$set", new Document("status", MainTaskStatus.ERROR.name())));
                            }
                        }
                    }
                } else {
                }
            }
            mongoClient.close();
        }
    }
}