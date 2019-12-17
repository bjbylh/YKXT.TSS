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
import common.xmlutil.xmlParser;
import core.taskplan.VisibilityCalculation;
import org.bson.Document;
import org.bson.conversions.Bson;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisTaskSubscriber extends JedisPubSub {
    private Date startTime = Date.from(Instant.now().plusSeconds(24 * 60 * 60 * 10000));
    private Date endTime = Date.from(Instant.now().minusSeconds(24 * 60 * 60 * 10000));
    private Instant BASE_TIME = ZonedDateTime.of(1949, 12, 31, 0, 0, 0, 0, ZoneOffset.ofHours(8)).toInstant();

    public RedisTaskSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        try {
            JsonParser parse = new JsonParser();  //创建json解析器
            JsonObject msg = (JsonObject) parse.parse(message);

            String asString = msg.getAsJsonObject("Head").get("type").getAsString();
            String id = msg.getAsJsonObject("Head").get("id").getAsString();

            JsonObject json = msg.getAsJsonObject("Data");

            if (asString.equals(MsgType.NEW_TASK.name())) {

                procNewTask(json, id);

            } else if (asString.equals(MsgType.CHECK_QUERY.name())) {

                procCheckQuery(json, id);

            } else if (asString.equals(MsgType.ORBIT_DATA_IMPORT.name())) {

                procOrbitDataImport(json, id);

            } else if (asString.equals(MsgType.INS_CLEAR.name())) {

                procInsClear(json, id);

            } else if (asString.equals(MsgType.INS_GEN.name())) {

                procInsGen(json, id);

            } else if (asString.equals(MsgType.TRANSMISSION_EXPORT.name())) {

                procTransmissionExport(json, id);

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

    private void procInsClear(JsonObject json, String id) {
        RedisPublish.CommonReturn(id, true, "", MsgType.INS_CLEAR_FINISHED);
    }

    private void procInsGen(JsonObject json, String id) {
        RedisPublish.CommonReturn(id, true, "", MsgType.INS_GEN_FINISHED);
    }


    private void procTransmissionExport(JsonObject json, String id) {
        try {
            String ids = json.get("content").getAsString();
            String[] transmission_numbers_array = ids.split(",");

            ArrayList<String> transmission_numbers = new ArrayList<>();
            for (String s : transmission_numbers_array) {
                transmission_numbers.add(s);
            }

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            //卫星资源表
            FindIterable<Document> transmission_missions = mongoDatabase.getCollection("transmission_mission").find();

            for (Document transmission_mission : transmission_missions) {
                if (transmission_numbers.contains(transmission_mission.getString("mission_number"))) {

                }
            }
            //todo

            mongoClient.close();
            RedisPublish.CommonReturn(id, true, "", MsgType.ORBIT_DATA_IMPORT_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.ORBIT_DATA_IMPORT_FINISHED);
        }
    }

    private void procOrbitDataImport(JsonObject json, String id) {
        try {
            String xmlString = json.get("content").getAsString();
            HashMap<String, String> parser = xmlParser.parser(xmlString);

            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            //卫星资源表
            MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
            Document Satllitejson = Data_Satllitejson.find().first();
            Document newSateInfo = Document.parse(Satllitejson.toJson());

            ArrayList<Document> properties = (ArrayList<Document>) newSateInfo.get("properties");
            for (Document d : properties) {
                if (d.getString("group").equals("轨道参数")) {
                    if (d.getString("key").equals("update_time")) {
                        int jd = Integer.parseInt(parser.get("JD"));
                        long js = (long) (Double.parseDouble(parser.get("JS")) * 1000);
                        Instant fileTime = BASE_TIME.plusSeconds(jd * 86400L).plusMillis(js);
                        d.append("value", Date.from(fileTime));
                    } else if (d.getString("key").equals("a")) {
                        d.append("value", String.valueOf(Double.parseDouble(parser.get("A")) / 1000.0));
                    } else if (d.getString("key").equals("e")) {
                        d.append("value", parser.get("E"));
                    } else if (d.getString("key").equals("i")) {
                        d.append("value", parser.get("I"));
                    } else if (d.getString("key").equals("RAAN")) {
                        d.append("value", parser.get("O"));
                    } else if (d.getString("key").equals("perigee_angle")) {
                        d.append("value", parser.get("W"));
                    } else if (d.getString("key").equals("mean_anomaly")) {
                        d.append("value", parser.get("M"));
                    } else continue;
                }
            }
            Data_Satllitejson.updateOne(Filters.eq("_id", Satllitejson.getObjectId("_id")), new Document("$set", newSateInfo));

            mongoClient.close();

            RedisPublish.CommonReturn(id, true, "", MsgType.ORBIT_DATA_IMPORT_FINISHED);


        } catch (Exception e) {
            String message = e.getMessage();
            RedisPublish.CommonReturn(id, false, message, MsgType.ORBIT_DATA_IMPORT_FINISHED);
        }
    }

    public static void main(String[] args) {
        String xmlString = "<?xml version='1.0' encoding=\"gb2312\" ?>\n" +
                "<ORBIT>\n" +
                "  <J2000>\n" +
                "    <OSCU>\n" +
                "      <JD>25540</JD>\n" +
                "      <JS>32400.000000</JS>\n" +
                "      <A>7139979.794400</A>\n" +
                "      <E>0.0005256608</E>\n" +
                "      <I>98.6705509000</I>\n" +
                "      <O>45.7002402000</O>\n" +
                "      <W>265.5190993000</W>\n" +
                "      <M>182.8945976000</M>\n" +
                "    </OSCU>\n" +
                "  </J2000>\n" +
                "</ORBIT>";
        JsonObject json = new JsonObject();
        json.addProperty("content", xmlString);
        String id = "0";
        RedisTaskSubscriber n = new RedisTaskSubscriber();
        n.procOrbitDataImport(json, id);
    }

    private void procNewTask(JsonObject json, String id) {
        try {

            if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name())) {
                String imageorder = json.get("imageorder").getAsString();
                String stationmission = json.get("stationmission").getAsString();

                TaskInit.initRTTaskForTaskPlan(json.get("name").getAsString(), imageorder, stationmission, id);
            } else if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initCronTaskForOrbitForecast(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initRTTaskForOrbitForecast(json.get("name").getAsString());
            else {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void procCheckQuery(JsonObject json, String id) {
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
            Map<Integer, Map<String, Boolean>> integerMapMap = VisibilityCalculation.VisibilityCalculationEmergency(Satllitejson, D_orbitjson, count, GroundStationjson, Missionjson, StationMissionjson);
            RedisPublish.checkResult(id, integerMapMap);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        mongoClient.close();
    }
}
