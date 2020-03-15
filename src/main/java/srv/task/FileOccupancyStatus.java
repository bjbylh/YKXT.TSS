package srv.task;

import com.google.common.collect.Sets;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

/**
 * Created by lihan on 2019/11/12.
 */
public class FileOccupancyStatus {
    private static FileOccupancyStatus ourInstance = new FileOccupancyStatus();

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    public static FileOccupancyStatus getInstance() {
        return ourInstance;
    }

    @SuppressWarnings("unchecked")
    private FileOccupancyStatus() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

    }

    public void startup() throws IOException, InterruptedException {
        FileOccupancyStatus.DoWork doWork = new FileOccupancyStatus.DoWork();
        doWork.start();
    }

    class DoWork extends Thread {
        public void run() {
            while (true) {
                try {
                    check();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    if (mongoClient == null) {
                        mongoClient = MangoDBConnector.getClient();
                        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
                    }
                    e.printStackTrace();
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void check() throws IOException {
            MongoCollection<Document> image_order = mongoDatabase.getCollection("image_order");
            FindIterable<Document> image_orders = image_order.find();

            Set<String> files = Sets.newHashSet();

            for (Document document : image_orders) {
                if (document.getString("order_state").equals("待规划") || document.getString("order_state").equals("未提交")) {
//                    System.out.println(document.toJson().toString());
                    if (document.containsKey("record_file_no") && !document.getString("record_file_no").equals("")) {
                        String fileno = document.getString("record_file_no");

                        if (!files.contains(fileno))
                            files.add(fileno);
                    }
                }

//            mongoClient.close();
            }
            ArrayList<Document> stepRcd = new ArrayList<>();


            long pool_size = 64;

            Document doc = new Document();
            doc.append("time_stamp", Date.from(Instant.now()));
            doc.append("pool_size", pool_size);
            for (int i = 0; i < 64; i++) {
                Document data = new Document();
                if (files.contains(String.valueOf(i))) {
//                    data.append("size", 0.0);
                    data.append("valid", false);
                } else {
//                    data.append("size", 0.0);
                    data.append("valid", true);
                }

                doc.append("file_" + i, data);
            }
//            doc.append("flash_usage", 0.0);
//
//            doc.append("replayed_size", 0.0);

            stepRcd.add(doc);

            Document save = new Document();

            MongoCollection<Document> pool_files = mongoDatabase.getCollection("pool_files");
            Bson queryBson;

            save.append("type", "OCCUPANCY");
            queryBson = Filters.eq("type", "OCCUPANCY");

            save.append("data", stepRcd);

            if (pool_files.count(queryBson) <= 0)
                pool_files.insertOne(save);
            else {
                Document first = pool_files.find(queryBson).first();
                Document modifiers = new Document();
                modifiers.append("$set", save);
                pool_files.updateOne(new Document("_id", first.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
            }
        }
    }
}

