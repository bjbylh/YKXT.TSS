package common.mongo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.SubTaskStatus;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Date;
import java.time.Instant;

/**
 * Created by lihan on 2019/8/28.
 */
public class CommUtils {
    public static MainTaskStatus checkMainTaskStatus(String id) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> maintasks = mongoDatabase.getCollection("main_task");

        Document cond = new Document();
        cond.put("_id", new ObjectId(id));
        Document first = maintasks.find(cond).first();

        String status = first.getString("status");

        return MainTaskStatus.valueOf(status);
    }

    public static  String[] getSubList(String id) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        Document document = tasks.find(new Document("_id", new ObjectId(id))).first();

        JsonParser parse = new JsonParser();  //??json???
        JsonObject json = (JsonObject) parse.parse(document.toJson());

        JsonArray asJsonArray = json.getAsJsonObject("tp_info").getAsJsonArray("sub_tasks");

        String[] ret = new String[5];
        for (int i = 0; i < 5; i++) {
            ret[i] = asJsonArray.get(i).getAsJsonObject().get("sub_taskid").getAsString();
        }

        mongoClient.close();

        return ret;
    }

    public static  void updateSubStatus(String id, SubTaskStatus subTaskStatus) {
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
                                        .append("update_time", Date.from(Instant.now()))
                        )
                )
        );
        mongoClient.close();
    }

    public static void updateMainStatus(String id, MainTaskStatus mainTaskStatus) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

        tasks.updateOne(Filters.eq("_id", new ObjectId(id)), new Document("$set", new Document("status", mainTaskStatus.name())));

        mongoClient.close();
    }
}
