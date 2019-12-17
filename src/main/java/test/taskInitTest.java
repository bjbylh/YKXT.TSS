package test;

import com.cast.wss.client.*;

/**
 * Created by lihan on 2018/11/19.
 */
public class taskInitTest {
    public static void main(String[] args) {
//        String fileName = "orbit_attitude";
//        BufferedWriter writer = null;
//        String destDirName = "C:\\jsonfiles\\";
//        File dir = new File(destDirName);
//        if (dir.exists()) {
//            System.out.println("创建目录" + destDirName + "失败，目标目录已经存在");
//        }
//        if (!destDirName.endsWith(File.separator)) {
//            destDirName = destDirName + File.separator;
//        }
//        //创建目录
//        if (dir.mkdirs()) {
//            System.out.println("创建目录" + destDirName + "成功！");
//        } else {
//            System.out.println("创建目录" + destDirName + "失败！");
//        }
//        File file = new File(destDirName + fileName + ".txt");
//        System.out.println(file + "file");
//        //如果文件不存在，则新建一个
//        if (!file.exists()) {
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            System.out.println(fileName + ".txt文件不存在");
//        }
//        //写入
//        MongoClient mongoClient = MangoDBConnector.getClient();
//        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
//        Document main_task = mongoDatabase.getCollection("main_task").find().first();
//        Object tp_info = main_task.get("tp_info");
//        String s = tp_info.getClass().toString();
//        System.out.println(s);
//
//        Object name = main_task.get("name");
//        s = name.getClass().toString();
//        System.out.println(s);
        ObjectFactory objectFactory = new ObjectFactory();
        HeadType headType = objectFactory.createHeadType();
        headType.setCreationTime("2010-10-10 00:00:00.000");

        DtplanType dtplanType = objectFactory.createDtplanType();
        dtplanType.setHead(headType);

        PlanType planType = objectFactory.createPlanType();
        MissionType missionType = objectFactory.createMissionType();
        missionType.setSatelliteID("XX-1");
        missionType.setEndTime("2010-10-10 00:00:00.000");
        missionType.setStartTime("2010-10-10 00:00:00.000");
        missionType.setStationID("1");
        missionType.setTplanID("111");

        planType.setMission(missionType);

        dtplanType.setPlan(planType);

        System.out.println(dtplanType.toString());
    }
}
