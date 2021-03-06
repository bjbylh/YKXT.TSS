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

    public String fetchMongoDBPort() {

        return bundle.getString("MONGODB_PORT");
    }

    public String fetchRecvIP(){
        return bundle.getString("RECV_IP");
    }

    public String fetchLocalIP(){
        return bundle.getString("LOCAL_IP");
    }

    public int fetchRecvPort() {
        return Integer.parseInt(bundle.getString("RECV_PORT"));
    }

    public String fetchRedisAddress() {
        return bundle.getString("REDIS_IP");
    }

    public int fetchRedisPort() {
        return Integer.parseInt(bundle.getString("REDIS_PORT"));
    }

    public String fetchRedisAuth() {
        return bundle.getString("REDIS_AUTH");
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
        if (bundle.getString("INS_GEN_ENABLE").toLowerCase().equals("true")) return true;
        else return false;
    }

    public static void main(String[] args) {
        String s = ConfigManager.getInstance().fetchRedisAuth();
        Boolean b = s.equals("");
        System.out.println(b);
    }

    public String fetch502FilePath() {
        //return "";
        return bundle.getString("502_PATH");
    }
}
