package srv.task;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.ConfigManager;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskInit {
    private static String[] subtaskname = new String[]{
            "订单统筹",
            "可见性分析",
            "时间分配",
            "姿态角计算",
            "任务复核复算"
    };

    public static void initRTTaskForOrbitForecast(String taskname) throws IOException {

        String path = ConfigManager.getInstance().fetchJsonPath();
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

        String path = ConfigManager.getInstance().fetchJsonPath();
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

        doc.append("cron_core", cron_core);

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(doc);
        mongoClient.close();

        System.out.println(doc);

    }

    public static void initCronTaskForTaskPlan(String taskname, String first, String cycle, String count) throws IOException {

        String path = ConfigManager.getInstance().fetchJsonPath();

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


        List<String> list = initTaskPlanSubTask(new ArrayList<>(), new ArrayList<>(), "");
        for (int i = 0; i < subtaskname.length; i++) {
            json.get("tp_info").getAsJsonObject().get("sub_tasks").getAsJsonArray().get(i).getAsJsonObject().addProperty("sub_taskid", list.get(i));
        }

        json.remove("_id");

        Document doc = Document.parse(json.toString());

        Document cron_core = new Document();
        cron_core.append("first_time", Date.from(Instant.parse(first)));
        cron_core.append("cycle", cycle);
        cron_core.append("count", count);

        doc.append("cron_core", cron_core);


        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(doc);
        mongoClient.close();

        System.out.println(doc);

    }

    private static List<String> initTaskPlanSubTask(ArrayList<String> params, ArrayList<String> params2, String param3) throws IOException {
        List<String> ids = Lists.newLinkedList();

        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> sub_task = mongoDatabase.getCollection("sub_task");

        for (int i = 0; i < subtaskname.length; i++) {
            Document parse1 = new Document();
            ObjectId pId = new ObjectId();
            ids.add(pId.toString());
            parse1.append("name", subtaskname[i]);

            if (i == 0) {
                parse1.append("param", params);
                parse1.append("param2", params2);
                parse1.append("param3", param3);
            } else {
                parse1.append("param", new ArrayList<String>());
                parse1.append("param2", new ArrayList<String>());
                parse1.append("param3", "");
            }
            ArrayList<Document> historys = new ArrayList<>();

            Document history = new Document();
            history.append("status", "NEW");
            history.append("message", "");
            history.append("update_time", Date.from(Instant.now()));
            historys.add(history);

            parse1.append("history", historys);

            parse1.put("_id", pId);
            sub_task.insertOne(parse1);
        }
        mongoClient.close();
        return ids;
    }

    public static void main(String[] args) throws IOException {
        initCronTaskForTaskPlan("task_" + Instant.now().toEpochMilli(), Instant.now().toString(), "60000", "0");
    }

    public static void initRTTaskForTaskPlan(String taskname, String content, String content2, String content3) throws IOException {
        String[] order_numbers = content.split(",");
        String[] station_numbers = content2.split(",");

        String path = ConfigManager.getInstance().fetchJsonPath();

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
            ArrayList<String> params = new ArrayList<>();
            ArrayList<String> params2 = new ArrayList<>();
            List<String> list;
            if (order_numbers.length > 0) {
                for (String order_number : order_numbers)
                    params.add(order_number);
            }

            if (station_numbers.length > 0) {
                for (String station_number : station_numbers)
                    params2.add(station_number);
            }

            list = initTaskPlanSubTask(params, params2, content3);
            for (int i = 0; i < subtaskname.length; i++) {
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