package common.mongo;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import common.ConfigManager;

import java.util.Arrays;
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
        List<String> dbAddress = Arrays.asList(ConfigManager.getInstance().fetchMongoDBAddress().split(","));
        int port = ConfigManager.getInstance().fetchMongoDBPort();
        if (dbAddress.isEmpty()) {
            System.exit(-2001);
        }

        List<ServerAddress> rst = Lists.newArrayList();

        for (String address : dbAddress) {
            rst.add(new ServerAddress(address, port));
        }

        return rst;
    }
//}
}
