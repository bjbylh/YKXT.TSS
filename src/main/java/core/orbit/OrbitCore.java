package core.orbit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;

/**
 * Created by lihan on 2019/7/23.
 */
public class OrbitCore {
    private String taskId;

    public OrbitCore(String taskId) {
        this.taskId = taskId;
    }

    public void startup() {
        Thread t = new Thread(new OrbitCore.proc(taskId));
        t.start();
    }

    private class proc implements Runnable {
        private String taskId;

        public proc(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");
            Document first = sate_res.find().first();
            Document[] properties = (Document[]) first.get("properties");

            Instant start = Instant.now();

            double[] orbits = new double[6];
            for (Document document : properties) {
                if (document.getString("group").equals("轨道参数") && document.getString("key").equals("update_time")) {
                    start = document.getDate("value").toInstant();
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("a")) {
                    orbits[0] = document.getDouble("value");
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("e")) {
                    orbits[1] = document.getDouble("value");
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("i")) {
                    orbits[2] = document.getDouble("value");
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("RAAN")) {
                    orbits[3] = document.getDouble("value");
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("perigee_angle")) {
                    orbits[4] = document.getDouble("value");
                } else if (document.getString("group").equals("轨道参数") && document.getString("key").equals("true_anomaly")) {
                    orbits[5] = document.getDouble("value");
                } else {
                }
            }

            Instant end = start.plusMillis(1000 * 60 * 60 * 24 * 2L);//2 days

            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject json = (JsonObject) parse.parse(first.toJson());

            JsonArray jsonElements = OrbitPrediction.OrbitPredictorII(start, end, 1, orbits, json);

            for(JsonElement jsonElement : jsonElements){


            }

            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");
            Instant instant_ft = Instant.now();
            Date nt_str = Date.from(instant_ft);
            tasks.updateOne(Filters.eq("_id", new ObjectId(taskId)), new Document("$set", new Document("rt_core.end_time", nt_str)));
            tasks.updateOne(Filters.eq("_id", new ObjectId(taskId)), new Document("$set", new Document("status", MainTaskStatus.FINISHED.name())));
            mongoClient.close();
        }
    }

}
