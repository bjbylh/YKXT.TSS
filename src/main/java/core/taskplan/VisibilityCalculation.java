package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static java.lang.Math.*;

//import common.mongo.MangoDBConnector;

//import common.mongo.MangoDBConnector;

public class VisibilityCalculation {
    //任务变量
    private static int MissionNumber;                   //任务数量
    private static double[][] MissionStarTime;          //任务起始时间，格式：每行代表一个任务，每行格式[年，月，日，时，分，秒]
    private static double[][] MissionStopTime;          //任务结束时间，格式：每行代表一个任务，每行格式[年，月，日，时，分，秒]
    private static String[] MissionSerialNumber;        //任务编号
    private static int[] MissionImagingMode;            //成像模式，格式：每行代表一个任务。1：常规模式，2：凝视模式，3：定标模式
    private static int[] MissionTargetType;             //成像目标类型，格式：每行代表一个任务。1：点目标，2：区域目标
    private static double[][] MissionTargetArea;        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
    private static double[][] MisssionTargetHeight;     //成像高度要求，格式：每行代表一个任务，每行格式[最低成像要求，最高成像要求]，单位：米
    private static int[] TargetNum;                     //成像区域的目标点个数
    private static int[][] MissionLoadType;//任务对相机的需求，格式，每行达标一个任务，每行格式[是否使用相机1，……]，1代表是，0代表否，第1,2列表示高分相机，第3,4列表示多光谱相机

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
    public static double[] SatelliteManeuverEuler = {5 * Math.PI * 180.0, 5 * Math.PI * 180.0, 5 * Math.PI * 180.0};

    //地面站变量
    private static int StationNumber;                   //地面站数量
    private static String[] StationSerialNumber;        //地面站编号，格式：每行代表一个地面站
    private static double[][] StationPosition;          //地面站位置，格式，每行代表一个地面站，每行格式：[地面站经度，地面站纬度，地面站高度]，高度单位：米
    private static double[] StationPitch;               //地面站最低仰角要求，格式：每行代表一个地面站，每行格式：最小仰角角度，单位：弧度
    private static double[][] StationMissionStar;           //地面站传输任务时间
    private static double[][] stationMissionStop;       //地面站传输任务时间


    //常量
    private static double Re = 6371393;                  //地球半径，单位为：米


