package srv.task;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
import java.sql.Date;
import java.time.Instant;
import java.util.List;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskInit {
    private static String[] subtaskname = new String[]{
            "订单统筹与规划",
            "多星任务规划",
            "单星任务规划"
    };
    private static String[] paths = new String[]{
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\",
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\",
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\"
    };

    private static String[] exenames = new String[]{
            "ReqAgg.exe",
            "ReqAlloc.exe",
            "MisPlan.exe"
    };

    public static void initRTTaskForOrbitForecast(String taskname) throws IOException {

        String path = TaskInit.class.getClassLoader().getResource("maintask.json").getPath();
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        json.addProperty("name", taskname);
        json.addProperty("type", TaskType.REALTIME.name());
        json.addProperty("templet", TempletType.ORBIT_FORECAST.name());


        json.remove("_id");

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(Document.parse(json.toString()));
        mongoClient.close();

        System.out.println(json);

    }

    public static void initCronTaskForOrbitForecast(String taskname, String first, String cycle, String count) throws IOException {

        String path = TaskInit.class.getClassLoader().getResource("maintask.json").getPath();
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        json.addProperty("name", taskname);
        json.addProperty("type", TaskType.CRONTAB.name());
        json.addProperty("templet", TempletType.ORBIT_FORECAST.name());


        json.remove("_id");

        Document doc = Document.parse(json.toString());

        Document cron_core = new Document();
        cron_core.append("first_time", Date.from(Instant.parse(first)));
        cron_core.append("cycle", cycle);
        cron_core.append("count", count);

        doc.append("cron_core",cron_core);

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(doc);
        mongoClient.close();

        System.out.println(doc);

    }

    public static void initCronTaskForTaskPlan(String taskname, String first, String cycle, String count) throws IOException {

        String path = TaskInit.class.getClassLoader().getResource("maintask.json").getPath();
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        json.addProperty("name", taskname);
        json.addProperty("type", TaskType.CRONTAB.name());
        json.addProperty("templet", TempletType.TASK_PLAN.name());

        List<String> list = initTaskPlanSubTask();
        for (int i = 0; i < 3; i++) {
            json.get("tp_info").getAsJsonObject().get("sub_tasks").getAsJsonArray().get(i).getAsJsonObject().addProperty("sub_taskid", list.get(i));
        }

        json.remove("_id");

        Document doc = Document.parse(json.toString());

        Document cron_core = new Document();
        cron_core.append("first_time", Date.from(Instant.parse(first)));
        cron_core.append("cycle", cycle);
        cron_core.append("count", count);

        doc.append("cron_core",cron_core);


        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(doc);
        mongoClient.close();

        System.out.println(doc);

    }

    private static List<String> initTaskPlanSubTask() throws IOException {
        List<String> ids = Lists.newLinkedList();
        String path = TaskInit.class.getClassLoader().getResource("subtask.json").getPath();
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sub_task = mongoDatabase.getCollection("sub_task");

        for (int i = 0; i < 3; i++) {
            JsonObject p = json.deepCopy();
            ObjectId pId = new ObjectId();
            ids.add(pId.toString());
            p.addProperty("name", subtaskname[i]);
            p.addProperty("exename", exenames[i]);
            p.getAsJsonObject("method").getAsJsonObject("param").addProperty("path", paths[i]);
            JsonArray history = p.get("history").getAsJsonArray();
            history.get(0).getAsJsonObject().addProperty("update_time", Instant.now().toString());
            Document parse1 = Document.parse(p.toString());
            parse1.put("_id", pId);
            sub_task.insertOne(parse1);
        }
        mongoClient.close();
        return ids;
    }

    public static void main(String[] args) throws IOException {
        //initCronTaskForTaskPlan("task_" + Instant.now().toEpochMilli(), Instant.now(), 60 * 1000, 0);
    }

    public static void initRTTaskForTaskPlan(String taskname) throws IOException {

        String path = TaskInit.class.getClassLoader().getResource("maintask.json").getPath();
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        json.addProperty("name", taskname);
        json.addProperty("type", TaskType.REALTIME.name());
        json.addProperty("templet", TempletType.TASK_PLAN.name());

        if (TempletType.TASK_PLAN.name().equals("TASK_PLAN")) {
            List<String> list = initTaskPlanSubTask();
            for (int i = 0; i < 3; i++) {
                json.get("tp_info").getAsJsonObject().get("sub_tasks").getAsJsonArray().get(i).getAsJsonObject().addProperty("sub_taskid", list.get(i));
            }
        }

        json.remove("_id");

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(Document.parse(json.toString()));
        mongoClient.close();

        System.out.println(json);

    }
}