package common.redis;

import com.google.gson.JsonObject;
import common.def.InsType;
import common.def.TaskType;
import common.def.TempletType;

import java.time.Instant;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisDataEntity {
    private static long num = 0;

    public static String GenDbRefreshData(String taskId) {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.DB_REFRESH, "TSS", "MAG","");
        JsonObject data = new JsonObject();
        data.addProperty("taskID", taskId);
        ret.add("Head",head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenHeartBeatData() {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.KEEP_ALIVE, "TSS", "MAG","");
        JsonObject data = new JsonObject();
        data.addProperty("status", num++);
        ret.add("Head",head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenNewTask() {
        JsonObject jsonObject = GenHead(MsgType.NEW_TASK, "MAG", "TSS","");
        JsonObject data = new JsonObject();
        data.addProperty("name", "task12345");
        data.addProperty("tasktype", TaskType.CRONTAB.name());
        data.addProperty("templet", TempletType.TASK_PLAN.name());
        data.addProperty("firsttime", Instant.now().toString());
        data.addProperty("cycle", "60000");
        data.addProperty("count", "0");
        jsonObject.add("data", data);
        return jsonObject.toString();
    }

    public static String GenTaskStatusChange(String taskID, MsgType topic, InsType insType) {
        JsonObject jsonObject = GenHead(topic, "MAG", "TSS","");
        JsonObject data = new JsonObject();
        data.addProperty("taskID", taskID);
        data.addProperty("status", insType.name());
        return jsonObject.toString();
    }

    private static JsonObject GenHead(MsgType type, String from, String to, String extra) {
        Instant now = Instant.now();
        JsonObject json = new JsonObject();
        json.addProperty("id", from + "@" + now.toEpochMilli());
        json.addProperty("time", now.toString().substring(0, now.toString().length() - 1));
        json.addProperty("type", type.name());
        json.addProperty("from", from);
        json.addProperty("to", to);
        return json;
    }

    public static void main(String[] args) {
        System.out.println(GenDbRefreshData("33131"));

    }
}
