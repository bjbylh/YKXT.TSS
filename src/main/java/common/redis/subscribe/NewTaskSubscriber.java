package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.MsgType;
import common.redis.RedisPublish;
import core.taskplan.VisibilityCalculation;
import org.bson.Document;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lihan on 2018/11/15.
 */
public class NewTaskSubscriber extends JedisPubSub {
    public NewTaskSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject msg = (JsonObject) parse.parse(message);

        String asString = msg.getAsJsonObject("Head").get("type").getAsString();

        JsonObject json = msg.getAsJsonObject("data");

        if (asString.equals(MsgType.NEW_TASK.name())) {

            try {

                if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                    TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

                else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name())) {
                    String content = json.get("content").getAsString();
                    TaskInit.initRTTaskForTaskPlan(json.get("name").getAsString(), content);
                } else if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                    TaskInit.initCronTaskForOrbitForecast(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

                else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                    TaskInit.initRTTaskForOrbitForecast(json.get("name").getAsString());
                else {
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (asString.equals(MsgType.CHECK_QUERY.name())) {
            ArrayList<String> mission_numbners = new ArrayList<>();

            String content = json.get("content").getAsString();
            String[] mns = content.split(",");

            if (mns.length > 0) {
                for(String mn : mns)
                    mission_numbners.add(mn);
            }

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            //卫星资源表
            MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
            Document Satllitejson = Data_Satllitejson.find().first();
            //轨道数据表
            MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");
            FindIterable<Document> D_orbitjson = Data_Orbitjson.find();
            long count = Data_Orbitjson.count();

            //地面站资源表
            MongoCollection<Document> Data_GroundStationjson = mongoDatabase.getCollection("groundstation_resource");
            FindIterable<Document> D_GroundStationjson = Data_GroundStationjson.find();
            ArrayList<Document>GroundStationjson = new ArrayList<>();
            for (Document document : D_GroundStationjson) {
                GroundStationjson.add(document);
            }
            MongoCollection<Document> Data_Missionjson = mongoDatabase.getCollection("image_mission");
            FindIterable<Document> D_Missionjson = Data_Missionjson.find();
            ArrayList<Document> Missionjson = new ArrayList<>();
            for (Document document : D_Missionjson) {
                if (mission_numbners.contains(document.getString("mission_number")))
                    Missionjson.add(document);
            }

            //todo
            try {
                Map<String, Boolean> stringBooleanMap = VisibilityCalculation.VisibilityCalculationEmergency(Satllitejson, D_orbitjson, count, GroundStationjson, Missionjson);
                RedisPublish.checkResult(stringBooleanMap);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } else return;
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("subscribe redis channel success, channel %s, subscribedChannels %d",
                channel, subscribedChannels));
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println(String.format("unsubscribe redis channel, channel %s, subscribedChannels %d",
                channel, subscribedChannels));

    }
}
