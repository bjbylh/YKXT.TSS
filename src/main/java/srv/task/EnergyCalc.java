package srv.task;

import com.google.common.collect.Maps;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lihan on 2020/3/1.
 */
public class EnergyCalc {

    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    public EnergyCalc() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
    }

    public void calcEnergy(Instant start, Instant end, double initValue, boolean useInitValue) {

        Map<Instant,Document> pool = Maps.newTreeMap();


        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
        FindIterable<Document> image_missions = image_mission.find();

        for (Document document : image_missions) {
            if (document.getString("mission_state").equals("待执行") || document.getString("mission_state").equals("已执行")) {

                if (document.getString("work_mode").contains("擦除")) continue;

                if (!document.containsKey("instruction_info"))
                    continue;

                ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                if (!CommUtils.checkInstructionInfo(instruction_info))
                    continue;

                if (!document.containsKey("image_window"))
                    continue;

                ArrayList<Document> image_window = (ArrayList<Document>) document.get("image_window");

                if (instruction_info.size() > 0 && image_window.size() > 0) {

                }
            }
        }

        MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
        FindIterable<Document> transmission_missions = transmission_mission.find();

        for (Document document : transmission_missions) {
            if (!document.containsKey("fail_reason") || document.getString("fail_reason").equals("")) {
                if (!document.containsKey("instruction_info") || !document.getString("fail_reason").equals(""))
                    continue;

                ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

                if (!CommUtils.checkInstructionInfo(instruction_info))
                    continue;

                if (instruction_info != null && instruction_info.size() > 0) {
//                    pool_inss_trans.add(document);
//                    pool_files_trans.add(document);
                }
            }
        }
    }

    public double fetchEnergy(Instant time) {
        return 0.0;
    }


}
