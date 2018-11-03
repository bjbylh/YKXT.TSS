import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskInit {
    public static void initCronTask(String taskname, TempletType templetType, Instant first, int cycle, int count) throws IOException {
        InputStream in = TaskInit.class.getResourceAsStream("maintask.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String str;
        String data = "";
        while ((str = br.readLine()) != null) {
            data = data + str + "\n";
        }

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(data);

        json.addProperty("name", taskname);
        json.addProperty("type", TaskType.CRONTAB.name());
        json.addProperty("templet", templetType.name());

        if (templetType.name().equals("TASK_PLAN")) {
            List<String> list = initTaskPlanSubTask();
            for (int i = 0; i < 2; i++) {
                json.get("tp_core").getAsJsonObject().get("sub_tasks").getAsJsonArray().get(i).getAsJsonObject().addProperty("sub_taskid", list.get(i));
            }
        }
        json.get("cron_core").getAsJsonObject().addProperty("first_time", first.toString());
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
        InputStream in = TaskInit.class.getResourceAsStream("subtask.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
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

        for (int i = 0; i < 2; i++) {
            JsonObject p = json.deepCopy();
            String pId = new ObjectId().toString();
            ids.add(pId);
            p.addProperty("_id", pId);
            JsonArray history = p.get("history").getAsJsonArray();
            history.get(0).getAsJsonObject().addProperty("update_time", Instant.now().toString());
            sub_task.insertOne(Document.parse(p.toString()));
            System.out.println(p);
        }
        mongoClient.close();
        return ids;
    }

    public static void main(String[] args) throws IOException {
        initCronTask("task_" + Instant.now().toEpochMilli(), TempletType.TASK_PLAN, Instant.now(), 60 * 1000, 0);
    }
}