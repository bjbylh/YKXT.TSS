package core.orbit;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.ConfigManager;
import common.def.MainTaskStatus;
import common.def.TaskType;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.process.Run;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by lihan on 2019/7/23.
 */
public class OrbitCore502 {
    private String taskId;
    private TaskType taskType;

    public OrbitCore502(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
    }

    public void startup() {
        Thread t = new Thread(new OrbitCore502.proc(taskId));
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
            //启动入库模块EXE
            Process saveToDataBaseProcess = Run.callExtExeWithoutWait("C:\\yk\\TSS\\502\\20210130\\Debug\\123.exe", null);
            //从卫星资源表加载轨道六根数信息
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
            //删除数据库中外推时间段内已有的旧轨道数据
            Instant end = start.plusMillis(1000 * 60 * 60 * 24 * 2L);//7 days
            MongoCollection<Document> orbit_attitude = mongoDatabase.getCollection("orbit_attitude");
            MongoCollection<Document> orbit_attitude_sample = mongoDatabase.getCollection("orbit_attitude_sample");
            Bson queryBson = Filters.and(Filters.gte("time_point", Date.from(start)), Filters.lte("time_point", Date.from(end)));
            orbit_attitude.deleteMany(queryBson);
            orbit_attitude_sample.deleteMany(queryBson);

            //组装轨道外推exe模块输入字符串
            Document inputStringOrbit = new Document();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            inputStringOrbit
                    .append("name", "orbit")
                    .append("time", sdf.format(Date.from(start)))
                    .append("orbit_a", orbits[0])
                    .append("orbit_e", orbits[1])
                    .append("orbit_i", orbits[2])
                    .append("orbit_Omg", orbits[3])
                    .append("orbit_w", orbits[4])
                    .append("orbit_M", orbits[5])
                    .append("step", 1)
                    .append("period", 3600);
            //调用502轨道外推算法模块
            Run.callExtExeAndWait("C:\\yk\\TSS\\502\\20210130\\Calculator\\地面运控系统控制计算软件.exe", inputStringOrbit.toJson() + "#");
//            inputStringOrbit = new Document();
//            inputStringOrbit
//                    .append("name", "orbit")
//                    .append("time", sdf.format(Date.from(start)))
//                    .append("orbit_a", orbits[0])
//                    .append("orbit_e", orbits[1])
//                    .append("orbit_i", orbits[2])
//                    .append("orbit_Omg", orbits[3])
//                    .append("orbit_w", orbits[4])
//                    .append("orbit_M", orbits[5])
//                    .append("step", 60)
//                    .append("period", 3600);
//            Run.callExtExeAndWait("C:\\yk\\TSS\\502\\20210130\\Calculator\\地面运控系统控制计算软件.exe", inputStringOrbit.toJson() + "#");
            //调用502阳光规避算法模块
            Document inputStringSunAvoid = new Document();
            inputStringSunAvoid
                    .append("name", "sun_avoid")
                    .append("time", sdf.format(Date.from(start)))
                    .append("orbit_a", orbits[0])
                    .append("orbit_e", orbits[1])
                    .append("orbit_i", orbits[2])
                    .append("orbit_Omg", orbits[3])
                    .append("orbit_w", orbits[4])
                    .append("orbit_M", orbits[5])
                    .append("step", 10)
                    .append("period", 36000)
                    .append("ref_type", "orbit")
                    .append("avoid_type", "transfer");
            Run.callExtExeAndWait("C:\\yk\\TSS\\502\\20210130\\Calculator\\地面运控系统控制计算软件.exe", inputStringSunAvoid.toJson() + "#");

            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");
            Instant instant_ft = Instant.now();
            Date nt_str = Date.from(instant_ft);
            tasks.updateOne(Filters.eq("_id", new ObjectId(taskId)), new Document("$set", new Document("rt_core.end_time", nt_str)));
            if (TaskType.REALTIME == taskType) {
                CommUtils.updateMainStatus(taskId, MainTaskStatus.FINISHED);
            } else {
                CommUtils.updateMainStatus(taskId, MainTaskStatus.SUSPEND);
            }
            mongoClient.close();
            RedisPublish.dbRefresh(taskId);
            saveToDataBaseProcess.destroy();
        }
    }

    public static void main(String[] args) {
        OrbitCore502 orbitCore = new OrbitCore502("5d4bd356de590f3744f9c708", TaskType.REALTIME);
        orbitCore.startup();
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
