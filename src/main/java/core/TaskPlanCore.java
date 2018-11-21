package core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.SubTaskStatus;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    public static void main(String[] args) throws InterruptedException {
        String id = args[0];

        String[] subList = getSubList(id);

        for (int i = 0; i < subList.length; i++) {
            updateStatus(subList[i], SubTaskStatus.TODO);
        }

        Thread.sleep(1000);

        RedisPublish.dbRefresh(id);

        Thread.sleep(1000);

        for (int i = 0; i < subList.length; i++) {

        }

    }

    private static String[] getSubList(String id) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        Document document = tasks.find(new Document("_id", id)).first();

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject json = (JsonObject) parse.parse(document.toJson());

        JsonArray asJsonArray = json.getAsJsonObject("tp_core").getAsJsonArray("sub_tasks");

        String[] ret = new String[3];
        for (int i = 0; i < 3; i++) {
            ret[i] = asJsonArray.get(i).getAsJsonObject().get("sub_taskid").getAsString();
        }

        mongoClient.close();

        return ret;
    }

    private static void updateStatus(String id, SubTaskStatus subTaskStatus) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");

        subtasks.find(Filters.eq("_id", id)).first();
        Document doc = new Document();
        doc.append("_id", new ObjectId(id));
        subtasks.updateOne(doc,
                new Document("$push",
                        new Document("history",
                                new Document()
                                        .append("status", subTaskStatus.name())
                                        .append("message", "")
                                        .append("update_time", Instant.now().toString())
                        )
                )
        );
    }
}
