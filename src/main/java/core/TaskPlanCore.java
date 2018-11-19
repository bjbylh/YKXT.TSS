package core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    public static void main(String[] args) throws InterruptedException {
        String id = args[0];

        String[] subList = getSubList(id);

        for (int i = 0; i < subList.length; i++) {

        }

//        Thread.sleep(2000);
//
//        System.out.println("执行订单收集与统筹任务...");
//
//        Thread.sleep(5000);
//
//        System.out.println("执行多星任务规划任务...");
//
//        Thread.sleep(5000);
//
//        System.out.println("执行单星任务规划任务...");
//
//        Thread.sleep(2000);
//
//        System.out.println("本次任务执行完成...");
//
//        MongoClient mongoClient = new MongoClient("localhost", 27017);
//        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");
//
//        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");
//
//        Document document = tasks.find(new Document("_id", id)).first();
//
//        tasks.updateOne(Filters.eq("_id", document.get("_id").toString()), new Document("$set", new Document("status", "PENDING")));
//
//        mongoClient.close();
//
//        System.exit(0);//正常退出
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
}
