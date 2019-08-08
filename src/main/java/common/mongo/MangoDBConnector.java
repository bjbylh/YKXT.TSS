package common.mongo;

import com.mongodb.MongoClient;

/**
 * Created by lihan on 2018/11/29.
 */
public class MangoDBConnector {
    public static MongoClient getClient() {
        return new MongoClient("39.98.93.230", 27017);
    }
}
