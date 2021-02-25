package srv.task;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import common.ConfigManager;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import common.network.UdpReceiver;
import core.taskplan.MeanToTrueAnomaly;
import org.bson.Document;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by lihan on 2018/11/13.
 */
public class OrbitCalcTaskLisener {
    private static OrbitCalcTaskLisener ourInstance;

    static {
        ourInstance = new OrbitCalcTaskLisener();
    }

    public static OrbitCalcTaskLisener getInstance() {
        return ourInstance;
    }

    private OrbitCalcTaskLisener() {

    }

    public void startup() throws IOException, InterruptedException {
        DoWork doWork = new DoWork();
        doWork.start();
    }


    class DoWork extends Thread {
        private UdpReceiver udpReceiver;

        public DoWork() throws UnknownHostException {
            String recvip = ConfigManager.getInstance().fetchRecvIP();
            int rectport = ConfigManager.getInstance().fetchRecvPort();
            String localip = ConfigManager.getInstance().fetchLocalIP();

            udpReceiver = new UdpReceiver(recvip, rectport, localip);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    MongoClient mongoClient = MangoDBConnector.getClient();
                    //获取名为"temp"的数据库
                    MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);

                    ByteBuffer byteBuffer = udpReceiver.receiveFrame();

                    long t = byteBuffer.getLong();
                    double a = byteBuffer.getDouble();
                    double e = byteBuffer.getDouble();
                    double i = byteBuffer.getDouble();
                    double o = byteBuffer.getDouble();
                    double w = byteBuffer.getDouble();
                    double m = byteBuffer.getDouble();

                    MongoCollection<Document> Data_Satllitejson = mongoDatabase.getCollection("satellite_resource");
                    Document Satllitejson = Data_Satllitejson.find().first();
                    Document newSateInfo = Document.parse(Satllitejson.toJson());

                    double trueAnomaly = MeanToTrueAnomaly.MeanToTrueAnomalyII(a, e, i, w, o, m);

                    ArrayList<Document> properties = (ArrayList<Document>) newSateInfo.get("properties");
                    for (Document d : properties) {
                        if (d.getString("group").equals("轨道参数")) {
                            if (d.getString("key").equals("update_time")) {
                                d.append("value", Date.from(Instant.ofEpochMilli(t)));
                            } else if (d.getString("key").equals("a")) {
                                d.append("value", String.valueOf(a / 1000.0));
                            } else if (d.getString("key").equals("e")) {
                                d.append("value", String.valueOf(e));
                            } else if (d.getString("key").equals("i")) {
                                d.append("value", String.valueOf(i));
                            } else if (d.getString("key").equals("RAAN")) {
                                d.append("value", String.valueOf(o));
                            } else if (d.getString("key").equals("perigee_angle")) {
                                d.append("value", String.valueOf(w));
                            } else if (d.getString("key").equals("true_anomaly")) {//todo
                                d.append("value", String.valueOf(trueAnomaly));
                            } else if (d.getString("key").equals("mean_anomaly")) {//todo
                                d.append("value", String.valueOf(m));
                            } else continue;
                        }
                    }
                    Data_Satllitejson.updateOne(Filters.eq("_id", Satllitejson.getObjectId("_id")), new Document("$set", newSateInfo));

                    mongoClient.close();

                    TaskInit.initRTTaskForOrbitForecast("新建任务调度任务指令");
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}