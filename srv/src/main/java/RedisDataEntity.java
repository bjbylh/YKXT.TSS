import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisDataEntity {
    public static String GenDbRefreshData(String taskId) {
        JsonObject jsonObject = GenHead(Topic.DB_REFRESH, "TSS", "MAG");
        JsonObject data = new JsonObject();
        data.addProperty("taskID", taskId);
        jsonObject.add("data", data);
        return jsonObject.toString();
    }


    private static JsonObject GenHead(Topic type, String from, String extra) {
        Instant now = Instant.now();
        JsonObject json = new JsonObject();
        json.addProperty("id", from + "@" + now.toEpochMilli());
        json.addProperty("time", now.toString().substring(0, now.toString().length() - 1));
        json.addProperty("type", type.name());
        json.addProperty("from", from);
        json.addProperty("extra", extra);
        return json;
    }

    public static void main(String[] args) {
        System.out.println(GenDbRefreshData("33131"));

    }
}
