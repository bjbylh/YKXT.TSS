package test;

import java.util.UUID;

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
//        FindIterable<Document> image_mission = mongoDatabase.getCollection(fileName).find();
//        try {
//            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));
//            for(Document d : image_mission) {
//                d.remove("_id");
//                writer.write(d.toJson());
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (writer != null) {
//                    writer.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        System.out.println("文件写入成功！");


        for (int i = 0; i < 9; i++) {
            //注意replaceAll前面的是正则表达式
            String uuid = UUID.randomUUID().toString();
            System.out.println(uuid);
//            System.out.println(uuid.length());
        }

    }
}
