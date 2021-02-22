package common.redis.subscribe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.redis.RedisPublish;
import core.taskplan.GuidePlanning;
import org.bson.Document;
import org.bson.conversions.Bson;
import redis.clients.jedis.JedisPubSub;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * @author lihan
 * @date 2021/1/18
 */
public class GuidanceTaskSubscriber extends JedisPubSub {


    private DateTimeFormatter sf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public GuidanceTaskSubscriber() {

    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));
        MongoClient mongoClient = null;

        try {
            //创建json解析器
            mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

            JsonParser parse = new JsonParser();
            JsonObject msg = (JsonObject) parse.parse(message);

            String reqid = msg.get("reqid").getAsString();
            String t = msg.get("t").getAsString();
            String x = msg.get("x").getAsString();
            String y = msg.get("y").getAsString();
            String z = msg.get("z").getAsString();
            String vx = msg.get("vx").getAsString();
            String vy = msg.get("vy").getAsString();
            String vz = msg.get("vz").getAsString();

            ArrayList<Double> inputDatas = new ArrayList<>();
            inputDatas.add(Double.parseDouble(x));
            inputDatas.add(Double.parseDouble(y));
            inputDatas.add(Double.parseDouble(z));
            inputDatas.add(Double.parseDouble(vx));
            inputDatas.add(Double.parseDouble(vy));
            inputDatas.add(Double.parseDouble(vz));

            MongoCollection<Document> orbit_attitude = mongoDatabase.getCollection("orbit_attitude");

            LocalDateTime start_r = LocalDateTime.parse(t, sf);
            Date startTime = Date.from(start_r.toInstant(ZoneOffset.of("+8")));

            Date endTime = Date.from(startTime.toInstant().plusSeconds(5 * 60));


            Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

            FindIterable<Document> orbits = orbit_attitude.find(Filters.and(queryBson));

            GuidePlanning guidePlanning = new GuidePlanning();

            Map<Integer, String> integerStringMap = guidePlanning.GuidePlanningII(orbits, 0, inputDatas);

            String visible;
            String maneuvering;
            JsonArray inslist = new JsonArray();

            if (integerStringMap.size() == 1 && integerStringMap.containsKey(0)) {
                visible = "false";
                maneuvering = "false";

            } else if (integerStringMap.size() == 1 && integerStringMap.containsKey(1)) {
                visible = "true";
                maneuvering = "false";

            } else if (integerStringMap.size() == 1 && integerStringMap.containsKey(2)) {
                visible = "true";
                maneuvering = "true";

                JsonObject ins = new JsonObject();
                ins.addProperty("ins", integerStringMap.get(2));
                ins.addProperty("additional", "");

                inslist.add(ins);
            } else {
                visible = "false";
                maneuvering = "false";
            }

            JsonObject ret = new JsonObject();
            ret.addProperty("reqid", reqid);
            ret.addProperty("visible", visible);
            ret.addProperty("maneuvering", maneuvering);
            ret.add("inslist", inslist);

            RedisPublish.guidanceTaskReturn(ret.toString());

            mongoClient.close();
        } catch (Exception e) {
            e.printStackTrace();
            if(mongoClient != null){
                mongoClient.close();
            }
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

    public static void main(String[] args) {
        //J2000下的xyz值，可修改
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        //开始时间，默认为当前时，可修改
        Date startTime = Date.from(Instant.now());
        //结束时间，自开始时间向后延迟5分钟
        Date endTime = Date.from(startTime.toInstant().plusSeconds(5 * 60));

        ArrayList<Double> inputDatas = new ArrayList<>();
        inputDatas.add(x);
        inputDatas.add(y);
        inputDatas.add(z);

        MongoClient mongoClient = MangoDBConnector.getClient();

        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> orbit_attitude = mongoDatabase.getCollection("orbit_attitude");

        Bson queryBson = Filters.and(Filters.gte("time_point", startTime), Filters.lte("time_point", endTime));

        FindIterable<Document> orbits = orbit_attitude.find(Filters.and(queryBson));

        GuidePlanning guidePlanning = new GuidePlanning();

        Map<Integer, String> integerStringMap = guidePlanning.GuidePlanningII(orbits, 0, inputDatas);

        System.out.println(integerStringMap.toString());
    }
}
