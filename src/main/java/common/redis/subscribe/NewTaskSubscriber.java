package common.redis.subscribe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.def.TaskType;
import common.def.TempletType;
import common.redis.MsgType;
import redis.clients.jedis.JedisPubSub;
import srv.task.TaskInit;

import java.io.IOException;

/**
 * Created by lihan on 2018/11/15.
 */
public class NewTaskSubscriber extends JedisPubSub {
    public NewTaskSubscriber() {
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));

        JsonParser parse = new JsonParser();  //´´½¨json½âÎöÆ÷
        JsonObject msg = (JsonObject) parse.parse(message);

        String asString = msg.getAsJsonObject("Head").get("type").getAsString();

        if (!asString.equals(MsgType.NEW_TASK.name()))
            return;

        JsonObject json = msg.getAsJsonObject("data");
        try {

            if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                TaskInit.initCronTaskForTaskPlan(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.TASK_PLAN.name()))
                TaskInit.initRTTaskForTaskPlan(json.get("name").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.CRONTAB.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initCronTaskForOrbitForecast(json.get("name").getAsString(), json.get("firsttime").getAsString(), json.get("cycle").getAsString(), json.get("count").getAsString());

            else if (json.get("tasktype").getAsString().equals(TaskType.REALTIME.name()) && json.get("templet").getAsString().equals(TempletType.ORBIT_FORECAST.name()))
                TaskInit.initRTTaskForOrbitForecast(json.get("name").getAsString());
            else {
            }
        } catch (IOException e) {
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
