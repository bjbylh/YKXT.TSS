package core.orbit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.MainTaskStatus;
import common.def.TaskType;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by lihan on 2019/7/23.
 */
public class OrbitCore {
    private String taskId;
    private TaskType taskType;

    public OrbitCore(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
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
            ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");

            Instant start = Instant.now();

            double[] orbits = new double[6];
            for (Document document : properties) {

                if (document.getString("key").equals("update_time")) {
                    start = document.getDate("value").toInstant();
                } else if (document.getString("key").equals("a")) {
                    orbits[0] = Double.parseDouble(document.getString("value"));
                } else if (document.getString("key").equals("e")) {
                    orbits[1] = Double.parseDouble(document.getString("value"));
                } else if (document.getString("key").equals("i")) {
                    orbits[2] = Double.parseDouble(document.getString("value"));
                } else if (document.getString("key").equals("RAAN")) {
                    orbits[3] = Double.parseDouble(document.getString("value"));
                } else if (document.getString("key").equals("perigee_angle")) {
                    orbits[4] = Double.parseDouble(document.getString("value"));
                } else if (document.getString("key").equals("true_anomaly")) {
                    orbits[5] = Double.parseDouble(document.getString("value"));
                } else {
                }
            }

            Instant end = start.plusMillis(1000 * 60 * 60 * 24 * 7L);//7 days

            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject json = (JsonObject) parse.parse(first.toJson());

            MongoCollection<Document> orbit_attitude = mongoDatabase.getCollection("orbit_attitude");
            MongoCollection<Document> orbit_attitude_sample = mongoDatabase.getCollection("orbit_attitude_sample");

            Bson queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));
            orbit_attitude.deleteMany(queryBson);
            orbit_attitude_sample.deleteMany(queryBson);

            OrbitPrediction.OrbitPredictorII(start, OrbitPrediction.dateConvertToLocalDateTime(Date.from(start)), OrbitPrediction.dateConvertToLocalDateTime(Date.from(end)), 1, orbits, json);


            MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");

            queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));

            FindIterable<Document> D_orbitjson = Data_Orbitjson.find(Filters.and(queryBson));
            long count = Data_Orbitjson.count(Filters.and(queryBson));

            AvoidSunshine.AvoidSunshineII(D_orbitjson, count, start);


            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");
            Instant instant_ft = Instant.now();
            Date nt_str = Date.from(instant_ft);
            tasks.updateOne(Filters.eq("_id", new ObjectId(taskId)), new Document("$set", new Document("rt_core.end_time", nt_str)));
            if (TaskType.REALTIME == taskType)
                CommUtils.updateMainStatus(taskId, MainTaskStatus.FINISHED);
            else
                CommUtils.updateMainStatus(taskId, MainTaskStatus.SUSPEND);
            mongoClient.close();
            RedisPublish.dbRefresh(taskId);
        }
    }

    public static void main(String[] args) {
//        OrbitCore orbitCore = new OrbitCore("5d4bd356de590f3744f9c708");
//        orbitCore.startup();
//
//        JsonArray jsonArray = new JsonArray();
//
//        for (JsonElement jsonElement : jsonArray) {
//            Document document = new Document();
//            document = Document.parse(jsonElement.getAsJsonObject().toString());
//            document.getDate("time_point");
//        }
    }
}
