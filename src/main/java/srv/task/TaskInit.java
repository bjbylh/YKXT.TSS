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
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
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
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\ReqAgg.exe",
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\ReqAlloc.exe",
            "C:\\Users\\lihan\\Desktop\\ykxt\\bin\\MisPlan.exe"
    };

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

        if (TempletType.TASK_PLAN.name().equals("TASK_PLAN")) {
            List<String> list = initTaskPlanSubTask();
            for (int i = 0; i < 3; i++) {
                json.get("tp_core").getAsJsonObject().get("sub_tasks").getAsJsonArray().get(i).getAsJsonObject().addProperty("sub_taskid", list.get(i));
            }
        }
        json.get("cron_core").getAsJsonObject().addProperty("first_time", first);
        json.get("cron_core").getAsJsonObject().addProperty("cycle", cycle);
        json.get("cron_core").getAsJsonObject().addProperty("count", count);
        json.addProperty("_id", new ObjectId().toString());

        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> main_task = mongoDatabase.getCollection("main_task");

        main_task.insertOne(Document.parse(json.toString()));
        mongoClient.close();

        System.out.println(json);

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

        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> sub_task = mongoDatabase.getCollection("sub_task");

        for (int i = 0; i < 3; i++) {
            JsonObject p = json.deepCopy();
            String pId = new ObjectId().toString();
            ids.add(pId);
            p.addProperty("_id", pId);
            p.addProperty("name",subtaskname[i]);
            p.getAsJsonObject("method").getAsJsonObject("param").addProperty("path",paths[i]);
            JsonArray history = p.get("history").getAsJsonArray();
            history.get(0).getAsJsonObject().addProperty("update_time", Instant.now().toString());
            sub_task.insertOne(Document.parse(p.toString()));
        }
        mongoClient.close();
        return ids;
    }

    public static void main(String[] args) throws IOException {
        //initCronTaskForTaskPlan("task_" + Instant.now().toEpochMilli(), Instant.now(), 60 * 1000, 0);
    }
}