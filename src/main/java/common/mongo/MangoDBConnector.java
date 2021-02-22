package common.mongo;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import common.ConfigManager;

import java.util.List;

/**
 * Created by lihan on 2018/11/29.
 */
public class MangoDBConnector {
    //    public static MongoClient getClient() {
//        return new MongoClient("127.0.0.1", 27017);
//    }
//
    public static MongoClient getClient() {
        return new MongoClient(LookupClusterAccessPointer());
    }

    //public static MongoClient getClient() {
//    return new MongoClient("39.98.93.230", 27017);
    private static List<ServerAddress> LookupClusterAccessPointer() {
        String[] dbAddress = ConfigManager.getInstance().fetchMongoDBAddress().split(",");
        String[] ports = ConfigManager.getInstance().fetchMongoDBPort().split(",");
        if (dbAddress.length != ports.length) {
            System.exit(-2001);
        }

        List<ServerAddress> rst = Lists.newArrayList();

        for (int i = 0; i < dbAddress.length; i++) {
            rst.add(new ServerAddress(dbAddress[i], Integer.parseInt(ports[i])));
        }

        return rst;
    }
//}
}
