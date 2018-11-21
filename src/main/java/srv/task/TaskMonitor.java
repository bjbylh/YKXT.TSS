package srv.task;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.TaskType;
import org.bson.Document;

import java.io.IOException;
import java.time.Instant;

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

    private void createProc(String id) throws IOException {
//        String[] args = new String[]{"C:\\Users\\lihan\\Desktop\\ykxt\\bin\\TSS-CORE\\startup.bat", id};
        Runtime.getRuntime().exec("java -jar C:\\Users\\lihan\\Desktop\\ykxt\\bin\\TSS-CORE\\core-1.0-SNAPSHOT.jar " + id);
    }

    class DoWork extends Thread {
        public void run() {
            while (true) {
                try {
                    check();
                    Thread.sleep(1000 * 60);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void check() throws IOException {
            MongoClient mongoClient = new MongoClient("localhost", 27017);
            MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

            FindIterable<Document> main_task = tasks.find();

            for (Document document : main_task) {
                if (document.getString("status").equals(MainTaskStatus.NEW.name())) {
                    if (document.getString("type").equals(TaskType.REALTIME.name())) {

                        System.out.println("Found a new REALTIME TASK, Task Info:");
                        System.out.println(document.toString());
                        tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));

                    } else if (document.getString("type").equals(TaskType.CRONTAB.name())) {

                        JsonParser parse = new JsonParser();  //创建json解析器
                        JsonObject json = (JsonObject) parse.parse(document.toJson());

                        String ft = json.get("cron_core").getAsJsonObject().get("first_time").getAsString();
                        Instant instant_ft = Instant.parse(ft);
                        long nt = instant_ft.toEpochMilli() + 1000 * Integer.parseInt(json.get("cron_core").getAsJsonObject().get("cycle").getAsString());
                        String nt_str = Instant.ofEpochMilli(nt).toString();
                        if (Instant.now().isAfter(instant_ft)) {
                            System.out.println("Found a new CRONTAB TASK, Task Info:");
                            System.out.println(document.toString());
                            tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));
                            tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("cron_core.first_time", nt_str)));


                            Run.Exec(document.getString("_id"));
                        }
                    } else {
                        System.out.println("Found a new PERMANENT TASK, Task Info:");
                        System.out.println(document.toString());
                    }
                } else if (document.getString("status").equals(MainTaskStatus.SUSPEND.name())) {
                    if (document.getString("type").equals(TaskType.CRONTAB.name())) {
                        JsonParser parse = new JsonParser();  //创建json解析器
                        JsonObject json = (JsonObject) parse.parse(document.toJson());

                        String ft = json.get("cron_core").getAsJsonObject().get("first_time").getAsString();
                        Instant instant_ft = Instant.parse(ft);
                        long nt = instant_ft.toEpochMilli() + 1000 * Integer.parseInt(json.get("cron_core").getAsJsonObject().get("cycle").getAsString());
                        String nt_str = Instant.ofEpochMilli(nt).toString();
                        if (Instant.now().isAfter(instant_ft)) {
                            System.out.println("Found a SUSPEND CRONTAB TASK, Task Info:");
                            System.out.println(document.toString());
                            tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("status", MainTaskStatus.RUNNING.name())));
                            tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("cron_core.first_time", nt_str)));

                            createProc(document.getString("_id"));
                        }
                    }
                } else {
                }
            }
            mongoClient.close();
        }
    }

}
