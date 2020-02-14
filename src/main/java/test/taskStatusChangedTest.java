package test;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.util.Objects;

/**
 * Created by lihan on 2018/11/30.
 */
public class taskStatusChangedTest {
    public static void main(String[] args) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        ;
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

        MongoCollection<Document> meta_instrunction1 = mongoDatabase.getCollection("meta_instrunction");

        FindIterable<Document> meta_instrunction = mongoDatabase.getCollection("meta_instrunction").find();

        for (Document document : meta_instrunction) {
            if (document.containsKey("hex") && !Objects.equals(document.getString("hex"), "")) {
                int size = document.getString("hex").length();
                String hex = "";
                for (int i = 0; i < size; i++) {
                    hex += "0";
                }

                document.append("hex",hex);
                Document modifiers = new Document();
                modifiers.append("$set", document);
                meta_instrunction1.updateOne(new Document("_id", document.getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
            }
        }
    }
}
