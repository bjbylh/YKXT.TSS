package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;

import java.io.IOException;

/**
 * Created by lihan on 2018/11/15.
 */
public class NewTaskSubscriber extends JedisPubSub {
    public NewTaskSubscriber(){}
    @Override
    public void onMessage(String channel, String message) {       //�յ���Ϣ??��
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        JsonParser parse = new JsonParser();  //?��json������
        JsonObject msg = (JsonObject) parse.parse(message);
        JsonObject json = msg.getAsJsonObject("data");
        try {
            TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(),json.get("firsttime").getAsString(),json.get("cycle").getAsString(),json.get("count").getAsString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onSubscribe(String channel, int subscribedChannels) {    //??��?��??��
        System.out.println(String.format("subscribe redis channel success, channel %s, subscribedChannels %d",
                channel, subscribedChannels));
    }
    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {   //ȡ��?? ??��
        System.out.println(String.format("unsubscribe redis channel, channel %s, subscribedChannels %d",
                channel, subscribedChannels));

    }
}
