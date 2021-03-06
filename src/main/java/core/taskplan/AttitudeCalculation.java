package core.taskplan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.*;


public class AttitudeCalculation {
    //载荷变量
    // 载荷安装 矩阵，格式：每行代表一个载荷，每行格式[光轴与本体系x轴夹角，光轴与本体系y轴夹角，光轴与本体系z轴夹角]，单位：弧度
    private static int LoadNumber = 5;                    //载荷数量
    private static double[][] LoadInstall = {{90 * Math.PI * 180.0, 86.3 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 93.7 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 85.6 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 94.4 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI / 180.0, 90 * Math.PI / 180.0, 0 * Math.PI / 180.0}};
    //载荷视场角，格式：每行代表一个载荷，每行格式[内视角，外视角，上视角，下视角]，单位：弧度
    private static double[][] LoadViewAng = {{3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {0.1 * Math.PI / 180.0, 0.1 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 / Math.PI / 180.0}};

    //卫星变量
    //卫星最大机动能力，最大机动欧拉角，格式[绕x轴最大机动角度，绕y轴最大机动角度，绕z轴最大机动角度]，单位：弧度
    private static double[] SatelliteManeuverEuler = {10 * Math.PI / 180.0, 10 * Math.PI / 180.0, 10 * Math.PI / 180.0};
    private static double[] SatelliteManeuverVelocity = {10 * Math.PI / 180.0, 10 * Math.PI / 180.0, 10 * Math.PI / 180.0};//最大机动角速度

    //任务变量
    private static int MissionNumber;                   //任务数量
    private static double[][] MissionStarTime;          //任务起始时间，格式：每行代表一个任务，每行格式[年，月，日，时，分，秒]
    private static double[][] MissionStopTime;          //任务结束时间，格式：每行代表一个任务，每行格式[年，月，日，时，分，秒]
    private static String[] MissionSerialNumber;        //任务编号
    private static int[] MissionImagingMode;            //成像模式，格式：每行代表一个任务。1：常规模式，2：凝视模式，3：定标模式
    private static int[] MissionTargetType;             //成像目标类型，格式：每行代表一个任务。1：点目标，2：区域目标
    private static double[][] MissionTargetArea;        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
    private static double[][] MisssionTargetHeight;     //成像高度要求，格式：每行代表一个任务，每行格式[最低成像要求，最高成像要求]，单位：米
    private static int[] PlanningMissionLoad;

    //常量
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double Step = 1;                        //数据步长
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;
    private static int TimeZone = -8;                     //北京时区到世界时-8
    private static double MissionLimbAirHeighLimt = 200000;   //临边观测模式大气高度范围

    public static void AttitudeCalculationII(Document Satllitejson, FindIterable<Document> Orbitjson, long OrbitDataCount, ArrayList<Document> ImageMissionjson) {
        Boolean ESDStatus = true;
        String AxisType = "";
        //载荷参数更新
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        int ii = 0;
        for (Document document : properties) {
            if (document.getString("key").equals("amount_load")) {
                LoadNumber = Integer.parseInt(document.get("value").toString());
            } else if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "1")) {
                LoadViewAng[1 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("in_side_sight") && document.getString("group").equals("payload" + "1")) {
                LoadViewAng[1 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("up_side_sight") && document.getString("group").equals("payload" + "1")) {
                LoadViewAng[1 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("down_side_sight") && document.getString("group").equals("payload" + "1")) {
                LoadViewAng[1 - 1][3] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Xaxis") && document.getString("group").equals("payload" + "1")) {
                LoadInstall[1 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Yaxis") && document.getString("group").equals("payload" + "1")) {
                LoadInstall[1 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Zaxis") && document.getString("group").equals("payload" + "1")) {
                LoadInstall[1 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "2")) {
                LoadViewAng[2 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("in_side_sight") && document.getString("group").equals("payload" + "2")) {
                LoadViewAng[2 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("up_side_sight") && document.getString("group").equals("payload" + "2")) {
                LoadViewAng[2 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("down_side_sight") && document.getString("group").equals("payload" + "2")) {
                LoadViewAng[2 - 1][3] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Xaxis") && document.getString("group").equals("payload" + "2")) {
                LoadInstall[2 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Yaxis") && document.getString("group").equals("payload" + "2")) {
                LoadInstall[2 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Zaxis") && document.getString("group").equals("payload" + "2")) {
                LoadInstall[2 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "3")) {
                LoadViewAng[3 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("in_side_sight") && document.getString("group").equals("payload" + "3")) {
                LoadViewAng[3 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("up_side_sight") && document.getString("group").equals("payload" + "3")) {
                LoadViewAng[3 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("down_side_sight") && document.getString("group").equals("payload" + "3")) {
                LoadViewAng[3 - 1][3] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Xaxis") && document.getString("group").equals("payload" + "3")) {
                LoadInstall[3 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Yaxis") && document.getString("group").equals("payload" + "3")) {
                LoadInstall[3 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Zaxis") && document.getString("group").equals("payload" + "3")) {
                LoadInstall[3 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "4")) {
                LoadViewAng[4 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("in_side_sight") && document.getString("group").equals("payload" + "4")) {
                LoadViewAng[4 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("up_side_sight") && document.getString("group").equals("payload" + "4")) {
                LoadViewAng[4 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("down_side_sight") && document.getString("group").equals("payload" + "4")) {
                LoadViewAng[4 - 1][3] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Xaxis") && document.getString("group").equals("payload" + "4")) {
                LoadInstall[4 - 1][0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Yaxis") && document.getString("group").equals("payload" + "4")) {
                LoadInstall[4 - 1][1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("angle_sight_Zaxis") && document.getString("group").equals("payload" + "4")) {
                LoadInstall[4 - 1][2] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("roll_angle_max")) {
                SatelliteManeuverEuler[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("pitch_angle_max")) {
                SatelliteManeuverEuler[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("v_roll_angle")) {
                SatelliteManeuverVelocity[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("v_pitch_angle")) {
                SatelliteManeuverVelocity[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("axis")) {
                AxisType = document.getString("value");
            } else
                continue;
        }

        if (AxisType.contains("轨道"))
            ESDStatus = false;

//        System.out.println(ESDStatus);

        //读入轨道数据
        String StringTime;
        double[][] Time = new double[(int) OrbitDataCount][6];
        double[][] SatPosition_GEI = new double[(int) OrbitDataCount][3];
        double[][] SatVelocity_GEI = new double[(int) OrbitDataCount][3];
        double[][] SatPosition_LLA = new double[(int) OrbitDataCount][3];
        Date[] Time_Point = new Date[(int) OrbitDataCount];

        int OrbitalDataNum = 0;
        for (Document document : Orbitjson) {
            Date time_point = document.getDate("time_point");
            //时间转换为doubule型
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(time_point);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            Time_Point[OrbitalDataNum] = time_point;
            Time[OrbitalDataNum][0] = Double.parseDouble(StringTime.substring(0, 4));
            Time[OrbitalDataNum][1] = Double.parseDouble(StringTime.substring(5, 7));
            Time[OrbitalDataNum][2] = Double.parseDouble(StringTime.substring(8, 10));
            Time[OrbitalDataNum][3] = Double.parseDouble(StringTime.substring(11, 13));
            Time[OrbitalDataNum][4] = Double.parseDouble(StringTime.substring(14, 16));
            Time[OrbitalDataNum][5] = Double.parseDouble(StringTime.substring(17, 19));

            SatPosition_GEI[OrbitalDataNum][0] = Double.parseDouble(document.get("P_x").toString());
            SatPosition_GEI[OrbitalDataNum][1] = Double.parseDouble(document.get("P_y").toString());
            SatPosition_GEI[OrbitalDataNum][2] = Double.parseDouble(document.get("P_z").toString());
            SatVelocity_GEI[OrbitalDataNum][0] = Double.parseDouble(document.get("Vx").toString());
            SatVelocity_GEI[OrbitalDataNum][1] = Double.parseDouble(document.get("Vy").toString());
            SatVelocity_GEI[OrbitalDataNum][2] = Double.parseDouble(document.get("Vz").toString());
            SatPosition_LLA[OrbitalDataNum][0] = Double.parseDouble(document.get("lon").toString());
            SatPosition_LLA[OrbitalDataNum][1] = Double.parseDouble(document.get("lat").toString());
            SatPosition_LLA[OrbitalDataNum][2] = Double.parseDouble(document.get("H").toString());

            OrbitalDataNum = OrbitalDataNum + 1;

            if (OrbitalDataNum >= OrbitDataCount)
                break;
        }

        //恒星定标、临边观测
        ArrayList<Document> MissionStarDocument = new ArrayList<>();
        ArrayList<Document> MissionLimbDocument = new ArrayList<>();
        ArrayList<Document> MissionCalibrateDocument=new ArrayList<>();
        ArrayList<Document> ImageMissionTemp = new ArrayList<>();
        ArrayList<Document> MissionAll=new ArrayList<>();
        for (Document document : ImageMissionjson) {
            //将恒星定标、定标和临边单独提出
            if (document.get("image_mode").toString().equals("恒星定标")) {
                if (document.get("mission_state").toString().equals("待执行")) {
                    MissionStarDocument.add(document);
                }
            } else if (document.get("image_mode").toString().equals("临边观测")) {
                if (document.get("mission_state").toString().equals("待执行")) {
                    MissionLimbDocument.add(document);
                }
            } else if (document.get("image_mode").toString().equals("定标")) {
                if (document.get("mission_state").toString().equals("待执行")) {
                    MissionCalibrateDocument.add(document);
                }
            }else {
                ImageMissionTemp.add(document);
            }
            MissionAll.add(document);
        }
        ImageMissionjson.clear();
        for (Document document : ImageMissionTemp) {
            ImageMissionjson.add(document);
        }

        //任务读入
        MissionStarTime = new double[ImageMissionjson.size()][6];
        MissionStopTime = new double[ImageMissionjson.size()][6];
        MissionSerialNumber = new String[ImageMissionjson.size()];
        MissionImagingMode = new int[ImageMissionjson.size()];
        MissionTargetType = new int[ImageMissionjson.size()];
        MissionTargetArea = new double[ImageMissionjson.size()][200];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MisssionTargetHeight = new double[ImageMissionjson.size()][2];
        PlanningMissionLoad = new int[ImageMissionjson.size()];
        MissionNumber = 0;
        int[] TargetNum = new int[ImageMissionjson.size()];
        ArrayList<ArrayList<Double>> MissionStareTimeZXQH=new ArrayList<>();
        for (Document document : ImageMissionjson) {
            try {
                Document target_region = (Document) document.get("image_region");

                //读取目标区域
                ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                MissionTargetArea_iList = GetRegionPoint(target_region);
                TargetNum[MissionNumber] = MissionTargetArea_iList.size();
                for (int i = 0; i < TargetNum[MissionNumber]; i++) {
                    MissionTargetArea[MissionNumber][2 * i] = MissionTargetArea_iList.get(i)[0];
                    MissionTargetArea[MissionNumber][2 * i + 1] = MissionTargetArea_iList.get(i)[1];
                }
                ArrayList<Document> image_window = (ArrayList<Document>) document.get("image_window");
                if (image_window == null || document.get("mission_state").equals("待执行") == false) {
                    MissionStarTime[MissionNumber][0] = Time[1][0];
                    MissionStarTime[MissionNumber][1] = Time[1][1];
                    MissionStarTime[MissionNumber][2] = Time[1][2];
                    MissionStarTime[MissionNumber][3] = Time[1][3];
                    MissionStarTime[MissionNumber][4] = Time[1][4];
                    MissionStarTime[MissionNumber][5] = Time[1][5];
                    MissionStopTime[MissionNumber][0] = Time[0][0];
                    MissionStopTime[MissionNumber][1] = Time[0][1];
                    MissionStopTime[MissionNumber][2] = Time[0][2];
                    MissionStopTime[MissionNumber][3] = Time[0][3];
                    MissionStopTime[MissionNumber][4] = Time[0][4];
                    MissionStopTime[MissionNumber][5] = Time[0][5];
                } else {
                    for (Document document1 : image_window) {
                        PlanningMissionLoad[MissionNumber] = Integer.parseInt(document1.get("load_number").toString());
                        Date start_time = document1.getDate("start_time");
                        //时间转换为doubule型
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(start_time);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        MissionStarTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
                        MissionStarTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
                        MissionStarTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
                        MissionStarTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
                        MissionStarTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
                        MissionStarTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
                        Date end_time = document1.getDate("end_time");
                        //时间转换为doubule型
                        cal.setTime(end_time);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        MissionStopTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
                        MissionStopTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
                        MissionStopTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
                        MissionStopTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
                        MissionStopTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
                        MissionStopTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
                    }
                }
                MissionSerialNumber[MissionNumber] = document.getString("mission_number");
                if (document.getString("image_mode").equals("常规")) {
                    MissionImagingMode[MissionNumber] = 1;
                }
                if (document.getString("image_type").equals("Point")) {
                    MissionTargetType[MissionNumber] = 1;
                }
                ArrayList<Double> MissionStareTimeZXQHChild=new ArrayList<>();
                if (document.getString("image_type").equals("LineString") && document.getString("image_mode").equals("指向切换")) {
                    MissionTargetType[MissionNumber] = 3;
                    double sumtemp=0;
                    ArrayList<String> minstare= (ArrayList<String>) document.get("min_stare_time_zxqh");
                    for (String minstaretime:minstare) {
                        sumtemp=sumtemp+Double.parseDouble(minstaretime)+100.0;
                        MissionStareTimeZXQHChild.add(Double.parseDouble(minstaretime));
                    }
                }
                MissionStareTimeZXQH.add(MissionNumber,MissionStareTimeZXQHChild);

                MisssionTargetHeight[MissionNumber][0] = Double.parseDouble(document.getString("min_height_orbit"));
                MisssionTargetHeight[MissionNumber][1] = Double.parseDouble(document.getString("max_height_orbit"));
                MissionNumber = MissionNumber + 1;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //姿态计算，欧拉角1-2-3转序
        double[][] SatAttitud = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel = new double[(int) OrbitalDataNum][3];

        //姿态计算，欧拉角3-1-2转序，东南下
        double[][] SatAttitud_ESD = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel_ESD = new double[(int) OrbitDataCount][3];

        //姿态计算，欧拉角3-2-1转序，东南下
        double[][] SatAttitud_ESD321 = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel_ESD321 = new double[(int) OrbitDataCount][3];

        //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
        double[][] SatAttitud_ORF312 = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel_ORF312 = new double[(int) OrbitDataCount][3];

        //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
        double[][] SatAttitud_ORF_ESD321 = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel_ORF_ESD321 = new double[(int) OrbitDataCount][3];

        //姿态计算，更改后欧拉角3-2-1转序，东南下
        double[][] SatAttitude_ESD321_Euler=new double[(int) OrbitDataCount][3];
        double[][] SatAttitudeVel_ESD321_Euler=new double[(int) OrbitDataCount][3];

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");
        ArrayList<Document> os = new ArrayList<>();
        long cnt = 0;
        for (int i = 0; i < OrbitalDataNum; i++) {
            boolean MissionFlag = false;
            int LoadNum = 0;
            double Mission_FLag = 0;
            double NowTime_JD = JD(Time[i]);
            double[] Target_LLA = new double[3];
            for (int j = 0; j < MissionNumber; j++) {
                double StarTime_JD = JD(MissionStarTime[j]);
                double StopTime_JD = JD(MissionStopTime[j]);
                if (NowTime_JD >= StarTime_JD && NowTime_JD <= StopTime_JD) {
                    if (TargetNum[j] == 1) {
                        Target_LLA[0] = MissionTargetArea[j][0];
                        Target_LLA[1] = MissionTargetArea[j][1];
                        Target_LLA[2] = 0;
                        LoadNum = PlanningMissionLoad[j];
                        Mission_FLag = 1;
                        break;
                    } else {
                        if (MissionTargetType[j] == 3) {
                            double timelong=(StopTime_JD-StarTime_JD)*(24*60*60);
                            if (timelong <= ((TargetNum[j]-1)*100)) {
                                double latSum = 0;
                                double lonSum = 0;
                                for (int k = 0; k < TargetNum[j]; k++) {
                                    latSum = latSum + MissionTargetArea[j][2 * k];
                                    lonSum = lonSum + MissionTargetArea[j][2 * k + 1];
                                }
                                Target_LLA[0] = latSum / TargetNum[j];
                                Target_LLA[1] = lonSum / TargetNum[j];
                                Target_LLA[2] = 0;
                                LoadNum = PlanningMissionLoad[j];
                                Mission_FLag = 1;
                                break;
                            }else {
                                double Nowtime=(NowTime_JD-StarTime_JD)*(24*60*60);
                                int TargetInx= 0;
                                double sumtemp=0;
                                Boolean CXFlag=true;
                                for (int k = 0; k < MissionStareTimeZXQH.get(j).size(); k++) {
                                    sumtemp=sumtemp+MissionStareTimeZXQH.get(j).get(k);
                                    if (Nowtime <= sumtemp) {
                                        TargetInx=k;
                                        CXFlag=true;
                                        break;
                                    }else if (Nowtime > sumtemp && ((int)Nowtime)<=sumtemp+100) {
                                        TargetInx=k;
                                        CXFlag=false;
                                        break;
                                    }
                                    sumtemp=sumtemp+100;
                                }
                                if (CXFlag) {
                                    Target_LLA[0] = MissionTargetArea[j][2 * TargetInx];
                                    Target_LLA[1] = MissionTargetArea[j][2 * TargetInx + 1];
                                    Target_LLA[2] = 0;
                                    LoadNum = PlanningMissionLoad[j];
                                    Mission_FLag = 1;
                                    break;
                                }else {
                                    if (TargetInx==MissionStareTimeZXQH.get(j).size()-1) {
                                        Target_LLA[0] = 0;
                                        Target_LLA[1] = 0;
                                        Target_LLA[2] = 0;
                                        LoadNum = PlanningMissionLoad[j];
                                        Mission_FLag = 0;
                                        break;
                                    }
                                    double timeadd=Nowtime-sumtemp;
                                    double lon2=MissionTargetArea[j][2 * TargetInx+2];
                                    double lon1=MissionTargetArea[j][2 * TargetInx];
                                    double lat2=MissionTargetArea[j][2 * TargetInx + 3];
                                    double lat1=MissionTargetArea[j][2 * TargetInx + 1];
                                    Target_LLA[0] = lon1+(((lon2-lon1)/100.0)*timeadd);
                                    Target_LLA[1] = lat1+(((lat2-lat1)/100.0)*timeadd);
                                    Target_LLA[2] = 0;
                                    LoadNum = PlanningMissionLoad[j];
                                    Mission_FLag = 1;
                                    break;
                                }
                            }
                        }else {
                            double latSum = 0;
                            double lonSum = 0;
                            for (int k = 0; k < TargetNum[j]; k++) {
                                latSum = latSum + MissionTargetArea[j][2 * k];
                                lonSum = lonSum + MissionTargetArea[j][2 * k + 1];
                            }
                            Target_LLA[0] = latSum / TargetNum[j];
                            Target_LLA[1] = lonSum / TargetNum[j];
                            Target_LLA[2] = 0;
                            LoadNum = PlanningMissionLoad[j];
                            Mission_FLag = 1;
                            break;
                        }
                    }
                }
            }

            //太阳矢量
            double[] r_sun = new double[3];//惯性系下太阳矢量
            double[] su = new double[2];
            double rd_sun = Sun(NowTime_JD, r_sun, su);
            //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
            boolean FlyOrientationFlag = true;
            double[] r_sun_n = new double[]{r_sun[0] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                    r_sun[1] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                    r_sun[2] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2])};
            double[] r_ORF_ESD_N=new double[3];
            double[] r_ORF_ECEF_N=new double[3];
            if (ESDStatus) {
                //东南系
                ICRSToECEF(MissionStarTime[i], r_sun_n, r_ORF_ECEF_N);
                ECEFToESDForAvoidSunshine(SatPosition_LLA[i], r_ORF_ECEF_N, r_ORF_ESD_N);
            }else {
                //轨道系
                GEIToORF_Ellipse(SatPosition_GEI[i], SatVelocity_GEI[i], r_sun_n, r_ORF_ESD_N);
            }
            double[] v_sat_n = new double[]{SatVelocity_GEI[i][0] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                    SatVelocity_GEI[i][1] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                    SatVelocity_GEI[i][2] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2])};
            double CosTheta_SunVel = (r_sun_n[0] * v_sat_n[0] + r_sun_n[1] * v_sat_n[1] + r_sun_n[2] * v_sat_n[2]) /
                    (sqrt(r_sun_n[0] * r_sun_n[0] + r_sun_n[1] * r_sun_n[1] + r_sun_n[2] * r_sun_n[2]) * sqrt(v_sat_n[0] * v_sat_n[0] + v_sat_n[1] * v_sat_n[1] + v_sat_n[2] * v_sat_n[2]));
            if (r_ORF_ESD_N[0] >= 0) {
                FlyOrientationFlag = true;
            } else {
                FlyOrientationFlag = false;
            }

            if (Mission_FLag == 1) {
//                System.out.println(Math.acos(r_ORF_ESD_N[0])*180/PI);
                //AttitudeCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], Target_LLA, Time[i], LoadInstall[LoadNum - 1], SatAttitud[i]);
                //北东地1-2-3
                AttitudeCalculationTest(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud[i], FlyOrientationFlag);
                //东南下3-1-2
                AttitudeCalculationESD312(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud_ESD[i], FlyOrientationFlag);
                //东南下3-2-1
                AttitudeCalculationESD321(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud_ESD321[i], FlyOrientationFlag);
                //椭圆轨道坐标系3-1-2
                AttitudeCalculationORF312(SatPosition_GEI[i], SatVelocity_GEI[i], Target_LLA, Time[i], LoadInstall[LoadNum - 1], SatAttitud_ORF312[i], FlyOrientationFlag);
                //轨道系下姿态，转为东南系姿态3-2-1
                AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i], SatAttitud_ORF_ESD321[i]);
                //更改后欧拉角东南下321
                AttitudeCalculationEulerESD321(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitude_ESD321_Euler[i], FlyOrientationFlag);

                MissionFlag = true;
            } else {
                SatAttitud[i][0] = 0;
                SatAttitud[i][1] = 0;
                SatAttitud[i][2] = 0;
                //东南下3-1-2
                SatAttitud_ESD[i][0] = 0;
                SatAttitud_ESD[i][1] = 0;
                SatAttitud_ESD[i][2] = 0;
                //东南下3-2-1
                SatAttitud_ESD321[i][0] = 0;
                SatAttitud_ESD321[i][1] = 0;
                SatAttitud_ESD321[i][2] = 0;
                //椭圆轨道坐标系3-1-2
                SatAttitud_ORF312[i][0] = 0;
                SatAttitud_ORF312[i][1] = 0;
                SatAttitud_ORF312[i][2] = 0;
                //轨道系下姿态，转为东南系姿态3-2-1
                SatAttitud_ORF_ESD321[i][0] = 0;
                SatAttitud_ORF_ESD321[i][1] = 0;
                SatAttitud_ORF_ESD321[i][2] = 0;
                //AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i],SatPosition_LLA[i],Time[i],SatAttitud_ORF312[i],SatAttitud_ORF_ESD321[i]);
                //更改后欧拉角东南下321
                SatAttitude_ESD321_Euler[i][0]=0;
                SatAttitude_ESD321_Euler[i][1]=0;
                SatAttitude_ESD321_Euler[i][2]=0;
            }

            //只存成像任务过程中的姿态
            if (!MissionFlag) {
                continue;
            }

            if (i == 0) {
                SatAttitudVel[i][0] = 0;
                SatAttitudVel[i][1] = 0;
                SatAttitudVel[i][2] = 0;
                //东南下3-1-2
                SatAttitudVel_ESD[i][0] = 0;
                SatAttitudVel_ESD[i][1] = 0;
                SatAttitudVel_ESD[i][2] = 0;
                //东南下3-2-1
                SatAttitudVel_ESD321[i][0] = 0;
                SatAttitudVel_ESD321[i][1] = 0;
                SatAttitudVel_ESD321[i][2] = 0;
                //椭圆轨道坐标系3-1-2
                SatAttitudVel_ORF312[i][0] = 0;
                SatAttitudVel_ORF312[i][1] = 0;
                SatAttitudVel_ORF312[i][2] = 0;
                //轨道系下姿态，转为东南系姿态3-2-1
                SatAttitudVel_ORF_ESD321[i][0] = 0;
                SatAttitudVel_ORF_ESD321[i][1] = 0;
                SatAttitudVel_ORF_ESD321[i][2] = 0;
                //更改后欧拉角东南下321
                SatAttitudeVel_ESD321_Euler[i][0]=0;
                SatAttitudeVel_ESD321_Euler[i][1]=0;
                SatAttitudeVel_ESD321_Euler[i][2]=0;
            } else {
                double[][] AngRaid = {{(SatAttitud[i][0] - SatAttitud[i - 1][0]) / Step},
                        {(SatAttitud[i][1] - SatAttitud[i - 1][1]) / Step},
                        {(SatAttitud[i][2] - SatAttitud[i - 1][2]) / Step}};
                double theta1 = SatAttitud[i][0];
                double theta2 = SatAttitud[i][1];
                double theta3 = SatAttitud[i][2];
                double[][] Tran = {{1, 0, -sin(theta2)},
                        {0, cos(theta1), sin(theta1) * cos(theta2)},
                        {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                SatAttitudVel[i][0] = Vel[0][0];
                SatAttitudVel[i][1] = Vel[1][0];
                SatAttitudVel[i][2] = Vel[2][0];
                //东南下3-1-2
                double[][] AngRaid_ESD = {{(SatAttitud_ESD[i][0] - SatAttitud_ESD[i - 1][0]) / Step},
                        {(SatAttitud_ESD[i][1] - SatAttitud_ESD[i - 1][1]) / Step},
                        {(SatAttitud_ESD[i][2] - SatAttitud_ESD[i - 1][2]) / Step}};
                double theta1_ESD = SatAttitud_ESD[i][0];
                double theta2_ESD = SatAttitud_ESD[i][1];
                double theta3_ESD = SatAttitud_ESD[i][2];
                double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                        {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                        {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                SatAttitudVel_ESD[i][0] = Vel_ESD[0][0];
                SatAttitudVel_ESD[i][1] = Vel_ESD[1][0];
                SatAttitudVel_ESD[i][2] = Vel_ESD[2][0];
                //东南下3-2-1
                double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i][0] - SatAttitud_ESD321[i - 1][0]) / Step},
                        {(SatAttitud_ESD321[i][1] - SatAttitud_ESD321[i - 1][1]) / Step},
                        {(SatAttitud_ESD321[i][2] - SatAttitud_ESD321[i - 1][2]) / Step}};
                double theta1_ESD321 = SatAttitud_ESD321[i][0];
                double theta2_ESD321 = SatAttitud_ESD321[i][1];
                double theta3_ESD321 = SatAttitud_ESD321[i][2];
                double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                        {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                        {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                SatAttitudVel_ESD321[i][0] = Vel_ESD321[0][0];
                SatAttitudVel_ESD321[i][1] = Vel_ESD321[1][0];
                SatAttitudVel_ESD321[i][2] = Vel_ESD321[2][0];
                //轨道坐标系3-1-2
                double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i][0] - SatAttitud_ORF312[i - 1][0]) / Step},
                        {(SatAttitud_ORF312[i][1] - SatAttitud_ORF312[i - 1][1]) / Step},
                        {(SatAttitud_ORF312[i][2] - SatAttitud_ORF312[i - 1][2]) / Step}};
                double theta1_ORF312 = SatAttitud_ORF312[i][0];
                double theta2_ORF312 = SatAttitud_ORF312[i][1];
                double theta3_ORF312 = SatAttitud_ORF312[i][2];
                double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                        {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                        {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                SatAttitudVel_ORF312[i][0] = Vel_ORF312[0][0];
                SatAttitudVel_ORF312[i][1] = Vel_ORF312[1][0];
                SatAttitudVel_ORF312[i][2] = Vel_ORF312[2][0];
                //轨道系下姿态，转为东南系姿态3-2-1
                double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i][0] - SatAttitud_ORF_ESD321[i - 1][0]) / Step},
                        {(SatAttitud_ORF_ESD321[i][1] - SatAttitud_ORF_ESD321[i - 1][1]) / Step},
                        {(SatAttitud_ORF_ESD321[i][2] - SatAttitud_ORF_ESD321[i - 1][2]) / Step}};
                double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i][0];
                double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i][1];
                double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i][2];
                double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                        {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                        {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                SatAttitudVel_ORF_ESD321[i][0] = Vel_ORF_ESD321[0][0];
                SatAttitudVel_ORF_ESD321[i][1] = Vel_ORF_ESD321[1][0];
                SatAttitudVel_ORF_ESD321[i][2] = Vel_ORF_ESD321[2][0];
                //更改后欧拉角东南下3-2-1
                double[][] AngRaid_ESD321_Euler = {{(SatAttitude_ESD321_Euler[i][0] - SatAttitude_ESD321_Euler[i - 1][0]) / Step},
                        {(SatAttitude_ESD321_Euler[i][1] - SatAttitude_ESD321_Euler[i - 1][1]) / Step},
                        {(SatAttitude_ESD321_Euler[i][2] - SatAttitude_ESD321_Euler[i - 1][2]) / Step}};
                double theta1_ESD321_Euler = SatAttitude_ESD321_Euler[i][0];
                double theta2_ESD321_Euler = SatAttitude_ESD321_Euler[i][1];
                double theta3_ESD321_Euler = SatAttitude_ESD321_Euler[i][2];
                double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                        {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                        {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                SatAttitudeVel_ESD321_Euler[i][0] = Vel_ESD321_Euler[0][0];
                SatAttitudeVel_ESD321_Euler[i][1] = Vel_ESD321_Euler[1][0];
                SatAttitudeVel_ESD321_Euler[i][2] = Vel_ESD321_Euler[2][0];
            }

            //计算当前姿态下卫星视场四个顶点的经纬度
            double Time_UTC = 0;
            double ViewAreaPoint[][] = new double[LoadNumber][8];
            AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);


            //数据输出，计算一步传出一组姿态数据
            JsonObject jsonObject = new JsonObject();
            //北东地1-2-3
            //Document jsonObject_NED=new Document();
            jsonObject.addProperty("yaw_angle", SatAttitud[i][2]);
            jsonObject.addProperty("roll_angle", SatAttitud[i][0]);
            jsonObject.addProperty("pitch_angle", SatAttitud[i][1]);
            jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i][2]);
            jsonObject.addProperty("V_roll_angle", SatAttitudVel[i][0]);
            jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i][1]);
            //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
            //东南下3-1-2
            JsonObject jsonObject_ESD = new JsonObject();
            jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i][2]);
            jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i][0]);
            jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i][1]);
            jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i][2]);
            jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i][0]);
            jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i][1]);
            jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
            //东南下3-2-1
            JsonObject jsonObject_ESD321 = new JsonObject();
            jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i][2]);
            jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i][0]);
            jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i][1]);
            jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i][2]);
            jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i][0]);
            jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i][1]);
            jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
            //轨道坐标系3-1-2
            JsonObject jsonObject_ORF312 = new JsonObject();
            jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i][2]);
            jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i][0]);
            jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i][1]);
            jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i][2]);
            jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i][0]);
            jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i][1]);
            jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

            //东南系计算的姿态计算为轨道系
            JsonObject jsonObject_ESD_ORF312 = new JsonObject();
            jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i][2]);
            jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i][0]);
            jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i][1]);
            jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i][2]);
            jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i][0]);
            jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i][1]);
            jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

            //更改后欧拉角东南系321转序
            JsonObject jsonObject_ESD312_Euler = new JsonObject();
            jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitude_ESD321_Euler[i][2]);
            jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitude_ESD321_Euler[i][0]);
            jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitude_ESD321_Euler[i][1]);
            jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudeVel_ESD321_Euler[i][2]);
            jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudeVel_ESD321_Euler[i][0]);
            jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudeVel_ESD321_Euler[i][1]);
            jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

            //视场顶点，左上、右下
            JsonArray jsonObject_ViewArea = new JsonArray();
            JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
            jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
            jsonObject_ViewArea_Load1.addProperty("load_number", 1);
            jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
            jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
            jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
            jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
            jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
            JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
            jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
            jsonObject_ViewArea_Load2.addProperty("load_number", 2);
            jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
            jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
            jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
            jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
            jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
            JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
            jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
            jsonObject_ViewArea_Load3.addProperty("load_number", 3);
            jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
            jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
            jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
            jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
            jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
            JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
            jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
            jsonObject_ViewArea_Load4.addProperty("load_number", 3);
            jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
            jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
            jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
            jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
            jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

            jsonObject.add("payload_view_area", jsonObject_ViewArea);

            if (MissionFlag) {
                jsonObject.addProperty("tag", "1");
            } else {
                jsonObject.addProperty("tag", "0");
            }

            Document doc = Document.parse(jsonObject.toString());
            doc.append("time_point", Time_Point[i]);
            os.add(doc);

            cnt++;

            if (os.size() > 10000) {
                normal_attitude.insertMany(os);
                os.clear();
                cnt = 0;
            }
        }
        if (os.size() > 0) {
            normal_attitude.insertMany(os);
            os.clear();
        }

        mongoClient.close();

        if (MissionStarDocument.size() > 0 || MissionLimbDocument.size() > 0 || MissionCalibrateDocument.size()>0) {
            StarLimbAttitudeCalculation(MissionStarDocument, MissionLimbDocument,MissionCalibrateDocument, Time, SatPosition_GEI, SatVelocity_GEI, SatPosition_LLA, Time_Point,OrbitDataCount);
        }

        //载荷
        mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        mongoDatabase = mongoClient.getDatabase("temp");
        //任务规划结果数据传出
        for (Document document:MissionAll) {
            try {
                if (document.containsKey("available_window") && document.get("available_window")!=null) {
                    ArrayList<Document> AvailableWindow = (ArrayList<Document>) document.get("available_window");
                    ArrayList<Document> AvailableWindownew=new ArrayList<>();
                    for (Document window:AvailableWindow) {
                        Document temp=window;
                        int loadnumber=window.getInteger("load_number");
                        temp.append("load_number_int",loadnumber);
                        if (loadnumber == 5) {
                            temp.append("load_number","1,2");
                        }else {
                            temp.append("load_number",Integer.toString(loadnumber));
                        }
                        AvailableWindownew.add(temp);
                    }
                    document.append("available_window",AvailableWindownew);
                }
                if (document.containsKey("image_window") && document.get("image_window")!=null) {
                    ArrayList<Document> AvailableWindow = (ArrayList<Document>) document.get("image_window");
                    ArrayList<Document> AvailableWindownew=new ArrayList<>();
                    for (Document window:AvailableWindow) {
                        Document temp=window;
                        int loadnumber=window.getInteger("load_number");
                        temp.append("load_number_int",loadnumber);
                        if (loadnumber == 5) {
                            temp.append("load_number","1,2");
                        }else {
                            temp.append("load_number",Integer.toString(loadnumber));
                        }
                        AvailableWindownew.add(temp);
                    }
                    document.append("image_window",AvailableWindownew);
                }
                Document modifiers = new Document();
                modifiers.append("$set", document);
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.updateOne(new Document("mission_number", document.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

        }
        mongoClient.close();
    }

    //姿态角计算
    private static void AttitudeCalculation(double[] SatPosition_GEI, double[] SatVelocity_GEI, double[] Target_LLA, double[] Time, double[] ViewInstall, double[] Attitude) {
        double[] SatelliteTime = new double[6];
        double[][] BToS;
        double[][] BToO = new double[3][3];
        double[][] SToO = new double[3][3];
        double[] x_sensor;
        double[] y_sensor;
        double[] z_sensor = new double[3];
        double[] Target_GEI = new double[3];
        double[] Error_GEI = new double[3];
        double[] Error_ORF = new double[3];
        double[] Position_ECEF = new double[3];
        double[] cross_xyz = new double[3];
        double flag;
        double sy;
        double x, y, z;
        for (int i = 0; i < 6; i++) {
            SatelliteTime[i] = Time[i];
        }
        double Time_JD = JD(SatelliteTime);
        double[] Satllite_GEI = {SatPosition_GEI[0], SatPosition_GEI[1], SatPosition_GEI[2]};
        double[] SatVelocity_GEI1 = {SatVelocity_GEI[0], SatVelocity_GEI[1], SatVelocity_GEI[2]};
        LLAToECEF(Target_LLA, Position_ECEF);//Position_ECEF
        ECEFToICRS(Time_JD, Position_ECEF, Target_GEI);//Target_GEI
        for (int i = 0; i < 3; i++) {
            Error_GEI[i] = Target_GEI[i] - Satllite_GEI[i];
        }
        GEIToORF(Satllite_GEI, SatVelocity_GEI1, Target_GEI, Error_ORF);//Error_ORF
        //(double SatPosition_GEI[],double SatVelocity_GEI[],double Position_GEI[],double Position_ORF[]
        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Error_ORF[i] / sqrt(Error_ORF[0] * Error_ORF[0] + Error_ORF[1] * Error_ORF[1] + Error_ORF[2] * Error_ORF[2]);
        }
        cross_xyz[0] = 1;
        cross_xyz[1] = 0;
        cross_xyz[2] = 0;
        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result

        //欧拉角转序位1-2-3
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[1][0] * BToO[1][0]);
        if (sy < pow(10, -6))
            flag = 1;
        else
            flag = 0;
        if (flag == 0) {
            //atan2(X,Y)的含义和atan(X/Y)的含义是一样的。
            //x = atan2(BToO[2][1], BToO[2][2]);
            //y = atan2(-BToO[2][0], sy);
            //z = atan2(BToO[1][0], BToO[0][0]);
            x = atan(BToO[2][1] / BToO[2][2]);
            y = atan(-BToO[2][0] / sy);
            z = atan(BToO[1][0] / BToO[0][0]);
        } else {
            //x = atan2(-BToO[1][2], BToO[1][1]);
            //y = atan2(-BToO[2][0], sy);
            //z = 0;
            x = atan(-BToO[1][2] / BToO[1][1]);
            y = atan(-BToO[2][0] / sy);
            z = 0;
        }

        Attitude[0] = x;
        Attitude[1] = y;
        Attitude[2] = z;
    }

    //临边观测、恒星定标模式姿态角计算
    private static void StarLimbAttitudeCalculation_old(ArrayList<Document> MissionStarDocument, ArrayList<Document> MissionLimbDocument, ArrayList<Document> MissionCalibrateDocument, double[][] Time, double[][] SatPosition_GEI, double[][] SatVelocity_GEI, double[][] SatPosition_LLA, Date[] Time_Point) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");
        ArrayList<Document> os = new ArrayList<>();
        for (Document document : MissionStarDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double MissionAng = Double.parseDouble(document.get("scan_roll_bias").toString());
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[OrbitNum][3];
                            double[][] SatAttitudVel = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF312 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[OrbitNum][3];

                            //姿态计算，更改后欧拉角3-2-1z转序，东南下
                            double[][] SatAttitud_ESD321_Euler=new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321_Euler=new double[OrbitNum][3];

                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                double NowTime_JD = JD(Time[i]);
                                //太阳矢量
                                double[] r_sun = new double[3];//惯性系下太阳矢量
                                double[] su = new double[2];
                                double rd_sun = Sun(NowTime_JD, r_sun, su);
                                //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                                boolean FlyOrientationFlag = true;
                                double[] r_sun_n = new double[]{r_sun[0] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[1] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[2] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2])};
                                double[] v_sat_n = new double[]{SatVelocity_GEI[i][0] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][1] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][2] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2])};
                                double CosTheta_SunVel = (r_sun_n[0] * v_sat_n[0] + r_sun_n[1] * v_sat_n[1] + r_sun_n[2] * v_sat_n[2]) /
                                        (sqrt(r_sun_n[0] * r_sun_n[0] + r_sun_n[1] * r_sun_n[1] + r_sun_n[2] * r_sun_n[2]) * sqrt(v_sat_n[0] * v_sat_n[0] + v_sat_n[1] * v_sat_n[1] + v_sat_n[2] * v_sat_n[2]));
                                if (CosTheta_SunVel > 0) {
                                    FlyOrientationFlag = false;
                                } else {
                                    FlyOrientationFlag = true;
                                }

                                if (FlyOrientationFlag) {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = PI;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = PI;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = PI;
                                }
                                if (i == MissionWindow_int[0]) {
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i - MissionWindow_int[0]][0] - SatAttitud[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][1] - SatAttitud[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][2] - SatAttitud[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i - MissionWindow_int[0]][0];
                                    double theta2 = SatAttitud[i - MissionWindow_int[0]][1];
                                    double theta3 = SatAttitud[i - MissionWindow_int[0]][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = Vel[0][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = Vel[1][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i - MissionWindow_int[0]][0] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][1] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][2] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][0];
                                    double theta2_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][1];
                                    double theta3_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i - MissionWindow_int[0]][0] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][1] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][2] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角，东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i - MissionWindow_int[0]], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i - MissionWindow_int[0]][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i - MissionWindow_int[0]][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                jsonObject.addProperty("tag", "1");

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                }
                            }

                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        for (Document document : MissionLimbDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double MissionAng = 0;
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[OrbitNum][3];
                            double[][] SatAttitudVel = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF312 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[OrbitNum][3];

                            //姿态计算，更改后欧拉角3-2-1z转序，东南下
                            double[][] SatAttitud_ESD321_Euler=new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321_Euler=new double[OrbitNum][3];

                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                if (i == MissionWindow_int[0]) {
                                    MissionAng = LimbAngCalculation(SatPosition_GEI[i]);
                                }

                                //北东地1-2-3
                                SatAttitud[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud[i - MissionWindow_int[0]][2] = 0;
                                //东南下3-1-2
                                SatAttitud_ESD[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD[i - MissionWindow_int[0]][2] = 0;
                                //东南下3-2-1
                                SatAttitud_ESD321[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD321[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD321[i - MissionWindow_int[0]][2] = 0;
                                //椭圆轨道坐标系3-1-2
                                SatAttitud_ORF312[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ORF312[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ORF312[i - MissionWindow_int[0]][2] = 0;
                                //轨道系下姿态，转为东南系姿态3-2-1
                                AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                //更改后欧拉角，东南下3-2-1
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;

                                if (i == MissionWindow_int[0]) {
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i - MissionWindow_int[0]][0] - SatAttitud[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][1] - SatAttitud[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][2] - SatAttitud[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i - MissionWindow_int[0]][0];
                                    double theta2 = SatAttitud[i - MissionWindow_int[0]][1];
                                    double theta3 = SatAttitud[i - MissionWindow_int[0]][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = Vel[0][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = Vel[1][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i - MissionWindow_int[0]][0] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][1] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][2] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][0];
                                    double theta2_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][1];
                                    double theta3_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i - MissionWindow_int[0]][0] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][1] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][2] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角，东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i - MissionWindow_int[0]], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i - MissionWindow_int[0]][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i - MissionWindow_int[0]][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                jsonObject.addProperty("tag", "1");

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                }
                            }

                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        for (Document document : MissionCalibrateDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double MissionAng = 0;
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[OrbitNum][3];
                            double[][] SatAttitudVel = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF312 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[OrbitNum][3];

                            //姿态计算，更改后欧拉角3-2-1z转序，东南下
                            double[][] SatAttitud_ESD321_Euler=new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321_Euler=new double[OrbitNum][3];

                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                double NowTime_JD = JD(Time[i]);
                                //太阳矢量
                                double[] r_sun = new double[3];//惯性系下太阳矢量
                                double[] su = new double[2];
                                double rd_sun = Sun(NowTime_JD, r_sun, su);
                                //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                                boolean FlyOrientationFlag = true;
                                double[] r_sun_n = new double[]{r_sun[0] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[1] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[2] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2])};
                                double[] v_sat_n = new double[]{SatVelocity_GEI[i][0] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][1] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][2] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2])};
                                double CosTheta_SunVel = (r_sun_n[0] * v_sat_n[0] + r_sun_n[1] * v_sat_n[1] + r_sun_n[2] * v_sat_n[2]) /
                                        (sqrt(r_sun_n[0] * r_sun_n[0] + r_sun_n[1] * r_sun_n[1] + r_sun_n[2] * r_sun_n[2]) * sqrt(v_sat_n[0] * v_sat_n[0] + v_sat_n[1] * v_sat_n[1] + v_sat_n[2] * v_sat_n[2]));
                                if (CosTheta_SunVel > 0) {
                                    FlyOrientationFlag = false;
                                } else {
                                    FlyOrientationFlag = true;
                                }

                                if (FlyOrientationFlag) {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = PI;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = PI;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = PI;
                                }
                                if (i == MissionWindow_int[0]) {
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i - MissionWindow_int[0]][0] - SatAttitud[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][1] - SatAttitud[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][2] - SatAttitud[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i - MissionWindow_int[0]][0];
                                    double theta2 = SatAttitud[i - MissionWindow_int[0]][1];
                                    double theta3 = SatAttitud[i - MissionWindow_int[0]][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = Vel[0][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = Vel[1][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i - MissionWindow_int[0]][0] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][1] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][2] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][0];
                                    double theta2_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][1];
                                    double theta3_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i - MissionWindow_int[0]][0] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][1] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][2] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角，东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i - MissionWindow_int[0]], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i - MissionWindow_int[0]][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i - MissionWindow_int[0]][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitud_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitud_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitud_ESD321_Euler[i][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudVel_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudVel_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudVel_ESD321_Euler[i][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                jsonObject.addProperty("tag", "1");

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                }
                            }

                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        mongoClient.close();
    }

    //临边观测、恒星定标模式姿态角计算,2020-12-8修改恒星定标提出目标点为经纬高
    private static void StarLimbAttitudeCalculation(ArrayList<Document> MissionStarDocument, ArrayList<Document> MissionLimbDocument, ArrayList<Document> MissionCalibrateDocument, double[][] Time, double[][] SatPosition_GEI, double[][] SatVelocity_GEI, double[][] SatPosition_LLA, Date[] Time_Point,long OrbitDataCount) {
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");
        ArrayList<Document> os = new ArrayList<>();
        for (Document document : MissionStarDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double j2000x=Double.parseDouble(document.get("j2000x").toString())*1000;
                    double j2000y=Double.parseDouble(document.get("j2000y").toString())*1000;
                    double j2000z=Double.parseDouble(document.get("j2000z").toString())*1000;
                    double[] Target_GEI=new double[]{j2000x,j2000y,j2000z};
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int LoadNum = 0;
                            LoadNum=document1.getInteger("load_number");
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;
                            double StarTime_JD = JD(MissionStarTime_iList);
                            double StopTime_JD = JD(MissionStopTime_iList);

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudVel = new double[(int) OrbitDataCount][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudVel_ESD = new double[(int) OrbitDataCount][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudVel_ESD321 = new double[(int) OrbitDataCount][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudVel_ORF312 = new double[(int) OrbitDataCount][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[(int) OrbitDataCount][3];

                            //姿态计算，更改后欧拉角3-2-1转序，东南下
                            double[][] SatAttitude_ESD321_Euler=new double[(int) OrbitDataCount][3];
                            double[][] SatAttitudeVel_ESD321_Euler=new double[(int) OrbitDataCount][3];
                            long cnt = 0;
                            for (int i = 0; i < OrbitDataCount; i++) {
                                boolean MissionFlag = false;
                                double Mission_FLag = 0;
                                double NowTime_JD = JD(Time[i]);
                                if (NowTime_JD >= StarTime_JD && NowTime_JD <= StopTime_JD) {
                                    Mission_FLag = 1;
                                }

                                double[] Target_LLA=new double[3];
                                ECI_ECEF(NowTime_JD, Target_GEI, Target_LLA);
                                Target_LLA[0]=Target_LLA[0]*180.0/PI;
                                Target_LLA[1]=Target_LLA[1]*180.0/PI;

                                //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                                boolean FlyOrientationFlag = true;

                                if (Mission_FLag == 1) {
                                    //北东地1-2-3
                                    AttitudeCalculationTest(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud[i], FlyOrientationFlag);
                                    //东南下3-1-2
                                    AttitudeCalculationESD312(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud_ESD[i], FlyOrientationFlag);
                                    //东南下3-2-1
                                    AttitudeCalculationESD321(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitud_ESD321[i], FlyOrientationFlag);
                                    //椭圆轨道坐标系3-1-2
                                    AttitudeCalculationORF312(SatPosition_GEI[i], SatVelocity_GEI[i], Target_LLA, Time[i], LoadInstall[LoadNum - 1], SatAttitud_ORF312[i], FlyOrientationFlag);
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i], SatAttitud_ORF_ESD321[i]);
                                    //更改后欧拉角东南下321
                                    AttitudeCalculationEulerESD321(SatPosition_LLA[i], Target_LLA, LoadInstall[LoadNum - 1], SatAttitude_ESD321_Euler[i], FlyOrientationFlag);

                                    MissionFlag = true;
                                } else {
                                    SatAttitud[i][0] = 0;
                                    SatAttitud[i][1] = 0;
                                    SatAttitud[i][2] = 0;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i][0] = 0;
                                    SatAttitud_ESD[i][1] = 0;
                                    SatAttitud_ESD[i][2] = 0;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i][0] = 0;
                                    SatAttitud_ESD321[i][1] = 0;
                                    SatAttitud_ESD321[i][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i][0] = 0;
                                    SatAttitud_ORF312[i][1] = 0;
                                    SatAttitud_ORF312[i][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitud_ORF_ESD321[i][0] = 0;
                                    SatAttitud_ORF_ESD321[i][1] = 0;
                                    SatAttitud_ORF_ESD321[i][2] = 0;
                                    //AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i],SatPosition_LLA[i],Time[i],SatAttitud_ORF312[i],SatAttitud_ORF_ESD321[i]);
                                    //更改后欧拉角东南下321
                                    SatAttitude_ESD321_Euler[i][0]=0;
                                    SatAttitude_ESD321_Euler[i][1]=0;
                                    SatAttitude_ESD321_Euler[i][2]=0;
                                }

                                //只存成像任务过程中的姿态
                                if (!MissionFlag) {
                                    continue;
                                }

                                if (i == 0) {
                                    SatAttitudVel[i][0] = 0;
                                    SatAttitudVel[i][1] = 0;
                                    SatAttitudVel[i][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i][0] = 0;
                                    SatAttitudVel_ESD[i][1] = 0;
                                    SatAttitudVel_ESD[i][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i][0] = 0;
                                    SatAttitudVel_ESD321[i][1] = 0;
                                    SatAttitudVel_ESD321[i][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i][0] = 0;
                                    SatAttitudVel_ORF312[i][1] = 0;
                                    SatAttitudVel_ORF312[i][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i][2] = 0;
                                    //更改后欧拉角东南下321
                                    SatAttitudeVel_ESD321_Euler[i][0]=0;
                                    SatAttitudeVel_ESD321_Euler[i][1]=0;
                                    SatAttitudeVel_ESD321_Euler[i][2]=0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i][0] - SatAttitud[i - 1][0]) / Step},
                                            {(SatAttitud[i][1] - SatAttitud[i - 1][1]) / Step},
                                            {(SatAttitud[i][2] - SatAttitud[i - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i][0];
                                    double theta2 = SatAttitud[i][1];
                                    double theta3 = SatAttitud[i][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i][0] = Vel[0][0];
                                    SatAttitudVel[i][1] = Vel[1][0];
                                    SatAttitudVel[i][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i][0] - SatAttitud_ESD[i - 1][0]) / Step},
                                            {(SatAttitud_ESD[i][1] - SatAttitud_ESD[i - 1][1]) / Step},
                                            {(SatAttitud_ESD[i][2] - SatAttitud_ESD[i - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i][0];
                                    double theta2_ESD = SatAttitud_ESD[i][1];
                                    double theta3_ESD = SatAttitud_ESD[i][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i][0] - SatAttitud_ESD321[i - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i][1] - SatAttitud_ESD321[i - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i][2] - SatAttitud_ESD321[i - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i][0] - SatAttitud_ORF312[i - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i][1] - SatAttitud_ORF312[i - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i][2] - SatAttitud_ORF312[i - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i][0] - SatAttitud_ORF_ESD321[i - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i][1] - SatAttitud_ORF_ESD321[i - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i][2] - SatAttitud_ORF_ESD321[i - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitude_ESD321_Euler[i][0] - SatAttitude_ESD321_Euler[i - 1][0]) / Step},
                                            {(SatAttitude_ESD321_Euler[i][1] - SatAttitude_ESD321_Euler[i - 1][1]) / Step},
                                            {(SatAttitude_ESD321_Euler[i][2] - SatAttitude_ESD321_Euler[i - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitude_ESD321_Euler[i][0];
                                    double theta2_ESD321_Euler = SatAttitude_ESD321_Euler[i][1];
                                    double theta3_ESD321_Euler = SatAttitude_ESD321_Euler[i][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudeVel_ESD321_Euler[i][0] = Vel_ESD321_Euler[0][0];
                                    SatAttitudeVel_ESD321_Euler[i][1] = Vel_ESD321_Euler[1][0];
                                    SatAttitudeVel_ESD321_Euler[i][2] = Vel_ESD321_Euler[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitude_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitude_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitude_ESD321_Euler[i][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudeVel_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudeVel_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudeVel_ESD321_Euler[i][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                if (MissionFlag) {
                                    jsonObject.addProperty("tag", "1");
                                } else {
                                    jsonObject.addProperty("tag", "0");
                                }

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                cnt++;

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                    cnt = 0;
                                }
                            }
                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        for (Document document : MissionLimbDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double MissionAng = 0;
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[OrbitNum][3];
                            double[][] SatAttitudVel = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF312 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[OrbitNum][3];

                            //姿态计算，更改后欧拉角3-2-1z转序，东南下
                            double[][] SatAttitud_ESD321_Euler=new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321_Euler=new double[OrbitNum][3];

                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                if (i == MissionWindow_int[0]) {
                                    MissionAng = LimbAngCalculation(SatPosition_GEI[i]);
                                }

                                //北东地1-2-3
                                SatAttitud[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud[i - MissionWindow_int[0]][2] = 0;
                                //东南下3-1-2
                                SatAttitud_ESD[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD[i - MissionWindow_int[0]][2] = 0;
                                //东南下3-2-1
                                SatAttitud_ESD321[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD321[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD321[i - MissionWindow_int[0]][2] = 0;
                                //椭圆轨道坐标系3-1-2
                                SatAttitud_ORF312[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ORF312[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ORF312[i - MissionWindow_int[0]][2] = 0;
                                //轨道系下姿态，转为东南系姿态3-2-1
                                AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                //更改后欧拉角，东南下3-2-1
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = -MissionAng;
                                SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;

                                if (i == MissionWindow_int[0]) {
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i - MissionWindow_int[0]][0] - SatAttitud[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][1] - SatAttitud[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][2] - SatAttitud[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i - MissionWindow_int[0]][0];
                                    double theta2 = SatAttitud[i - MissionWindow_int[0]][1];
                                    double theta3 = SatAttitud[i - MissionWindow_int[0]][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = Vel[0][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = Vel[1][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i - MissionWindow_int[0]][0] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][1] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][2] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][0];
                                    double theta2_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][1];
                                    double theta3_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i - MissionWindow_int[0]][0] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][1] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][2] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角，东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i - MissionWindow_int[0]], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i - MissionWindow_int[0]][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i - MissionWindow_int[0]][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitud_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudVel_ESD321_Euler[i- MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                jsonObject.addProperty("tag", "1");

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                }
                            }

                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        for (Document document : MissionCalibrateDocument) {
            try {
                if (document.containsKey("image_window")) {
                    ArrayList<Document> MissionwWindow = (ArrayList<Document>) document.get("image_window");
                    double MissionAng = 0;
                    if (MissionwWindow.size() > 0) {
                        for (Document document1 : MissionwWindow) {
                            //读取任务时间
                            Date window_start_time = document1.getDate("start_time");
                            double[] MissionStarTime_iList = DateToDouble(window_start_time);
                            Date window_stop_time = document1.getDate("end_time");
                            double[] MissionStopTime_iList = DateToDouble(window_stop_time);
                            int[] MissionWindow_int = new int[2];
                            MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Time[0])) * (24 * 60 * 60));
                            int OrbitNum = MissionWindow_int[1] - MissionWindow_int[0] + 1;

                            //姿态计算，欧拉角1-2-3转序
                            double[][] SatAttitud = new double[OrbitNum][3];
                            double[][] SatAttitudVel = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，东南下
                            double[][] SatAttitud_ESD = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，东南下
                            double[][] SatAttitud_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-1-2转序，椭圆轨道坐标系
                            double[][] SatAttitud_ORF312 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF312 = new double[OrbitNum][3];

                            //姿态计算，欧拉角3-2-1转序，椭圆轨道坐标系下转到东南系
                            double[][] SatAttitud_ORF_ESD321 = new double[OrbitNum][3];
                            double[][] SatAttitudVel_ORF_ESD321 = new double[OrbitNum][3];

                            //姿态计算，更改后欧拉角3-2-1z转序，东南下
                            double[][] SatAttitud_ESD321_Euler=new double[OrbitNum][3];
                            double[][] SatAttitudVel_ESD321_Euler=new double[OrbitNum][3];

                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                double NowTime_JD = JD(Time[i]);
                                //太阳矢量
                                double[] r_sun = new double[3];//惯性系下太阳矢量
                                double[] su = new double[2];
                                double rd_sun = Sun(NowTime_JD, r_sun, su);
                                //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                                boolean FlyOrientationFlag = true;
                                double[] r_sun_n = new double[]{r_sun[0] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[1] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]),
                                        r_sun[2] / sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2])};
                                double[] v_sat_n = new double[]{SatVelocity_GEI[i][0] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][1] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2]),
                                        SatVelocity_GEI[i][2] / sqrt(SatVelocity_GEI[i][0] * SatVelocity_GEI[i][0] + SatVelocity_GEI[i][1] * SatVelocity_GEI[i][1] + SatVelocity_GEI[i][2] * SatVelocity_GEI[i][2])};
                                double CosTheta_SunVel = (r_sun_n[0] * v_sat_n[0] + r_sun_n[1] * v_sat_n[1] + r_sun_n[2] * v_sat_n[2]) /
                                        (sqrt(r_sun_n[0] * r_sun_n[0] + r_sun_n[1] * r_sun_n[1] + r_sun_n[2] * r_sun_n[2]) * sqrt(v_sat_n[0] * v_sat_n[0] + v_sat_n[1] * v_sat_n[1] + v_sat_n[2] * v_sat_n[2]));
                                if (CosTheta_SunVel > 0) {
                                    FlyOrientationFlag = false;
                                } else {
                                    FlyOrientationFlag = true;
                                }

                                if (FlyOrientationFlag) {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    //北东地1-2-3
                                    SatAttitud[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-1-2
                                    SatAttitud_ESD[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD[i - MissionWindow_int[0]][2] = PI;
                                    //东南下3-2-1
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321[i - MissionWindow_int[0]][2] = PI;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ORF312[i - MissionWindow_int[0]][2] = PI;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    AttitudeCalculationORF312ToESD(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], Time[i], SatAttitud_ORF312[i - MissionWindow_int[0]], SatAttitud_ORF_ESD321[i - MissionWindow_int[0]]);
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] = -MissionAng;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] = PI;
                                }
                                if (i == MissionWindow_int[0]) {
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-1-2
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = 0;
                                    //东南下3-2-1
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //椭圆轨道坐标系3-1-2
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = 0;
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = 0;
                                    //更改后欧拉角，东南下3-2-1
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = 0;
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = 0;
                                } else {
                                    double[][] AngRaid = {{(SatAttitud[i - MissionWindow_int[0]][0] - SatAttitud[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][1] - SatAttitud[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud[i - MissionWindow_int[0]][2] - SatAttitud[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1 = SatAttitud[i - MissionWindow_int[0]][0];
                                    double theta2 = SatAttitud[i - MissionWindow_int[0]][1];
                                    double theta3 = SatAttitud[i - MissionWindow_int[0]][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudVel[i - MissionWindow_int[0]][0] = Vel[0][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][1] = Vel[1][0];
                                    SatAttitudVel[i - MissionWindow_int[0]][2] = Vel[2][0];
                                    //东南下3-1-2
                                    double[][] AngRaid_ESD = {{(SatAttitud_ESD[i - MissionWindow_int[0]][0] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][1] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD[i - MissionWindow_int[0]][2] - SatAttitud_ESD[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][0];
                                    double theta2_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][1];
                                    double theta3_ESD = SatAttitud_ESD[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD = {{1, 0, -sin(theta2_ESD)},
                                            {0, cos(theta1_ESD), sin(theta1_ESD) * cos(theta2_ESD)},
                                            {0, -sin(theta1_ESD), cos(theta1_ESD) * cos(theta2_ESD)}};
                                    double[][] Vel_ESD = MatrixMultiplication(Tran_ESD, AngRaid_ESD);
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][0] = Vel_ESD[0][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][1] = Vel_ESD[1][0];
                                    SatAttitudVel_ESD[i - MissionWindow_int[0]][2] = Vel_ESD[2][0];
                                    //东南下3-2-1
                                    double[][] AngRaid_ESD321 = {{(SatAttitud_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321 = SatAttitud_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321 = {{1, 0, -sin(theta2_ESD321)},
                                            {0, cos(theta1_ESD321), sin(theta1_ESD321) * cos(theta2_ESD321)},
                                            {0, -sin(theta1_ESD321), cos(theta1_ESD321) * cos(theta2_ESD321)}};
                                    double[][] Vel_ESD321 = MatrixMultiplication(Tran_ESD321, AngRaid_ESD321);
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                    //轨道坐标系3-1-2
                                    double[][] AngRaid_ORF312 = {{(SatAttitud_ORF312[i - MissionWindow_int[0]][0] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][1] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF312[i - MissionWindow_int[0]][2] - SatAttitud_ORF312[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][0];
                                    double theta2_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][1];
                                    double theta3_ORF312 = SatAttitud_ORF312[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF312 = {{1, 0, -sin(theta2_ORF312)},
                                            {0, cos(theta1_ORF312), sin(theta1_ORF312) * cos(theta2_ORF312)},
                                            {0, -sin(theta1_ORF312), cos(theta1_ORF312) * cos(theta2_ORF312)}};
                                    double[][] Vel_ORF312 = MatrixMultiplication(Tran_ORF312, AngRaid_ORF312);
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][0] = Vel_ORF312[0][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][1] = Vel_ORF312[1][0];
                                    SatAttitudVel_ORF312[i - MissionWindow_int[0]][2] = Vel_ORF312[2][0];
                                    //轨道系下姿态，转为东南系姿态3-2-1
                                    double[][] AngRaid_ORF_ESD321 = {{(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2] - SatAttitud_ORF_ESD321[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0];
                                    double theta2_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1];
                                    double theta3_ORF_ESD321 = SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ORF_ESD321 = {{1, 0, -sin(theta2_ORF_ESD321)},
                                            {0, cos(theta1_ORF_ESD321), sin(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)},
                                            {0, -sin(theta1_ORF_ESD321), cos(theta1_ORF_ESD321) * cos(theta2_ORF_ESD321)}};
                                    double[][] Vel_ORF_ESD321 = MatrixMultiplication(Tran_ORF_ESD321, AngRaid_ORF_ESD321);
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][0] = Vel_ORF_ESD321[0][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][1] = Vel_ORF_ESD321[1][0];
                                    SatAttitudVel_ORF_ESD321[i - MissionWindow_int[0]][2] = Vel_ORF_ESD321[2][0];
                                    //更改后欧拉角，东南下3-2-1
                                    double[][] AngRaid_ESD321_Euler = {{(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][0]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][1]) / Step},
                                            {(SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2] - SatAttitud_ESD321_Euler[i - MissionWindow_int[0] - 1][2]) / Step}};
                                    double theta1_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][0];
                                    double theta2_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][1];
                                    double theta3_ESD321_Euler = SatAttitud_ESD321_Euler[i - MissionWindow_int[0]][2];
                                    double[][] Tran_ESD321_Euler = {{1, 0, -sin(theta2_ESD321_Euler)},
                                            {0, cos(theta1_ESD321_Euler), sin(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)},
                                            {0, -sin(theta1_ESD321_Euler), cos(theta1_ESD321_Euler) * cos(theta2_ESD321_Euler)}};
                                    double[][] Vel_ESD321_Euler = MatrixMultiplication(Tran_ESD321_Euler, AngRaid_ESD321_Euler);
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][0] = Vel_ESD321[0][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][1] = Vel_ESD321[1][0];
                                    SatAttitudVel_ESD321_Euler[i - MissionWindow_int[0]][2] = Vel_ESD321[2][0];
                                }

                                //计算当前姿态下卫星视场四个顶点的经纬度
                                double Time_UTC = 0;
                                double ViewAreaPoint[][] = new double[LoadNumber][8];
                                AttitudeViewCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], SatPosition_LLA[i], SatAttitud_ESD[i - MissionWindow_int[0]], LoadInstall, LoadViewAng, LoadNumber, Time[i], Time_UTC, ViewAreaPoint);

                                //数据输出，计算一步传出一组姿态数据
                                JsonObject jsonObject = new JsonObject();
                                //北东地1-2-3
                                //Document jsonObject_NED=new Document();
                                jsonObject.addProperty("yaw_angle", SatAttitud[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("roll_angle", SatAttitud[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("pitch_angle", SatAttitud[i - MissionWindow_int[0]][1]);
                                jsonObject.addProperty("V_yaw_angle", SatAttitudVel[i - MissionWindow_int[0]][2]);
                                jsonObject.addProperty("V_roll_angle", SatAttitudVel[i - MissionWindow_int[0]][0]);
                                jsonObject.addProperty("V_pitch_angle", SatAttitudVel[i - MissionWindow_int[0]][1]);
                                //jsonObject.append("Attitude_NorthEastDown_123", jsonObject_NED);
                                //东南下3-1-2
                                JsonObject jsonObject_ESD = new JsonObject();
                                jsonObject_ESD.addProperty("yaw_angle", SatAttitud_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("roll_angle", SatAttitud_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("pitch_angle", SatAttitud_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD.addProperty("V_yaw_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD.addProperty("V_roll_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD.addProperty("V_pitch_angle", SatAttitudVel_ESD[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_312", jsonObject_ESD);
                                //东南下3-2-1
                                JsonObject jsonObject_ESD321 = new JsonObject();
                                jsonObject_ESD321.addProperty("yaw_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("roll_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("pitch_angle", SatAttitud_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD321.addProperty("V_yaw_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD321.addProperty("V_roll_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD321.addProperty("V_pitch_angle", SatAttitudVel_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_EastSouthDown_321", jsonObject_ESD321);
                                //轨道坐标系3-1-2
                                JsonObject jsonObject_ORF312 = new JsonObject();
                                jsonObject_ORF312.addProperty("yaw_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("roll_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("pitch_angle", SatAttitud_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject_ORF312.addProperty("V_yaw_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][2]);
                                jsonObject_ORF312.addProperty("V_roll_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][0]);
                                jsonObject_ORF312.addProperty("V_pitch_angle", SatAttitudVel_ORF312[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReference_312", jsonObject_ORF312);

                                //东南系计算的姿态计算为轨道系
                                JsonObject jsonObject_ESD_ORF312 = new JsonObject();
                                jsonObject_ESD_ORF312.addProperty("yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject_ESD_ORF312.addProperty("V_yaw_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][2]);
                                jsonObject_ESD_ORF312.addProperty("V_roll_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][0]);
                                jsonObject_ESD_ORF312.addProperty("V_pitch_angle", SatAttitud_ORF_ESD321[i - MissionWindow_int[0]][1]);
                                jsonObject.add("Attitude_OrbitReferenceToEastSouthDown_321", jsonObject_ESD_ORF312);

                                //更改后欧拉角东南系321转序
                                JsonObject jsonObject_ESD312_Euler = new JsonObject();
                                jsonObject_ESD312_Euler.addProperty("yaw_angle", SatAttitud_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("roll_angle", SatAttitud_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("pitch_angle", SatAttitud_ESD321_Euler[i][1]);
                                jsonObject_ESD312_Euler.addProperty("V_yaw_angle", SatAttitudVel_ESD321_Euler[i][2]);
                                jsonObject_ESD312_Euler.addProperty("V_roll_angle", SatAttitudVel_ESD321_Euler[i][0]);
                                jsonObject_ESD312_Euler.addProperty("V_pitch_angle", SatAttitudVel_ESD321_Euler[i][1]);
                                jsonObject.add("Attitude_EulerAngle_EastSouthDown_321", jsonObject_ESD312_Euler);

                                //视场顶点，左上、右下
                                JsonArray jsonObject_ViewArea = new JsonArray();
                                JsonObject jsonObject_ViewArea_Load1 = new JsonObject();
                                jsonObject_ViewArea_Load1.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load1.addProperty("load_number", 1);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lon", ViewAreaPoint[0][0]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_left_lat", ViewAreaPoint[0][1]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lon", ViewAreaPoint[0][4]);
                                jsonObject_ViewArea_Load1.addProperty("ViewArea_right_lat", ViewAreaPoint[0][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load1);
                                JsonObject jsonObject_ViewArea_Load2 = new JsonObject();
                                jsonObject_ViewArea_Load2.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load2.addProperty("load_number", 2);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lon", ViewAreaPoint[1][0]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_left_lat", ViewAreaPoint[1][1]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lon", ViewAreaPoint[1][4]);
                                jsonObject_ViewArea_Load2.addProperty("ViewArea_right_lat", ViewAreaPoint[1][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load2);
                                JsonObject jsonObject_ViewArea_Load3 = new JsonObject();
                                jsonObject_ViewArea_Load3.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load3.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lon", ViewAreaPoint[2][0]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_left_lat", ViewAreaPoint[2][1]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lon", ViewAreaPoint[2][4]);
                                jsonObject_ViewArea_Load3.addProperty("ViewArea_right_lat", ViewAreaPoint[2][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load3);
                                JsonObject jsonObject_ViewArea_Load4 = new JsonObject();
                                jsonObject_ViewArea_Load4.addProperty("load_amount", 4);
                                jsonObject_ViewArea_Load4.addProperty("load_number", 3);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lon", ViewAreaPoint[3][0]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_left_lat", ViewAreaPoint[3][1]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lon", ViewAreaPoint[3][4]);
                                jsonObject_ViewArea_Load4.addProperty("ViewArea_right_lat", ViewAreaPoint[3][5]);
                                jsonObject_ViewArea.add(jsonObject_ViewArea_Load4);

                                jsonObject.add("payload_view_area", jsonObject_ViewArea);

                                jsonObject.addProperty("tag", "1");

                                Document doc = Document.parse(jsonObject.toString());
                                doc.append("time_point", Time_Point[i]);
                                os.add(doc);

                                if (os.size() > 10000) {
                                    normal_attitude.insertMany(os);
                                    os.clear();
                                }
                            }

                            if (os.size() > 0) {
                                normal_attitude.insertMany(os);
                                os.clear();
                            }
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        mongoClient.close();
    }


    private static double[] DateToDouble(Date Time_Date) {
        //时间转换为doubule型
        double[] Time = new double[6];
        Calendar cal = Calendar.getInstance();
        cal.setTime(Time_Date);
        cal.add(Calendar.HOUR_OF_DAY, TimeZone);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String StringTime = sdf.format(cal.getTime());
        Time[0] = Double.parseDouble(StringTime.substring(0, 4));
        Time[1] = Double.parseDouble(StringTime.substring(5, 7));
        Time[2] = Double.parseDouble(StringTime.substring(8, 10));
        Time[3] = Double.parseDouble(StringTime.substring(11, 13));
        Time[4] = Double.parseDouble(StringTime.substring(14, 16));
        Time[5] = Double.parseDouble(StringTime.substring(17, 19));

        return Time;
    }

    //临边观测模式下，由卫星位置计算卫星绕y轴向前侧摆的角度
    private static double LimbAngCalculation(double SatPositionGEI[]) {
        double Ang;
        double r_Sat = sqrt(SatPositionGEI[0] * SatPositionGEI[0] + SatPositionGEI[1] * SatPositionGEI[1] + SatPositionGEI[2] * SatPositionGEI[2]);
        Ang = Math.asin((Re + MissionLimbAirHeighLimt) / r_Sat);

        return Ang;
    }

    //姿态角计算，北东地1-2-3
    private static void AttitudeCalculationTest(double[] Satellite_LLA, double[] Target_LLA, double[] ViewInstall, double[] Attitude, boolean FlyOrientationFlag) {
        //计算本体系相对于北东地坐标系的姿态
        double[] Target_NED = new double[3];
        double[] z_sensor = new double[3];
        double[] cross_xyz = new double[3];
        double[] x_sensor;
        double[] y_sensor;
        double[][] SToO = new double[3][3];
        double[][] BToS;
        double[][] BToO = new double[3][3];

        Target_LLA[2] = Target_LLA[2];
        ECEFToNED(Satellite_LLA, Target_LLA, Target_NED);
        /*
        Target_NED[0]=-3375480.440;
        Target_NED[1]=-2576716.849;
        Target_NED[2]=14149996.760;
         */
        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Target_NED[i] / sqrt(Target_NED[0] * Target_NED[0] + Target_NED[1] * Target_NED[1] + Target_NED[2] * Target_NED[2]);
        }
        //加入正飞/倒飞考虑
        if (FlyOrientationFlag) {
            cross_xyz[0] = 1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        } else {
            cross_xyz[0] = -1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        }

        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }

        /*
        //安装倾角x轴旋转
        //安装倾角x轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
         */

        /*
        //安装倾角y轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{cos(ViewInstall[2]), 0, -sin(ViewInstall[2])}, {0, 1, 0}, {sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{cos(ViewInstall[2]), 0, sin(ViewInstall[2])}, {0, 1, 0}, {-sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
         */
        //不考虑安装矩阵
        //BToS=new double[][]{{1,0,0},{0,1,0},{0,0,1}};

        //通用化
        double[] z_SatSensor = new double[3];
        double[] cross_SatSxyz = new double[3];
        double[] x_SatSensor;
        double[] y_SatSensor;
        for (int i = 0; i < 3; i++) {
            z_SatSensor[i] =cos(ViewInstall[i]);
        }
        cross_SatSxyz[0] = 1;
        cross_SatSxyz[1] = 0;
        cross_SatSxyz[2] = 0;
        y_SatSensor = VectorCross(z_SatSensor, cross_SatSxyz);//y_sensor=Result
        x_SatSensor = VectorCross(y_SatSensor, z_SatSensor);//x_sensor=Result
        BToS = new double[][]{{x_SatSensor[0], x_SatSensor[1], x_SatSensor[2]},
                {y_SatSensor[0], y_SatSensor[1], y_SatSensor[2]},
                {z_SatSensor[0], z_SatSensor[1], z_SatSensor[2]}};
        if (FlyOrientationFlag) {
            BToS=MatrixInverse(BToS);
        }

        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result
        BToO = MatrixInverse(BToO);

        double flag;
        double sy;
        double x, y, z;
        //欧拉角转序位1-2-3
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[1][0] * BToO[1][0]);
        if (sy < pow(10, -6))
            flag = 1;
        else
            flag = 0;
        if (flag == 0) {
            //atan2(X,Y)的含义和atan(X/Y)的含义是一样的。
            //x = atan2(BToO[2][1], BToO[2][2]);
            //y = atan2(-BToO[2][0], sy);
            //z = atan2(BToO[1][0], BToO[0][0]);
            x = atan(BToO[2][1] / BToO[2][2]);
            y = atan(-BToO[2][0] / sy);
            z = atan(BToO[1][0] / BToO[0][0]);
        } else {
            //x = atan2(-BToO[1][2], BToO[1][1]);
            //y = atan2(-BToO[2][0], sy);
            //z = 0;
            x = atan(-BToO[1][2] / BToO[1][1]);
            y = atan(-BToO[2][0] / sy);
            z = 0;
        }

        Attitude[0] = x;
        Attitude[1] = y;
        if (abs(z) > PI/2) {
            Attitude[2] = PI;
        }else {
            Attitude[2] = 0;
        }
        Attitude[2] = z;

        if (abs(Attitude[0]) > SatelliteManeuverEuler[0]) {
            Attitude[0]=(Attitude[0]/abs(Attitude[0]))*SatelliteManeuverEuler[0];
        }
        if (abs(Attitude[1]) > SatelliteManeuverEuler[1]) {
            Attitude[1]=(Attitude[1]/abs(Attitude[1]))*SatelliteManeuverEuler[1];
        }

        /*
        //加入惯性系
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {SatPosition_GEI[0] / r, SatPosition_GEI[1] / r, SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        double[][] OR = {{xs[0], ys[0], zs[0]},
                {xs[1], ys[1], zs[1]},
                {xs[2], ys[2], zs[2]}};
        double[][] OToI=new double[3][3];
        OToI=MatrixInverse(OR);
        BToO=MatrixMultiplication(OToI, BToO);
        //加入本体系
        double[][] BToB={{0,-1,0},{1,0,0},{0,0,1}};
        BToO=MatrixMultiplication(BToO, BToB);
        BToO=MatrixInverse(BToO);
        //

         */


    }

    //姿态角计算，东南地3-1-2
    private static void AttitudeCalculationESD312(double[] Satellite_LLA, double[] Target_LLA, double[] ViewInstall, double[] Attitude, boolean FlyOrientationFlag) {
        //计算本体系相对于东南地坐标系的姿态
        double[] Target_ESD = new double[3];
        double[] z_sensor = new double[3];
        double[] cross_xyz = new double[3];
        double[] x_sensor;
        double[] y_sensor;
        double[][] SToO = new double[3][3];
        double[][] BToS;
        double[][] BToO = new double[3][3];

        Target_LLA[2] = Target_LLA[2];
        double[] Target_NED = new double[3];
        ECEFToNED(Satellite_LLA, Target_LLA, Target_NED);
        ECEFToESD(Satellite_LLA, Target_LLA, Target_ESD);
        /*
        Target_NED[0]=-3375480.440;
        Target_NED[1]=-2576716.849;
        Target_NED[2]=14149996.760;
         */
        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Target_ESD[i] / sqrt(Target_ESD[0] * Target_ESD[0] + Target_ESD[1] * Target_ESD[1] + Target_ESD[2] * Target_ESD[2]);
        }

        //加入正飞/倒飞考虑
        if (FlyOrientationFlag) {
            cross_xyz[0] = 1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        } else {
            cross_xyz[0] = -1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        }

        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }

/*
        //安装倾角x轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
*/


/*
        //安装倾角y轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{cos(ViewInstall[2]), 0, -sin(ViewInstall[2])}, {0, 1, 0}, {sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{cos(ViewInstall[2]), 0, sin(ViewInstall[2])}, {0, 1, 0}, {-sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
*/

        //不考虑安装矩阵
        //BToS=new double[][]{{1,0,0},{0,1,0},{0,0,1}};

        //通用化
        double[] z_SatSensor = new double[3];
        double[] cross_SatSxyz = new double[3];
        double[] x_SatSensor;
        double[] y_SatSensor;
        for (int i = 0; i < 3; i++) {
            z_SatSensor[i] =cos(ViewInstall[i]);
        }
        cross_SatSxyz[0] = 1;
        cross_SatSxyz[1] = 0;
        cross_SatSxyz[2] = 0;
        y_SatSensor = VectorCross(z_SatSensor, cross_SatSxyz);//y_sensor=Result
        x_SatSensor = VectorCross(y_SatSensor, z_SatSensor);//x_sensor=Result
        BToS = new double[][]{{x_SatSensor[0], x_SatSensor[1], x_SatSensor[2]},
                                {y_SatSensor[0], y_SatSensor[1], y_SatSensor[2]},
                                {z_SatSensor[0], z_SatSensor[1], z_SatSensor[2]}};
        if (FlyOrientationFlag) {
            BToS=MatrixInverse(BToS);
        }

        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result
        BToO = MatrixInverse(BToO);

        double flag;
        double sy;
        double x, y, z;
        //欧拉角转序为3-1-2
        sy = sqrt(BToO[1][0] * BToO[1][0] + BToO[1][1] * BToO[1][1]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(-BToO[1][2], sy);
            y = atan2(BToO[0][2], BToO[2][2]);
            z = atan2(BToO[1][0], BToO[1][1]);
        } else {
            x = atan2(-BToO[1][2], sy);
            y = atan2(-BToO[2][0], BToO[0][0]);
            z = 0;
        }

        Attitude[0] = x;
        Attitude[1] = y;
        if (abs(z) > PI/2) {
            Attitude[2] = PI;
        }else {
            Attitude[2] = 0;
        }

        if (abs(Attitude[0]) > SatelliteManeuverEuler[0]) {
            Attitude[0]=(Attitude[0]/abs(Attitude[0]))*SatelliteManeuverEuler[0];
        }
        if (abs(Attitude[1]) > SatelliteManeuverEuler[1]) {
            Attitude[1]=(Attitude[1]/abs(Attitude[1]))*SatelliteManeuverEuler[1];
        }
    }

    //姿态角计算，东南地3-2-1
    private static void AttitudeCalculationESD321(double[] Satellite_LLA, double[] Target_LLA, double[] ViewInstall, double[] Attitude, boolean FlyOrientationFlag) {
        //计算本体系相对于东南地坐标系的姿态
        double[] Target_ESD = new double[3];
        double[] z_sensor = new double[3];
        double[] cross_xyz = new double[3];
        double[] x_sensor;
        double[] y_sensor;
        double[][] SToO = new double[3][3];
        double[][] BToS;
        double[][] BToO = new double[3][3];

        Target_LLA[2] = Target_LLA[2];
        double[] Target_NED = new double[3];
        ECEFToNED(Satellite_LLA, Target_LLA, Target_NED);
        ECEFToESD(Satellite_LLA, Target_LLA, Target_ESD);
        /*
        Target_NED[0]=-3375480.440;
        Target_NED[1]=-2576716.849;
        Target_NED[2]=14149996.760;
         */
        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Target_ESD[i] / sqrt(Target_ESD[0] * Target_ESD[0] + Target_ESD[1] * Target_ESD[1] + Target_ESD[2] * Target_ESD[2]);
        }

        //加入卫星正飞/倒飞考虑
        if (FlyOrientationFlag) {
            cross_xyz[0] = 1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        } else {
            cross_xyz[0] = -1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        }

        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }

        /*
        //安装倾角x轴旋转
        //安装倾角x轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
         */

        //安装倾角y轴旋转
        /*
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{cos(ViewInstall[2]), 0, -sin(ViewInstall[2])}, {0, 1, 0}, {sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{cos(ViewInstall[2]), 0, sin(ViewInstall[2])}, {0, 1, 0}, {-sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
         */
        //不考虑安装矩阵
        //BToS=new double[][]{{1,0,0},{0,1,0},{0,0,1}};

        //通用化
        double[] z_SatSensor = new double[3];
        double[] cross_SatSxyz = new double[3];
        double[] x_SatSensor;
        double[] y_SatSensor;
        for (int i = 0; i < 3; i++) {
            z_SatSensor[i] =cos(ViewInstall[i]);
        }
        cross_SatSxyz[0] = 1;
        cross_SatSxyz[1] = 0;
        cross_SatSxyz[2] = 0;
        y_SatSensor = VectorCross(z_SatSensor, cross_SatSxyz);//y_sensor=Result
        x_SatSensor = VectorCross(y_SatSensor, z_SatSensor);//x_sensor=Result

        BToS = new double[][]{{x_SatSensor[0], x_SatSensor[1], x_SatSensor[2]},
                {y_SatSensor[0], y_SatSensor[1], y_SatSensor[2]},
                {z_SatSensor[0], z_SatSensor[1], z_SatSensor[2]}};
        if (FlyOrientationFlag) {
            BToS=MatrixInverse(BToS);
        }


        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result
        BToO = MatrixInverse(BToO);

        double flag;
        double sy;
        double x, y, z;

        //欧拉角转序为3-2-1
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[0][1] * BToO[0][1]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(-BToO[1][2], BToO[2][2]);
            y = atan2(BToO[0][2], sy);
            z = atan2(-BToO[0][1], BToO[0][0]);
        } else {
            x = atan2(BToO[2][1], BToO[1][1]);
            y = atan2(BToO[1][2], sy);
            z = 0;
        }
        if (abs(z) > 3 * PI / 4) {
            z = abs(z);
        }

        /*
        //欧拉角转序为3-2-1
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[1][0] * BToO[1][0]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(BToO[2][1], BToO[2][2]);
            y = atan2(-BToO[0][2], sy);
            z = atan2(BToO[1][0], BToO[0][0]);
        } else {
            x = atan2(-BToO[1][2], BToO[1][1]);
            y = atan2(-BToO[2][0], sy);
            z = 0;
        }
        */

        Attitude[0] = x;
        Attitude[1] = y;
        if (abs(z) > PI/2) {
            Attitude[2] = PI;
        }else {
            Attitude[2] = 0;
        }
        //Attitude[2]=z;

        if (abs(Attitude[0]) > SatelliteManeuverEuler[0]) {
            Attitude[0]=(Attitude[0]/abs(Attitude[0]))*SatelliteManeuverEuler[0];
        }
        if (abs(Attitude[1]) > SatelliteManeuverEuler[1]) {
            Attitude[1]=(Attitude[1]/abs(Attitude[1]))*SatelliteManeuverEuler[1];
        }
    }

    //更改后欧拉角姿态角计算，东南地3-2-1
    private static void AttitudeCalculationEulerESD321(double[] Satellite_LLA, double[] Target_LLA, double[] ViewInstall, double[] Attitude, boolean FlyOrientationFlag) {
        //计算本体系相对于东南地坐标系的姿态
        double[] Target_ESD = new double[3];
        double[] z_sensor = new double[3];
        double[] cross_xyz = new double[3];
        double[] x_sensor;
        double[] y_sensor;
        double[][] SToO = new double[3][3];
        double[][] BToS;
        double[][] BToO = new double[3][3];

        Target_LLA[2] = Target_LLA[2];
        double[] Target_NED = new double[3];
        ECEFToNED(Satellite_LLA, Target_LLA, Target_NED);
        ECEFToESD(Satellite_LLA, Target_LLA, Target_ESD);
        /*
        Target_NED[0]=-3375480.440;
        Target_NED[1]=-2576716.849;
        Target_NED[2]=14149996.760;
         */
        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Target_ESD[i] / sqrt(Target_ESD[0] * Target_ESD[0] + Target_ESD[1] * Target_ESD[1] + Target_ESD[2] * Target_ESD[2]);
        }

        //加入卫星正飞/倒飞考虑
        if (FlyOrientationFlag) {
            cross_xyz[0] = 1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        } else {
            cross_xyz[0] = -1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        }

        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }

        /*
        //安装倾角x轴旋转
        //安装倾角x轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
         */

        //安装倾角y轴旋转
        /*
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{cos(ViewInstall[2]), 0, -sin(ViewInstall[2])}, {0, 1, 0}, {sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{cos(ViewInstall[2]), 0, sin(ViewInstall[2])}, {0, 1, 0}, {-sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
         */
        //不考虑安装矩阵
        //BToS=new double[][]{{1,0,0},{0,1,0},{0,0,1}};

        //通用化
        double[] z_SatSensor = new double[3];
        double[] cross_SatSxyz = new double[3];
        double[] x_SatSensor;
        double[] y_SatSensor;
        for (int i = 0; i < 3; i++) {
            z_SatSensor[i] =cos(ViewInstall[i]);
        }
        cross_SatSxyz[0] = 1;
        cross_SatSxyz[1] = 0;
        cross_SatSxyz[2] = 0;
        y_SatSensor = VectorCross(z_SatSensor, cross_SatSxyz);//y_sensor=Result
        x_SatSensor = VectorCross(y_SatSensor, z_SatSensor);//x_sensor=Result

        BToS = new double[][]{{x_SatSensor[0], x_SatSensor[1], x_SatSensor[2]},
                {y_SatSensor[0], y_SatSensor[1], y_SatSensor[2]},
                {z_SatSensor[0], z_SatSensor[1], z_SatSensor[2]}};
        if (FlyOrientationFlag) {
            BToS=MatrixInverse(BToS);
        }


        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result
        BToO = MatrixInverse(BToO);

        double flag;
        double sy;
        double x, y, z;
        /*
        //欧拉角转序为3-2-1
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[0][1] * BToO[0][1]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(-BToO[1][2], BToO[2][2]);
            y = atan2(BToO[0][2], sy);
            z = atan2(-BToO[0][1], BToO[0][0]);
        } else {
            x = atan2(BToO[2][1], BToO[1][1]);
            y = atan2(BToO[1][2], sy);
            z = 0;
        }
        if (abs(z) > 3 * PI / 4) {
            z = abs(z);
        }
        */

        //欧拉角转序为3-2-1
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[1][0] * BToO[1][0]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(BToO[2][1], BToO[2][2]);
            y = atan2(-BToO[2][0], sy);
            z = atan2(BToO[1][0], BToO[0][0]);
        } else {
            x = atan2(-BToO[1][2], BToO[1][1]);
            y = atan2(-BToO[2][0], sy);
            z = 0;
        }

        Attitude[0] = x;
        Attitude[1] = y;
        if (abs(z) > PI/2) {
            Attitude[2] = PI;
        }else {
            Attitude[2] = 0;
        }
        //Attitude[2]=z;

        if (abs(Attitude[0]) > SatelliteManeuverEuler[0]) {
            Attitude[0]=(Attitude[0]/abs(Attitude[0]))*SatelliteManeuverEuler[0];
        }
        if (abs(Attitude[1]) > SatelliteManeuverEuler[1]) {
            Attitude[1]=(Attitude[1]/abs(Attitude[1]))*SatelliteManeuverEuler[1];
        }
    }

    //姿态角计算，轨道坐标系3-1-2
    private static void AttitudeCalculationORF312(double[] SatPosition_GEI, double[] SatVelocity_GEI, double[] Target_LLA, double[] Time, double[] ViewInstall, double[] Attitude, boolean FlyOrientationFlag) {
        double[] SatelliteTime = new double[6];
        double[][] BToS;
        double[][] BToO = new double[3][3];
        double[][] SToO = new double[3][3];
        double[] x_sensor;
        double[] y_sensor;
        double[] z_sensor = new double[3];
        double[] Target_GEI = new double[3];
        double[] Error_GEI = new double[3];
        double[] Error_ORF = new double[3];
        double[] Position_ECEF = new double[3];
        double[] cross_xyz = new double[3];
        double flag;
        double sy;
        double x, y, z;
        for (int i = 0; i < 6; i++) {
            SatelliteTime[i] = Time[i];
        }
        double Time_JD = JD(SatelliteTime);
        double[] Satllite_GEI = {SatPosition_GEI[0], SatPosition_GEI[1], SatPosition_GEI[2]};
        double[] SatVelocity_GEI1 = {SatVelocity_GEI[0], SatVelocity_GEI[1], SatVelocity_GEI[2]};
        LLAToECEF(Target_LLA, Position_ECEF);//Position_ECEF
        ECEFToICRS(Time_JD, Position_ECEF, Target_GEI);//Target_GEI
        for (int i = 0; i < 3; i++) {
            Error_GEI[i] = Target_GEI[i] - Satllite_GEI[i];
        }

        GEIToORF_Ellipse(Satllite_GEI, SatVelocity_GEI1, Error_GEI, Error_ORF);//Error_ORF

        for (int i = 0; i < 3; i++) {
            z_sensor[i] = Error_ORF[i] / sqrt(Error_ORF[0] * Error_ORF[0] + Error_ORF[1] * Error_ORF[1] + Error_ORF[2] * Error_ORF[2]);
        }

        //加入正飞/倒飞考虑
        if (FlyOrientationFlag) {
            cross_xyz[0] = 1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        } else {
            cross_xyz[0] = -1;
            cross_xyz[1] = 0;
            cross_xyz[2] = 0;
        }

        y_sensor = VectorCross(z_sensor, cross_xyz);//y_sensor=Result
        x_sensor = VectorCross(y_sensor, z_sensor);//x_sensor=Result
        for (int i = 0; i < 3; i++) {
            SToO[0][i] = x_sensor[i];
            SToO[1][i] = y_sensor[i];
            SToO[2][i] = z_sensor[i];
        }

        /*
        //安装倾角x轴旋转
        //安装倾角x轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), -sin(ViewInstall[2])}, {0, sin(ViewInstall[2]), cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{1, 0, 0}, {0, cos(ViewInstall[2]), sin(ViewInstall[2])}, {0, -sin(ViewInstall[2]), cos(ViewInstall[2])}};
         */

        /*
        //安装倾角y轴旋转
        if (ViewInstall[1] > PI / 2)
            BToS = new double[][]{{cos(ViewInstall[2]), 0, -sin(ViewInstall[2])}, {0, 1, 0}, {sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
        else
            BToS = new double[][]{{cos(ViewInstall[2]), 0, sin(ViewInstall[2])}, {0, 1, 0}, {-sin(ViewInstall[2]), 0, cos(ViewInstall[2])}};
         */

        //不考虑安装矩阵
        //BToS=new double[][]{{1,0,0},{0,1,0},{0,0,1}};

        //通用化
        double[] z_SatSensor = new double[3];
        double[] cross_SatSxyz = new double[3];
        double[] x_SatSensor;
        double[] y_SatSensor;
        for (int i = 0; i < 3; i++) {
            z_SatSensor[i] =cos(ViewInstall[i]);
        }
        cross_SatSxyz[0] = 1;
        cross_SatSxyz[1] = 0;
        cross_SatSxyz[2] = 0;
        y_SatSensor = VectorCross(z_SatSensor, cross_SatSxyz);//y_sensor=Result
        x_SatSensor = VectorCross(y_SatSensor, z_SatSensor);//x_sensor=Result
        BToS = new double[][]{{x_SatSensor[0], x_SatSensor[1], x_SatSensor[2]},
                {y_SatSensor[0], y_SatSensor[1], y_SatSensor[2]},
                {z_SatSensor[0], z_SatSensor[1], z_SatSensor[2]}};
        if (FlyOrientationFlag) {
            BToS=MatrixInverse(BToS);
        }

        BToO = MatrixMultiplication(SToO, BToS);//BToO=Result
        BToO = MatrixInverse(BToO);

        //欧拉角转序为3-1-2
        sy = sqrt(BToO[1][0] * BToO[1][0] + BToO[1][1] * BToO[1][1]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(-BToO[1][2], sy);
            y = atan2(BToO[0][2], BToO[2][2]);
            z = atan2(BToO[1][0], BToO[1][1]);
        } else {
            x = atan2(-BToO[1][2], sy);
            y = atan2(-BToO[2][0], BToO[0][0]);
            z = 0;
        }

        Attitude[0] = x;
        Attitude[1] = y;
        if (abs(z) > PI/2) {
            Attitude[2] = PI;
        }else {
            Attitude[2] = 0;
        }

        if (abs(Attitude[0]) > SatelliteManeuverEuler[0]) {
            Attitude[0]=(Attitude[0]/abs(Attitude[0]))*SatelliteManeuverEuler[0];
        }
        if (abs(Attitude[1]) > SatelliteManeuverEuler[1]) {
            Attitude[1]=(Attitude[1]/abs(Attitude[1]))*SatelliteManeuverEuler[1];
        }
    }

    ////轨道系下姿态，转为东南系姿态3-2-1
    private static void AttitudeCalculationORF312ToESD(double[] SatPosition_GEI, double[] SatVelocity_GEI, double[] Satellite_LLA, double[] Time, double[] Attitude_ORF, double[] Attitude_ESD) {
        //本体系到轨道系
        double x = Attitude_ORF[0];
        double y = Attitude_ORF[1];
        double z = Attitude_ORF[2];
        double[][] R_BFToOR = {{cos(y) * cos(z) + sin(y) * sin(x) * sin(z), -cos(y) * sin(z) + sin(y) * sin(x) * cos(z), sin(y) * cos(x)},
                {cos(x) * sin(z), cos(x) * cos(z), -sin(x)},
                {-sin(y) * cos(z) + cos(y) * sin(x) * sin(z), sin(y) * sin(z) + cos(y) * sin(x) * cos(z), cos(y) * cos(x)}};

        //惯性系到轨道系
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        double r_ys = sqrt(pow(ys[0], 2) + pow(ys[1], 2) + pow(ys[2], 2));
        ys[0] = ys[0] / r_ys;
        ys[1] = ys[1] / r_ys;
        ys[2] = ys[2] / r_ys;
        xs = VectorCross(ys, zs);
        double[][] OR = {{xs[0], xs[1], xs[2]},
                {ys[0], ys[1], ys[2]},
                {zs[0], zs[1], zs[2]}};
        double[][] ORFToGEI = MatrixInverse(OR);

        double JD = JD(Time);
        double T = (JD - 2451545.0) / 36525.0;
        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506 * T + 0.3019015 * T * T + 0.0179663 * T * T * T - 0.0000327 * T * T * T * T - 0.0000002 * T * T * T * T * T;//秒
        double Theta_A = 2004.1917476 * T - 0.4269353 * T * T - 0.041825 * T * T * T - 0.0000601 * T * T * T * T - 0.0000001 * T * T * T * T * T;
        double Z_A = -2.5976176 + 2306.0803226 * T + 1.094779 * T * T + 0.0182273 * T * T * T + 0.000047 * T * T * T * T - 0.0000003 * T * T * T * T * T;
        Zeta_A = Zeta_A / 3600.0;//度
        Theta_A = Theta_A / 3600.0;
        Z_A = Z_A / 3600.0;
        //岁差矩阵
        double[][] R3Z_A = {{cos(-Z_A * PI / 180.0), sin(-Z_A * PI / 180.0), 0},
                {-sin(-Z_A * PI / 180.0), cos(-Z_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R2Theta_A = {{cos(Theta_A * PI / 180.0), 0, -sin(Theta_A * PI / 180.0)},
                {0, 1, 0},
                {sin(Theta_A * PI / 180.0), 0, cos(Theta_A * PI / 180.0)}};
        double[][] R3_Zeta_A = {{cos(-Zeta_A * PI / 180.0), sin(-Zeta_A * PI / 180.0), 0},
                {-sin(-Zeta_A * PI / 180.0), cos(-Zeta_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] PR = new double[3][3];
        double[][] PR_mid = new double[3][3];
        PR_mid = MatrixMultiplication(R3Z_A, R2Theta_A);
        PR = MatrixMultiplication(PR_mid, R3_Zeta_A);
        //章动计算
        double Epsilon_A = 84381.448 - 46.8150 * T - 0.00059 * T * T + 0.001813 * T * T * T;
        Epsilon_A = Epsilon_A / 3600.0;
        double L = 280.4665 + 36000.7698 * T;
        double dL = 218.3165 + 481267.8813 * T;
        double Omega = 125.04452 - 1934.136261 * T;
        double DeltaPsi = -17.20 * sin(Omega * PI / 180.0) - 1.32 * sin(2 * L * PI / 180.0) - 0.23 * sin(2 * dL * PI / 180.0) + 0.21 * sin(2 * Omega * PI / 180.0);
        double DeltaEpsilon = 9.20 * cos(Omega * PI / 180.0) + 0.57 * cos(2 * L * PI / 180.0) + 0.10 * cos(2 * dL * PI / 180.0) - 0.09 * cos(2 * Omega * PI / 180.0);
        DeltaPsi = DeltaPsi / 3600.0;
        DeltaEpsilon = DeltaEpsilon / 3600.0;
        //章动矩阵
        double[][] R1_DEA = {{1, 0, 0},
                {0, cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)},
                {0, -sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)}};
        double[][] R3_DeltaPsi = {{cos(-DeltaPsi * PI / 180.0), sin(-DeltaPsi * PI / 180.0), 0},
                {-sin(-DeltaPsi * PI / 180.0), cos(-DeltaPsi * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R1_Epsilon = {{1, 0, 0},
                {0, cos(Epsilon_A * PI / 180.0), sin(Epsilon_A * PI / 180.0)},
                {0, -sin(Epsilon_A * PI / 180.0), cos(Epsilon_A * PI / 180.0)}};
        double[][] NR = new double[3][3];
        double[][] NR_mid = new double[3][3];
        NR_mid = MatrixMultiplication(R1_DEA, R3_DeltaPsi);
        NR = MatrixMultiplication(NR_mid, R1_Epsilon);
        //地球自转
        double GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0;
        GMST = GMST % 360;
        double GAST = GMST + DeltaPsi * cos((DeltaEpsilon + Epsilon_A) * PI / 180.0);
        GAST = GAST % 360;
        double[][] ER = {{cos(GAST * PI / 180.0), sin(GAST * PI / 180.0), 0},
                {-sin(GAST * PI / 180.0), cos(GAST * PI / 180.0), 0},
                {0, 0, 1}};
        //极移坐标
        double Xp = 0.001674 * 0.955;
        double Yp = 0.001462 * 0.955;
        // 极移矩阵
        double[][] R1_YP = {{1, 0, 0},
                {0, cos(-Yp * PI / 180.0), sin(-Yp * PI / 180.0)},
                {0, -sin(-Yp * PI / 180.0), cos(-Yp * PI / 180.0)}};
        double[][] R2_XP = {{cos(-Xp * PI / 180.0), 0, -sin(-Xp * PI / 180.0)},
                {0, 1, 0},
                {sin(-Xp * PI / 180.0), 0, cos(-Xp * PI / 180.0)}};
        double[][] EP = new double[3][3];
        EP = MatrixMultiplication(R1_YP, R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER = new double[3][3];
        double[][] EPERNR = new double[3][3];
        double[][] ECEF;
        EPER = MatrixMultiplication(EP, ER);
        EPERNR = MatrixMultiplication(EPER, NR);
        ECEF = MatrixMultiplication(EPERNR, PR);

        double[] Satellite_ECEF = new double[3];
        double[] Target_ECEF = new double[3];
        LLAToECEF(Satellite_LLA, Satellite_ECEF);
        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double LL = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(LL), -sin(B) * sin(LL), cos(B)},
                {-sin(LL), cos(LL), 0},
                {-cos(B) * cos(LL), -cos(B) * sin(LL), -sin(B)}};
        double Ang_z = -PI / 2;
        double[][] R_NEDToESD = {{cos(Ang_z), -sin(Ang_z), 0},
                {sin(Ang_z), cos(Ang_z), 0},
                {0, 0, 1}};
        double[][] R_ECEFToESD = MatrixMultiplication(R_NEDToESD, R_ECEFToNED);

        double[][] R_BFToESD = MatrixMultiplication(R_ECEFToESD, MatrixMultiplication(ECEF, MatrixMultiplication(ORFToGEI, R_BFToOR)));

        double flag;
        double sy;
        //欧拉角转序为3-2-1
        sy = sqrt(R_BFToESD[0][0] * R_BFToESD[0][0] + R_BFToESD[0][1] * R_BFToESD[0][1]);
        if (sy < pow(10, -6)) {
            flag = 1;
        } else {
            flag = 0;
        }
        if (flag == 0) {
            x = atan2(-R_BFToESD[1][2], R_BFToESD[2][2]);
            y = atan2(R_BFToESD[0][2], sy);
            z = atan2(-R_BFToESD[0][1], R_BFToESD[0][0]);
        } else {
            x = atan2(R_BFToESD[2][1], R_BFToESD[1][1]);
            y = atan2(R_BFToESD[1][2], sy);
            z = 0;
        }

        Attitude_ESD[0] = x;
        Attitude_ESD[1] = y;
        Attitude_ESD[2] = z;
    }

    //当前姿态下卫星视场四个顶点的经纬度
    private static void AttitudeViewCalculation(double Position[], double Velocity[], double Satellite_LLA[], double Attitude[], double ViewInstall[][], double ViewAng[][], int ViewNum, double Time[], double Time_UTC, double ViewAreaPoint[][]) {
        double r = Math.sqrt(Math.pow(Position[0], 2) + Math.pow(Position[1], 2) + Math.pow(Position[2], 2));
        double theta = asin(Re / r);

        for (int j = 0; j < ViewNum; j++) {
            //顶点1
            double[][] r_install01 = {{cos(ViewInstall[j][1])}, {cos(ViewInstall[j][0])}, {cos(ViewInstall[j][2])}};
            double[][] R_x01 = new double[][]{{1, 0, 0}, {0, cos(ViewAng[j][0]), -sin(ViewAng[j][0])}, {0, sin(ViewAng[j][0]), cos(ViewAng[j][0])}};
            double[][] R_y01 = new double[][]{{cos(-ViewAng[j][3]), 0, sin(-ViewAng[j][3])}, {0, 1, 0}, {-sin(-ViewAng[j][3]), 0, cos(-ViewAng[j][3])}};
            double[][] r_install_mid01 = MatrixMultiplication(R_x01, r_install01);
            double[][] r_install_BF01 = MatrixMultiplication(R_y01, r_install_mid01);
            //东南地312转序姿态
            double y = Attitude[0];
            double x = Attitude[1];
            double z = Attitude[2];
            double[][] R_Attitude_mid = new double[][]{{cos(y) * cos(z) + sin(y) * sin(x) * sin(z), -cos(y) * sin(z) + sin(y) * sin(x) * cos(z), sin(y) * cos(x)},
                    {cos(x) * sin(z), cos(x) * cos(z), -sin(x)},
                    {-sin(y) * cos(z) + cos(y) * sin(x) * sin(z), sin(y) * sin(z) + cos(y) * sin(x) * cos(z), cos(y) * cos(x)}};
            double[][] R_Attitude = MatrixInverse(R_Attitude_mid);
            double[][] r_install_ESD01 = MatrixMultiplication(R_Attitude, r_install_BF01);
            double theta_installxz01 = atan2(r_install_ESD01[0][0], r_install_ESD01[2][0]);
            double theta_installyz01 = atan2(r_install_ESD01[1][0], r_install_ESD01[2][0]);
            double betaxz01, betayz01;
            if (abs(theta_installxz01) >= theta) {
                if (theta_installxz01 > 0) {
                    betaxz01 = theta - PI;
                } else if (theta_installxz01 < 0) {
                    betaxz01 = theta + PI;
                } else {
                    betaxz01 = 0;
                }
            } else {
                if (theta_installxz01 > 0) {
                    betaxz01 = theta_installxz01 - asin((sin(theta_installxz01) * r) / Re);
                } else if (theta_installxz01 < 0) {
                    betaxz01 = theta_installxz01 + asin((sin(-theta_installxz01) * r) / Re);
                } else {
                    betaxz01 = 0;
                }
            }
            if (abs(theta_installyz01) >= theta) {
                if (theta_installyz01 > 0) {
                    betayz01 = PI - theta;
                } else if (theta_installyz01 < 0) {
                    betayz01 = -theta - PI;
                } else {
                    betayz01 = 0;
                }
            } else {
                if (theta_installyz01 > 0) {
                    betayz01 = asin((sin(theta_installyz01) * r) / Re) - theta_installyz01;
                } else if (theta_installyz01 < 0) {
                    betayz01 = -theta_installyz01 - asin((sin(-theta_installyz01) * r) / Re);
                } else {
                    betayz01 = 0;
                }
            }
            double[][] RESD_x01 = new double[][]{{cos(betaxz01), 0, -sin(betaxz01)}, {0, 1, 0}, {sin(betaxz01), 0, cos(betaxz01)}};
            double[][] RESD_y01 = new double[][]{{1, 0, 0}, {0, cos(betayz01), -sin(betayz01)}, {0, sin(betayz01), cos(betayz01)}};
            double[][] r_Satellite_ESD01 = new double[][]{{0}, {0}, {-r}};
            double[][] r_Satellite_ESD01_mid = new double[3][1];
            double[][] r_Satellite_ESD01_mid2 = new double[3][1];
            double[] r_Target_ECEF = new double[3];
            double[] r_Target_GEI = new double[3];
            r_Satellite_ESD01_mid = MatrixMultiplication(RESD_x01, r_Satellite_ESD01);
            r_Satellite_ESD01_mid2 = MatrixMultiplication(RESD_y01, r_Satellite_ESD01_mid);
            double[] r_Target_ESD_mid01 = new double[]{r_Satellite_ESD01_mid2[0][0], r_Satellite_ESD01_mid2[1][0], r_Satellite_ESD01_mid2[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid01, r_Target_ECEF);
            double JD_Time = JD(Time);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            double SubSat[] = new double[3];
            double SubSat_GEI[] = new double[3];
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][0] = SubSat[0];
            ViewAreaPoint[j][1] = SubSat[1];
            //顶点2
            double[][] r_install02 = {{cos(ViewInstall[j][1])}, {cos(ViewInstall[j][0])}, {cos(ViewInstall[j][2])}};
            double[][] R_x02 = new double[][]{{1, 0, 0}, {0, cos(ViewAng[j][0]), -sin(ViewAng[j][0])}, {0, sin(ViewAng[j][0]), cos(ViewAng[j][0])}};
            double[][] R_y02 = new double[][]{{cos(ViewAng[j][2]), 0, sin(ViewAng[j][2])}, {0, 1, 0}, {-sin(ViewAng[j][2]), 0, cos(ViewAng[j][2])}};
            double[][] r_install_mid02 = MatrixMultiplication(R_x02, r_install02);
            double[][] r_install_BF02 = MatrixMultiplication(R_y02, r_install_mid02);
            double[][] r_install_ESD02 = MatrixMultiplication(R_Attitude, r_install_BF02);
            double theta_installxz02 = atan2(r_install_ESD02[0][0], r_install_ESD02[2][0]);
            double theta_installyz02 = atan2(r_install_ESD02[1][0], r_install_ESD02[2][0]);
            double betaxz02, betayz02;
            if (abs(theta_installxz02) >= theta) {
                if (theta_installxz02 > 0) {
                    betaxz02 = theta - PI;
                } else if (theta_installxz02 < 0) {
                    betaxz02 = theta + PI;
                } else {
                    betaxz02 = 0;
                }
            } else {
                if (theta_installxz02 > 0) {
                    betaxz02 = theta_installxz02 - asin((sin(theta_installxz02) * r) / Re);
                } else if (theta_installxz02 < 0) {
                    betaxz02 = theta_installxz02 + asin((sin(-theta_installxz02) * r) / Re);
                } else {
                    betaxz02 = 0;
                }
            }
            if (abs(theta_installyz02) >= theta) {
                if (theta_installyz02 > 0) {
                    betayz02 = PI - theta;
                } else if (theta_installyz02 < 0) {
                    betayz02 = -theta - PI;
                } else {
                    betayz02 = 0;
                }
            } else {
                if (theta_installyz02 > 0) {
                    betayz02 = asin((sin(theta_installyz02) * r) / Re) - theta_installyz02;
                } else if (theta_installyz02 < 0) {
                    betayz02 = -theta_installyz02 - asin((sin(-theta_installyz02) * r) / Re);
                } else {
                    betayz02 = 0;
                }
            }
            double[][] RESD_x02 = new double[][]{{cos(betaxz02), 0, -sin(betaxz02)}, {0, 1, 0}, {sin(betaxz02), 0, cos(betaxz02)}};
            double[][] RESD_y02 = new double[][]{{1, 0, 0}, {0, cos(betayz02), -sin(betayz02)}, {0, sin(betayz02), cos(betayz02)}};
            double[][] r_Satellite_ESD02 = new double[][]{{0}, {0}, {-r}};
            double[][] r_Satellite_ESD02_mid = new double[3][1];
            double[][] r_Satellite_ESD02_mid2 = new double[3][1];
            r_Satellite_ESD02_mid = MatrixMultiplication(RESD_x02, r_Satellite_ESD02);
            r_Satellite_ESD02_mid2 = MatrixMultiplication(RESD_y02, r_Satellite_ESD02_mid);
            double[] r_Target_ESD_mid02 = new double[]{r_Satellite_ESD02_mid2[0][0], r_Satellite_ESD02_mid2[1][0], r_Satellite_ESD02_mid2[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid02, r_Target_ECEF);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][2] = SubSat[0];
            ViewAreaPoint[j][3] = SubSat[1];
            //顶点3
            double[][] r_install03 = {{cos(ViewInstall[j][1])}, {cos(ViewInstall[j][0])}, {cos(ViewInstall[j][2])}};
            double[][] R_x03 = new double[][]{{1, 0, 0}, {0, cos(-ViewAng[j][1]), -sin(-ViewAng[j][1])}, {0, sin(-ViewAng[j][1]), cos(-ViewAng[j][1])}};
            double[][] R_y03 = new double[][]{{cos(ViewAng[j][3]), 0, sin(ViewAng[j][3])}, {0, 1, 0}, {-sin(ViewAng[j][3]), 0, cos(ViewAng[j][3])}};
            double[][] r_install_mid03 = MatrixMultiplication(R_x03, r_install03);
            double[][] r_install_BF03 = MatrixMultiplication(R_y03, r_install_mid03);
            double[][] r_install_ESD03 = MatrixMultiplication(R_Attitude, r_install_BF03);
            double theta_installxz03 = atan2(r_install_ESD03[0][0], r_install_ESD03[2][0]);
            double theta_installyz03 = atan2(r_install_ESD03[1][0], r_install_ESD03[2][0]);
            double betaxz03, betayz03;
            if (abs(theta_installxz03) >= theta) {
                if (theta_installxz03 > 0) {
                    betaxz03 = theta - PI;
                } else if (theta_installxz03 < 0) {
                    betaxz03 = theta + PI;
                } else {
                    betaxz03 = 0;
                }
            } else {
                if (theta_installxz03 > 0) {
                    betaxz03 = theta_installxz03 - asin((sin(theta_installxz03) * r) / Re);
                } else if (theta_installxz03 < 0) {
                    betaxz03 = theta_installxz03 + asin((sin(-theta_installxz03) * r) / Re);
                } else {
                    betaxz03 = 0;
                }
            }
            if (abs(theta_installyz03) >= theta) {
                if (theta_installyz03 > 0) {
                    betayz03 = PI - theta;
                } else if (theta_installyz03 < 0) {
                    betayz03 = -theta - PI;
                } else {
                    betayz03 = 0;
                }
            } else {
                if (theta_installyz03 > 0) {
                    betayz03 = asin((sin(theta_installyz03) * r) / Re) - theta_installyz03;
                } else if (theta_installyz03 < 0) {
                    betayz03 = -theta_installyz03 - asin((sin(-theta_installyz03) * r) / Re);
                } else {
                    betayz03 = 0;
                }
            }
            double[][] RESD_x03 = new double[][]{{cos(betaxz03), 0, -sin(betaxz03)}, {0, 1, 0}, {sin(betaxz03), 0, cos(betaxz03)}};
            double[][] RESD_y03 = new double[][]{{1, 0, 0}, {0, cos(betayz03), -sin(betayz03)}, {0, sin(betayz03), cos(betayz03)}};
            double[][] r_Satellite_ESD03 = new double[][]{{0}, {0}, {-r}};
            double[][] r_Satellite_ESD03_mid = new double[3][1];
            double[][] r_Satellite_ESD03_mid2 = new double[3][1];
            r_Satellite_ESD03_mid = MatrixMultiplication(RESD_x03, r_Satellite_ESD03);
            r_Satellite_ESD03_mid2 = MatrixMultiplication(RESD_y03, r_Satellite_ESD03_mid);
            double[] r_Target_ESD_mid03 = new double[]{r_Satellite_ESD03_mid2[0][0], r_Satellite_ESD03_mid2[1][0], r_Satellite_ESD03_mid2[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid03, r_Target_ECEF);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][4] = SubSat[0];
            ViewAreaPoint[j][5] = SubSat[1];
            //顶点4
            double[][] r_install04 = {{cos(ViewInstall[j][1])}, {cos(ViewInstall[j][0])}, {cos(ViewInstall[j][2])}};
            double[][] R_x04 = new double[][]{{1, 0, 0}, {0, cos(-ViewAng[j][1]), -sin(-ViewAng[j][1])}, {0, sin(-ViewAng[j][1]), cos(-ViewAng[j][1])}};
            double[][] R_y04 = new double[][]{{cos(-ViewAng[j][2]), 0, sin(-ViewAng[j][2])}, {0, 1, 0}, {-sin(-ViewAng[j][2]), 0, cos(-ViewAng[j][2])}};
            double[][] r_install_mid04 = MatrixMultiplication(R_x04, r_install04);
            double[][] r_install_BF04 = MatrixMultiplication(R_y04, r_install_mid04);
            double[][] r_install_ESD04 = MatrixMultiplication(R_Attitude, r_install_BF04);
            double theta_installxz04 = atan2(r_install_ESD04[0][0], r_install_ESD04[2][0]);
            double theta_installyz04 = atan2(r_install_ESD04[1][0], r_install_ESD04[2][0]);
            double betaxz04, betayz04;
            if (abs(theta_installxz04) >= theta) {
                if (theta_installxz04 > 0) {
                    betaxz04 = theta - PI;
                } else if (theta_installxz04 < 0) {
                    betaxz04 = theta + PI;
                } else {
                    betaxz04 = 0;
                }
            } else {
                if (theta_installxz04 > 0) {
                    betaxz04 = theta_installxz04 - asin((sin(theta_installxz04) * r) / Re);
                } else if (theta_installxz04 < 0) {
                    betaxz04 = theta_installxz04 + asin((sin(-theta_installxz04) * r) / Re);
                } else {
                    betaxz04 = 0;
                }
            }
            if (abs(theta_installyz04) >= theta) {
                if (theta_installyz04 > 0) {
                    betayz04 = PI - theta;
                } else if (theta_installyz04 < 0) {
                    betayz04 = -theta - PI;
                } else {
                    betayz04 = 0;
                }
            } else {
                if (theta_installyz04 > 0) {
                    betayz04 = asin((sin(theta_installyz04) * r) / Re) - theta_installyz04;
                } else if (theta_installyz04 < 0) {
                    betayz04 = -theta_installyz04 - asin((sin(-theta_installyz04) * r) / Re);
                } else {
                    betayz04 = 0;
                }
            }
            double[][] RESD_x04 = new double[][]{{cos(betaxz04), 0, -sin(betaxz04)}, {0, 1, 0}, {sin(betaxz04), 0, cos(betaxz04)}};
            double[][] RESD_y04 = new double[][]{{1, 0, 0}, {0, cos(betayz04), -sin(betayz04)}, {0, sin(betayz04), cos(betayz04)}};
            double[][] r_Satellite_ESD04 = new double[][]{{0}, {0}, {-r}};
            double[][] r_Satellite_ESD04_mid = new double[3][1];
            double[][] r_Satellite_ESD04_mid2 = new double[3][1];
            r_Satellite_ESD04_mid = MatrixMultiplication(RESD_x04, r_Satellite_ESD04);
            r_Satellite_ESD04_mid2 = MatrixMultiplication(RESD_y04, r_Satellite_ESD04_mid);
            double[] r_Target_ESD_mid04 = new double[]{r_Satellite_ESD04_mid2[0][0], r_Satellite_ESD04_mid2[1][0], r_Satellite_ESD04_mid2[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid04, r_Target_ECEF);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][6] = SubSat[0];
            ViewAreaPoint[j][7] = SubSat[1];

            //System.out.println(ViewAreaPoint[j][0]+","+ViewAreaPoint[j][1]+","+ViewAreaPoint[j][2]+","+ViewAreaPoint[j][3]+","+ViewAreaPoint[j][4]+","+ViewAreaPoint[j][5]+","+ViewAreaPoint[j][6]+","+ViewAreaPoint[j][7]);
        }
    }

    //当前姿态下卫星视场四个顶点的经纬度
    private static void AttitudeViewCalculationold(double Position[], double Velocity[], double Satellite_LLA[], double Attitude[], double ViewInstall[][], double ViewAng[][], int ViewNum, double Time[], double Time_UTC, double ViewAreaPoint[][]) {
        double r = Math.sqrt(Math.pow(Position[0], 2) + Math.pow(Position[1], 2) + Math.pow(Position[2], 2));
        double theta = asin(Re / r);

        for (int j = 0; j < ViewNum; j++) {
            //顶点1
            double[][] r_install01 = {{cos(ViewInstall[j][0])}, {cos(ViewInstall[j][1])}, {cos(ViewInstall[j][2])}};
            double[][] R_x01 = new double[][]{{1, 0, 0}, {0, cos(ViewAng[j][0]), -sin(ViewAng[j][0])}, {0, sin(ViewAng[j][0]), cos(ViewAng[j][0])}};
            double[][] R_y01 = new double[][]{{cos(-ViewAng[j][3]), 0, sin(-ViewAng[j][3])}, {0, 1, 0}, {-sin(-ViewAng[j][3]), 0, cos(-ViewAng[j][3])}};
            double[][] r_install_mid01 = MatrixMultiplication(R_x01, r_install01);
            double[][] r_install_BF01 = MatrixMultiplication(R_y01, r_install_mid01);
            //东南地312转序姿态
            double x = Attitude[0];
            double y = Attitude[1];
            double z = Attitude[2];
            double[][] R_Attitude = new double[][]{{cos(y) * cos(z) + sin(y) * sin(x) * sin(z), -cos(y) * sin(z) + sin(y) * sin(x) * cos(z), sin(y) * cos(x)},
                    {cos(x) * sin(z), cos(x) * cos(z), -sin(x)},
                    {-sin(y) * cos(z) + cos(y) * sin(x) * sin(z), sin(y) * sin(z) + cos(y) * sin(x) * cos(z), cos(y) * cos(x)}};
            double[][] r_install_ESD01 = MatrixMultiplication(R_Attitude, r_install_BF01);
            double theta_install01 = acos(r_install_ESD01[2][0] / (r_install_ESD01[0][0] * r_install_ESD01[0][0] + r_install_ESD01[1][0] * r_install_ESD01[1][0] + r_install_ESD01[2][0] * r_install_ESD01[2][0]));
            double beta01;
            if (abs(theta_install01) >= theta) {
                beta01 = PI - theta;
            } else {
                beta01 = asin((sin(theta_install01) * r) / Re) - theta_install01;
            }
            //法线矢量
            double[] v101 = new double[]{r_install_ESD01[0][0], r_install_ESD01[1][0], r_install_ESD01[2][0]};
            double[] v201 = new double[]{0, 0, 1};
            double[] n01 = VectorCross(v101, v201);
            double[][] R_n01 = new double[3][3];
            R_n01[0][0] = n01[0] * n01[0] * (1 - Math.cos(beta01)) + Math.cos(beta01);
            R_n01[0][1] = n01[0] * n01[1] * (1 - Math.cos(beta01)) + n01[2] * Math.sin(beta01);
            R_n01[0][2] = n01[0] * n01[2] * (1 - Math.cos(beta01)) - n01[1] * Math.sin(beta01);
            R_n01[1][0] = n01[0] * n01[1] * (1 - Math.cos(beta01)) - n01[2] * Math.sin(beta01);
            R_n01[1][1] = n01[1] * n01[1] * (1 - Math.cos(beta01)) + Math.cos(beta01);
            R_n01[1][2] = n01[1] * n01[2] * (1 - Math.cos(beta01)) + n01[0] * Math.sin(beta01);
            R_n01[2][0] = n01[0] * n01[2] * (1 - Math.cos(beta01)) + n01[1] * Math.sin(beta01);
            R_n01[2][1] = n01[1] * n01[2] * (1 - Math.cos(beta01)) - n01[0] * Math.sin(beta01);
            R_n01[2][2] = n01[2] * n01[2] * (1 - Math.cos(beta01)) + Math.cos(beta01);
            double[][] r_Target_ESD01 = new double[3][1];
            double[] r_Target01_ECEF = new double[3];
            double[] r_Target01_GEI = new double[3];
            double[][] r_Satellite_ESD01 = new double[][]{{0}, {0}, {-r}};
            r_Target_ESD01 = MatrixMultiplication(R_n01, r_Satellite_ESD01);
            double[] r_Target_ESD01_mid = new double[]{r_Target_ESD01[0][0], r_Target_ESD01[1][0], r_Target_ESD01[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD01_mid, r_Target01_ECEF);
            double JD_Time = JD(Time);
            ECEFToICRS(JD_Time, r_Target01_ECEF, r_Target01_GEI);
            double SubSat[] = new double[3];
            double SubSat_GEI[] = new double[3];
            PosionToSubSat(r_Target01_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][0] = SubSat[0];
            ViewAreaPoint[j][1] = SubSat[1];
            //顶点2
            double[][] r_install02 = {{cos(ViewInstall[j][0])}, {cos(ViewInstall[j][1])}, {cos(ViewInstall[j][2])}};
            double[][] R_x02 = new double[][]{{1, 0, 0}, {0, cos(ViewAng[j][0]), -sin(ViewAng[j][0])}, {0, sin(ViewAng[j][0]), cos(ViewAng[j][0])}};
            double[][] R_y02 = new double[][]{{cos(ViewAng[j][3]), 0, sin(ViewAng[j][3])}, {0, 1, 0}, {-sin(ViewAng[j][3]), 0, cos(ViewAng[j][3])}};
            double[][] r_install_mid02 = MatrixMultiplication(R_x02, r_install02);
            double[][] r_install_BF02 = MatrixMultiplication(R_y02, r_install_mid02);
            double[][] r_install_ESD02 = MatrixMultiplication(R_Attitude, r_install_BF02);
            double theta_install02 = acos(r_install_ESD02[2][0] / (r_install_ESD02[0][0] * r_install_ESD02[0][0] + r_install_ESD02[1][0] * r_install_ESD02[1][0] + r_install_ESD02[2][0] * r_install_ESD02[2][0]));
            double beta02;
            if (abs(theta_install02) >= theta) {
                beta02 = PI - theta;
            } else {
                beta02 = asin((sin(theta_install02) * r) / Re) - theta_install02;
            }
            //法线矢量
            double[] v102 = new double[]{r_install_ESD02[0][0], r_install_ESD02[1][0], r_install_ESD02[2][0]};
            double[] v202 = new double[]{0, 0, 1};
            double[] n02 = VectorCross(v102, v202);
            double[][] R_n02 = new double[3][3];
            R_n02[0][0] = n02[0] * n02[0] * (1 - Math.cos(beta02)) + Math.cos(beta02);
            R_n02[0][1] = n02[0] * n02[1] * (1 - Math.cos(beta02)) + n02[2] * Math.sin(beta02);
            R_n02[0][2] = n02[0] * n02[2] * (1 - Math.cos(beta02)) - n02[1] * Math.sin(beta02);
            R_n02[1][0] = n02[0] * n02[1] * (1 - Math.cos(beta02)) - n02[2] * Math.sin(beta02);
            R_n02[1][1] = n02[1] * n02[1] * (1 - Math.cos(beta02)) + Math.cos(beta02);
            R_n02[1][2] = n02[1] * n02[2] * (1 - Math.cos(beta02)) + n02[0] * Math.sin(beta02);
            R_n02[2][0] = n02[0] * n02[2] * (1 - Math.cos(beta02)) + n02[1] * Math.sin(beta02);
            R_n02[2][1] = n02[1] * n02[2] * (1 - Math.cos(beta02)) - n02[0] * Math.sin(beta02);
            R_n02[2][2] = n02[2] * n02[2] * (1 - Math.cos(beta02)) + Math.cos(beta02);
            double[][] r_Target_ESD02 = new double[3][1];
            double[] r_Target02_ECEF = new double[3];
            double[] r_Target02_GEI = new double[3];
            double[][] r_Satellite_ESD02 = new double[][]{{0}, {0}, {-r}};
            r_Target_ESD02 = MatrixMultiplication(R_n02, r_Satellite_ESD02);
            double[] r_Target_ESD02_mid = new double[]{r_Target_ESD02[0][0], r_Target_ESD02[1][0], r_Target_ESD02[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD02_mid, r_Target02_ECEF);
            ECEFToICRS(JD_Time, r_Target02_ECEF, r_Target02_GEI);
            PosionToSubSat(r_Target02_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][2] = SubSat[0];
            ViewAreaPoint[j][3] = SubSat[1];
            //顶点3
            double[][] r_install03 = {{cos(ViewInstall[j][0])}, {cos(ViewInstall[j][1])}, {cos(ViewInstall[j][2])}};
            double[][] R_x03 = new double[][]{{1, 0, 0}, {0, cos(-ViewAng[j][0]), -sin(-ViewAng[j][0])}, {0, sin(-ViewAng[j][0]), cos(-ViewAng[j][0])}};
            double[][] R_y03 = new double[][]{{cos(ViewAng[j][3]), 0, sin(ViewAng[j][3])}, {0, 1, 0}, {-sin(ViewAng[j][3]), 0, cos(ViewAng[j][3])}};
            double[][] r_install_mid03 = MatrixMultiplication(R_x03, r_install03);
            double[][] r_install_BF03 = MatrixMultiplication(R_y03, r_install_mid03);
            double[][] r_install_ESD03 = MatrixMultiplication(R_Attitude, r_install_BF03);
            double theta_install03 = acos(r_install_ESD03[2][0] / (r_install_ESD03[0][0] * r_install_ESD03[0][0] + r_install_ESD03[1][0] * r_install_ESD03[1][0] + r_install_ESD03[2][0] * r_install_ESD03[2][0]));
            double beta03;
            if (abs(theta_install03) >= theta) {
                beta03 = PI - theta;
            } else {
                beta03 = asin((sin(theta_install03) * r) / Re) - theta_install03;
            }
            //法线矢量
            double[] v103 = new double[]{r_install_ESD03[0][0], r_install_ESD03[1][0], r_install_ESD03[2][0]};
            double[] v203 = new double[]{0, 0, 1};
            double[] n03 = VectorCross(v103, v203);
            double[][] R_n03 = new double[3][3];
            R_n03[0][0] = n03[0] * n03[0] * (1 - Math.cos(beta03)) + Math.cos(beta03);
            R_n03[0][1] = n03[0] * n03[1] * (1 - Math.cos(beta03)) + n03[2] * Math.sin(beta03);
            R_n03[0][2] = n03[0] * n03[2] * (1 - Math.cos(beta03)) - n03[1] * Math.sin(beta03);
            R_n03[1][0] = n03[0] * n03[1] * (1 - Math.cos(beta03)) - n03[2] * Math.sin(beta03);
            R_n03[1][1] = n03[1] * n03[1] * (1 - Math.cos(beta03)) + Math.cos(beta03);
            R_n03[1][2] = n03[1] * n03[2] * (1 - Math.cos(beta03)) + n03[0] * Math.sin(beta03);
            R_n03[2][0] = n03[0] * n03[2] * (1 - Math.cos(beta03)) + n03[1] * Math.sin(beta03);
            R_n03[2][1] = n03[1] * n03[2] * (1 - Math.cos(beta03)) - n03[0] * Math.sin(beta03);
            R_n03[2][2] = n03[2] * n03[2] * (1 - Math.cos(beta03)) + Math.cos(beta03);
            double[][] r_Target_ESD03 = new double[3][1];
            double[] r_Target03_ECEF = new double[3];
            double[] r_Target03_GEI = new double[3];
            double[][] r_Satellite_ESD03 = new double[][]{{0}, {0}, {-r}};
            r_Target_ESD03 = MatrixMultiplication(R_n03, r_Satellite_ESD03);
            double[] r_Target_ESD03_mid = new double[]{r_Target_ESD03[0][0], r_Target_ESD03[1][0], r_Target_ESD03[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD03_mid, r_Target03_ECEF);
            ECEFToICRS(JD_Time, r_Target03_ECEF, r_Target03_GEI);
            PosionToSubSat(r_Target03_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][4] = SubSat[0];
            ViewAreaPoint[j][5] = SubSat[1];
            //顶点4
            double[][] r_install04 = {{cos(ViewInstall[j][0])}, {cos(ViewInstall[j][1])}, {cos(ViewInstall[j][2])}};
            double[][] R_x04 = new double[][]{{1, 0, 0}, {0, cos(-ViewAng[j][0]), -sin(-ViewAng[j][0])}, {0, sin(-ViewAng[j][0]), cos(-ViewAng[j][0])}};
            double[][] R_y04 = new double[][]{{cos(-ViewAng[j][3]), 0, sin(-ViewAng[j][3])}, {0, 1, 0}, {-sin(-ViewAng[j][3]), 0, cos(-ViewAng[j][3])}};
            double[][] r_install_mid04 = MatrixMultiplication(R_x04, r_install04);
            double[][] r_install_BF04 = MatrixMultiplication(R_y04, r_install_mid04);
            double[][] r_install_ESD04 = MatrixMultiplication(R_Attitude, r_install_BF04);
            double theta_install04 = acos(r_install_ESD04[2][0] / (r_install_ESD04[0][0] * r_install_ESD04[0][0] + r_install_ESD04[1][0] * r_install_ESD04[1][0] + r_install_ESD04[2][0] * r_install_ESD04[2][0]));
            double beta04;
            if (abs(theta_install04) >= theta) {
                beta04 = PI - theta;
            } else {
                beta04 = asin((sin(theta_install04) * r) / Re) - theta_install04;
            }
            //法线矢量
            double[] v104 = new double[]{r_install_ESD04[0][0], r_install_ESD04[1][0], r_install_ESD04[2][0]};
            double[] v204 = new double[]{0, 0, 1};
            double[] n04 = VectorCross(v104, v204);
            double[][] R_n04 = new double[3][3];
            R_n04[0][0] = n04[0] * n04[0] * (1 - Math.cos(beta04)) + Math.cos(beta04);
            R_n04[0][1] = n04[0] * n04[1] * (1 - Math.cos(beta04)) + n04[2] * Math.sin(beta04);
            R_n04[0][2] = n04[0] * n04[2] * (1 - Math.cos(beta04)) - n04[1] * Math.sin(beta04);
            R_n04[1][0] = n04[0] * n04[1] * (1 - Math.cos(beta04)) - n04[2] * Math.sin(beta04);
            R_n04[1][1] = n04[1] * n04[1] * (1 - Math.cos(beta04)) + Math.cos(beta04);
            R_n04[1][2] = n04[1] * n04[2] * (1 - Math.cos(beta04)) + n04[0] * Math.sin(beta04);
            R_n04[2][0] = n04[0] * n04[2] * (1 - Math.cos(beta04)) + n04[1] * Math.sin(beta04);
            R_n04[2][1] = n04[1] * n04[2] * (1 - Math.cos(beta04)) - n04[0] * Math.sin(beta04);
            R_n04[2][2] = n04[2] * n04[2] * (1 - Math.cos(beta04)) + Math.cos(beta04);
            double[][] r_Target_ESD04 = new double[3][1];
            double[] r_Target04_ECEF = new double[3];
            double[] r_Target04_GEI = new double[3];
            double[][] r_Satellite_ESD04 = new double[][]{{0}, {0}, {-r}};
            r_Target_ESD04 = MatrixMultiplication(R_n04, r_Satellite_ESD04);
            double[] r_Target_ESD04_mid = new double[]{r_Target_ESD04[0][0], r_Target_ESD04[1][0], r_Target_ESD04[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD04_mid, r_Target04_ECEF);
            ECEFToICRS(JD_Time, r_Target04_ECEF, r_Target04_GEI);
            PosionToSubSat(r_Target04_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[j][6] = SubSat[0];
            ViewAreaPoint[j][7] = SubSat[1];
        }
    }

    //计算卫星可见走廊
    private static void ViewArea_ESD(double Position[], double Velocity[], double Satellite_LLA[], double ViewInstall[][], double ViewAng[][], int ViewNum, double RollMax, double Time[], double Time_UTC, double ViewAreaPoint[]) {


        double[] nv = {Velocity[0] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2)),
                Velocity[1] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2)),
                Velocity[2] / Math.sqrt(Math.pow(Velocity[0], 2) + Math.pow(Velocity[1], 2) + Math.pow(Velocity[2], 2))};
        double r = Math.sqrt(Math.pow(Position[0], 2) + Math.pow(Position[1], 2) + Math.pow(Position[2], 2));
        double theta = Math.asin(Re / r);

        double alpha, beta, theta_V, theta_xz, theta_yz, ViewAng_min;
        double R[][] = new double[3][3];
        double r_beta[] = new double[3];
        double SubSat[] = new double[3];
        double SubSat_GEI[] = new double[3];
        for (int j = 0; j < ViewNum; j++) {
            theta_xz = Math.atan(Math.cos(ViewInstall[j][0]) / Math.cos(ViewInstall[j][2]));
            theta_yz = Math.atan(Math.cos(ViewInstall[j][1]) / Math.cos(ViewInstall[j][2]));
            if ((ViewAng[j][2] + theta_yz + RollMax) >= theta) {
                theta_V = Math.asin(Re / r);
                beta = -(Math.PI / 2 - theta_V);
            } else {
                alpha = Math.asin((Math.sin(theta_yz + ViewAng[j][2] + RollMax) * r) / Re);
                beta = -(alpha - (theta_yz + ViewAng[j][2] + RollMax));
            }

            double[][] r_Satellite_ESD = new double[][]{{0}, {0}, {-r}};
            //double[][] R_x=new double[][]{{1, 0, 0}, {0, cos(beta), -sin(beta)}, {0, sin(beta), cos(beta)}};
            double[][] R_x = new double[][]{{cos(beta), 0, -sin(beta)}, {0, 1, 0}, {sin(beta), 0, cos(beta)}};
            double[][] r_Target_ESD = new double[3][1];
            double[] r_Target_ECEF = new double[3];
            double[] r_Target_GEI = new double[3];
            r_Target_ESD = MatrixMultiplication(R_x, r_Satellite_ESD);
            double[] r_Target_ESD_mid = new double[]{r_Target_ESD[0][0], r_Target_ESD[1][0], r_Target_ESD[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid, r_Target_ECEF);
            double JD_Time = JD(Time);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[4 * j + 0] = SubSat[0];
            ViewAreaPoint[4 * j + 1] = SubSat[1];

            if (Math.abs(theta_yz - ViewAng[j][3] - RollMax) >= theta) {
                theta_V = Math.asin(Re / r);
                beta = Math.PI / 2 - theta_V;
                //beta = -beta;
            } else {
                alpha = Math.asin((Math.sin(theta_yz - ViewAng[j][3] - RollMax) * r) / Re);
                beta = alpha - (theta_yz - ViewAng[j][3] - RollMax);
                beta = -beta;
            }
            //double[][] R_x_2=new double[][]{{1, 0, 0}, {0, cos(beta), -sin(beta)}, {0, sin(beta), cos(beta)}};
            double[][] R_x_2 = new double[][]{{cos(beta), 0, -sin(beta)}, {0, 1, 0}, {sin(beta), 0, cos(beta)}};
            r_Target_ESD = MatrixMultiplication(R_x_2, r_Satellite_ESD);
            double[] r_Target_ESD_mid_2 = new double[]{r_Target_ESD[0][0], r_Target_ESD[1][0], r_Target_ESD[2][0]};
            ESDToECEF(Satellite_LLA, r_Target_ESD_mid_2, r_Target_ECEF);
            ECEFToICRS(JD_Time, r_Target_ECEF, r_Target_GEI);
            PosionToSubSat(r_Target_GEI, Time, Time_UTC, SubSat, SubSat_GEI);
            ViewAreaPoint[4 * j + 2] = SubSat[0];
            ViewAreaPoint[4 * j + 3] = SubSat[1];
        }
    }

    //儒略日计算
    private static double JD(double Time[]) {
        double year_UT = Time[0];
        double month_UT = Time[1];
        double day_UT = Time[2];
        double hour_UT = Time[3];
        double minute_UT = Time[4];
        double second_UT = Time[5];

        double D = day_UT;
        double M, Y, B;
        double JD;
        if (month_UT == 1 || month_UT == 2) {
            M = month_UT + 12;
            Y = year_UT - 1;
        } else {
            Y = year_UT;
            M = month_UT;
        }
        B = 0;
        if (Y > 1582 || (Y == 1582 && M > 10) || (Y == 1582 && M == 10 && D >= 15)) {
            B = 2 - (int) Math.floor(Y / 100.0) + (int) Math.floor(Y / 400.0);
        }
        JD = (int) Math.floor(365.25 * (Y + 4716)) + (int) Math.floor(30.6 * (M + 1)) + D + B - 1524.5;
        JD = JD - 0.5 + hour_UT / 24.0 + minute_UT / 1440 + second_UT / 86400;
        JD = JD + 0.5;
        return JD;
    }

    //惯性系转地心地固
    private static void ECI_ECEF(double JD, double[] R, double[] sa) {
        double DtR = 3.1415926 / 180;
        double R_earth = 6378136.3;
        double eccent = 0.08182;
        double gast, Range, Radius, sphi, x, y, z;
        double[] R_ECEF = new double[3];
        //Compute GAST
        gast = app_sidereal_time(JD);
        gast = gast * DtR;
        //Rotate ECI vector to ECEF frame
        R_ECEF[0] = Math.cos(gast) * R[0] + Math.sin(gast) * R[1];
        R_ECEF[1] = -Math.sin(gast) * R[0] + Math.cos(gast) * R[1];
        R_ECEF[2] = R[2];
        //calculate geocentric lon, lat in radians
        x = R_ECEF[0];
        y = R_ECEF[1];
        z = R_ECEF[2];
        Range = x * x + y * y + z * z;
        Radius = Math.sqrt(Range);
        sa[0] = Math.atan2(y, x);//GeoLon
        sphi = z / Radius;
        sa[1] = Math.asin(sphi);//GeoLat
        //Altitude
        //*(sa+2) = Radius - R_earth;
        sa[2] = Radius - Math.sqrt(R_earth * R_earth * (1.0 - eccent * eccent) / (1.0 - Math.pow(eccent * Math.cos(sa[1]), 2)));
        return;
    }

    private static double app_sidereal_time(double JD) {
        double T_TDB = (JD - 2451545.0) / 36525.0;
        //double hour = (JD - (int) JD) * 24;
        double hour = (JD - (int) JD - 0.5) * 24;
        return mod(6.697374558 + 2400.05133691 * T_TDB + 2.586222 * 0.00001 * Math.pow(T_TDB, 2)
                - 1.722222 * 0.000000001 * Math.pow(T_TDB, 3) + 1.002737791737697 * hour, 24) * 15;
    }

    //求余
    private static double mod(double x, double y) {
        int n;
        n = (int) (x / y);
        return x - n * y;
    }

    //地固坐标系到卫星北东地坐标系
    private static void ECEFToNED(double[] Satellite_LLA, double[] Target_LLA, double[] Target_NED) {
        double[] Satellite_ECEF = new double[3];
        double[] Target_ECEF = new double[3];
        LLAToECEF(Satellite_LLA, Satellite_ECEF);
        LLAToECEF(Target_LLA, Target_ECEF);

        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double L = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(L), -sin(B) * sin(L), cos(B)},
                {-sin(L), cos(L), 0},
                {-cos(B) * cos(L), -cos(B) * sin(L), -sin(B)}};
        double[][] Error_r = new double[3][1];
        Error_r[0][0] = Target_ECEF[0] - Satellite_ECEF[0];
        Error_r[1][0] = Target_ECEF[1] - Satellite_ECEF[1];
        Error_r[2][0] = Target_ECEF[2] - Satellite_ECEF[2];
        double[][] Target_NED_mid = new double[3][1];
        Target_NED_mid = MatrixMultiplication(R_ECEFToNED, Error_r);
        Target_NED[0] = Target_NED_mid[0][0];
        Target_NED[1] = Target_NED_mid[1][0];
        Target_NED[2] = Target_NED_mid[2][0];
    }

    //地固坐标系到卫星东南地坐标系
    private static void ECEFToESD(double[] Satellite_LLA, double[] Target_LLA, double[] Target_ESD) {
        double[] Satellite_ECEF = new double[3];
        double[] Target_ECEF = new double[3];
        LLAToECEF(Satellite_LLA, Satellite_ECEF);
        LLAToECEF(Target_LLA, Target_ECEF);

        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double L = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(L), -sin(B) * sin(L), cos(B)},
                {-sin(L), cos(L), 0},
                {-cos(B) * cos(L), -cos(B) * sin(L), -sin(B)}};
        double[][] Error_r = new double[3][1];
        Error_r[0][0] = Target_ECEF[0] - Satellite_ECEF[0];
        Error_r[1][0] = Target_ECEF[1] - Satellite_ECEF[1];
        Error_r[2][0] = Target_ECEF[2] - Satellite_ECEF[2];
        double[][] Target_NED_mid = new double[3][1];
        Target_NED_mid = MatrixMultiplication(R_ECEFToNED, Error_r);
        double Ang_z = -PI / 2;
        double[][] R_NEDToESD = {{cos(Ang_z), -sin(Ang_z), 0},
                {sin(Ang_z), cos(Ang_z), 0},
                {0, 0, 1}};
        double[][] Target_ESD_mid = new double[3][1];
        Target_ESD_mid = MatrixMultiplication(R_NEDToESD, Target_NED_mid);
        Target_ESD[0] = Target_ESD_mid[0][0];
        Target_ESD[1] = Target_ESD_mid[1][0];
        Target_ESD[2] = Target_ESD_mid[2][0];
    }

    //地固坐标系到卫星东南地坐标系
    private static void ECEFToESDForAvoidSunshine(double[] Satellite_LLA, double[] Target_ECEF, double[] Target_ESD) {
        double[] Satellite_ECEF = new double[3];
        LLAToECEF(Satellite_LLA, Satellite_ECEF);

        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double L = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(L), -sin(B) * sin(L), cos(B)},
                {-sin(L), cos(L), 0},
                {-cos(B) * cos(L), -cos(B) * sin(L), -sin(B)}};
        double[][] Error_r = new double[3][1];
        Error_r[0][0] = Target_ECEF[0] - Satellite_ECEF[0];
        Error_r[1][0] = Target_ECEF[1] - Satellite_ECEF[1];
        Error_r[2][0] = Target_ECEF[2] - Satellite_ECEF[2];
        double[][] Target_NED_mid = new double[3][1];
        Target_NED_mid = MatrixMultiplication(R_ECEFToNED, Error_r);
        double Ang_z = -PI / 2;
        double[][] R_NEDToESD = {{cos(Ang_z), -sin(Ang_z), 0},
                {sin(Ang_z), cos(Ang_z), 0},
                {0, 0, 1}};
        double[][] Target_ESD_mid = new double[3][1];
        Target_ESD_mid = MatrixMultiplication(R_NEDToESD, Target_NED_mid);
        Target_ESD[0] = Target_ESD_mid[0][0];
        Target_ESD[1] = Target_ESD_mid[1][0];
        Target_ESD[2] = Target_ESD_mid[2][0];
    }

    //地固坐标系转到惯性坐标系
    private static void ECEFToICRS(double JD, double position_ECEF[], double position_GEI[]) {
        double T = (JD - 2451545.0) / 36525.0;

        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506 * T + 0.3019015 * T * T + 0.0179663 * T * T * T - 0.0000327 * T * T * T * T - 0.0000002 * T * T * T * T * T;//秒
        double Theta_A = 2004.1917476 * T - 0.4269353 * T * T - 0.041825 * T * T * T - 0.0000601 * T * T * T * T - 0.0000001 * T * T * T * T * T;
        double Z_A = -2.5976176 + 2306.0803226 * T + 1.094779 * T * T + 0.0182273 * T * T * T + 0.000047 * T * T * T * T - 0.0000003 * T * T * T * T * T;
        Zeta_A = Zeta_A / 3600.0;//度
        Theta_A = Theta_A / 3600.0;
        Z_A = Z_A / 3600.0;
        //岁差矩阵
        double[][] R3Z_A = {{cos(-Z_A * PI / 180.0), sin(-Z_A * PI / 180.0), 0},
                {-sin(-Z_A * PI / 180.0), cos(-Z_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R2Theta_A = {{cos(Theta_A * PI / 180.0), 0, -sin(Theta_A * PI / 180.0)},
                {0, 1, 0},
                {sin(Theta_A * PI / 180.0), 0, cos(Theta_A * PI / 180.0)}};
        double[][] R3_Zeta_A = {{cos(-Zeta_A * PI / 180.0), sin(-Zeta_A * PI / 180.0), 0},
                {-sin(-Zeta_A * PI / 180.0), cos(-Zeta_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] PR = new double[3][3];
        double[][] PR_mid = new double[3][3];
        PR_mid = MatrixMultiplication(R3Z_A, R2Theta_A);
        PR = MatrixMultiplication(PR_mid, R3_Zeta_A);

        //章动计算
        double Epsilon_A = 84381.448 - 46.8150 * T - 0.00059 * T * T + 0.001813 * T * T * T;
        Epsilon_A = Epsilon_A / 3600.0;
        // http://blog.sina.com.cn/s/blog_852e40660100w1m6.html
        double L = 280.4665 + 36000.7698 * T;
        double dL = 218.3165 + 481267.8813 * T;
        double Omega = 125.04452 - 1934.136261 * T;
        double DeltaPsi = -17.20 * sin(Omega * PI / 180.0) - 1.32 * sin(2 * L * PI / 180.0) - 0.23 * sin(2 * dL * PI / 180.0) + 0.21 * sin(2 * Omega * PI / 180.0);
        double DeltaEpsilon = 9.20 * cos(Omega * PI / 180.0) + 0.57 * cos(2 * L * PI / 180.0) + 0.10 * cos(2 * dL * PI / 180.0) - 0.09 * cos(2 * Omega * PI / 180.0);
        DeltaPsi = DeltaPsi / 3600.0;
        DeltaEpsilon = DeltaEpsilon / 3600.0;

        //章动矩阵
        double[][] R1_DEA = {{1, 0, 0},
                {0, cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)},
                {0, -sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)}};
        double[][] R3_DeltaPsi = {{cos(-DeltaPsi * PI / 180.0), sin(-DeltaPsi * PI / 180.0), 0},
                {-sin(-DeltaPsi * PI / 180.0), cos(-DeltaPsi * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R1_Epsilon = {{1, 0, 0},
                {0, cos(Epsilon_A * PI / 180.0), sin(Epsilon_A * PI / 180.0)},
                {0, -sin(Epsilon_A * PI / 180.0), cos(Epsilon_A * PI / 180.0)}};
        double[][] NR = new double[3][3];
        double[][] NR_mid = new double[3][3];
        NR_mid = MatrixMultiplication(R1_DEA, R3_DeltaPsi);
        NR = MatrixMultiplication(NR_mid, R1_Epsilon);

        //地球自转
        double GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0;
        GMST = GMST % 360;
        double GAST = GMST + DeltaPsi * cos((DeltaEpsilon + Epsilon_A) * PI / 180.0);
        GAST = GAST % 360;
        double[][] ER = {{cos(GAST * PI / 180.0), sin(GAST * PI / 180.0), 0},
                {-sin(GAST * PI / 180.0), cos(GAST * PI / 180.0), 0},
                {0, 0, 1}};

        //极移坐标
        //  https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html
        // https://datacenter.iers.org/data/html/finals.all.html
        double Xp = 0.001674 * 0.955;
        double Yp = 0.001462 * 0.955;
        // 极移矩阵
        double[][] R1_YP = {{1, 0, 0},
                {0, cos(-Yp * PI / 180.0), sin(-Yp * PI / 180.0)},
                {0, -sin(-Yp * PI / 180.0), cos(-Yp * PI / 180.0)}};
        double[][] R2_XP = {{cos(-Xp * PI / 180.0), 0, -sin(-Xp * PI / 180.0)},
                {0, 1, 0},
                {sin(-Xp * PI / 180.0), 0, cos(-Xp * PI / 180.0)}};
        double[][] EP = new double[3][3];
        EP = MatrixMultiplication(R1_YP, R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER = new double[3][3];
        double[][] EPERNR = new double[3][3];
        double[][] ECEF;
        EPER = MatrixMultiplication(EP, ER);
        EPERNR = MatrixMultiplication(EPER, NR);
        ECEF = MatrixMultiplication(EPERNR, PR);
        //地固坐标系到惯性坐标系的转换矩阵
        double[][] R_inv = new double[3][3];
        R_inv = MatrixInverse(ECEF);
        double[][] p_ECEF = {{position_ECEF[0]}, {position_ECEF[1]}, {position_ECEF[2]}};
        double[][] pp_GEI = new double[3][1];
        pp_GEI = MatrixMultiplication(R_inv, p_ECEF);

        position_GEI[0] = pp_GEI[0][0];
        position_GEI[1] = pp_GEI[1][0];
        position_GEI[2] = pp_GEI[2][0];
    }

    //惯性坐标系到地固坐标系转
    private static void ICRSToECEF(double[] Time, double position_GEI[], double position_ECEF[]) {
        double JD = JD(Time);
        double T = (JD - 2451545.0) / 36525.0;

        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506 * T + 0.3019015 * T * T + 0.0179663 * T * T * T - 0.0000327 * T * T * T * T - 0.0000002 * T * T * T * T * T;//秒
        double Theta_A = 2004.1917476 * T - 0.4269353 * T * T - 0.041825 * T * T * T - 0.0000601 * T * T * T * T - 0.0000001 * T * T * T * T * T;
        double Z_A = -2.5976176 + 2306.0803226 * T + 1.094779 * T * T + 0.0182273 * T * T * T + 0.000047 * T * T * T * T - 0.0000003 * T * T * T * T * T;
        Zeta_A = Zeta_A / 3600.0;//度
        Theta_A = Theta_A / 3600.0;
        Z_A = Z_A / 3600.0;
        //岁差矩阵
        double[][] R3Z_A = {{cos(-Z_A * PI / 180.0), sin(-Z_A * PI / 180.0), 0},
                {-sin(-Z_A * PI / 180.0), cos(-Z_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R2Theta_A = {{cos(Theta_A * PI / 180.0), 0, -sin(Theta_A * PI / 180.0)},
                {0, 1, 0},
                {sin(Theta_A * PI / 180.0), 0, cos(Theta_A * PI / 180.0)}};
        double[][] R3_Zeta_A = {{cos(-Zeta_A * PI / 180.0), sin(-Zeta_A * PI / 180.0), 0},
                {-sin(-Zeta_A * PI / 180.0), cos(-Zeta_A * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] PR = new double[3][3];
        double[][] PR_mid = new double[3][3];
        PR_mid = MatrixMultiplication(R3Z_A, R2Theta_A);
        PR = MatrixMultiplication(PR_mid, R3_Zeta_A);

        //章动计算
        double Epsilon_A = 84381.448 - 46.8150 * T - 0.00059 * T * T + 0.001813 * T * T * T;
        Epsilon_A = Epsilon_A / 3600.0;
        // http://blog.sina.com.cn/s/blog_852e40660100w1m6.html
        double L = 280.4665 + 36000.7698 * T;
        double dL = 218.3165 + 481267.8813 * T;
        double Omega = 125.04452 - 1934.136261 * T;
        double DeltaPsi = -17.20 * sin(Omega * PI / 180.0) - 1.32 * sin(2 * L * PI / 180.0) - 0.23 * sin(2 * dL * PI / 180.0) + 0.21 * sin(2 * Omega * PI / 180.0);
        double DeltaEpsilon = 9.20 * cos(Omega * PI / 180.0) + 0.57 * cos(2 * L * PI / 180.0) + 0.10 * cos(2 * dL * PI / 180.0) - 0.09 * cos(2 * Omega * PI / 180.0);
        DeltaPsi = DeltaPsi / 3600.0;
        DeltaEpsilon = DeltaEpsilon / 3600.0;

        //章动矩阵
        double[][] R1_DEA = {{1, 0, 0},
                {0, cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)},
                {0, -sin(-(DeltaEpsilon + Epsilon_A) * PI / 180.0), cos(-(DeltaEpsilon + Epsilon_A) * PI / 180.0)}};
        double[][] R3_DeltaPsi = {{cos(-DeltaPsi * PI / 180.0), sin(-DeltaPsi * PI / 180.0), 0},
                {-sin(-DeltaPsi * PI / 180.0), cos(-DeltaPsi * PI / 180.0), 0},
                {0, 0, 1}};
        double[][] R1_Epsilon = {{1, 0, 0},
                {0, cos(Epsilon_A * PI / 180.0), sin(Epsilon_A * PI / 180.0)},
                {0, -sin(Epsilon_A * PI / 180.0), cos(Epsilon_A * PI / 180.0)}};
        double[][] NR = new double[3][3];
        double[][] NR_mid = new double[3][3];
        NR_mid = MatrixMultiplication(R1_DEA, R3_DeltaPsi);
        NR = MatrixMultiplication(NR_mid, R1_Epsilon);

        //地球自转
        double GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0;
        GMST = GMST % 360;
        double GAST = GMST + DeltaPsi * cos((DeltaEpsilon + Epsilon_A) * PI / 180.0);
        GAST = GAST % 360;
        double[][] ER = {{cos(GAST * PI / 180.0), sin(GAST * PI / 180.0), 0},
                {-sin(GAST * PI / 180.0), cos(GAST * PI / 180.0), 0},
                {0, 0, 1}};

        //极移坐标
        //  https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html
        // https://datacenter.iers.org/data/html/finals.all.html
        double Xp = 0.001674 * 0.955;
        double Yp = 0.001462 * 0.955;
        // 极移矩阵
        double[][] R1_YP = {{1, 0, 0},
                {0, cos(-Yp * PI / 180.0), sin(-Yp * PI / 180.0)},
                {0, -sin(-Yp * PI / 180.0), cos(-Yp * PI / 180.0)}};
        double[][] R2_XP = {{cos(-Xp * PI / 180.0), 0, -sin(-Xp * PI / 180.0)},
                {0, 1, 0},
                {sin(-Xp * PI / 180.0), 0, cos(-Xp * PI / 180.0)}};
        double[][] EP = new double[3][3];
        EP = MatrixMultiplication(R1_YP, R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER = new double[3][3];
        double[][] EPERNR = new double[3][3];
        double[][] ECEF;
        EPER = MatrixMultiplication(EP, ER);
        EPERNR = MatrixMultiplication(EPER, NR);
        ECEF = MatrixMultiplication(EPERNR, PR);

        double[][] p_GEI = {{position_GEI[0]}, {position_GEI[1]}, {position_GEI[2]}};
        double[][] pp_ECEF = new double[3][1];
        pp_ECEF = MatrixMultiplication(ECEF, p_GEI);

        position_ECEF[0] = pp_ECEF[0][0];
        position_ECEF[1] = pp_ECEF[1][0];
        position_ECEF[2] = pp_ECEF[2][0];
    }

    //地固直角坐标系转换为地心地固坐标系
    private static void LLAToECEF(double Position_LLA[], double Position_ECEF[]) {
        double L = Position_LLA[0] * Math.PI / 180.0;
        double B = Position_LLA[1] * Math.PI / 180.0;
        double H = Position_LLA[2];

        Position_ECEF[0] = (Re + H) * Math.cos(B) * Math.cos(L);
        Position_ECEF[1] = (Re + H) * Math.cos(B) * Math.sin(L);
        Position_ECEF[2] = (Re + H) * Math.sin(B);
    }

    //惯性坐标系转到轨道坐标系
    private static void GEIToORF(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {SatPosition_GEI[0] / r, SatPosition_GEI[1] / r, SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        double[][] OR = {{xs[0], ys[0], zs[0]},
                {xs[1], ys[1], zs[1]},
                {xs[2], ys[2], zs[2]}};
        double[][] pS_GEI = {{Position_GEI[0] - SatPosition_GEI[0]}, {Position_GEI[1] - SatPosition_GEI[1]}, {Position_GEI[2] - SatPosition_GEI[2]}};
        double[][] pS_ORF = new double[3][1];
        pS_ORF = MatrixMultiplication(OR, pS_GEI);
        Position_ORF[0] = pS_ORF[0][0];
        Position_ORF[1] = pS_ORF[1][0];
        Position_ORF[2] = pS_ORF[2][0];
    }

    //惯性坐标系转到轨道坐标系，大椭圆轨道
    private static void GEIToORF_Ellipse(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        //System.out.println(Double.toString(xs[0])+","+Double.toString(xs[1])+","+Double.toString(xs[2]));
        //System.out.println(Double.toString(ys[0])+","+Double.toString(ys[1])+","+Double.toString(ys[2]));
        //System.out.println(Double.toString(zs[0])+","+Double.toString(zs[1])+","+Double.toString(zs[2]));
        double r_ys = sqrt(pow(ys[0], 2) + pow(ys[1], 2) + pow(ys[2], 2));
        ys[0] = ys[0] / r_ys;
        ys[1] = ys[1] / r_ys;
        ys[2] = ys[2] / r_ys;
        xs = VectorCross(ys, zs);
        /*
        double[][] OR = {{xs[0], ys[0], zs[0]},
                {xs[1], ys[1], zs[1]},
                {xs[2], ys[2], zs[2]}};
         */
        double[][] OR = {{xs[0], xs[1], xs[2]},
                {ys[0], ys[1], ys[2]},
                {zs[0], zs[1], zs[2]}};
        double[][] pS_GEI = {{Position_GEI[0]}, {Position_GEI[1]}, {Position_GEI[2]}};
        double[][] pS_ORF = new double[3][1];
        pS_ORF = MatrixMultiplication(OR, pS_GEI);
        Position_ORF[0] = pS_ORF[0][0];
        Position_ORF[1] = pS_ORF[1][0];
        Position_ORF[2] = pS_ORF[2][0];
    }

    //地固坐标系到卫星东南地坐标系
    private static void ESDToECEF(double[] Satellite_LLA, double[] Position_ESD, double[] Position_ECEF) {
        double[] Satellite_ECEF = new double[3];
        LLAToECEF(Satellite_LLA, Satellite_ECEF);

        double B = Satellite_LLA[1] * Math.PI / 180.0;//经度
        double L = Satellite_LLA[0] * Math.PI / 180.0;//纬度
        double[][] R_ECEFToNED = {{-sin(B) * cos(L), -sin(B) * sin(L), cos(B)},
                {-sin(L), cos(L), 0},
                {-cos(B) * cos(L), -cos(B) * sin(L), -sin(B)}};
        double[][] R_NEDToECEF = MatrixInverse(R_ECEFToNED);
        double[][] Target_ESD_mid = new double[3][1];
        double[][] Position_ESD_mid = new double[][]{{Position_ESD[0]}, {Position_ESD[1]}, {Position_ESD[2]}};
        Target_ESD_mid = MatrixMultiplication(R_NEDToECEF, Position_ESD_mid);
        Position_ECEF[0] = Target_ESD_mid[0][0];
        Position_ECEF[1] = Target_ESD_mid[1][0];
        Position_ECEF[2] = Target_ESD_mid[2][0];
    }

    //由卫星位置计算星下点的经纬度，以及卫星位置的经纬高，以及卫星星下点在地心赤道惯性系中的位置
    private static void PosionToSubSat(double posion_GEI[], double Time[], double Time_UTC, double SubSat[], double SubSat_GEI[]) {
        double omega = 1.0027 * 180 / 43200;//地球自转角速度，单位为：度/秒


        double x = posion_GEI[0];
        double y = posion_GEI[1];
        double z = posion_GEI[2];

        double Dec;
        if (x != 0 || y != 0) {
            Dec = Math.atan(z / (Math.pow((Math.pow(x, 2) + Math.pow(y, 2)), 0.5))) * 180 / Math.PI;
        } else {
            if (z > 0)
                Dec = 90;
            else
                Dec = -90;
        }
        double RA = 0;
        if (x > 0)
            RA = Math.atan(y / x) * 180 / Math.PI;
        if (x < 0) {
            if (y >= 0)
                RA = 180 + Math.atan(y / x) * 180 / Math.PI;
            else
                RA = -180 + Math.atan(y / x) * 180 / Math.PI;
        }
        if (x == 0) {
            if (y >= 0)
                RA = 90;
            else
                RA = -90;
        }

        double lat = Dec;
        double JD_Time = JD(Time);
        double GAST = Time_GAST(JD_Time);
        GAST = GAST + omega * Time_UTC;
        double lon = RA - GAST;

        //限定范围
        if (lon > 180)
            lon = lon - 360;
        else if (lon <= -180)
            lon = lon + 360;

        SubSat[0] = lon;
        SubSat[1] = lat;
        SubSat[2] = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - Re;

        double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        SubSat_GEI[0] = Re * x / r;
        SubSat_GEI[1] = Re * y / r;
        SubSat_GEI[2] = Re * z / r;
    }

    //计算参考时间的格林尼治赤经
    private static double Time_GAST(double JD) {
        double D = JD - 2451545.0;
        double T = D / 36525.0;
        double GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * Math.pow(T, 2) - Math.pow(T, 3) / 38710000.0;
        GMST = GMST % 360;
        double Epsilon_m = (23 + 26 / 60.0 + 21.448 / 3600.0) - (46.8150 / 3600.0) * T - (0.00059 / 3600.0) * Math.pow(T, 2) + (0.001813 / 3600.0) * Math.pow(T, 3);

        double L = 280.4665 + 36000.7698 * T;
        double dL = 218.3165 + 481267.8813 * T;
        double Omega = 125.04452 - 1934.136261 * T;
        double dPsi = -17.20 * Math.sin(Omega * Math.PI / 180.0) - 1.32 * Math.sin(2 * L * Math.PI / 180.0) - 0.23 * Math.sin(2 * dL * Math.PI / 180.0) + 0.21 * Math.sin(2 * Omega * Math.PI / 180.0);
        double dEpsilon = 9.20 * Math.cos(Omega * Math.PI / 180.0) + 0.57 * Math.cos(2 * L * Math.PI / 180.0) + 0.10 * Math.cos(2 * dL * Math.PI / 180.0) - 0.09 * Math.cos(2 * Omega * Math.PI / 180.0);
        dPsi = dPsi / 3600.0;
        dEpsilon = dEpsilon / 3600.0;

        double GAST = GMST + dPsi * Math.cos((Epsilon_m + dEpsilon) * Math.PI / 180.0);
        GAST = GAST % 360;

        return GAST;
    }

    //太阳矢量
    private static double Sun(double JD, double[] r_sun, double[] su) {
        double rad_sun;
        double T_TDB, L_sun, M_sun, C, lambda_sun, e, ecc, v;
        /*...Compute Julian centuries*/
        T_TDB = (JD - 2451545.0) / 36525.0;
        /*...Compute the Sun’s mean anomaly*/
        M_sun = 357.52911 + 35999.05029 * T_TDB - 0.0001537 * T_TDB * T_TDB;
        //M_sun = quadrant(M_sun);
        /*...Compute the Mean geometric longitude of the sun*/
        L_sun = 280.46646 + 36000.76983 * T_TDB + 0.0003032 * T_TDB * T_TDB;
        //L_sun = quadrant(L_sun);
        /*...Compute the centre*/
        C = (1.914602 - 0.004817 * T_TDB - 0.000014 * T_TDB * T_TDB) * Math.sin(M_sun * PI / 180.0)
                + (0.019993 - 0.000101 * T_TDB) * Math.sin(2 * M_sun * PI / 180.0) +
                0.000289 * Math.sin(3 * M_sun * PI / 180.0);
        /*...Compute true geometric longitude*/
        lambda_sun = L_sun + C;
        /*...Compute the mean obliquity of the ecliptic*/
        e = 23.439291 - 0.0130042 * T_TDB - 1.64e-07 * T_TDB * T_TDB
                + 5.04e-07 * T_TDB * T_TDB * T_TDB;
        /*...Compute the Eccentricty of Earth’s orbit*/
        ecc = 0.016708634 - 0.000042037 * T_TDB - 0.0000001267 * T_TDB * T_TDB;
        /*...Compute Sun’s true anomaly*/
        v = M_sun + C;
        /*...Compute radial distance from Earth to the Sun*/
        rad_sun = 1.000001018 * (1 - ecc * ecc) / (1 - ecc * Math.cos(v * PI / 180.0)); //in AU
        /*...Compute position of the sun in ECI*/
        r_sun[0] = rad_sun * Math.cos(lambda_sun * PI / 180.0);
        r_sun[1] = rad_sun * Math.cos(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0);
        r_sun[2] = rad_sun * Math.sin(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0);
        rad_sun = rad_sun * AUtokm * kmtom; //in meters
        for (int i = 0; i <= 2; i++) {
            r_sun[i] = r_sun[i] * AUtokm * kmtom;
        }
        /*...Compute the right ascension and declination*/
        su[0] = Math.atan2(Math.cos(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0),
                Math.cos(lambda_sun * PI / 180.0));
        //isu = su[0];
        //su[0] = QuadRad(isu);
        //su[0] =isu*180/PI;
        su[1] = Math.asin(Math.sin(e * PI / 180.0) * Math.sin(lambda_sun * PI / 180.0));
        return rad_sun;
    }

    //矩阵乘法
    private static double[][] MatrixMultiplication(double A[][], double B[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        int B_rowNum = B.length;
        int B_columnNum = B[0].length;
        if (A_columnNum != B_rowNum)
            JOptionPane.showMessageDialog(null, "乘法矩阵维数不一致", "矩阵乘法错误", JOptionPane.ERROR_MESSAGE);
        double[][] Result = new double[A_rowNum][B_columnNum];
        for (int i = 0; i < A_rowNum; i++) {
            for (int j = 0; j < B_columnNum; j++) {
                Result[i][j] = 0;
                for (int k = 0; k < A_columnNum; k++)
                    Result[i][j] = Result[i][j] + A[i][k] * B[k][j];
            }
        }
        return Result;
    }

    //矩阵求逆
    private static double[][] MatrixInverse(double A[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求逆矩阵不是方阵", "矩阵求逆错误", JOptionPane.ERROR_MESSAGE);

        double[][] Result = new double[A_rowNum][A_columnNum];
        double Matrix_R = MatrixResult(A);
        if (Matrix_R == 0)
            JOptionPane.showMessageDialog(null, "求逆矩阵的值为零", "矩阵求逆错误", JOptionPane.ERROR_MESSAGE);
        for (int i = 0; i < A_rowNum; i++) {
            for (int j = 0; j < A_columnNum; j++) {
                if ((i + j) % 2 == 0) {
                    Result[i][j] = MatrixResult(MatrixCofactor(A, i + 1, j + 1)) / Matrix_R;
                } else {
                    Result[i][j] = -MatrixResult(MatrixCofactor(A, i + 1, j + 1)) / Matrix_R;
                }
            }
        }
        Result = MatrixTransposition(Result);
        return Result;
    }

    //求矩阵(h,v)位置的余子式，用于矩阵求逆
    private static double[][] MatrixCofactor(double[][] A, int h, int v) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求余子式矩阵不是方阵", "矩阵求余子式错误", JOptionPane.ERROR_MESSAGE);
        double[][] Cofactor = new double[A_rowNum - 1][A_columnNum - 1];
        for (int i = 0; i < A_rowNum - 1; i++) {
            if (i < h - 1) {
                for (int j = 0; j < A_columnNum - 1; j++) {
                    if (j < v - 1)
                        Cofactor[i][j] = A[i][j];
                    else
                        Cofactor[i][j] = A[i][j + 1];
                }
            } else {
                for (int j = 0; j < A_columnNum - 1; j++) {
                    if (j < v - 1)
                        Cofactor[i][j] = A[i + 1][j];
                    else
                        Cofactor[i][j] = A[i + 1][j + 1];
                }
            }
        }
        return Cofactor;
    }

    //计算行列式的值
    private static double MatrixResult(double A[][]) {
        int A_rowNum = A.length;
        int A_columnNum = A[0].length;
        if (A_rowNum != A_columnNum)
            JOptionPane.showMessageDialog(null, "求矩阵值的矩阵不是方阵", "矩阵求值错误", JOptionPane.ERROR_MESSAGE);
        //一维矩阵计算
        if (A_rowNum == 1)
            return A[0][0];

        //二维矩阵计算
        if (A_rowNum == 2)
            return A[0][0] * A[1][1] - A[0][1] * A[1][0];

        //计算二维以上矩阵
        double result = 0;
        double[] nums = new double[A_rowNum];
        for (int i = 0; i < A_rowNum; i++) {
            if (i % 2 == 0) {
                nums[i] = A[0][i] * MatrixResult(MatrixCofactor(A, 1, i + 1));
            } else {
                nums[i] = -A[0][i] * MatrixResult(MatrixCofactor(A, 1, i + 1));
            }
        }
        for (int i = 0; i < A_rowNum; i++)
            result = result + nums[i];

        return result;
    }

    //矩阵的转置
    private static double[][] MatrixTransposition(double A[][]) {
        double[][] Result = new double[A[0].length][A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++)
                Result[j][i] = A[i][j];
        }
        return Result;
    }

    //矢量叉乘
    private static double[] VectorCross(double A[], double B[]) {
        if (A.length != 3 || B.length != 3)
            JOptionPane.showMessageDialog(null, "求矢量的叉乘输入不合法", "求矢量叉乘错误", JOptionPane.ERROR_MESSAGE);
        double[] Result = new double[3];
        Result[0] = A[1] * B[2] - A[2] * B[1];
        Result[1] = A[2] * B[0] - A[0] * B[2];
        Result[2] = A[0] * B[1] - A[1] * B[0];

        return Result;
    }

    //读取任务区域目标点
    private static ArrayList<double[]> GetRegionPoint(Document target_region) {
        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
        Document geomety = new Document();
        if (target_region.get("type").equals("Feature")) {
            geomety = (Document) target_region.get("geometry");
            CoordinatesList = GetGeometryPoint(geomety);
        } else if (target_region.get("type").equals("FeatureCollection")) {
            ArrayList<Document> features = (ArrayList<Document>) target_region.get("features");
            for (Document subfeatures : features) {
                geomety = (Document) subfeatures.get("geometry");
                ArrayList<double[]> subCoordinatesList = new ArrayList<double[]>();
                subCoordinatesList = GetGeometryPoint(geomety);
                for (double[] subsubCoordinatesList : subCoordinatesList) {
                    CoordinatesList.add(subsubCoordinatesList);
                }
            }
        } else if (target_region.get("type").equals("GeometryCollection")) {
            ArrayList<Document> geometries = (ArrayList<Document>) target_region.get("geometries");
            for (Document subgeometries : geometries) {
                ArrayList<double[]> subCoordinatesList = new ArrayList<double[]>();
                subCoordinatesList = GetGeometryPoint(subgeometries);
                for (double[] subsubCoordinatesList : subCoordinatesList) {
                    CoordinatesList.add(subsubCoordinatesList);
                }
            }
        } else {

        }

        return CoordinatesList;
    }

    private static ArrayList<double[]> GetGeometryPoint(Document geometry) {
        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
        if (geometry.get("type").equals("Point")) {
            ArrayList<Object> coordinates = (ArrayList<Object>) geometry.get("coordinates");
            double[] Target = new double[2];
            if (coordinates.get(0).getClass().getName().equals("java.lang.Double")) {
                Target[0] = (Double) coordinates.get(0);
            }else {
                Target[0] = (double) (Integer)coordinates.get(0);
            }
            if (coordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                Target[1] = (Double) coordinates.get(1);
            }else {
                Target[1] = (double)(Integer) coordinates.get(1);
            }
            CoordinatesList.add(Target);
        } else if (geometry.get("type").equals("LineString")) {
            ArrayList<ArrayList<Object>> coordinates = (ArrayList<ArrayList<Object>>) geometry.get("coordinates");
            for (ArrayList<Object> document : coordinates) {
                double[] Target = new double[2];
                if (document.get(0).getClass().getName().equals("java.lang.Double")) {
                    Target[0] = (Double) document.get(0);
                }else {
                    Target[0] = (double) (Integer)document.get(0);
                }
                if (document.get(1).getClass().getName().equals("java.lang.Double")) {
                    Target[1] = (Double) document.get(1);
                }else {
                    Target[1] = (double)(Integer) document.get(1);
                }
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("Polygon")) {
            ArrayList<ArrayList<ArrayList<Object>>> coordinates = (ArrayList<ArrayList<ArrayList<Object>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Object>> subcoordinates : coordinates) {
                for (ArrayList<Object> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    if (subsubcoordinates.get(0).getClass().getName().equals("java.lang.Double")) {
                        Target[0] = (Double) subsubcoordinates.get(0);
                    }else {
                        Target[0] = (double) (Integer)subsubcoordinates.get(0);
                    }
                    if (subsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                        Target[1] = (Double) subsubcoordinates.get(1);
                    }else {
                        Target[1] = (double)(Integer) subsubcoordinates.get(1);
                    }
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPoint")) {
            ArrayList<ArrayList<Object>> coordinates = (ArrayList<ArrayList<Object>>) geometry.get("coordinates");
            for (ArrayList<Object> document : coordinates) {
                double[] Target = new double[2];
                if (document.get(0).getClass().getName().equals("java.lang.Double")) {
                    Target[0] = (Double) document.get(0);
                }else {
                    Target[0] = (double) (Integer)document.get(0);
                }
                if (document.get(1).getClass().getName().equals("java.lang.Double")) {
                    Target[1] = (Double) document.get(1);
                }else {
                    Target[1] = (double)(Integer) document.get(1);
                }
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("MultiLineString")) {
            ArrayList<ArrayList<ArrayList<Object>>> coordinates = (ArrayList<ArrayList<ArrayList<Object>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Object>> subcoordinates : coordinates) {
                for (ArrayList<Object> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    if (subsubcoordinates.get(0).getClass().getName().equals("java.lang.Double")) {
                        Target[0] = (Double) subsubcoordinates.get(0);
                    }else {
                        Target[0] = (double) (Integer)subsubcoordinates.get(0);
                    }
                    if (subsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                        Target[1] = (Double) subsubcoordinates.get(1);
                    }else {
                        Target[1] = (double)(Integer) subsubcoordinates.get(1);
                    }
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPolygon")) {
            ArrayList<ArrayList<ArrayList<ArrayList<Object>>>> coordinates = (ArrayList<ArrayList<ArrayList<ArrayList<Object>>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<ArrayList<Object>>> subcoordinates : coordinates) {
                for (ArrayList<ArrayList<Object>> subsubcoordinates : subcoordinates) {
                    for (ArrayList<Object> subsubsubcoordinates : subsubcoordinates) {
                        double[] Target = new double[2];
                        if (subsubsubcoordinates.get(0).getClass().getName().equals("java.lang.Double")) {
                            Target[0] = (Double) subsubsubcoordinates.get(0);
                        }else {
                            Target[0] = (double) (Integer)subsubsubcoordinates.get(0);
                        }
                        if (subsubsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                            Target[1] = (Double) subsubsubcoordinates.get(1);
                        }else {
                            Target[1] = (double)(Integer) subsubsubcoordinates.get(1);
                        }
                        CoordinatesList.add(Target);
                    }
                }
            }
        }

        return CoordinatesList;
    }

}
