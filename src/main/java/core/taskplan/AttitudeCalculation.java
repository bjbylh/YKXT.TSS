package core.taskplan;

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

//import common.mongo.DbDefine;
//import common.mongo.MangoDBConnector;

public class AttitudeCalculation {
    //载荷变量
    // 载荷安装 矩阵，格式：每行代表一个载荷，每行格式[光轴与本体系x轴夹角，光轴与本体系y轴夹角，光轴与本体系z轴夹角]，单位：弧度
    private static int LoadNumber = 4;                    //载荷数量
    private static double[][] LoadInstall = {{90 * Math.PI * 180.0, 86.3 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 93.7 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 85.6 * Math.PI * 180.0, 3.7 * Math.PI * 180.0},
            {90 * Math.PI * 180.0, 94.4 * Math.PI * 180.0, 3.7 * Math.PI * 180.0}};
    //载荷视场角，格式：每行代表一个载荷，每行格式[内视角，外视角，上视角，下视角]，单位：弧度
    private static double[][] LoadViewAng = {{3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0},
            {3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0, 3 * Math.PI * 180.0}};

    //卫星变量
    //卫星最大机动能力，最大机动欧拉角，格式[绕x轴最大机动角度，绕y轴最大机动角度，绕z轴最大机动角度]，单位：弧度
    private static double[] SatelliteManeuverEuler = {5 * Math.PI / 180.0, 5 * Math.PI / 180.0, 5 * Math.PI / 180.0};
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