    //可见弧段计算
    //VisibilityTimePeriod[k][i][]：第k个载荷第i个任务的可见弧段，两个一组，分别为可见弧段的起始索引和可见弧段的结束索引
    //TimePeriodNum[k][i]：第k个载荷第i个任务的可见弧段数量
    //Time：输入，轨道数据对应的时间
    //SatPosition_GEI：输入：卫星在惯性系下的位置
    //SatVelocity_GEI：输入：卫星在惯性系下的速度
    //SatPosition_LLA：输入：卫星的经纬高
    //Json格式
    public static Document VisibilityCalculationII(Document Satllitejson, FindIterable<Document> Orbitjson, long orbitDataCount, ArrayList<Document> GroundStationjson, ArrayList<Document> Missionjson) throws ParseException {
        //载荷参数更新
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        int ii = 0;
        for (Document document : properties) {

            if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "1")) {
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
            } else
                continue;

        }
        String StringTime;
        double[][] Time = new double[(int) orbitDataCount][6];
        double[][] SatPosition_GEI = new double[(int) orbitDataCount][3];
        double[][] SatVelocity_GEI = new double[(int) orbitDataCount][3];
        double[][] SatPosition_LLA = new double[(int) orbitDataCount][3];
        Date[] Time_Point = new Date[(int) orbitDataCount];

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

        //地面站资源读入
        StationSerialNumber = new String[GroundStationjson.size()];
        StationPosition = new double[GroundStationjson.size()][3];
        StationPitch = new double[GroundStationjson.size()];
        StationNumber = 0;
        for (Document document : GroundStationjson) {
            ArrayList<Document> Stationproperties = (ArrayList<Document>) document.get("properties");
            for (Document document1 : Stationproperties) {
                if (document1.getString("key").equals("station_name")) {
                    StationSerialNumber[StationNumber] = document1.getString("value");
                } else if (document1.getString("key").equals("station_lon")) {
                    StationPosition[StationNumber][0] = Double.parseDouble(document1.getString("value"));
                } else if (document1.getString("key").equals("station_lat")) {
                    StationPosition[StationNumber][1] = Double.parseDouble(document1.getString("value"));
                } else if (document1.getString("key").equals("altitude")) {
                    StationPosition[StationNumber][2] = Double.parseDouble(document1.getString("value"));
                } else if (document1.getString("key").equals("angle_pitch_min")) {
                    StationPitch[StationNumber] = Double.parseDouble(document1.getString("value")) * PI / 180.0;
                }
            }
            StationNumber = StationNumber + 1;
        }

        //任务读入
        MissionStarTime = new double[Missionjson.size()][6];
        MissionStopTime = new double[Missionjson.size()][6];
        MissionSerialNumber = new String[Missionjson.size()];
        MissionImagingMode = new int[Missionjson.size()];
        MissionTargetType = new int[Missionjson.size()];
        MissionTargetArea = new double[Missionjson.size()][20];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MisssionTargetHeight = new double[Missionjson.size()][2];
        MissionNumber = 0;
        TargetNum = new int[Missionjson.size()];
        for (Document document : Missionjson) {
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
            Date expected_start_time = document.getDate("expected_start_time");
            //时间转换为doubule型
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(expected_start_time);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            MissionStarTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
            MissionStarTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
            MissionStarTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
            MissionStarTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
            MissionStarTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
            MissionStarTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
            Date expected_end_time = document.getDate("expected_end_time");
            //时间转换为doubule型
            cal.setTime(expected_end_time);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            MissionStopTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
            MissionStopTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
            MissionStopTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
            MissionStopTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
            MissionStopTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
            MissionStopTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
            MissionSerialNumber[MissionNumber] = document.getString("mission_number");
            if (document.getString("image_mode").equals("常规")) {
                MissionImagingMode[MissionNumber] = 1;
            } else if (document.getString("image_mode").equals("凝视")) {
                MissionImagingMode[MissionNumber] = 2;
            } else if (document.getString("image_mode").equals("定标")) {
                MissionImagingMode[MissionNumber] = 3;
            }
            if (document.getString("image_type").equals("Point")) {
                MissionTargetType[MissionNumber] = 1;
            } else if (document.getString("image_type").equals("Polygon")) {
                MissionTargetType[MissionNumber] = 2;
            }
            MisssionTargetHeight[MissionNumber][0] = Double.parseDouble(document.getString("min_height_orbit"));
            MisssionTargetHeight[MissionNumber][1] = Double.parseDouble(document.getString("max_height_orbit"));
            MissionNumber = MissionNumber + 1;
        }

        //可见弧段计算
        double[] Target_LLA = new double[3];
        double[] Target_ECEF = new double[3];
        double[] Target_GEI = new double[3];
        double[] SatPositionRe_LLA = new double[3];
        double[] SatPosition_ECEF = new double[3];
        double Time_JD;
        int[][][] VisibilityTimePeriod = new int[LoadNumber][MissionNumber][100];
        int[][] TimePeriodNum = new int[LoadNumber][MissionNumber];

        //任务循环
        for (int i = 0; i < MissionNumber; i++) {
            for (int k = 0; k < LoadNumber; k++) {
                int Flag_tBefore = 0;
                int Visibility_Flag = 0;
                int Flag_t = 0;
                int PeriodNum = 0;
                //轨道数据循环
                int OrbitalStepPlus = 10;
                for (int j = 0; j < OrbitalDataNum; ) {
                    Visibility_Flag = VisibilityJudgeAll(i, j, k, Time[j], SatPosition_LLA[j], SatPosition_GEI[j], SatVelocity_GEI[j]);
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                    //判定开始结束时间，精确判断
                    if (j == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                        VisibilityTimePeriod[k][i][2 * PeriodNum] = j;
                    } else if (j != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                        for (int l = j - OrbitalStepPlus; l <= j; l++) {
                            Visibility_Flag = VisibilityJudgeAll(i, l, k, Time[l], SatPosition_LLA[l], SatPosition_GEI[l], SatVelocity_GEI[l]);
                            if (Visibility_Flag == 1) {
                                VisibilityTimePeriod[k][i][2 * PeriodNum] = l;
                                break;
                            }
                        }
                    } else if (Flag_tBefore == 1 && Flag_t == 0) {
                        for (int l = j - OrbitalStepPlus; l <= j; l++) {
                            Visibility_Flag = VisibilityJudgeAll(i, l, k, Time[l], SatPosition_LLA[l], SatPosition_GEI[l], SatVelocity_GEI[l]);
                            if (Visibility_Flag == 0) {
                                VisibilityTimePeriod[k][i][2 * PeriodNum + 1] = l - 1;
                                PeriodNum = PeriodNum + 1;
                                break;
                            }
                        }
                    } else if (j == OrbitalDataNum - 1 && Flag_t == 1) {
                        VisibilityTimePeriod[k][i][2 * PeriodNum + 1] = j;
                        PeriodNum = PeriodNum + 1;
                    }
                    j = j + OrbitalStepPlus;
                }
                TimePeriodNum[k][i] = PeriodNum;
            }
        }

        //地面站可见弧段
        int[][] StationVisibilityTimePeriod = new int[StationNumber][10];
        int[] StationTimePeriodNum = new int[StationNumber];

        for (int i = 0; i < StationNumber; i++) {
            int Side_Flag = 0;
            int Flag_tBefore = 0;
            int Visibility_Flag = 0;
            int Flag_t = 0;
            int PeriodNum = 0;
            Target_LLA[0] = StationPosition[i][0];
            Target_LLA[1] = StationPosition[i][1];
            Target_LLA[2] = StationPosition[i][2] + Re;
            for (int j = 0; j < OrbitalDataNum; ) {
                LLAToECEF(Target_LLA, Target_ECEF);
                Time_JD = JD(Time[j]);
                ECEFToICRS(Time_JD, Target_ECEF, Target_GEI);
                SatPositionRe_LLA[0] = SatPosition_LLA[j][0];
                SatPositionRe_LLA[1] = SatPosition_LLA[j][1];
                SatPositionRe_LLA[2] = SatPosition_LLA[j][2] + Re;
                LLAToECEF(SatPositionRe_LLA, SatPosition_ECEF);
                Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                if (Side_Flag == 1) {
                    Visibility_Flag = StationVisibilityJudge(Target_ECEF, SatPosition_ECEF, StationPitch[i]);
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                } else {
                    Visibility_Flag = 0;
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                }

                if (Flag_tBefore == 0 && Flag_t == 1) {
                    StationVisibilityTimePeriod[i][2 * PeriodNum] = j;
                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                    StationVisibilityTimePeriod[i][2 * PeriodNum + 1] = j - 1;
                    PeriodNum = PeriodNum + 1;
                }
                if (j == OrbitalDataNum - 1 && Flag_t == 1) {
                    StationVisibilityTimePeriod[i][2 * PeriodNum + 1] = j;
                    PeriodNum = PeriodNum + 1;
                }
                j += 1;
            }
            StationTimePeriodNum[i] = PeriodNum;
        }
        /*
        TimePeriodNum[k][i]表示第k个载荷，第i个任务的可见弧段个数
        VisibilityTimePeriod[k][i][2*l+0]表示第k个载荷，第i个任务，第l个弧段开始时刻的索引号，对应开始时间为Time[VisibilityTimePeriod[k][i][2*j]][]
        VisibilityTimePeriod[k][i][2*l+1]表示第k个载荷，第i个任务，第l个弧段开始时刻的索引号，对应开始时间为Time[VisibilityTimePeriod[k][i][2*j]][]
        StationTimePeriodNum[i]表示第i个地面站的可见弧段个数
        StationVisibilityTimePeriod[i][2*l+0]表示第i个地面站，第l个弧段开始时刻的索引号，对应开始时间为Time[VisibilityTimePeriod[k][i][2*j]][]
        StationVisibilityTimePeriod[i][2*l+1]表示第i个地面站，第l个弧段结束时刻的索引号，对应开始时间为Time[VisibilityTimePeriod[k][i][2*j]][]
         */
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        ArrayList<String> mission_numbers = new ArrayList<>();

        String transmission_number = "tn_" + Instant.now().toEpochMilli();
        //数据传出
        for (int i = 0; i < MissionNumber; i++) {
            int WindowsNum = 0;
//            JsonArray AvailWindowjsonArray=new JsonArray();
            ArrayList<Document> AvailWindowjsonArray = new ArrayList<>();
            for (int j = 0; j < LoadNumber; j++) {
                for (int k = 0; k < TimePeriodNum[j][i]; k++) {
//                    JsonObject AvailWindowjsonObject = new JsonObject();
                    Document AvailWindowjsonObject = new Document();
                    AvailWindowjsonObject.append("load_number", j + 1);
                    AvailWindowjsonObject.append("amount_window", TimePeriodNum[j][i]);
                    AvailWindowjsonObject.append("window_number", k + 1);
                    AvailWindowjsonObject.append("window_start_time", Time_Point[VisibilityTimePeriod[j][i][2 * k]]);
                    AvailWindowjsonObject.append("window_end_time", Time_Point[VisibilityTimePeriod[j][i][2 * k + 1]]);
                    AvailWindowjsonArray.add(AvailWindowjsonObject);
                    WindowsNum = WindowsNum + 1;
                }
            }
            if (WindowsNum == 0) {
                Missionjson.get(i).append("fail_reason", "不可见");
            } else {
                Missionjson.get(i).append("available_window", AvailWindowjsonArray);
            }
            Missionjson.get(i).append("transmission_number", transmission_number);
            mission_numbers.add(Missionjson.get(i).getString("mission_number"));
            Document modifiers = new Document();
            modifiers.append("$set", Missionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("_id", Missionjson.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
        }


        MongoCollection<Document> transmission_misison = mongoDatabase.getCollection("transmission_mission");

        Document Transmissionjson = new Document();
        Transmissionjson.append("transmission_number", transmission_number);
        Transmissionjson.append("mission_numbers", mission_numbers);

        ArrayList<Document> stationInfos = new ArrayList<>();
        int WindowsNum = 0;

        for (int i = 0; i < StationNumber; i++) {

            Document stationInfo = new Document();
            stationInfo.append("station_name", StationSerialNumber[i]);

//            JsonArray StationWindowjsonArray=new JsonArray();
            ArrayList<Document> StationWindowjsonArray = new ArrayList<>();
            for (int j = 0; j < StationTimePeriodNum[i]; j++) {
//                JsonObject StationWindowjsonObject = new JsonObject();
                Document StationWindowjsonObject = new Document();
                StationWindowjsonObject.append("amount_window", StationTimePeriodNum[i]);
                StationWindowjsonObject.append("window_number", j + 1);
                StationWindowjsonObject.append("window_start_time", Time_Point[StationVisibilityTimePeriod[i][2 * j]]);
                StationWindowjsonObject.append("window_end_time", Time_Point[StationVisibilityTimePeriod[i][2 * j + 1]]);
                StationWindowjsonArray.add(StationWindowjsonObject);
                WindowsNum = WindowsNum + 1;
            }

            stationInfo.append("available_window", StationWindowjsonArray);
            stationInfos.add(stationInfo);

        }
        if (WindowsNum == 0) {
            Transmissionjson.append("fail_reason", "不可见");
        } else {
            Transmissionjson.append("fail_reason", "");
        }
        Transmissionjson.append("station_info", stationInfos);
        transmission_misison.insertOne(Transmissionjson);

        return Transmissionjson;
        //transmission_misison.insertOne(Transmissionjson);
//        Document modifiers = new Document();


//        modifiers.append("$set", Transmissionjson);

        //       transmission_misison.updateOne(new Document("time_point", Transmissionjson.getDate("time_point")).append("station_number", Transmissionjson.getString("station_number")), modifiers, new UpdateOptions().upsert(true));
    }

    //应急任务可见性计算
    public static Map<String, Boolean> VisibilityCalculationEmergency(Document Satllitejson, FindIterable<Document> Orbitjson, long orbitDataCount, ArrayList<Document> GroundStationjson, ArrayList<Document> OrderMissionjson) throws ParseException {
        //载荷参数更新
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        int ii = 0;
        for (Document document : properties) {

            if (document.getString("key").equals("out_side_sight") && document.getString("group").equals("payload" + "1")) {
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
            } else
                continue;

        }
        String StringTime;
        double[][] Time = new double[(int) orbitDataCount][6];
        double[][] SatPosition_GEI = new double[(int) orbitDataCount][3];
        double[][] SatVelocity_GEI = new double[(int) orbitDataCount][3];
        double[][] SatPosition_LLA = new double[(int) orbitDataCount][3];
        Date[] Time_Point = new Date[(int) orbitDataCount];

        int OrbitalDataNum = 0;
        for (Document document : Orbitjson) {
            Date time_point = document.getDate("time_point");
            //时间转换为doubule型
            Calendar cal = Calendar.getInstance();
            cal.setTime(time_point);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        MissionStarTime = new double[OrderMissionjson.size()][6];
        MissionStopTime = new double[OrderMissionjson.size()][6];
        MissionSerialNumber = new String[OrderMissionjson.size()];
        MissionImagingMode = new int[OrderMissionjson.size()];
        MissionTargetType = new int[OrderMissionjson.size()];
        MissionTargetArea = new double[OrderMissionjson.size()][20];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MisssionTargetHeight = new double[OrderMissionjson.size()][2];
        MissionLoadType = new int[OrderMissionjson.size()][4];
        MissionNumber = 0;
        TargetNum = new int[OrderMissionjson.size()];
        for (Document document : OrderMissionjson) {
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
            ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
            if (expected_cam.size() == 0) {
                MissionLoadType[MissionNumber][0] = 1;
                MissionLoadType[MissionNumber][1] = 1;
                MissionLoadType[MissionNumber][2] = 1;
                MissionLoadType[MissionNumber][3] = 1;
            } else {
                for (Document document1 : expected_cam) {
                    if (document1.getString("sensor_name").equals("高分相机1")) {
                        MissionLoadType[MissionNumber][0] = 1;
                    } else if (document1.getString("sensor_name").equals("高分相机2")) {
                        MissionLoadType[MissionNumber][1] = 1;
                    } else if (document1.getString("sensor_name").equals("多光谱相机1")) {
                        MissionLoadType[MissionNumber][2] = 1;
                    } else if (document1.getString("sensor_name").equals("多光谱相机2")) {
                        MissionLoadType[MissionNumber][3] = 1;
                    }
                }
            }

            Date expected_start_time = document.getDate("expected_start_time");
            //时间转换为doubule型
            Calendar cal = Calendar.getInstance();
            cal.setTime(expected_start_time);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            StringTime = sdf.format(cal.getTime());
            MissionStarTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
            MissionStarTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
            MissionStarTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
            MissionStarTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
            MissionStarTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
            MissionStarTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
            Date expected_end_time = document.getDate("expected_end_time");
            //时间转换为doubule型
            cal.setTime(expected_end_time);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            MissionStopTime[MissionNumber][0] = Double.parseDouble(StringTime.substring(0, 4));
            MissionStopTime[MissionNumber][1] = Double.parseDouble(StringTime.substring(5, 7));
            MissionStopTime[MissionNumber][2] = Double.parseDouble(StringTime.substring(8, 10));
            MissionStopTime[MissionNumber][3] = Double.parseDouble(StringTime.substring(11, 13));
            MissionStopTime[MissionNumber][4] = Double.parseDouble(StringTime.substring(14, 16));
            MissionStopTime[MissionNumber][5] = Double.parseDouble(StringTime.substring(17, 19));
            MissionSerialNumber[MissionNumber] = document.getString("order_number");
            if (document.getString("image_mode").equals("常规")) {
                MissionImagingMode[MissionNumber] = 1;
            } else if (document.getString("image_mode").equals("凝视")) {
                MissionImagingMode[MissionNumber] = 2;
            } else if (document.getString("image_mode").equals("定标")) {
                MissionImagingMode[MissionNumber] = 3;
            }
            if (document.getString("image_type").equals("Point")) {
                MissionTargetType[MissionNumber] = 1;
            } else if (document.getString("image_type").equals("Polygon")) {
                MissionTargetType[MissionNumber] = 2;
            }
            MisssionTargetHeight[MissionNumber][0] = Double.parseDouble(document.get("min_height_orbit").toString());
            MisssionTargetHeight[MissionNumber][1] = Double.parseDouble(document.get("max_height_orbit").toString());
            MissionNumber = MissionNumber + 1;
        }

        //可见弧段计算
        double[] Target_LLA = new double[3];
        double[] Target_ECEF = new double[3];
        double[] Target_GEI = new double[3];
        double[] SatPositionRe_LLA = new double[3];
        double[] SatPosition_ECEF = new double[3];
        double Time_JD;
        int[][][] VisibilityTimePeriod = new int[LoadNumber][MissionNumber][100];
        int[][] TimePeriodNum = new int[LoadNumber][MissionNumber];

        //任务循环
        for (int i = 0; i < MissionNumber; i++) {
            for (int k = 0; k < LoadNumber; k++) {
                if (MissionLoadType[i][k] == 1) {
                    int Side_Flag = 0;
                    int High_Flag = 0;
                    int Flag_tBefore = 0;
                    int Visibility_Flag = 0;
                    int Flag_t = 0;
                    int PeriodNum = 0;
                    //轨道数据循环
                    int OrbitalStepPlus = 10;
                    for (int j = 0; j < OrbitalDataNum; ) {
                        Visibility_Flag = VisibilityJudgeAll(i, j, k, Time[j], SatPosition_LLA[j], SatPosition_GEI[j], SatVelocity_GEI[j]);
                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                        //判定开始结束时间，精确判断
                        if (Flag_tBefore == 0 && Flag_t == 1 && j == 0) {
                            VisibilityTimePeriod[k][i][2 * PeriodNum] = j;
                        } else if (Flag_tBefore == 0 && Flag_t == 1 && j != 0) {
                            for (int l = j - OrbitalStepPlus; l <= j; l++) {
                                Visibility_Flag = VisibilityJudgeAll(i, l, k, Time[l], SatPosition_LLA[l], SatPosition_GEI[l], SatVelocity_GEI[l]);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimePeriod[k][i][2 * PeriodNum] = l;
                                    break;
                                }
                            }
                        } else if (Flag_tBefore == 1 && Flag_t == 0) {
                            for (int l = j - OrbitalStepPlus; l <= j; l++) {
                                Visibility_Flag = VisibilityJudgeAll(i, l, k, Time[l], SatPosition_LLA[l], SatPosition_GEI[l], SatVelocity_GEI[l]);
                                if (Visibility_Flag == 0) {
                                    VisibilityTimePeriod[k][i][2 * PeriodNum + 1] = l - 1;
                                    PeriodNum = PeriodNum + 1;
                                    break;
                                }
                            }
                        } else if (j == OrbitalDataNum - 1 && Flag_t == 1) {
                            VisibilityTimePeriod[k][i][2 * PeriodNum + 1] = j;
                            PeriodNum = PeriodNum + 1;
                        }
                        j = j + OrbitalStepPlus;
                    }
                    TimePeriodNum[k][i] = PeriodNum;
                }
            }
        }

        //返回应急任务可见性结果
        Map<String, Boolean> map = new TreeMap<String, Boolean>();
        for (int i = 0; i < MissionNumber; i++) {
            int TimePeriodNumSum = 0;
            for (int j = 0; j < LoadNumber; j++) {
                TimePeriodNumSum = TimePeriodNumSum + TimePeriodNum[j][i];
            }
            if (TimePeriodNumSum >= 1) {
                map.put(MissionSerialNumber[i], true);
            } else {
                map.put(MissionSerialNumber[i], false);
            }
        }
        return map;
    }


    //可见性判定
    private static int VisibilityJudgeAll(int i, int j, int k, double[] NowTime, double[] SatPosition_LLA, double[] SatPosition_GEI, double[] SatVelocity_GEI) {
        int Visibility_Flag = 0;

        double NowTime_JD = JD(NowTime);
        double MissonStarTime_JD = JD(MissionStarTime[i]);
        double MissionStopTime_JD = JD(MissionStopTime[i]);

        if (NowTime_JD < MissonStarTime_JD || NowTime_JD > MissionStopTime_JD) {
            Visibility_Flag = 0;
        } else {
            int[] Area_Flag = new int[TargetNum[i]];
            for (int t = 0; t < TargetNum[i]; t++) {
                double[] Target_LLA = new double[3];
                double[] Target_ECEF = new double[3];
                double[] Target_GEI = new double[3];
                double[] SatPositionRe_LLA = new double[3];
                double[] SatPosition_ECEF = new double[3];

                Target_LLA[0] = MissionTargetArea[i][2 * t];
                Target_LLA[1] = MissionTargetArea[i][2 * t + 1];
                Target_LLA[2] = Re;
                LLAToECEF(Target_LLA, Target_ECEF);
                ECEFToICRS(NowTime_JD, Target_ECEF, Target_GEI);
                SatPositionRe_LLA[0] = SatPosition_LLA[0];
                SatPositionRe_LLA[1] = SatPosition_LLA[1];
                SatPositionRe_LLA[2] = SatPosition_LLA[2] + Re;
                LLAToECEF(SatPositionRe_LLA, SatPosition_ECEF);
                int Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                int High_Flag = HighJudge(SatPosition_LLA, MisssionTargetHeight[i]);
                if (Side_Flag == 1 && High_Flag == 1) {
                    Area_Flag[t] = VisibilityJudge(Target_GEI, SatPosition_GEI, SatVelocity_GEI, LoadInstall[k], LoadViewAng[k], SatelliteManeuverEuler);
                } else {
                    Area_Flag[t] = 0;
                }
            }
            int AreaFlagSum = 0;
            for (int l = 0; l < TargetNum[i]; l++) {
                AreaFlagSum = AreaFlagSum + Area_Flag[l];
            }
            if (AreaFlagSum == TargetNum[i]) {
                Visibility_Flag = 1;
            } else {
                Visibility_Flag = 0;
            }
        }
        return Visibility_Flag;
    }


    //判定地面目标是否与卫星在同一侧
    //返回1表示在同一侧，返回0代表不在同一侧
    private static int SideJudge(double Target_ECEF[], double SatPosition_ECEF[]) {
        double theta_max = Math.asin(Re / Math.sqrt(Math.pow(SatPosition_ECEF[0], 2) + Math.pow(SatPosition_ECEF[1], 2) + Math.pow(SatPosition_ECEF[2], 2)));
        double a = Target_ECEF[0] * SatPosition_ECEF[0] + Target_ECEF[1] * SatPosition_ECEF[1] + Target_ECEF[2] * SatPosition_ECEF[2];
        double b = Math.sqrt(Math.pow(Target_ECEF[0], 2) + Math.pow(Target_ECEF[1], 2) + Math.pow(Target_ECEF[2], 2));
        double c = Math.sqrt(Math.pow(SatPosition_ECEF[0], 2) + Math.pow(SatPosition_ECEF[1], 2) + Math.pow(SatPosition_ECEF[2], 2));
        double theta = Math.acos(a / (b * c));

        theta_max = PI / 2 - theta_max;

        int Side_Flag;
        if (theta_max >= theta)
            Side_Flag = 1;
        else
            Side_Flag = 0;
        return Side_Flag;
    }

    //判定卫星高度是否满足任务需求
    //返回1表示满足任务要求，返回0表示不满足任务要求
    private static int HighJudge(double SatPosition_LLA[], double TargetHeight[]) {
        int High_Flag;
        if (SatPosition_LLA[2] >= TargetHeight[0] && SatPosition_LLA[2] <= TargetHeight[1])
            High_Flag = 1;
        else
            High_Flag = 0;
        return High_Flag;
    }

    //判断目标点是否可见
    //返回1表示可见，返回0表示不可见
    private static int VisibilityJudge(double Target_GEI[], double SatPosition_GEI[], double SatVelocity_GEI[], double ViewInstall[], double ViewAngle[], double Euler_Max[]) {
        int Visibility_Flag;

        double[] SatTarget_GEI = {Target_GEI[0] - SatPosition_GEI[0], Target_GEI[1] - SatPosition_GEI[1], Target_GEI[2] - SatPosition_GEI[2]};
        double[] SatTarget_ORF = new double[3];
        GEIToORF(SatPosition_GEI, SatVelocity_GEI, SatTarget_GEI, SatTarget_ORF);

        double theta_xz = Math.atan(SatTarget_ORF[0] / SatTarget_ORF[2]);
        double theta_yz = Math.atan(SatTarget_ORF[1] / SatTarget_ORF[2]);

        double OpticalTheta_xz = Math.atan(Math.cos(ViewInstall[0]) / Math.cos(ViewInstall[2]));
        double OpticalTheta_yz = Math.atan(Math.cos(ViewInstall[1]) / Math.cos(ViewInstall[2]));
        double ViewAng_Min = ViewAngle[0];
        for (int i = 1; i < 4; i++) {
            if (ViewAng_Min > ViewAngle[i])
                ViewAng_Min = ViewAngle[i];
        }
        double[] ViewTheta_xz = {OpticalTheta_xz - ViewAng_Min - Euler_Max[1], OpticalTheta_xz + ViewAng_Min + Euler_Max[1]};
        double[] ViewTheta_yz = {OpticalTheta_yz - ViewAng_Min - Euler_Max[0], OpticalTheta_yz + ViewAng_Min + Euler_Max[0]};

        if (theta_xz >= ViewTheta_xz[0] && theta_xz <= ViewTheta_xz[1] && theta_yz >= ViewTheta_yz[0] && theta_yz <= ViewTheta_yz[1])
            Visibility_Flag = 1;
        else
            Visibility_Flag = 0;

        return Visibility_Flag;
    }

    //判断地面站是否可见
    private static int StationVisibilityJudge(double Target_ECEF[], double SatPosition_ECEF[], double StationPitch) {
        double[] error = new double[3];
        error[0] = SatPosition_ECEF[0] - Target_ECEF[0];
        error[1] = SatPosition_ECEF[1] - Target_ECEF[1];
        error[2] = SatPosition_ECEF[2] - Target_ECEF[2];
        double r_error = sqrt(error[0] * error[0] + error[1] * error[1] + error[2] * error[2]);
        double r_Target = sqrt(Target_ECEF[0] * Target_ECEF[0] + Target_ECEF[1] * Target_ECEF[1] + Target_ECEF[2] * Target_ECEF[2]);
        double a = error[0] * Target_ECEF[0] + error[1] * Target_ECEF[1] + error[2] * Target_ECEF[2];
        if (a / (r_error * r_Target) >= cos(StationPitch))
            return 1;
        else
            return 0;
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
