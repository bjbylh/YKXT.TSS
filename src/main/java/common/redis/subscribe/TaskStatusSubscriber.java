package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.mongo.MangoDBConnector;
import common.redis.MsgType;
import common.redis.RedisPublish;
import org.bson.Document;
import org.bson.types.ObjectId;
import redis.clients.jedis.JedisPubSub;

/**
 * Created by lihan on 2018/11/30.
 */
public class TaskStatusSubscriber extends JedisPubSub {
    public TaskStatusSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {       //收到消息??用
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));
        JsonParser parse = new JsonParser();  //?建json解析器
        JsonObject msg = (JsonObject) parse.parse(message);

        String asString = msg.getAsJsonObject("Head").get("type").getAsString();

        if(!asString.equals(MsgType.TASK_STATUS_CHANGE.name()))
            return;

        JsonObject json = msg.getAsJsonObject("Data");
        String taskID = json.get("taskID").getAsString();
        String status = json.get("status").getAsString();

        if(status.equals("SUSPEND") || status.equals("DELETE")){
            MongoClient mongoClient = MangoDBConnector.getClient();
            MongoDatabase mongoDatabase = mongoClient.getDatabase("OCS");

            MongoCollection<Document> tasks = mongoDatabase.getCollection("main_task");

            tasks.updateOne(Filters.eq("_id", new ObjectId(taskID)), new Document("$set", new Document("status", status)));

            RedisPublish.dbRefresh(taskID);
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
