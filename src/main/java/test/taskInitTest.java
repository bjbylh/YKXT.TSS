package test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by lihan on 2018/11/19.
 */
public class taskInitTest {
    public static void main(String[] args) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("int value",1);
        jsonObject.addProperty("string value","i am string 1!");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("int value",2);
        jsonObject2.addProperty("string value","i am string 2!");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject);
        jsonArray.add(jsonObject2);

        System.out.println(jsonArray);
    }
}
