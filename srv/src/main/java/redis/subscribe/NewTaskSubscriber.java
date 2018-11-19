package redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import redis.clients.jedis.JedisPubSub;
import task.TaskInit;

import java.io.IOException;

/**
 * Created by lihan on 2018/11/15.
 */
public class NewTaskSubscriber extends JedisPubSub {
    public NewTaskSubscriber(){}
    @Override
    public void onMessage(String channel, String message) {       //收到消息会调用
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        JsonParser parse = new JsonParser();  //创建json解析器
        JsonObject msg = (JsonObject) parse.parse(message);
        System.out.println(msg.toString());
        JsonObject json = msg.getAsJsonObject("data");
        try {
            TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(),json.get("firsttime").getAsString(),json.get("cycle").getAsString(),json.get("count").getAsString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onSubscribe(String channel, int subscribedChannels) {    //订阅了频道会调用
        System.out.println(String.format("subscribe redis channel success, channel %s, subscribedChannels %d",
                channel, subscribedChannels));
    }
    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {   //取消订阅 会调用
        System.out.println(String.format("unsubscribe redis channel, channel %s, subscribedChannels %d",
                channel, subscribedChannels));

    }
}
