package core.taskplan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.SubTaskStatus;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
import java.time.Instant;

/**
 * Created by lihan on 2018/10/24.
 */
public class TaskPlanCore {
    public static void main(String[] args) throws InterruptedException {
        String id = args[0];

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

        String subid1 = subList[1];
        String path1 = getPath(subid1);
        String exename1 = getExename(subid1);

        String subid2 = subList[2];
        String path2 = getPath(subid2);
        String exename2 = getExename(subid2);

        Run(id, subid, path, exename);

        MainTaskStatus mainTaskStatus = checkMainTaskStatus(id);
        if (mainTaskStatus.name().equals("DELETE"))
            return;
        else if (mainTaskStatus.name().equals("SUSPEND")) {
            updateStatus(subid1, SubTaskStatus.SUSPEND);
            updateStatus(subid2, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
        } else {
        }


        Run(id, subid1, path1, exename1);

        if (mainTaskStatus.name().equals("DELETE"))
            return;
        else if (mainTaskStatus.name().equals("SUSPEND")) {
            updateStatus(subid2, SubTaskStatus.SUSPEND);
            RedisPublish.dbRefresh(id);
        } else {
        }

        Run(id, subid2, path2, exename2);

        updateMainStatus(id, MainTaskStatus.SUSPEND);
        RedisPublish.dbRefresh(id);
    }


    private static MainTaskStatus checkMainTaskStatus(String id) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> maintasks = mongoDatabase.getCollection("main_task");

        Document cond = new Document();
        cond.put("_id", new ObjectId(id));
        Document first = maintasks.find(cond).first();

        String status = first.getString("status");

        return MainTaskStatus.valueOf(status);
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
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");
        Document iddoc = subtasks.find(new Document("_id", new ObjectId(subid))).first();
        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(iddoc.toJson());
        mongoClient.close();
        return json.getAsJsonObject("method").getAsJsonObject("param").get("path").getAsString();
    }

    private static String getExename(String subid) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");
        Document iddoc = subtasks.find(new Document("_id", new ObjectId(subid))).first();
        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(iddoc.toJson());
        mongoClient.close();
        return json.get("exename").getAsString();
    }

    private static String[] getSubList(String id) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        Document document = tasks.find(new Document("_id", new ObjectId(id))).first();

        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(document.toJson());

        JsonArray asJsonArray = json.getAsJsonObject("tp_info").getAsJsonArray("sub_tasks");

        String[] ret = new String[3];
        for (int i = 0; i < 3; i++) {
            ret[i] = asJsonArray.get(i).getAsJsonObject().get("sub_taskid").getAsString();
        }

        mongoClient.close();

        return ret;
    }

    private static void updateStatus(String id, SubTaskStatus subTaskStatus) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> subtasks = mongoDatabase.getCollection("sub_task");

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
        mongoClient.close();
    }

    private static void updateMainStatus(String id, MainTaskStatus mainTaskStatus) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        tasks.updateOne(Filters.eq("_id", new ObjectId(id)), new Document("$set", new Document("status", mainTaskStatus.name())));
        mongoClient.close();
    }

}
