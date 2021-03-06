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
import core.taskplan.MeanToTrueAnomaly;
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
//        try {
//            Thread.sleep(1000 * 3600 * 12);// 运行一断时间后中断线程
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        t.interrupt();
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
                } else if (document.getString("key").equals("mean_anomaly")) {
                    orbits[5] = Double.parseDouble(document.getString("value"));
                } else {
                }
            }

            orbits[5] = MeanToTrueAnomaly.MeanToTrueAnomalyII(orbits[0],orbits[1],orbits[2],orbits[3],orbits[4],orbits[5]);

            Instant end = start.plusMillis(1000 * 60 * 60 * 24 * 1L);//7 days

            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject json = (JsonObject) parse.parse(first.toJson());

            MongoCollection<Document> orbit_attitude = mongoDatabase.getCollection("orbit_attitude");
            MongoCollection<Document> orbit_attitude_sample = mongoDatabase.getCollection("orbit_attitude_sample");

            Bson queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));
            orbit_attitude.deleteMany(queryBson);
            orbit_attitude_sample.deleteMany(queryBson);
//start
            OrbitPrediction.OrbitPredictorII(start, OrbitPrediction.dateConvertToLocalDateTime(Date.from(start)), OrbitPrediction.dateConvertToLocalDateTime(Date.from(end)), 1, orbits, json);


            MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");

            queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));

            FindIterable<Document> D_orbitjson = Data_Orbitjson.find(Filters.and(queryBson));
            long count = Data_Orbitjson.count(Filters.and(queryBson));

            AvoidSunshine.AvoidSunshineII(D_orbitjson, count, start);

//end
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
        String s = "0303";
        Integer integer = Integer.valueOf(s.trim(), 16);
        System.out.println(integer);

        String s2 = "1800";//0001100000000000
        Integer integer2 = Integer.valueOf(s2.trim(), 16);
        System.out.println(integer2 & 0xFC00);

        int i = integer + integer2;

        System.out.println(Integer.toHexString(i));
//        Float f=0.008f;
//        System.out.println(Integer.toHexString(Float.floatToIntBits(f)));
    }
}
