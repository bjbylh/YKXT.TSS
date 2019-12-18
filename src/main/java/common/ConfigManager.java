package common;


import java.util.ResourceBundle;

/**
 * Created by lihan on 2017/10/26.
 */
public class ConfigManager {
    private static ConfigManager ourInstance = new ConfigManager();

    public static ConfigManager getInstance() {
        return ourInstance;
    }

    private static ResourceBundle bundle;

    private ConfigManager() {

    }

    static {
        bundle = ResourceBundle.getBundle("basic");
    }

    //Fetch From File
    public String fetchMongoDBAddress() {

        return bundle.getString("MONGODB_IP");
    }


    public String fetchRedisAddress() {
        return bundle.getString("REDIS_IP");
    }

    public String fetchJsonPath() {
        return bundle.getString("TASK_PATH");
    }

    public String fetchInsFilePath() {
        return bundle.getString("INS_PATH");
    }

    public String fetchXmlFilePath() {
        return bundle.getString("XML_PATH");
    }

    public boolean fetchDebug() {
        if (bundle.getString("DEBUG").toLowerCase().equals("true"))
            return true;
        else return false;
    }
}