    public static void AttitudeCalculationII(Document Satllitejson, FindIterable<Document> Orbitjson, long OrbitDataCount, ArrayList<Document> ImageMissionjson) {
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
            } else
                continue;
        }

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
        }

        //任务读入
        MissionStarTime = new double[ImageMissionjson.size()][6];
        MissionStopTime = new double[ImageMissionjson.size()][6];
        MissionSerialNumber = new String[ImageMissionjson.size()];
        MissionImagingMode = new int[ImageMissionjson.size()];
        MissionTargetType = new int[ImageMissionjson.size()];
        MissionTargetArea = new double[ImageMissionjson.size()][20];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MisssionTargetHeight = new double[ImageMissionjson.size()][2];
        PlanningMissionLoad = new int[ImageMissionjson.size()];
        MissionNumber = 0;
        int[] TargetNum = new int[ImageMissionjson.size()];
        for (Document document : ImageMissionjson) {
            Document target_region = (Document) document.get("image_region");
            ArrayList<Document> features = (ArrayList<Document>) target_region.get("features");
            for (Document document1 : features) {
                Document geometry = (Document) document1.get("geometry");
                ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
                int i = 0;
                for (ArrayList<Double> document2 : coordinates) {
                    MissionTargetArea[MissionNumber][2 * i] = document2.get(0);
                    MissionTargetArea[MissionNumber][2 * i + 1] = document2.get(1);
                    i = i + 1;
                }
                TargetNum[MissionNumber] = coordinates.size();
            }
            ArrayList<Document> image_window = (ArrayList<Document>) document.get("image_window");
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
            MissionSerialNumber[MissionNumber] = document.getString("mission_number");
            if (document.getString("image_mode").equals("常规")) {
                MissionImagingMode[MissionNumber] = 1;
            }
            if (document.getString("image_type").equals("Point")) {
                MissionTargetType[MissionNumber] = 1;
            }
            MisssionTargetHeight[MissionNumber][0] = Double.parseDouble(document.getString("min_height_orbit"));
            MisssionTargetHeight[MissionNumber][1] = Double.parseDouble(document.getString("max_height_orbit"));
            MissionNumber = MissionNumber + 1;
        }

        //姿态计算，欧拉角1-2-3转序
        double[][] SatAttitud = new double[(int) OrbitDataCount][3];
        double[][] SatAttitudVel = new double[(int) OrbitalDataNum][3];

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        MongoCollection<Document> normal_attitude = mongoDatabase.getCollection("normal_attitude");

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
                        Target_LLA[2] = Re;
                        LoadNum = PlanningMissionLoad[j];
                        Mission_FLag = 1;
                        break;
                    } else {
                        double latSum = 0;
                        double lonSum = 0;
                        for (int k = 0; k < TargetNum[j]; k++) {
                            latSum = latSum + MissionTargetArea[j][2 * k];
                            lonSum = lonSum + MissionTargetArea[j][2 * k + 1];
                        }
                        Target_LLA[0] = latSum / TargetNum[j];
                        Target_LLA[1] = lonSum / TargetNum[j];
                        Target_LLA[2] = Re;
                        LoadNum = PlanningMissionLoad[j];
                        Mission_FLag = 1;
                        break;
                    }
                }
            }
            if (Mission_FLag == 1) {
                AttitudeCalculation(SatPosition_GEI[i], SatVelocity_GEI[i], Target_LLA, Time[i], LoadInstall[LoadNum], SatAttitud[i]);
                MissionFlag = true;
            } else {
                SatAttitud[i][0] = 0;
                SatAttitud[i][1] = 0;
                SatAttitud[i][2] = 0;
            }
            if (i == 0) {
                SatAttitudVel[i][0] = 0;
                SatAttitudVel[i][1] = 0;
                SatAttitudVel[i][2] = 0;
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
            }


            if (MissionFlag == true) {
                //数据输出，计算一步传出一组姿态数据
                Document jsonObject = new Document();

                jsonObject.append("yaw_angle", SatAttitud[i][2]);
                jsonObject.append("roll_angle", SatAttitud[i][0]);
                jsonObject.append("pitch_angle", SatAttitud[i][1]);
                jsonObject.append("V_yaw_angle", SatAttitudVel[i][2]);
                jsonObject.append("V_roll_angle", SatAttitudVel[i][0]);
                jsonObject.append("V_pitch_angle", SatAttitudVel[i][1]);
                jsonObject.append("time_point", Time_Point[i]);
                jsonObject.append("tag","1");
                Document modifiers = new Document();
                modifiers.append("$set", jsonObject);
                normal_attitude.updateOne(new Document("time_point", jsonObject.getDate("time_point")), modifiers, new UpdateOptions().upsert(true));
            }else {
                Document jsonObject = new Document();

                jsonObject.append("yaw_angle", SatAttitud[i][2]);
                jsonObject.append("roll_angle", SatAttitud[i][0]);
                jsonObject.append("pitch_angle", SatAttitud[i][1]);
                jsonObject.append("V_yaw_angle", SatAttitudVel[i][2]);
                jsonObject.append("V_roll_angle", SatAttitudVel[i][0]);
                jsonObject.append("V_pitch_angle", SatAttitudVel[i][1]);
                jsonObject.append("time_point", Time_Point[i]);
                jsonObject.append("tag","0");
                Document modifiers = new Document();
                modifiers.append("$set", jsonObject);
                normal_attitude.updateOne(new Document("time_point", jsonObject.getDate("time_point")), modifiers, new UpdateOptions().upsert(true));
            }

        }
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

    //地固坐标系转到惯性坐标系
    private static void ECEFToICRS(double JD, double position_ECEF[], double position_GEI[]) {
        double T = (JD - 2451545.0) / 36525.0;
        double z = 2306.2182 * T + 1.09468 * Math.pow(T, 2) + 0.018203 * Math.pow(T, 3);
        double theta = 2004.3109 * T + 0.42665 * Math.pow(T, 2) - 0.041833 * Math.pow(T, 3);
        double zeta = 2306.2181 * T + 0.30188 * Math.pow(T, 2) + 0.017998 * Math.pow(T, 3);
        z = (z / 3600.0) * Math.PI / 180.0;
        theta = (theta / 3600.0) * Math.PI / 180.0;
        zeta = (zeta / 3600.0) * Math.PI / 180.0;
        double[][] R_P = {{Math.cos(z) * Math.cos(theta) * Math.cos(zeta) - Math.sin(z) * Math.sin(zeta), -Math.cos(z) * Math.cos(theta) * Math.sin(zeta) - Math.sin(z) * Math.cos(zeta), -Math.cos(z) * Math.sin(theta)},
                {Math.sin(z) * Math.cos(theta) * Math.cos(zeta) + Math.cos(z) * Math.sin(zeta), -Math.sin(z) * Math.cos(theta) * Math.sin(zeta) + Math.cos(z) * Math.cos(zeta), -Math.sin(z) * Math.sin(theta)},
                {Math.sin(theta) * Math.cos(zeta), -Math.sin(theta) * Math.sin(zeta), Math.cos(theta)}};
        double epsilon = (23 + 26 / 60) * Math.PI / 180.0;
        double depsilon = (-10.697 / Math.pow(60, 3)) * Math.PI / 180.0;
        double dPsi = (-104.170 / Math.pow(60, 3)) * Math.PI / 180.0;
        double epsilon_r = epsilon + depsilon;
        double[][] R_N = {{Math.cos(dPsi), -Math.sin(dPsi) * Math.cos(epsilon), -Math.sin(dPsi) * Math.sin(epsilon)},
                {Math.sin(dPsi) * Math.cos(epsilon_r), Math.cos(dPsi) * Math.cos(epsilon_r) * Math.cos(epsilon) + Math.sin(epsilon_r) * Math.sin(epsilon), Math.cos(dPsi) * Math.cos(epsilon_r) * Math.sin(epsilon) - Math.sin(epsilon_r) * Math.cos(epsilon)},
                {Math.sin(dPsi) * Math.sin(epsilon_r), Math.cos(dPsi) * Math.sin(epsilon_r) * Math.cos(epsilon) - Math.cos(epsilon_r) * Math.sin(epsilon), Math.cos(dPsi) * Math.sin(epsilon_r) * Math.sin(epsilon) + Math.cos(epsilon_r) * Math.cos(epsilon)}};

        double GAST = Time_GAST(JD);
        double[][] R_S = {{Math.cos(GAST), Math.sin(GAST), 0},
                {-Math.sin(GAST), Math.cos(GAST), 0},
                {0, 0, 1}};

        double X_p = 48.775 / Math.pow(60, 3);
        double Y_p = 384.034 / Math.pow(60, 3);
        double[][] R_M = {{1, 0, X_p},
                {0, 1, -Y_p},
                {-X_p, Y_p, 1}};

        double[][] R = new double[3][3];
        R = MatrixMultiplication(MatrixMultiplication(MatrixMultiplication(R_P, R_N), R_S), R_M);
        double[][] R_inv = new double[3][3];
        R_inv = MatrixInverse(R);
        double[][] p_ECEF = {{position_ECEF[0]}, {position_ECEF[1]}, {position_ECEF[2]}};
        double[][] pp_GEI = new double[3][1];
        pp_GEI = MatrixMultiplication(R, p_ECEF);

        position_GEI[0] = pp_GEI[0][0];
        position_GEI[1] = pp_GEI[1][0];
        position_GEI[2] = pp_GEI[2][0];
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

}
