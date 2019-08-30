package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Created by lihan on 2019/8/27.
 */
public class OrderOverallPlan {
    private ArrayList<String> orderIds;

    public OrderOverallPlan(ArrayList<String> orderIds) {
        this.orderIds = orderIds;
    }

    public ArrayList<String> Trans() {
        ArrayList<String> mission_numbers = new ArrayList<>();
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        //卫星资源表
        MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");

        FindIterable<Document> documents = image_order.find();

        for (Document order : documents) {
            if (orderIds.contains(order.getString("order_number"))) {
                Document dd = Document.parse(order.toJson());
                dd.append("order_state", "待规划");
                dd.append("modify_time", Date.from(Instant.now()));
                String mission_number = "im_" + Instant.now().toEpochMilli();
                mission_numbers.add(mission_number);
                dd.append("mission_number", mission_number);


                Document imageMissionFromImageOrder = getImageMissionFromImageOrder(order, mission_number);
                image_mission.insertOne(imageMissionFromImageOrder);

                Document modifiers = new Document();
                modifiers.append("$set", dd);
                image_order.updateOne(new Document("_id", dd.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));

            }
        }

        return mission_numbers;
    }

    private Document getImageMissionFromImageOrder(Document order, String mission_number) {

 //       order.remove("order_number");
        order.append("mission_number", mission_number);

        order.remove("user_account");
        order.remove("order_submit_time");
        order.remove("modify_time");
        order.remove("order_state");
        order.remove("mission_number");
        order.remove("_id");
        order.append("mission_state", "待规划");

        ArrayList<String> order_numbers = new ArrayList<>();
        order_numbers.add(order.getString("order_number"));
        order.remove("order_number");

        order.append("order_numbers", order_numbers);

        return order;

    }

    public static void main(String[] args) {
        ArrayList<String> ids = new ArrayList<>();
        ids.add("XXXXX");
        OrderOverallPlan orderOverallPlan = new OrderOverallPlan(ids);

        orderOverallPlan.Trans();
    }
}
