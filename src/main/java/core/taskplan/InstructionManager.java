package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import common.mongo.CommUtils;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Created by lihan on 2020/3/7.
 */
public class InstructionManager {
    private MongoClient mongoClient = null;

    private MongoDatabase mongoDatabase = null;

    public InstructionManager() {
        mongoClient = MangoDBConnector.getClient();
        mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
    }

    public void addInstrctionInfo(ArrayList<Document> instruction_info, String mission_number,String mission_name) {
        MongoCollection<Document> all_instruction = mongoDatabase.getCollection("all_instruction");

        Document document = new Document();
        document.append("time_point", Date.from(Instant.now()));
        document.append("mission_number", mission_number);
        document.append("mission_name", mission_name);
        document.append("instruction_info", instruction_info);

        all_instruction.insertOne(document);

    }

    public void init() {
        MongoCollection<Document> all_instruction = mongoDatabase.getCollection("all_instruction");
        all_instruction.drop();

        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
        FindIterable<Document> image_missions = image_mission.find();

        for (Document document : image_missions) {

            if (!document.containsKey("instruction_info"))
                continue;

            ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

            if (!CommUtils.checkInstructionInfo(instruction_info))
                continue;

            if (instruction_info.size() > 0) {
                addInstrctionInfo(instruction_info, document.getString("mission_number"),document.getString("name"));
            }

        }

        MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
        FindIterable<Document> transmission_missions = transmission_mission.find();

        for (Document document : transmission_missions) {
            if (!document.containsKey("instruction_info") || !document.getString("fail_reason").equals(""))
                continue;

            ArrayList<Document> instruction_info = (ArrayList<Document>) document.get("instruction_info");

            if (!CommUtils.checkInstructionInfo(instruction_info))
                continue;

            if (instruction_info != null && instruction_info.size() > 0) {
                addInstrctionInfo(instruction_info, document.getString("transmission_number"),"数传任务");
            }
        }
    }

    public void close() {
        if (mongoClient != null)
            mongoClient.close();
    }

    public static void main(String[] args) {
        InstructionManager instructionManager = new InstructionManager();
        instructionManager.init();
        instructionManager.close();
    }
}
