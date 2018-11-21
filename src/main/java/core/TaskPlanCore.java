package core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.SubTaskStatus;
import common.redis.RedisPublish;
import org.bson.Document;

import java.io.*;
import java.time.Instant;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    public static void main(String[] args) throws InterruptedException {
        String id = "5bf4ff600e35684dbc875716";//args[0];

        String[] subList = getSubList(id);

        for (String aSubList : subList) {
            updateStatus(aSubList, SubTaskStatus.TODO);
        }

        Thread.sleep(1000);

        RedisPublish.dbRefresh(id);

        Thread.sleep(1000);


        String subid = subList[0];
        String path = getPath(subid);
        String exename = getExename(subid);

        Run(id, subid, path, exename);

        subid = subList[1];
        path = getPath(subid);
        exename = getExename(subid);

        Run(id, subid, path, exename);

        subid = subList[2];
        path = getPath(subid);
        exename = getExename(subid);

        Run(id, subid, path, exename);

        updateMainStatus(id,MainTaskStatus.SUSPEND);
        RedisPublish.dbRefresh(id);
    }



    private static void Run(String id, String subid, String path, String exename) {
        Process p;
        try {
            ProcessBuilder builder = new ProcessBuilder(path + exename, "3", "1", "3", "4", "2", "2", "5", "3");
            builder.directory(new File(path));
            builder.redirectErrorStream(true);
            p = builder.start();

            updateStatus(subid, SubTaskStatus.RUNNING);
            RedisPublish.dbRefresh(id);

            InputStream fis = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            int i = p.exitValue();
            if (i == 0) {
                updateStatus(subid, SubTaskStatus.SUSPEND);
                RedisPublish.dbRefresh(id);
            } else {
                updateStatus(subid, SubTaskStatus.ERROR);
                RedisPublish.dbRefresh(id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPath(String subid) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");
        Document iddoc = subtasks.find(new Document("_id", subid)).first();
        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(iddoc.toJson());
        mongoClient.close();
        return json.getAsJsonObject("method").getAsJsonObject("param").get("path").getAsString();
    }

    private static String getExename(String subid) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");
        Document iddoc = subtasks.find(new Document("_id", subid)).first();
        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(iddoc.toJson());
        mongoClient.close();
        return json.get("exename").getAsString();
    }

    private static String[] getSubList(String id) {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        Document document = tasks.find(new Document("_id", id)).first();

        JsonParser parse = new JsonParser();  //??json???
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

        Document doc = new Document();
        doc.append("_id", id);
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
        mongoClient.close();
    }

    private static void updateMainStatus(String id, MainTaskStatus mainTaskStatus){
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("TSS");

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        tasks.updateOne(Filters.eq("_id", id), new Document("$set", new Document("status", mainTaskStatus.name())));
        mongoClient.close();
    }

}
