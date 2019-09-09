package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.def.TaskType;
import common.def.TempletType;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.MsgType;
import common.redis.RedisPublish;
import core.taskplan.VisibilityCalculation;
import org.bson.Document;
import org.bson.conversions.Bson;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Created by lihan on 2018/11/15.
 */
public class NewTaskSubscriber extends JedisPubSub {
    private Date startTime = Date.from(Instant.now().plusSeconds(24 * 60 * 60 * 10000));
    private Date endTime = Date.from(Instant.now().minusSeconds(24 * 60 * 60 * 10000));

    public NewTaskSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        try {
            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject msg = (JsonObject) parse.parse(message);

            String asString = msg.getAsJsonObject("Head").get("type").getAsString();

            JsonObject json = msg.getAsJsonObject("Data");

            if (asString.equals(MsgType.NEW_TASK.name())) {

                try {

                    if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                        TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

                    else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name())) {
                        String imageorder = json.get("imageorder").getAsString();
                        String stationmission = json.get("stationmission").getAsString();

                        TaskInit.initRTTaskForTaskPlan(json.get("name").getAsString(), imageorder, stationmission);
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
                ArrayList<String> order_numbners = new ArrayList<>();

                String ordermission = json.get("imageorder").getAsString();
                String[] mns = ordermission.split(",");

                //Map<String, Boolean> stringBooleanMap = new TreeMap<>();

                if (mns.length > 0) {
                    for (String mn : mns)
                        order_numbners.add(mn);
                    //stringBooleanMap.put("mn", true);
                }
                ArrayList<String> station_numbners = new ArrayList<>();

                String stationmission = json.get("stationmission").getAsString();
                mns = stationmission.split(",");

                //Map<String, Boolean> stringBooleanMap = new TreeMap<>();

                if (mns.length > 0) {
                    for (String mn : mns)
                        station_numbners.add(mn);
                    //stringBooleanMap.put("mn", true);
                }
                //RedisPublish.checkResult(stringBooleanMap);

                MongoClient mongoClient = MangoDBConnector.getClient();
                //获取名为"temp"的数据库
                MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

                //卫星资源表
                MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
                Document Satllitejson = Data_Satllitejson.find().first();


                //地面站资源表
                MongoCollection<Document> Data_GroundStationjson = mongoDatabase.getCollection("groundstation_resource");
                FindIterable<Document> D_GroundStationjson = Data_GroundStationjson.find();
                ArrayList<Document> GroundStationjson = new ArrayList<>();
                for (Document document : D_GroundStationjson) {
                    GroundStationjson.add(document);
                }


                MongoCollection<Document> Data_Orderjson = mongoDatabase.getCollection("image_order");
                FindIterable<Document> D_Orderjson = Data_Orderjson.find();
                ArrayList<Document> Missionjson = new ArrayList<>();
                for (Document document : D_Orderjson) {
                    if (order_numbners.contains(document.getString("order_number"))) {
                        Missionjson.add(document);
                        Date expected_start_time = document.getDate("expected_start_time");
                        if (expected_start_time.before(startTime))
                            startTime = expected_start_time;

                        Date expected_end_time = document.getDate("expected_end_time");
                        if (expected_end_time.after(endTime))
                            endTime = expected_end_time;
                    }
                }
                MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                FindIterable<Document> documents = station_mission.find();
                ArrayList<Document> StationMissionjson = new ArrayList<>();
                for (Document document : documents) {
                    if (station_numbners.contains(document.getString("mission_number"))) {
                        StationMissionjson.add(document);
                        Date expected_start_time = document.getDate("expected_start_time");
                        if (expected_start_time.before(startTime))
                            startTime = expected_start_time;

                        Date expected_end_time = document.getDate("expected_end_time");
                        if (expected_end_time.after(endTime))
                            endTime = expected_end_time;
                    }
                }
                //轨道数据表
                MongoCollection<Document> Data_Orbitjson = mongoDatabase.getCollection("orbit_attitude");

                Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

                FindIterable<Document> D_orbitjson = Data_Orbitjson.find(Filters.and(queryBson));

                long count = Data_Orbitjson.count(Filters.and(queryBson));

                try {
                    Map<String, Boolean> stringBooleanMap = VisibilityCalculation.VisibilityCalculationEmergency(Satllitejson, D_orbitjson, count, GroundStationjson, Missionjson, StationMissionjson);
                    RedisPublish.checkResult(stringBooleanMap);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                mongoClient.close();
            } else return;
        } catch (Exception e) {
            e.printStackTrace();
        }
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
