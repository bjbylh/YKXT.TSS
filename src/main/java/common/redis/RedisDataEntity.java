package common.redis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import common.def.InsType;
import common.def.TaskType;
import common.def.TempletType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisDataEntity {
    private static long num = 0;

    public static String GenDbRefreshData(String taskId) {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.DB_REFRESH, "TSS", "MAG", "");
        JsonObject data = new JsonObject();
        data.addProperty("taskID", taskId);
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenHeartBeatData() {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.KEEP_ALIVE, "TSS", "MAG", "");
        JsonObject data = new JsonObject();
        data.addProperty("status", num++);
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenNewTask() {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.NEW_TASK, "MAG", "TSS", "");
        JsonObject data = new JsonObject();
        data.addProperty("name", "ORBIT_FORECAST");
        data.addProperty("tasktype", TaskType.REALTIME.name());
        data.addProperty("templet", TempletType.ORBIT_FORECAST.name());
//        data.addProperty("firsttime", Instant.now().toString());
//        data.addProperty("cycle", "60000");
//        data.addProperty("count", "0");
        //data.addProperty("content","XXXXX");
//        String imageorder = "20190903095709744,20190903095709745,20190903095709746,20190903095709747,20190903095709748";
//        String stationmission = "11";
//        data.addProperty("imageorder", imageorder);
//        data.addProperty("stationmission", stationmission);
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenNewTaskErcy() {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.CHECK_QUERY, "MAG", "TSS", "");
        JsonObject data = new JsonObject();
        String imageorder = "20190903095709744,20190903095709745,20190903095709746,20190903095709747,20190903095709748";
        String stationmission = "11";
        data.addProperty("imageorder", imageorder);
        data.addProperty("stationmission", stationmission);
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenCheckResult(String id, Map<String, Boolean> trueorfalse) {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.CHECK_RESULT, "TSS", "MAG", id);
        JsonObject data = new JsonObject();

        JsonArray jsonArray = new JsonArray();
        for (String mission_number : trueorfalse.keySet()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("order_number", mission_number);
            jsonObject.addProperty("return", trueorfalse.get(mission_number).toString());
            jsonArray.add(jsonObject);
        }
        data.addProperty("content", jsonArray.toString());
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenFinishedInform(String id, ArrayList<String> orderList) {
        JsonObject ret = new JsonObject();
        JsonObject head = GenHead(MsgType.TP_FINISHED, "TSS", "MAG", id);
        JsonObject data = new JsonObject();

        String content = "";
        for (String order_number : orderList) {
            content += order_number;
            content += ",";
        }

        if (orderList.size() > 0)
            content = content.substring(0, content.length() - 1);

        data.addProperty("content", content);
        ret.add("Head", head);
        ret.add("Data", data);
        return ret.toString();
    }

    public static String GenTaskStatusChange(String taskID, MsgType topic, InsType insType) {
        JsonObject jsonObject = GenHead(topic, "MAG", "TSS", "");
        JsonObject data = new JsonObject();
        data.addProperty("taskID", taskID);
        data.addProperty("status", insType.name());
        return jsonObject.toString();
    }

    private static JsonObject GenHead(MsgType type, String from, String to, String id) {
        Instant now = Instant.now();
        JsonObject json = new JsonObject();
        if (id == "")
            json.addProperty("id", from + "@" + now.toEpochMilli());
        else
            json.addProperty("id", id);

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
