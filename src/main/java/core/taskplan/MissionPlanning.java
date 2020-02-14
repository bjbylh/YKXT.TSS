package core.taskplan;

//import com.company.MangoDBConnector;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.*;

//import common.mongo.DbDefine;
//import common.mongo.MangoDBConnector;

//import common.mongo.MangoDBConnector;

//import common.mongo.MangoDBConnector;

public class MissionPlanning {

    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;

    //阳光规避弧段
    private static int[] SunAvoidTimePeriod = new int[100];
    private static int SunAvoidTimePeriodNum;

    //常量
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double Step = 1;                        //数据步长
    private static int TimeZone = -8;                     //北京时区到世界时-8

    //卫星资源参数
    private static int LoadNumber = 4;                    //载荷数量
    private static double[][] LoadInstall = {{90 * Math.PI / 180.0, 86.3 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 93.7 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 85.6 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 94.4 * Math.PI / 180.0, 3.7 * Math.PI / 180.0}};
    //载荷视场角，格式：每行代表一个载荷，每行格式[内视角，外视角，上视角，下视角]，单位：弧度
    private static double[][] LoadViewAng = {{3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0}};
    //卫星变量
    //卫星最大机动能力，最大机动欧拉角，格式[绕x轴最大机动角度，绕y轴最大机动角度，绕z轴最大机动角度]，单位：弧度
    private static double[] SatelliteManeuverEuler = {5 * Math.PI / 180.0, 5 * Math.PI / 180.0, 5 * Math.PI / 180.0};
    private static double[] SatelliteManeuverVelocity = {10 * Math.PI / 180.0, 10 * Math.PI / 180.0, 10 * Math.PI / 180.0};//最大机动角速度
    private static double SateFlashData=0.5; //数据固存用量阈值

    private static double ImageTimeMin = 100;//任务间最短时间

    //地面站变量
    private static int StationNumber;                   //地面站数量
    private static String[] StationSerialNumber;        //地面站编号，格式：每行代表一个地面站
    private static double[][] StationPosition;          //地面站位置，格式，每行代表一个地面站，每行格式：[地面站经度，地面站纬度，地面站高度]，高度单位：米
    private static double[] StationPitch;               //地面站最低仰角要求，格式：每行代表一个地面站，每行格式：最小仰角角度，单位：弧度
    private static double[][] StationMissionStar;           //地面站传输任务时间
    private static double[][] stationMissionStop;       //地面站传输任务时间

    //地面站任务变量
    private static double[][] StationMissionStarTime;//传输任务期望开始时间
    private static double[][] StationMissionEndTime;//传输任务期望结束时间
    private static int StationMissionNum;//传输任务个数

    //轨道数据变量
    private static int OrbitalDataNum;
    private static double[][] Orbital_Time;
    private static double[][] Orbital_SatPosition;
    private static double[][] Orbital_SatVelocity;
    private static Date[] Time_Point;
    private static double[][] Orbital_SatPositionLLA;

    //任务变量
    private static int MissionNumber;                   //任务数量
    private static int[] MissionTargetNum;
    private static String[] MissionSerialNumber = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};        //任务编号
    private static int[] MissionImagingMode;            //成像模式，格式：每行代表一个任务。1：常规模式，2：凝视模式，3：定标模式
    private static int[] MissionTargetType;             //成像目标类型，格式：每行代表一个任务。1：点目标，2：区域目标
    private static double[][] MissionTargetArea;        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
    private static double[] MissionStareTime;//最小成像时长
    private static int[] MissionPriority;//任务优先级
    private static int[][] MissionLoadType;//任务对相机的需求，格式，每行达标一个任务，每行格式[是否使用相机1，……]，1代表是，0代表否，第1,2列表示高分相机，第3,4列表示多光谱相机
    //可见性输出
    //int[][][] VisibilityTimePeriod=new int[4][MissionNumber][20];
    //int[][] TimePeriodNum=new int[4][MissionNumber];
    private static int[][][] VisibilityTimePeriod;
    private static int[][] TimePeriodNum;
    //任务规划输出
    private static int[] PlanningMissionFailReason;//任务规划结果，0表示未规划，1表示规划成功，2表示无可见弧段，3表示任务冲突
    private static int[][] PlanningMissionTimePeriod;//任务弧段
    private static int[] PlanningMissionLoad;//为任务分配的载荷
    private static int[][] PlanningTransTimePeriod = new int[100][2];
    private static int[] PlanningTransStation = new int[100];
    private static int PlanningTransNum;


    public static void MissionPlanningII(Document Satllitejson, ArrayList<Document> GroundStationjson, FindIterable<Document> Orbitjson, long OrbitDataCount, ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson, ArrayList<Document> StationMissionJson,Document PoolFileJson) {
        //载荷参数更新
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        int ii = 0;
        String auto_obliterateFlag="false";
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
            } else if (document.getString("key").equals("image_time_min")) {
                ImageTimeMin = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("storage_threshold")) {
                SateFlashData=Double.parseDouble(document.get("value").toString());
            }else if (document.getString("key").equals("auto_obliterate")) {
                auto_obliterateFlag=document.get("value").toString();
            }else
                continue;
        }


        //首先判定是否生成固存清除任务
        ArrayList<Integer> PoolFileCanUse=new ArrayList<>();
        if (auto_obliterateFlag.equals("true")) {
            if (PoolFileJson != null) {
                Document PoolFileDataChild=PoolFileJson;
                double FlashDataNow=Double.parseDouble(PoolFileDataChild.get("flash_usage").toString());
                if (FlashDataNow >= SateFlashData) {
                    int sum=64;
                    for (int i = 0; i < 64; i++) {
                        String fileKey="file_"+i;
                        try {
                            Document FileChild= (Document) PoolFileDataChild.get(fileKey);
                            if (FileChild.get("valid").toString().equals("false") && FileChild.get("replayed").toString().equals("true")) {
                                sum--;
                            }else if (FileChild.get("valid").toString().equals("true")) {
                                sum--;
                                int CanuseNo=i;
                                PoolFileCanUse.add(i);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                    if (sum == 0) {
                        //获取时间
                        Date time_point=new Date();
                        try {
                            for (Document document : Orbitjson) {
                                time_point = document.getDate("time_point");
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //生成固存擦除任务
                        //数据传出
                        MongoClient mongoClient = MangoDBConnector.getClient();
                        //获取名为"temp"的数据库
                        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

                        Document FileClearMissionjson = new Document();
                        String mission_number = "im_" + Instant.now().toEpochMilli();
                        FileClearMissionjson.append("name", "固存擦除任务");
                        FileClearMissionjson.append("expected_start_time", time_point);
                        FileClearMissionjson.append("expected_end_time", time_point);
                        FileClearMissionjson.append("work_mode", "擦除");
                        FileClearMissionjson.append("mission_number", mission_number);
                        FileClearMissionjson.append("mission_state", "待执行");
                        FileClearMissionjson.append("mission_params", null);
                        FileClearMissionjson.append("clear_all", true);
                        FileClearMissionjson.append("clear_filenos", new ArrayList<Document>());

                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.insertOne(FileClearMissionjson);

                        mongoClient.close();
                    }
                }
            }
        }


        //获取时间
        Date time_point2=new Date();
        try {
            for (Document document : Orbitjson) {
                time_point2 = document.getDate("time_point");
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //地面站资源读入
        StationSerialNumber = new String[GroundStationjson.size()];
        StationPosition = new double[GroundStationjson.size()][3];
        StationPitch = new double[GroundStationjson.size()];
        StationNumber = 0;
        ArrayList<String> StationCode=new ArrayList<>();
        for (Document document : GroundStationjson) {
            String StationCode_i= document.get("ground_station_code").toString();
            StationCode.add(StationNumber,StationCode_i);
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

        //轨道数据读入
        Orbital_Time = new double[(int) OrbitDataCount][6];
        Orbital_SatPosition = new double[(int) OrbitDataCount][3];
        Orbital_SatVelocity = new double[(int) OrbitDataCount][3];
        Time_Point = new Date[(int) OrbitDataCount];
        Orbital_SatPositionLLA = new double[(int) OrbitDataCount][3];
        OrbitalDataNum = 0;

        ArrayList<double[]> OrbitTimeList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<>();
        for (Document document : Orbitjson) {
            Date time_point = document.getDate("time_point");
            //时间转换为doubule型
            String StringTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(time_point);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            Time_Point[OrbitalDataNum] = time_point;
            Orbital_Time[OrbitalDataNum][0] = Double.parseDouble(StringTime.substring(0, 4));
            Orbital_Time[OrbitalDataNum][1] = Double.parseDouble(StringTime.substring(5, 7));
            Orbital_Time[OrbitalDataNum][2] = Double.parseDouble(StringTime.substring(8, 10));
            Orbital_Time[OrbitalDataNum][3] = Double.parseDouble(StringTime.substring(11, 13));
            Orbital_Time[OrbitalDataNum][4] = Double.parseDouble(StringTime.substring(14, 16));
            Orbital_Time[OrbitalDataNum][5] = Double.parseDouble(StringTime.substring(17, 19));

            Orbital_SatPosition[OrbitalDataNum][0] = Double.parseDouble(document.get("P_x").toString());
            Orbital_SatPosition[OrbitalDataNum][1] = Double.parseDouble(document.get("P_y").toString());
            Orbital_SatPosition[OrbitalDataNum][2] = Double.parseDouble(document.get("P_z").toString());
            Orbital_SatVelocity[OrbitalDataNum][0] = Double.parseDouble(document.get("Vx").toString());
            Orbital_SatVelocity[OrbitalDataNum][1] = Double.parseDouble(document.get("Vy").toString());
            Orbital_SatVelocity[OrbitalDataNum][2] = Double.parseDouble(document.get("Vz").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][0] = Double.parseDouble(document.get("lon").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][1] = Double.parseDouble(document.get("lat").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][2] = Double.parseDouble(document.get("H").toString());

            //读取轨道时间戳
            double[] OrbitTime_iList = new double[6];
            OrbitTime_iList = DateToDouble(time_point);
            OrbitTimeList.add(OrbitalDataNum, OrbitTime_iList);
            //读取惯性系轨道位置
            double[] SatPositionGEI = new double[3];
            SatPositionGEI[0] = Orbital_SatPosition[OrbitalDataNum][0];
            SatPositionGEI[1] = Orbital_SatPosition[OrbitalDataNum][1];
            SatPositionGEI[2] = Orbital_SatPosition[OrbitalDataNum][2];
            OrbitSatPositionGEIList.add(OrbitalDataNum, SatPositionGEI);
            //读取惯性系轨道速度
            double[] SatVelocityGEI = new double[3];
            SatVelocityGEI[0] = Orbital_SatVelocity[OrbitalDataNum][0];
            SatVelocityGEI[1] = Orbital_SatVelocity[OrbitalDataNum][1];
            SatVelocityGEI[2] = Orbital_SatVelocity[OrbitalDataNum][2];
            OrbitSatVelocityGEIList.add(OrbitalDataNum, SatVelocityGEI);

            OrbitalDataNum = OrbitalDataNum + 1;

            if(OrbitalDataNum >= OrbitDataCount)
                break;
        }

        ArrayList<String> StationMissionTransmissionNumList=new ArrayList<>();
        ArrayList<String> StationMissionStationNameList=new ArrayList<>();
        ArrayList<String> StationMissionNumberList=new ArrayList<>();
        ArrayList<String> StationMissionTranNumberList=new ArrayList<>();
        //地面站任务读入
        if (StationMissionJson.size()==0){
            StationMissionNum = 1;
            StationMissionStarTime = new double[1][6];
            StationMissionEndTime=new double[1][6];
            for (int i = 0; i < 6; i++) {
                StationMissionStarTime[0][i]=Orbital_Time[1][i];
                StationMissionEndTime[0][i]=Orbital_Time[0][i];
            }
            StationMissionStationNameList.add(0,"nullName");
            StationMissionNumberList.add(0,"nullNumber");
        }else {
            StationMissionStarTime = new double[StationMissionJson.size()][6];
            StationMissionEndTime=new double[StationMissionJson.size()][6];
            StationMissionNum = 0;
            for (Document document : StationMissionJson) {
                try {
                    Date time_point=document.getDate("expected_start_time");
                    String StringTime;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(time_point);
                    cal.add(Calendar.HOUR_OF_DAY, -8);
                    StringTime = sdf.format(cal.getTime());
                    StationMissionStarTime[StationMissionNum][0] = Double.parseDouble(StringTime.substring(0, 4));
                    StationMissionStarTime[StationMissionNum][1] = Double.parseDouble(StringTime.substring(5, 7));
                    StationMissionStarTime[StationMissionNum][2] = Double.parseDouble(StringTime.substring(8, 10));
                    StationMissionStarTime[StationMissionNum][3] = Double.parseDouble(StringTime.substring(11, 13));
                    StationMissionStarTime[StationMissionNum][4] = Double.parseDouble(StringTime.substring(14, 16));
                    StationMissionStarTime[StationMissionNum][5] = Double.parseDouble(StringTime.substring(17, 19));
                    time_point=document.getDate("expected_end_time");
                    cal.setTime(time_point);
                    cal.add(Calendar.HOUR_OF_DAY, -8);
                    StringTime = sdf.format(cal.getTime());
                    StationMissionEndTime[StationMissionNum][0] = Double.parseDouble(StringTime.substring(0, 4));
                    StationMissionEndTime[StationMissionNum][1] = Double.parseDouble(StringTime.substring(5, 7));
                    StationMissionEndTime[StationMissionNum][2] = Double.parseDouble(StringTime.substring(8, 10));
                    StationMissionEndTime[StationMissionNum][3] = Double.parseDouble(StringTime.substring(11, 13));
                    StationMissionEndTime[StationMissionNum][4] = Double.parseDouble(StringTime.substring(14, 16));
                    StationMissionEndTime[StationMissionNum][5] = Double.parseDouble(StringTime.substring(17, 19));

                    //String TransmissionNumber= document.get("transmission_number").toString();
                    //StationMissionTransmissionNumList.add(StationMissionNum,TransmissionNumber);
                    String StationName=document.get("station_number").toString();
                    StationMissionStationNameList.add(StationMissionNum,StationName);
                    String StationmissionNumber_iList=document.get("mission_number").toString();
                    StationMissionNumberList.add(StationMissionNum,StationmissionNumber_iList);
                    if (document.containsKey("transmission_number") && document.get("transmission_number")!=null) {
                        StationMissionTranNumberList.add(StationMissionNum,document.get("transmission_number").toString());
                    }
                    StationMissionNum=StationMissionNum+1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }


        //任务读入
        //恒星定标、临边观测
        ArrayList<Document> MissionStarDocument=new ArrayList<>();
        ArrayList<Document> MissionLimbDocument=new ArrayList<>();
        ArrayList<Document> ImageMissionTemp=new ArrayList<>();
        for (Document document:ImageMissionjson) {
            //将恒星定标和临边单独提出
            if (document.get("image_mode").toString().equals("恒星定标")) {
                MissionStarDocument.add(document);
            }else if (document.get("image_mode").toString().equals("临边观测")){
                MissionLimbDocument.add(document);
            }else {
                ImageMissionTemp.add(document);
            }
        }
        ImageMissionjson.clear();
        for (Document document:ImageMissionTemp) {
            ImageMissionjson.add(document);
        }

        MissionNumber = ImageMissionjson.size();
        MissionTargetNum = new int[MissionNumber];
        MissionTargetArea = new double[MissionNumber][200];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MissionLoadType = new int[MissionNumber][LoadNumber];
        TimePeriodNum = new int[LoadNumber][MissionNumber];
        VisibilityTimePeriod = new int[LoadNumber][MissionNumber][200];
        MissionStareTime = new double[MissionNumber];
        MissionImagingMode = new int[MissionNumber];
        MissionPriority = new int[MissionNumber];
        PlanningMissionFailReason = new int[MissionNumber];
        PlanningMissionLoad = new int[MissionNumber];
        MissionTargetType = new int[MissionNumber];
        for (int i = 0; i < MissionNumber; i++) {
            PlanningMissionFailReason[i] = 0;
        }
        Date[][][] VisibilityDatePeriod = new Date[LoadNumber][MissionNumber][200];
        for (int i = 0; i < MissionNumber; i++) {
            for (int j = 0; j < LoadNumber; j++) {
                MissionLoadType[i][j] = 0;
            }
        }

        ArrayList<ArrayList<String>> MissionForOrderNumbers=new ArrayList<>();
        MissionNumber = 0;

        for (Document document : ImageMissionjson) {
            if (document.get("mission_state").equals("被退回")) {
                continue;
            }else{
                Document target_region = (Document) document.get("image_region");
                //读取目标区域
                ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                MissionTargetArea_iList = GetRegionPoint(target_region);
                MissionTargetNum[MissionNumber] = MissionTargetArea_iList.size();
                if (MissionTargetNum[MissionNumber] > 90) {
                    MissionTargetNum[MissionNumber] = 90;
                }
                for (int i = 0; i < MissionTargetNum[MissionNumber]; i++) {
                    MissionTargetArea[MissionNumber][2 * i] = MissionTargetArea_iList.get(i)[0];
                    MissionTargetArea[MissionNumber][2 * i + 1] = MissionTargetArea_iList.get(i)[1];
                }
                ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                if (expected_cam.size() == 0) {
                    MissionLoadType[MissionNumber][0] = 1;
                    MissionLoadType[MissionNumber][1] = 1;
                    MissionLoadType[MissionNumber][2] = 1;
                    MissionLoadType[MissionNumber][3] = 1;
                }else {
                    try {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType[MissionNumber][0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType[MissionNumber][1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType[MissionNumber][2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType[MissionNumber][3] = 1;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        MissionLoadType[MissionNumber][0] = 1;
                        MissionLoadType[MissionNumber][1] = 1;
                        MissionLoadType[MissionNumber][2] = 1;
                        MissionLoadType[MissionNumber][3] = 1;
                    }
                }
                ArrayList<Document> available_window = (ArrayList<Document>) document.get("available_window");
                if (available_window == null) {
                    TimePeriodNum[0][MissionNumber]=0;
                    TimePeriodNum[1][MissionNumber]=0;
                    TimePeriodNum[2][MissionNumber]=0;
                    TimePeriodNum[3][MissionNumber]=0;
                }else {
                    try {
                        for (Document document1 : available_window) {
                            if (Integer.parseInt(document1.get("load_number").toString()) == 1) {
                                TimePeriodNum[0][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[0][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[0][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[0][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[0][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 2) {
                                TimePeriodNum[1][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[1][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[1][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[1][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[1][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 3) {
                                TimePeriodNum[2][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[2][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[2][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[2][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[2][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 4) {
                                TimePeriodNum[3][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[3][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[3][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[3][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[3][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else {
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        TimePeriodNum[0][MissionNumber]=0;
                        TimePeriodNum[1][MissionNumber]=0;
                        TimePeriodNum[2][MissionNumber]=0;
                        TimePeriodNum[3][MissionNumber]=0;
                    }
                }

                for (int i = 0; i < LoadNumber; i++) {
                    for (int j = 0; j < TimePeriodNum[i][MissionNumber]; j++) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(VisibilityDatePeriod[i][MissionNumber][2 * j]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        String StringTime = sdf.format(cal.getTime());
                        double[] StarTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        cal.setTime(VisibilityDatePeriod[i][MissionNumber][2 * j + 1]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        double[] EndTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        cal.setTime(Time_Point[0]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        double[] ZeroTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        VisibilityTimePeriod[i][MissionNumber][2 * j] = (int) ((JD(StarTime) - JD(ZeroTime)) * (24 * 60 * 60));
                        VisibilityTimePeriod[i][MissionNumber][2 * j + 1] = (int) ((JD(EndTime) - JD(ZeroTime)) * (24 * 60 * 60));
                    }
                }

                MissionStareTime[MissionNumber] = Double.parseDouble(document.getString("min_stare_time"));
                MissionPriority[MissionNumber] = Integer.parseInt(document.getString("priority"));
                MissionSerialNumber[MissionNumber] = document.getString("mission_number");
                if (document.getString("image_mode").equals("常规")) {
                    MissionImagingMode[MissionNumber] = 1;
                    MissionStareTime[MissionNumber]=10;
                } else if (document.getString("image_mode").equals("凝视")) {
                    MissionImagingMode[MissionNumber] = 2;
                    MissionStareTime[MissionNumber] = Double.parseDouble(document.getString("min_stare_time"));
                } else if (document.getString("image_mode").equals("定标")) {
                    MissionImagingMode[MissionNumber] = 3;
                    MissionStareTime[MissionNumber]=10;
                }
                if (document.getString("image_type").equals("Point")) {
                    MissionTargetType[MissionNumber] = 1;
                } else if (document.getString("image_type").equals("Polygon")) {
                    MissionTargetType[MissionNumber] = 2;
                }

                //读取订单编号
                ArrayList<String> MissionForOrderNumbers_i=new ArrayList<>();
                MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");
                MissionForOrderNumbers.add(MissionNumber,MissionForOrderNumbers_i);
                MissionNumber = MissionNumber + 1;
            }
        }

        //为任务分配载荷
        int[][] VisibilityTimePeriodAll = new int[MissionNumber][400];
        int[] TimePeriodNumAll = new int[MissionNumber];
        int[][] VisibilityLoadType = new int[MissionNumber][200];
        double[][] VisibilityAttitude = new double[MissionNumber][200];

        //将目标可见弧段中所有点存在一个数组中
        ArrayList<ArrayList<ArrayList<Integer>>> VisibilityTimePointArray= new ArrayList<>();
        ArrayList<ArrayList<Integer>> VisibilityLoadTypeArray=new ArrayList<>();
        ArrayList<ArrayList<Integer>> VisibilityTimePointAllArray=new ArrayList<>();
        ArrayList<Integer> VisibilityTimePointAllNumArray=new ArrayList<>();
        ArrayList<ArrayList<Boolean>> VisibilityTimePointAllFlagArray=new ArrayList<>();

        for (int i = 0; i < MissionNumber; i++) {
            ArrayList<ArrayList<Integer>> VisibilityTimePointArray_i=new ArrayList<>();
            ArrayList<Integer> VisibilityLoadTypeArray_i=new ArrayList<>();
            ArrayList<Integer> VisibilityTimePointAllArray_i=new ArrayList<>();
            Integer VisibilityTimePointAllNumArray_i=0;
            ArrayList<Boolean> VisibilityTimePointAllFlagArray_i=new ArrayList<>();
            //目标点位置经纬度，区域目标取中心点
            double[] TargetPosition_LLA = {0, 0, 0};
            if (MissionTargetType[i] == 1) {
                TargetPosition_LLA[0] = MissionTargetArea[i][0];
                TargetPosition_LLA[1] = MissionTargetArea[i][1];
            } else {
                for (int j = 0; j < MissionTargetNum[i]; j++) {
                    TargetPosition_LLA[0] = TargetPosition_LLA[0] + MissionTargetArea[i][2 * j];
                    TargetPosition_LLA[1] = TargetPosition_LLA[1] + MissionTargetArea[i][2 * j + 1];
                }
                TargetPosition_LLA[0] = TargetPosition_LLA[0] / MissionTargetNum[i];
                TargetPosition_LLA[1] = TargetPosition_LLA[1] / MissionTargetNum[i];
            }
            double[] TargetPosition_ECEF = new double[3];
            LLAToECEF(TargetPosition_LLA, TargetPosition_ECEF);
            //将所有载荷的可见弧段存在一个数组中
            TimePeriodNumAll[i] = 0;
            for (int j = 0; j < LoadNumber; j++) {
                if (MissionLoadType[i][j] == 1) {
                    for (int k = 0; k < TimePeriodNum[j][i]; k++) {
                        //定义弧段是否满足成像时长，姿态机动需求标志
                        int StareTimeFlag = 1;
                        int AttitudeFlag = 1;
                        //判定可见弧段长度是否满足成像需求
                        double[] VisibilityStarTime = {Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][0],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][1],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][2],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][3],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][4],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][5]};
                        double[] VisibilityEndTime = {Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][0],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][1],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][2],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][3],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][4],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][5]};
                        double VisibilityStareTime = (JD(VisibilityEndTime) - JD(VisibilityStarTime)) * (24 * 60 * 60);
                        if (VisibilityStareTime >= MissionStareTime[i]) {
                            StareTimeFlag = 1;
                        } else {
                            StareTimeFlag = 0;
                        }
                        //判定该弧段是否满足凝视机动需求
                        double[] ViewInstall = LoadInstall[j];
                        if (MissionImagingMode[i] == 2 || MissionImagingMode[i] == 3) {
                            int VisibilityStarPeriod = VisibilityTimePeriod[j][i][2 * k];
                            int VisibilityEndPeriod = VisibilityTimePeriod[j][i][2 * k + 1];
                            int VisibilityPeriod = VisibilityEndPeriod - VisibilityStarPeriod + 1;
                            double[][] SatAttitudeAng = new double[VisibilityPeriod][3];
                            double[][] SatAttitudeVel = new double[VisibilityPeriod - 1][3];
                            for (int l = VisibilityStarPeriod; l < VisibilityEndPeriod; l++) {
                                double[] SatPosition_GEI = Orbital_SatPosition[l];
                                double[] SatVelocity_GEI = Orbital_SatVelocity[l];
                                double[] NowTime = Orbital_Time[l];
                                AttitudeCalculation(SatPosition_GEI, SatVelocity_GEI, TargetPosition_LLA, NowTime, ViewInstall, SatAttitudeAng[l - VisibilityStarPeriod]);
                                if (l > VisibilityStarPeriod) {
                                    //姿态角变化率转化为角速度
                                    double[][] AngRaid = {{(SatAttitudeAng[l - VisibilityStarPeriod][0] - SatAttitudeAng[l - VisibilityStarPeriod - 1][0]) / Step},
                                            {(SatAttitudeAng[l - VisibilityStarPeriod][1] - SatAttitudeAng[l - VisibilityStarPeriod - 1][1]) / Step},
                                            {(SatAttitudeAng[l - VisibilityStarPeriod][2] - SatAttitudeAng[l - VisibilityStarPeriod - 1][2]) / Step}};
                                    double theta1 = SatAttitudeAng[l - VisibilityStarPeriod][0];
                                    double theta2 = SatAttitudeAng[l - VisibilityStarPeriod][1];
                                    double theta3 = SatAttitudeAng[l - VisibilityStarPeriod][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][0] = Vel[0][0];
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][1] = Vel[1][0];
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][2] = Vel[2][0];
                                    if (Vel[0][0] > SatelliteManeuverVelocity[0] || Vel[1][0] > SatelliteManeuverVelocity[1]) {
                                        AttitudeFlag = 0;
                                        break;
                                    }
                                }

                            }
                        } else {
                            AttitudeFlag = 1;
                        }
                        if (StareTimeFlag == 1 && AttitudeFlag == 1) {
                            //可见弧段
                            VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]] = VisibilityTimePeriod[j][i][2 * k];
                            VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i] + 1] = VisibilityTimePeriod[j][i][2 * k + 1];
                            //载荷号
                            VisibilityLoadType[i][TimePeriodNumAll[i]] = j + 1;
                            //弧段开始时刻卫星位置
                            double[] SatllitePosition_GEI = {Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double[] SatlliteVelocity_GEI = {Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double[] SatlliteTime = {Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][3],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][4],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][5]};
                            double[] SatllitePosition_LLA = {Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double SatlliteTime_JD = JD(SatlliteTime);
                            double[] TargetPosition_GEI = new double[3];
                            ECEFToICRS(SatlliteTime_JD, TargetPosition_ECEF, TargetPosition_GEI);
                            double[] ErrorSatToTarget_GEI = {TargetPosition_GEI[0] - SatllitePosition_GEI[0],
                                    TargetPosition_GEI[1] - SatllitePosition_GEI[1],
                                    TargetPosition_GEI[2] - SatllitePosition_GEI[2]};
                            double[] ErrorSatToTarget_ORF = new double[3];
                            GEIToORF(SatllitePosition_GEI, SatlliteVelocity_GEI, ErrorSatToTarget_GEI, ErrorSatToTarget_ORF);
                            //卫星位置到目标点矢量与轨道系z轴夹角
                            double theta_yz = atan2(ErrorSatToTarget_ORF[1], ErrorSatToTarget_ORF[2]);
                            double LoadTheta_yz = atan2(cos(LoadInstall[j][1]), cos(LoadInstall[j][2]));
                            VisibilityAttitude[i][TimePeriodNumAll[i]] = abs(theta_yz - LoadTheta_yz);

                            //将目标可见弧段中所有点存在一个数组中
                            ArrayList<Integer> VisibilityTimePointArray_ii=new ArrayList<Integer>();
                            int Timeperiod_Num=0;
                            for (int l = VisibilityTimePeriod[j][i][2 * k]; l <= VisibilityTimePeriod[j][i][2 * k + 1]; l++) {
                                VisibilityTimePointArray_ii.add(Timeperiod_Num,l);
                                Timeperiod_Num=Timeperiod_Num+1;

                                VisibilityTimePointAllArray_i.add(VisibilityTimePointAllNumArray_i,l);
                                VisibilityTimePointAllFlagArray_i.add(VisibilityTimePointAllNumArray_i,true);
                                VisibilityTimePointAllNumArray_i=VisibilityTimePointAllNumArray_i+1;
                            }
                            VisibilityTimePointArray_i.add(TimePeriodNumAll[i],VisibilityTimePointArray_ii);
                            VisibilityLoadTypeArray_i.add(TimePeriodNumAll[i],j+1);

                            //弧段数加1
                            TimePeriodNumAll[i] = TimePeriodNumAll[i] + 1;
                        }
                    }
                }
            }
            VisibilityTimePointArray.add(i,VisibilityTimePointArray_i);
            VisibilityLoadTypeArray.add(i,VisibilityLoadTypeArray_i);
            VisibilityTimePointAllArray.add(i,VisibilityTimePointAllArray_i);
            VisibilityTimePointAllNumArray.add(i,VisibilityTimePointAllNumArray_i);
            VisibilityTimePointAllFlagArray.add(i,VisibilityTimePointAllFlagArray_i);


            //按照光轴指向目标点的姿态转动大小为可见弧段排序
            for (int j = 0; j < TimePeriodNumAll[i]; j++) {
                double VisibilityAngle_j;
                double[] Satellite_LLA=new double[3];
                Satellite_LLA[0]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][0];
                Satellite_LLA[1]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][1];
                Satellite_LLA[2]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][2];
                double[] Satellite_ECEF=new double[3];
                LLAToECEF(Satellite_LLA, Satellite_ECEF);
                VisibilityAngle_j=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                for (int j_k = VisibilityTimePeriodAll[i][2 * j]; j_k <= VisibilityTimePeriodAll[i][2 * j + 1]; j_k++) {
                    Satellite_LLA[0]=Orbital_SatPositionLLA[j_k][0];
                    Satellite_LLA[1]=Orbital_SatPositionLLA[j_k][1];
                    Satellite_LLA[2]=Orbital_SatPositionLLA[j_k][2];
                    LLAToECEF(Satellite_LLA, Satellite_ECEF);
                    double VisibilityAngle_j_k=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                    if (VisibilityAngle_j_k < VisibilityAngle_j) {
                        VisibilityAngle_j=VisibilityAngle_j_k;
                    }
                }
                for (int k = j + 1; k < TimePeriodNumAll[i]; k++) {
                    double VisibilityAngle_k;
                    Satellite_LLA[0]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * k]][0];
                    Satellite_LLA[1]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * k]][1];
                    Satellite_LLA[2]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * k]][2];
                    LLAToECEF(Satellite_LLA, Satellite_ECEF);
                    VisibilityAngle_k=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                    for (int k_k = VisibilityTimePeriodAll[i][2 * k]; k_k <= VisibilityTimePeriodAll[i][2 * k + 1]; k_k++) {
                        Satellite_LLA[0]=Orbital_SatPositionLLA[k_k][0];
                        Satellite_LLA[1]=Orbital_SatPositionLLA[k_k][1];
                        Satellite_LLA[2]=Orbital_SatPositionLLA[k_k][2];
                        LLAToECEF(Satellite_LLA, Satellite_ECEF);
                        double VisibilityAngle_k_k=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                        if (VisibilityAngle_k_k < VisibilityAngle_j) {
                            VisibilityAngle_k=VisibilityAngle_k_k;
                        }
                    }
                    int a, b, c;
                    if (VisibilityAngle_k < VisibilityAngle_j) {
                        a = VisibilityTimePeriodAll[i][2 * j];
                        b = VisibilityTimePeriodAll[i][2 * j + 1];
                        c = VisibilityLoadType[i][j];
                        VisibilityTimePeriodAll[i][2 * j] = VisibilityTimePeriodAll[i][2 * k];
                        VisibilityTimePeriodAll[i][2 * j + 1] = VisibilityTimePeriodAll[i][2 * k + 1];
                        VisibilityLoadType[i][j] = VisibilityLoadType[i][k];
                        VisibilityTimePeriodAll[i][2 * k] = a;
                        VisibilityTimePeriodAll[i][2 * k + 1] = b;
                        VisibilityLoadType[i][k] = c;
                    }
                }
            }

        }

        //将任务按照优先级降序，可见弧段升序排列
        int[] MissionSequence = new int[MissionNumber];
        for (int i = 0; i < MissionNumber; i++) {
            MissionSequence[i] = i;
        }
        for (int i = 0; i < MissionNumber; i++) {
            for (int j = i + 1; j < MissionNumber; j++) {
                if (MissionPriority[MissionSequence[j]] > MissionPriority[MissionSequence[i]]) {
                    int a = MissionSequence[i];
                    MissionSequence[i] = MissionSequence[j];
                    MissionSequence[j] = a;
                } else if (MissionPriority[MissionSequence[j]] == MissionPriority[MissionSequence[i]]) {
                    if (TimePeriodNumAll[MissionSequence[j]] < TimePeriodNumAll[MissionSequence[i]]) {
                        int a = MissionSequence[i];
                        MissionSequence[i] = MissionSequence[j];
                        MissionSequence[j] = a;
                    }
                }
            }
        }

        //分配标致
        //0表示该轨道点无分配任务，
        // 100表示该轨道点进行阳光规避，
        // 100<i表示该轨道点进行数传任务，大小表示第i-100个地面站
        //i>0表示该轨道点进行成像任务，大小表示第i个任务
        int[] PlanningFlag = new int[(int) OrbitDataCount];
        for (int i = 0; i < (int) OrbitDataCount; i++) {
            PlanningFlag[i] = 0;
        }

        //首先分配阳光规避弧段
        //模块内计算阳光规避弧段
        ArrayList<int[]> SunAvoidTimePeriodList=AvoidSunshineII(OrbitTimeList, OrbitSatPositionGEIList, OrbitSatVelocityGEIList );
        for (int i = 0; i < SunAvoidTimePeriodList.size(); i++) {
            for (int j = SunAvoidTimePeriodList.get(i)[0]; j <= SunAvoidTimePeriodList.get(i)[1]; j++) {
                //PlanningFlag[j] = 100;
            }
        }

        //启发式算法分配任务
        PlanningMissionTimePeriod = new int[MissionNumber][2];
        for (int i = 0; i < MissionNumber; i++) {
            double MissionTime = MissionStareTime[MissionSequence[i]];
            double MissionTimeMin = ImageTimeMin;
            int HalfTime = (int) (MissionTime / 2 + MissionTimeMin / 2);
            int HalfMissionTime=(int)(MissionTime / 2);
            double[] TargetPosition_ECEF = new double[3];
            //无可见弧段
            if (TimePeriodNumAll[MissionSequence[i]] == 0) {
                PlanningMissionFailReason[MissionSequence[i]] = 2;
            }else {
                //目标点位置经纬度，区域目标取中心点
                double[] TargetPosition_LLA = {0, 0, 0};
                if (MissionTargetType[MissionSequence[i]] == 1) {
                    TargetPosition_LLA[0] = MissionTargetArea[MissionSequence[i]][0];
                    TargetPosition_LLA[1] = MissionTargetArea[MissionSequence[i]][1];
                } else {
                    for (int j = 0; j < MissionTargetNum[MissionSequence[i]]; j++) {
                        TargetPosition_LLA[0] = TargetPosition_LLA[0] + MissionTargetArea[MissionSequence[i]][2 * j];
                        TargetPosition_LLA[1] = TargetPosition_LLA[1] + MissionTargetArea[MissionSequence[i]][2 * j + 1];
                    }
                    TargetPosition_LLA[0] = TargetPosition_LLA[0] / MissionTargetNum[MissionSequence[i]];
                    TargetPosition_LLA[1] = TargetPosition_LLA[1] / MissionTargetNum[MissionSequence[i]];
                }
                LLAToECEF(TargetPosition_LLA, TargetPosition_ECEF);
            }

            for (int j = 0; j < TimePeriodNumAll[MissionSequence[i]]; j++) {
                int MiddTimePeriod;

                double VisibilityAngle_j;
                double[] Satellite_LLA=new double[3];
                Satellite_LLA[0]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][0];
                Satellite_LLA[1]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][1];
                Satellite_LLA[2]=Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * j]][2];
                double[] Satellite_ECEF=new double[3];
                LLAToECEF(Satellite_LLA, Satellite_ECEF);
                VisibilityAngle_j=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                MiddTimePeriod=VisibilityTimePeriodAll[i][2 * j];
                for (int j_k = VisibilityTimePeriodAll[i][2 * j]; j_k <= VisibilityTimePeriodAll[i][2 * j + 1]; j_k++) {
                    Satellite_LLA[0]=Orbital_SatPositionLLA[j_k][0];
                    Satellite_LLA[1]=Orbital_SatPositionLLA[j_k][1];
                    Satellite_LLA[2]=Orbital_SatPositionLLA[j_k][2];
                    LLAToECEF(Satellite_LLA, Satellite_ECEF);
                    double VisibilityAngle_j_k=DoublePointToAngle(Satellite_ECEF,TargetPosition_ECEF);
                    if (VisibilityAngle_j_k < VisibilityAngle_j) {
                        VisibilityAngle_j=VisibilityAngle_j_k;
                        MiddTimePeriod=j_k;
                    }
                }
                //成像窗口前移
                int k_num=0;
                for (int k = MiddTimePeriod; k > VisibilityTimePeriodAll[MissionSequence[i]][2 * j] + (int) (MissionTime / 2); k--) {
                    int FlagSum = 0;
                    for (int l = k - HalfTime; l <= k + HalfTime; l++) {
                        if(l<0 || l>=(int) OrbitDataCount){
                            FlagSum=1;
                            break;
                        }else {
                            FlagSum = FlagSum + PlanningFlag[l];
                        }
                    }
                    if (k-HalfMissionTime >= VisibilityTimePeriodAll[MissionSequence[i]][2 * j] && k+HalfMissionTime <= VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1]) {
                    }else {
                        FlagSum=1;
                    }
                    if (FlagSum == 0) {
                        PlanningMissionFailReason[MissionSequence[i]] = 1;
                        PlanningMissionTimePeriod[MissionSequence[i]][0] = k - (int) (MissionTime / 2);
                        PlanningMissionTimePeriod[MissionSequence[i]][1] = k + (int) (MissionTime / 2);
                        PlanningMissionLoad[MissionSequence[i]] = VisibilityLoadType[MissionSequence[i]][j];
                        for (int l = k - HalfTime; l < k + HalfTime; l++) {
                            PlanningFlag[l] = MissionSequence[i] + 1;
                        }
                        break;
                    }
                    //成像窗口后移
                    if (PlanningMissionFailReason[MissionSequence[i]] == 0){
                        int k_mid=k-2*k_num;
                        FlagSum=0;
                        for (int l = k_mid-HalfTime; l <= k_mid+HalfTime; l++) {
                            if(l<0 || l>=(int) OrbitDataCount){
                                FlagSum=1;
                                break;
                            }else {
                                FlagSum = FlagSum + PlanningFlag[l];
                            }
                        }
                        if (k_mid-HalfMissionTime >= VisibilityTimePeriodAll[MissionSequence[i]][2 * j] && k_mid+HalfMissionTime <= VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1]) {
                        }else {
                            FlagSum=1;
                        }
                        if (FlagSum == 0) {
                            PlanningMissionFailReason[MissionSequence[i]] = 1;
                            PlanningMissionTimePeriod[MissionSequence[i]][0] = k_mid - (int) (MissionTime / 2);
                            PlanningMissionTimePeriod[MissionSequence[i]][1] = k_mid + (int) (MissionTime / 2);
                            PlanningMissionLoad[MissionSequence[i]] = VisibilityLoadType[MissionSequence[i]][j];
                            for (int l = k_mid - HalfTime; l < k_mid + HalfTime; l++) {
                                PlanningFlag[l] = MissionSequence[i] + 1;
                            }
                            break;
                        }
                    }
                    k_num=k_num+1;
                }

                if (PlanningMissionFailReason[MissionSequence[i]] == 0) {
                    //成像窗口后移
                    for (int k = MiddTimePeriod; k < VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1] - (int) (MissionTime / 2); k++) {
                        int FlagSum = 0;
                        for (int l = k - HalfTime; l <= k + HalfTime; l++) {
                            if(l<0 || l>=(int) OrbitDataCount){
                                FlagSum=1;
                                break;
                            }else {
                                FlagSum = FlagSum + PlanningFlag[l];
                            }
                        }
                        if (k-HalfMissionTime >= VisibilityTimePeriodAll[MissionSequence[i]][2 * j] && k+HalfMissionTime <= VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1]) {
                        }else {
                            FlagSum=1;
                        }
                        if (FlagSum == 0) {
                            PlanningMissionFailReason[MissionSequence[i]] = 1;
                            PlanningMissionTimePeriod[MissionSequence[i]][0] = k - (int) (MissionTime / 2);
                            PlanningMissionTimePeriod[MissionSequence[i]][1] = k + (int) (MissionTime / 2);
                            PlanningMissionLoad[MissionSequence[i]] = VisibilityLoadType[MissionSequence[i]][j];
                            for (int l = k - HalfTime; l < k + HalfTime; l++) {
                                PlanningFlag[l] = MissionSequence[i] + 1;
                            }
                            break;
                        }
                    }
                }
                if (PlanningMissionFailReason[MissionSequence[i]] == 1) {
                    break;
                }
                //冲突
                if (j == TimePeriodNumAll[MissionSequence[i]] - 1 && PlanningMissionFailReason[MissionSequence[i]] == 0) {
                    PlanningMissionFailReason[MissionSequence[i]] = 3;
                }
            }
        }

        //临边观测、恒星定标任务
        if (MissionStarDocument.size() > 0 || MissionLimbDocument.size() > 0) {
            StarLimbMissionPlanning( PlanningFlag, MissionStarDocument, MissionLimbDocument);
        }

        /*
        //传输任务分配
        ArrayList<int[][]> PlanningTransTimePeriodList=new ArrayList<>();
        ArrayList<Integer> PlanningTransNumList=new ArrayList<>();
        int MissionPeriodAll = 1;
        int StationNumber_ForMission = 0;
        for (int j = 0; j < StationMissionNum; j++) {
            try{
                PlanningTransNum = 0;
                int Side_Flag = 0;
                int Flag_tBefore = 0;
                int Visibility_Flag = 0;
                int Flag_t = 0;

                if (StationNumber == 0) {
                    continue;
                }
                //搜索地面站
                for (int i = 0; i < StationNumber; i++) {
                    if (StationMissionStationNameList.get(j)== StationCode.get(i)) {
                        StationNumber_ForMission=i;
                        break;
                    }
                }

                double[] Target_LLA = new double[3];
                double[] Target_ECEF = new double[3];
                Target_LLA[0] = StationPosition[StationNumber_ForMission][0];
                Target_LLA[1] = StationPosition[StationNumber_ForMission][1];
                Target_LLA[2] = StationPosition[StationNumber_ForMission][2];
                LLAToECEF(Target_LLA, Target_ECEF);
                for (int k = 0; k < (int) OrbitDataCount; k++) {
                    double Time_JD = JD(Orbital_Time[k]);
                    int Flag_StationTime=0;
                    double StationStarTime_JD=JD(StationMissionStarTime[j]);
                    double StationEndTime_JD=JD(StationMissionEndTime[j]);
                    if (Time_JD >= StationStarTime_JD && Time_JD<=StationEndTime_JD) {
                        Flag_StationTime=1;
                    }
                    if (Flag_StationTime == 1) {
                        double[] Target_GEI = new double[3];
                        ECEFToICRS(Time_JD, Target_ECEF, Target_GEI);
                        double[] SatPositionRe_LLA = new double[3];
                        double[] SatPosition_ECEF = new double[3];
                        SatPositionRe_LLA[0] = Orbital_SatPositionLLA[k][0];
                        SatPositionRe_LLA[1] = Orbital_SatPositionLLA[k][1];
                        SatPositionRe_LLA[2] = Orbital_SatPositionLLA[k][2];
                        LLAToECEF(SatPositionRe_LLA, SatPosition_ECEF);
                        Side_Flag = 1;
                        if (Side_Flag == 1) {
                            Visibility_Flag = StationVisibilityJudge(Target_ECEF, SatPosition_ECEF, StationPitch[StationNumber_ForMission]);
                            Flag_tBefore = Flag_t;
                            Flag_t = Visibility_Flag;
                        } else {
                            Visibility_Flag = 0;
                            Flag_tBefore = Flag_t;
                            Flag_t = Visibility_Flag;
                        }
                    }else {
                        Visibility_Flag = 0;
                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                    }

                    if (Flag_tBefore == 0 && Flag_t == 1) {
                        PlanningTransTimePeriod[PlanningTransNum][0] = k;
                        PlanningTransStation[PlanningTransNum] = StationNumber_ForMission+1;
                    } else if (Flag_tBefore == 1 && Flag_t == 0) {
                        PlanningTransTimePeriod[PlanningTransNum][1] = k - 1;
                        PlanningTransNum = PlanningTransNum + 1;
                    }
                    if (k == (int)OrbitDataCount - 1 && Flag_t == 1) {
                        PlanningTransTimePeriod[PlanningTransNum][1] = k;
                        PlanningTransNum = PlanningTransNum + 1;
                    }
                }

                MissionPeriodAll=MissionPeriodAll+PlanningTransNum;
                int[][] PlanningTransTimePeriod_iList=PlanningTransTimePeriod;
                int PlanningTransNum_iList=PlanningTransNum;
                PlanningTransTimePeriodList.add(j,PlanningTransTimePeriod_iList);
                PlanningTransNumList.add(j,PlanningTransNum_iList);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
         */

        //数据传出
        int PoolFileNum=0;
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        String transmission_number = "tn_" + Instant.now().toEpochMilli();
        //任务规划结果数据传出
        for (int i = 0; i < MissionNumber; i++) {
            ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
            if (PlanningMissionFailReason[i] == 0) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "待规划");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);
            } else if (PlanningMissionFailReason[i] == 1) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", PlanningMissionLoad[i]);
                ImageWindowjsonObject.append("start_time", Time_Point[PlanningMissionTimePeriod[i][0]]);
                ImageWindowjsonObject.append("end_time", Time_Point[PlanningMissionTimePeriod[i][1]]);
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "待执行");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);


                //添加文件号
                if (ImageMissionjson.get(i).containsKey("record_file_no") && ImageMissionjson.get(i).get("record_file_no").toString().equals("")) {
                    if (PoolFileNum < PoolFileCanUse.size()) {
                        ImageMissionjson.get(i).append("record_file_no", PoolFileCanUse.get(PoolFileNum).toString());
                        PoolFileNum++;
                    }
                }else if (!ImageMissionjson.get(i).containsKey("record_file_no")) {
                    if (PoolFileNum < PoolFileCanUse.size()) {
                        ImageMissionjson.get(i).append("record_file_no", PoolFileCanUse.get(PoolFileNum).toString());
                        PoolFileNum++;
                    }
                }


                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待执行");

                            //添加文件号
                            if (document.containsKey("record_file_no") && document.get("record_file_no").toString().equals("")) {
                                if (PoolFileNum-1 < PoolFileCanUse.size()) {
                                    document.append("record_file_no",PoolFileCanUse.get(PoolFileNum-1).toString());
                                }
                            }else if (!document.containsKey("record_file_no")) {
                                if (PoolFileNum-1 < PoolFileCanUse.size()) {
                                    document.append("record_file_no",PoolFileCanUse.get(PoolFileNum-1).toString());
                                }
                            }
                            
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }

            } else if (PlanningMissionFailReason[i] == 2) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "不可见");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);

                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","被退回");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }

            } else if (PlanningMissionFailReason[i] == 3) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "任务冲突");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);


                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","被退回");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }
            Document modifiers = new Document();
            modifiers.append("$set", ImageMissionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", ImageMissionjson.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }

        //传输任务数据传出
        if(StationMissionJson != null && StationMissionJson.size()>0) {
            ArrayList<Document> TranWindowjsonArry = new ArrayList<>();
            for (int i = 0; i < StationMissionNum; i++) {
                if (StationMissionTranNumberList.get(i).equals(TransmissionMissionJson.get("transmission_number").toString())) {
                    if (TransmissionMissionJson.containsKey("station_info") && TransmissionMissionJson.get("station_info")!=null) {
                        ArrayList<Document> TranStationInfo= (ArrayList<Document>) TransmissionMissionJson.get("station_info");
                        for (Document document:TranStationInfo) {
                            if (document.containsKey("station_name") && document.get("station_name")!=null) {
                                if (document.containsKey("available_window") && document.get("available_window")!=null) {
                                    ArrayList<Document> TranAvailableWindow= (ArrayList<Document>) document.get("available_window");
                                    for (Document document1:TranAvailableWindow) {
                                        if (document1.containsKey("window_start_time") && document1.get("window_start_time")!=null) {
                                            Document TranWindowjsonObject = new Document();
                                            TranWindowjsonObject.append("station_name", document.get("station_name").toString());
                                            TranWindowjsonObject.append("start_time", document1.getDate("window_start_time"));
                                            TranWindowjsonObject.append("end_time", document1.getDate("window_end_time"));
                                            TranWindowjsonArry.add(TranWindowjsonObject);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                /*
                //搜索地面站
                int StationNumber_ForMissionOut=0;
                for (int Station_i = 0; Station_i < StationNumber; Station_i++) {
                    if (StationMissionStationNameList.get(i)== StationCode.get(Station_i)) {
                        StationNumber_ForMissionOut=Station_i;
                        break;
                    }
                }
                for (int j = 0; j < PlanningTransNumList.get(i); j++) {
                    Document TranWindowjsonObject = new Document();
                    //TranWindowjsonObject.append("station_name", StationSerialNumber[StationNumber_ForMissionOut]);
                    TranWindowjsonObject.append("station_name", StationCode.get(StationNumber_ForMissionOut));
                    TranWindowjsonObject.append("start_time", Time_Point[PlanningTransTimePeriodList.get(i)[j][0]]);
                    TranWindowjsonObject.append("end_time", Time_Point[PlanningTransTimePeriodList.get(i)[j][1]]);
                    TranWindowjsonArry.add(TranWindowjsonObject);
                }
                 */
                //if (PlanningTransNumList.get(i) == 0) {
                if (StationMissionJson.get(i).get("tag").equals("被退回")) {
                    Document d=StationMissionJson.get(i);
                    if (d.containsKey("_id"))
                        d.remove("_id");
                    d.append("tag","被退回");
                    Document modifiers = new Document();
                    modifiers.append("$set", d);
                    MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                    station_mission.updateOne(new Document("mission_number", d.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                }else{
                    Document d=StationMissionJson.get(i);
                    if (d.containsKey("_id"))
                        d.remove("_id");
                    d.append("tag","待执行");
                    Document modifiers = new Document();
                    modifiers.append("$set", d);
                    MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                    station_mission.updateOne(new Document("mission_number", d.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                }
            }
            //if (MissionPeriodAll == 0) {
            /*
            if (!StationMissionJson.get(i).get("tag").equals("待规划")) {
                Document TranWindowjsonObject = new Document();
                TranWindowjsonObject.append("station_name", "");
                TranWindowjsonObject.append("start_time", "");
                TranWindowjsonObject.append("end_time", "");
                TranWindowjsonArry.add(TranWindowjsonObject);
            }
             */
            //地面站，传输任务更新？？？？
            TransmissionMissionJson.append("transmission_window", TranWindowjsonArry);

            if(TransmissionMissionJson.containsKey("_id"))
                TransmissionMissionJson.remove("_id");
            MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
            Document modifiers = new Document();
            modifiers.append("$set", TransmissionMissionJson);
            transmission_mission.updateOne(new Document("transmission_number", TransmissionMissionJson.getString("transmission_number")), modifiers, new UpdateOptions().upsert(true));

        }
        mongoClient.close();
    }

    //临边任务、恒星定标任务规划
    private static void StarLimbMissionPlanning(int[] PlanningFlag,ArrayList<Document> MissionStarDocument,ArrayList<Document> MissionLimbDocument){
        ArrayList<int[]> StarPlanningTransTimePeriodList=new ArrayList<>();
        ArrayList<Integer> StarPlanningResult=new ArrayList<>();
        ArrayList<int[]> LimbPlanningTransTimePeriodList=new ArrayList<>();
        ArrayList<Boolean> LimbPlanningResult=new ArrayList<>();

        //数据传出
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");

        //规划恒星定标任务
        int MissionNum=0;
        for (Document document:MissionStarDocument) {
            try{
                if (document.containsKey("available_window")) {
                    ArrayList<Document> MissionwWindow= (ArrayList<Document>) document.get("available_window");
                    int MissionIterval= (int) Double.parseDouble(document.get("mission_interval_min").toString());
                    //读取订单编号
                    ArrayList<String> MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");
                    boolean plannedFlag=false;
                    for (Document document1:MissionwWindow) {
                        //读取任务期望时间
                        Date window_start_time = document1.getDate("window_start_time");
                        double[] MissionStarTime_iList = DateToDouble(window_start_time);
                        Date window_stop_time=document1.getDate("window_end_time");
                        double[] MissionStopTime_iList =DateToDouble(window_stop_time);
                        int[] MissionWindow_int=new int[2];
                        MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Orbital_Time[0])) * (24 * 60 * 60));
                        MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Orbital_Time[0])) * (24 * 60 * 60));
                        int FlagSum = 0;
                        for (int i = MissionWindow_int[0]; i <= MissionWindow_int[0]+MissionIterval; i++) {
                            FlagSum = FlagSum + PlanningFlag[i];
                        }
                        if (FlagSum == 0) {
                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[0]+MissionIterval; i++) {
                                PlanningFlag[i]=200;
                            }
                            //数据传出
                            MissionWindow_int[1]=MissionWindow_int[0]+MissionIterval;

                            ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                            Document ImageWindowjsonObject = new Document();
                            ImageWindowjsonObject.append("start_time", Time_Point[MissionWindow_int[0]]);
                            ImageWindowjsonObject.append("end_time", Time_Point[MissionWindow_int[1]]);
                            ImageWindowjsonArry.add(ImageWindowjsonObject);
                            MissionStarDocument.get(MissionNum).append("mission_state", "待执行");
                            MissionStarDocument.get(MissionNum).append("fail_reason", "");
                            MissionStarDocument.get(MissionNum).append("image_window", ImageWindowjsonArry);

                            //回溯订单
                            for (String OrderNumber:MissionForOrderNumbers_i) {
                                MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                                FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                                ArrayList<Document> ImageOrderjson =new ArrayList<>();
                                for (Document document2:D_ImageOrderjson) {
                                    if (document2.get("order_number").equals(OrderNumber)) {
                                        document2.append("order_state","待执行");
                                        if(document2.containsKey("_id"))
                                            document2.remove("_id");
                                        Document modifiers_mid=new Document();
                                        modifiers_mid.append("$set",document2);
                                        Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                                    }
                                }
                            }
                            Document modifiers = new Document();
                            modifiers.append("$set", MissionStarDocument.get(MissionNum));
                            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                            image_mission.updateOne(new Document("mission_number", MissionStarDocument.get(MissionNum).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));

                            plannedFlag=true;
                            MissionNum++;
                            break;
                        }
                    }
                    if (plannedFlag == false && MissionwWindow.size()>0) {
                        ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                        Document ImageWindowjsonObject = new Document();
                        ImageWindowjsonObject.append("start_time", "");
                        ImageWindowjsonObject.append("end_time", "");
                        ImageWindowjsonArry.add(ImageWindowjsonObject);
                        MissionStarDocument.get(MissionNum).append("mission_state", "被退回");
                        MissionStarDocument.get(MissionNum).append("fail_reason", "任务冲突");
                        MissionStarDocument.get(MissionNum).append("image_window", ImageWindowjsonArry);


                        //回溯订单
                        for (String OrderNumber:MissionForOrderNumbers_i) {
                            MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                            FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                            ArrayList<Document> ImageOrderjson =new ArrayList<>();
                            for (Document document2:D_ImageOrderjson) {
                                if (document2.get("order_number").equals(OrderNumber)) {
                                    document2.append("order_state","被退回");
                                    if(document2.containsKey("_id"))
                                        document2.remove("_id");
                                    Document modifiers_mid=new Document();
                                    modifiers_mid.append("$set",document2);
                                    Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                                }
                            }
                        }
                        Document modifiers = new Document();
                        modifiers.append("$set", MissionStarDocument.get(MissionNum));
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.updateOne(new Document("mission_number", MissionStarDocument.get(MissionNum).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                    }
                }else {
                    MissionNum++;
                    continue;
                }
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                MissionNum++;
                continue;
            }
        }
        //规划临边观测任务
        MissionNum=0;
        for (Document document:MissionLimbDocument) {
            try{
                if (document.containsKey("available_window")) {
                    ArrayList<Document> MissionwWindow= (ArrayList<Document>) document.get("available_window");
                    //读取订单编号
                    ArrayList<String> MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");
                    boolean plannedFlag=false;
                    for (Document document1:MissionwWindow) {
                        //读取任务期望时间
                        Date window_start_time = document1.getDate("window_start_time");
                        double[] MissionStarTime_iList = DateToDouble(window_start_time);
                        Date window_stop_time=document1.getDate("window_end_time");
                        double[] MissionStopTime_iList =DateToDouble(window_stop_time);
                        int[] MissionWindow_int=new int[2];
                        MissionWindow_int[0] = (int) ((JD(MissionStarTime_iList) - JD(Orbital_Time[0])) * (24 * 60 * 60));
                        MissionWindow_int[1] = (int) ((JD(MissionStopTime_iList) - JD(Orbital_Time[0])) * (24 * 60 * 60));
                        int FlagSum = 0;
                        for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                            FlagSum = FlagSum + PlanningFlag[i];
                        }
                        if (FlagSum == 0) {
                            for (int i = MissionWindow_int[0]; i <= MissionWindow_int[1]; i++) {
                                PlanningFlag[i]=300;
                            }
                            //数据传出

                            ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                            Document ImageWindowjsonObject = new Document();
                            ImageWindowjsonObject.append("start_time", Time_Point[MissionWindow_int[0]]);
                            ImageWindowjsonObject.append("end_time", Time_Point[MissionWindow_int[1]]);
                            ImageWindowjsonArry.add(ImageWindowjsonObject);
                            MissionLimbDocument.get(MissionNum).append("mission_state", "待执行");
                            MissionLimbDocument.get(MissionNum).append("fail_reason", "");
                            MissionLimbDocument.get(MissionNum).append("image_window", ImageWindowjsonArry);

                            //回溯订单
                            for (String OrderNumber:MissionForOrderNumbers_i) {
                                MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                                FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                                ArrayList<Document> ImageOrderjson =new ArrayList<>();
                                for (Document document2:D_ImageOrderjson) {
                                    if (document2.get("order_number").equals(OrderNumber)) {
                                        document2.append("order_state","待执行");
                                        if(document2.containsKey("_id"))
                                            document2.remove("_id");
                                        Document modifiers_mid=new Document();
                                        modifiers_mid.append("$set",document2);
                                        Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                                    }
                                }
                            }
                            Document modifiers = new Document();
                            modifiers.append("$set", MissionLimbDocument.get(MissionNum));
                            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                            image_mission.updateOne(new Document("mission_number", MissionLimbDocument.get(MissionNum).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));

                            plannedFlag=true;
                            MissionNum++;
                            break;
                        }
                    }
                    if (plannedFlag == false && MissionwWindow.size()>0) {
                        ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                        Document ImageWindowjsonObject = new Document();
                        ImageWindowjsonObject.append("start_time", "");
                        ImageWindowjsonObject.append("end_time", "");
                        ImageWindowjsonArry.add(ImageWindowjsonObject);
                        MissionLimbDocument.get(MissionNum).append("mission_state", "被退回");
                        MissionLimbDocument.get(MissionNum).append("fail_reason", "任务冲突");
                        MissionLimbDocument.get(MissionNum).append("image_window", ImageWindowjsonArry);


                        //回溯订单
                        for (String OrderNumber:MissionForOrderNumbers_i) {
                            MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                            FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                            ArrayList<Document> ImageOrderjson =new ArrayList<>();
                            for (Document document2:D_ImageOrderjson) {
                                if (document2.get("order_number").equals(OrderNumber)) {
                                    document2.append("order_state","被退回");
                                    if(document2.containsKey("_id"))
                                        document2.remove("_id");
                                    Document modifiers_mid=new Document();
                                    modifiers_mid.append("$set",document2);
                                    Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                                }
                            }
                        }
                        Document modifiers = new Document();
                        modifiers.append("$set", MissionLimbDocument.get(MissionNum));
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.updateOne(new Document("mission_number", MissionLimbDocument.get(MissionNum).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                    }
                }else {
                    MissionNum++;
                    continue;
                }
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                MissionNum++;
                continue;
            }
        }

        mongoClient.close();
    }

    private static void MissionPlanningOld(Document Satllitejson, ArrayList<Document> GroundStationjson, FindIterable<Document> Orbitjson, long OrbitDataCount, ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson, ArrayList<Document> StationMissionJson) {
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
            } else if (document.getString("key").equals("image_time_min")) {
                ImageTimeMin = Double.parseDouble(document.getString("value"));
            } else
                continue;
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

        //轨道数据读入
        Orbital_Time = new double[(int) OrbitDataCount][6];
        Orbital_SatPosition = new double[(int) OrbitDataCount][3];
        Orbital_SatVelocity = new double[(int) OrbitDataCount][3];
        Time_Point = new Date[(int) OrbitDataCount];
        Orbital_SatPositionLLA = new double[(int) OrbitDataCount][3];
        OrbitalDataNum = 0;
        for (Document document : Orbitjson) {
            Date time_point = document.getDate("time_point");
            //时间转换为doubule型
            String StringTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(time_point);
            cal.add(Calendar.HOUR_OF_DAY, -8);
            StringTime = sdf.format(cal.getTime());
            Time_Point[OrbitalDataNum] = time_point;
            Orbital_Time[OrbitalDataNum][0] = Double.parseDouble(StringTime.substring(0, 4));
            Orbital_Time[OrbitalDataNum][1] = Double.parseDouble(StringTime.substring(5, 7));
            Orbital_Time[OrbitalDataNum][2] = Double.parseDouble(StringTime.substring(8, 10));
            Orbital_Time[OrbitalDataNum][3] = Double.parseDouble(StringTime.substring(11, 13));
            Orbital_Time[OrbitalDataNum][4] = Double.parseDouble(StringTime.substring(14, 16));
            Orbital_Time[OrbitalDataNum][5] = Double.parseDouble(StringTime.substring(17, 19));

            Orbital_SatPosition[OrbitalDataNum][0] = Double.parseDouble(document.get("P_x").toString());
            Orbital_SatPosition[OrbitalDataNum][1] = Double.parseDouble(document.get("P_y").toString());
            Orbital_SatPosition[OrbitalDataNum][2] = Double.parseDouble(document.get("P_z").toString());
            Orbital_SatVelocity[OrbitalDataNum][0] = Double.parseDouble(document.get("Vx").toString());
            Orbital_SatVelocity[OrbitalDataNum][1] = Double.parseDouble(document.get("Vy").toString());
            Orbital_SatVelocity[OrbitalDataNum][2] = Double.parseDouble(document.get("Vz").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][0] = Double.parseDouble(document.get("lon").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][1] = Double.parseDouble(document.get("lat").toString());
            Orbital_SatPositionLLA[OrbitalDataNum][2] = Double.parseDouble(document.get("H").toString());

            OrbitalDataNum = OrbitalDataNum + 1;

            if(OrbitalDataNum >= OrbitDataCount)
                break;
        }

        ArrayList<String> StationMissionTransmissionNumList=new ArrayList<>();
        ArrayList<String> StationMissionStationNameList=new ArrayList<>();
        ArrayList<String> StationMissionNumberList=new ArrayList<>();
        //地面站任务读入
        if (StationMissionJson.size()==0){
            StationMissionNum = 1;
            StationMissionStarTime = new double[1][6];
            StationMissionEndTime=new double[1][6];
            for (int i = 0; i < 6; i++) {
                StationMissionStarTime[0][i]=Orbital_Time[1][i];
                StationMissionEndTime[0][i]=Orbital_Time[0][i];
            }
        }else {
            StationMissionStarTime = new double[StationMissionJson.size()][6];
            StationMissionEndTime=new double[StationMissionJson.size()][6];
            StationMissionNum = 0;
            for (Document document : StationMissionJson) {
                try {
                    Date time_point=document.getDate("expected_start_time");
                    String StringTime;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(time_point);
                    cal.add(Calendar.HOUR_OF_DAY, -8);
                    StringTime = sdf.format(cal.getTime());
                    StationMissionStarTime[StationMissionNum][0] = Double.parseDouble(StringTime.substring(0, 4));
                    StationMissionStarTime[StationMissionNum][1] = Double.parseDouble(StringTime.substring(5, 7));
                    StationMissionStarTime[StationMissionNum][2] = Double.parseDouble(StringTime.substring(8, 10));
                    StationMissionStarTime[StationMissionNum][3] = Double.parseDouble(StringTime.substring(11, 13));
                    StationMissionStarTime[StationMissionNum][4] = Double.parseDouble(StringTime.substring(14, 16));
                    StationMissionStarTime[StationMissionNum][5] = Double.parseDouble(StringTime.substring(17, 19));
                    time_point=document.getDate("expected_end_time");
                    cal.setTime(time_point);
                    cal.add(Calendar.HOUR_OF_DAY, -8);
                    StringTime = sdf.format(cal.getTime());
                    StationMissionEndTime[StationMissionNum][0] = Double.parseDouble(StringTime.substring(0, 4));
                    StationMissionEndTime[StationMissionNum][1] = Double.parseDouble(StringTime.substring(5, 7));
                    StationMissionEndTime[StationMissionNum][2] = Double.parseDouble(StringTime.substring(8, 10));
                    StationMissionEndTime[StationMissionNum][3] = Double.parseDouble(StringTime.substring(11, 13));
                    StationMissionEndTime[StationMissionNum][4] = Double.parseDouble(StringTime.substring(14, 16));
                    StationMissionEndTime[StationMissionNum][5] = Double.parseDouble(StringTime.substring(17, 19));

                    //String TransmissionNumber= document.get("transmission_number").toString();
                    //StationMissionTransmissionNumList.add(StationMissionNum,TransmissionNumber);
                    String StationName=document.get("station_number").toString();
                    StationMissionStationNameList.add(StationMissionNum,StationName);
                    String StationmissionNumber_iList=document.get("mission_number").toString();
                    StationMissionNumberList.add(StationMissionNum,StationmissionNumber_iList);
                    StationMissionNum=StationMissionNum+1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }


        //任务读入
        MissionNumber = ImageMissionjson.size();
        MissionTargetNum = new int[MissionNumber];
        MissionTargetArea = new double[MissionNumber][200];        //成像区域描述，格式：每行代表一个任务，每行格式[经度，纬度，经度，纬度，……]
        MissionLoadType = new int[MissionNumber][LoadNumber];
        TimePeriodNum = new int[LoadNumber][MissionNumber];
        VisibilityTimePeriod = new int[LoadNumber][MissionNumber][200];
        MissionStareTime = new double[MissionNumber];
        MissionImagingMode = new int[MissionNumber];
        MissionPriority = new int[MissionNumber];
        PlanningMissionFailReason = new int[MissionNumber];
        PlanningMissionLoad = new int[MissionNumber];
        MissionTargetType = new int[MissionNumber];
        for (int i = 0; i < MissionNumber; i++) {
            PlanningMissionFailReason[i] = 0;
        }
        Date[][][] VisibilityDatePeriod = new Date[LoadNumber][MissionNumber][200];
        for (int i = 0; i < MissionNumber; i++) {
            for (int j = 0; j < LoadNumber; j++) {
                MissionLoadType[i][j] = 0;
            }
        }

        ArrayList<ArrayList<String>> MissionForOrderNumbers=new ArrayList<>();
        MissionNumber = 0;
        for (Document document : ImageMissionjson) {
            if (document.get("mission_state").equals("被退回")) {
                continue;
            }else{
                Document target_region = (Document) document.get("image_region");
                //读取目标区域
                ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                MissionTargetArea_iList = GetRegionPoint(target_region);
                MissionTargetNum[MissionNumber] = MissionTargetArea_iList.size();
                if (MissionTargetNum[MissionNumber] > 90) {
                    MissionTargetNum[MissionNumber] = 90;
                }
                for (int i = 0; i < MissionTargetNum[MissionNumber]; i++) {
                    MissionTargetArea[MissionNumber][2 * i] = MissionTargetArea_iList.get(i)[0];
                    MissionTargetArea[MissionNumber][2 * i + 1] = MissionTargetArea_iList.get(i)[1];
                }
                ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                if (expected_cam.size() == 0) {
                    MissionLoadType[MissionNumber][0] = 1;
                    MissionLoadType[MissionNumber][1] = 1;
                    MissionLoadType[MissionNumber][2] = 1;
                    MissionLoadType[MissionNumber][3] = 1;
                }else {
                    try {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType[MissionNumber][0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType[MissionNumber][1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType[MissionNumber][2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType[MissionNumber][3] = 1;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        MissionLoadType[MissionNumber][0] = 1;
                        MissionLoadType[MissionNumber][1] = 1;
                        MissionLoadType[MissionNumber][2] = 1;
                        MissionLoadType[MissionNumber][3] = 1;
                    }
                }
                ArrayList<Document> available_window = (ArrayList<Document>) document.get("available_window");
                if (available_window == null) {
                    TimePeriodNum[0][MissionNumber]=0;
                    TimePeriodNum[1][MissionNumber]=0;
                    TimePeriodNum[2][MissionNumber]=0;
                    TimePeriodNum[3][MissionNumber]=0;
                }else {
                    try {
                        for (Document document1 : available_window) {
                            if (Integer.parseInt(document1.get("load_number").toString()) == 1) {
                                TimePeriodNum[0][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[0][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[0][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[0][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[0][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 2) {
                                TimePeriodNum[1][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[1][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[1][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[1][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[1][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 3) {
                                TimePeriodNum[2][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[2][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[2][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[2][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[2][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else if (Integer.parseInt(document1.get("load_number").toString()) == 4) {
                                TimePeriodNum[3][MissionNumber] = Integer.parseInt(document1.get("amount_window").toString());
                                int a = Integer.parseInt(document1.get("window_number").toString()) - 1;
                                if (a > 90 || TimePeriodNum[3][MissionNumber]>90) {
                                    a=90;
                                    TimePeriodNum[3][MissionNumber]=90;
                                }
                                VisibilityDatePeriod[3][MissionNumber][2 * a] = document1.getDate("window_start_time");
                                VisibilityDatePeriod[3][MissionNumber][2 * a + 1] = document1.getDate("window_end_time");
                            } else {
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        TimePeriodNum[0][MissionNumber]=0;
                        TimePeriodNum[1][MissionNumber]=0;
                        TimePeriodNum[2][MissionNumber]=0;
                        TimePeriodNum[3][MissionNumber]=0;
                    }
                }

                for (int i = 0; i < LoadNumber; i++) {
                    for (int j = 0; j < TimePeriodNum[i][MissionNumber]; j++) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(VisibilityDatePeriod[i][MissionNumber][2 * j]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        String StringTime = sdf.format(cal.getTime());
                        double[] StarTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        cal.setTime(VisibilityDatePeriod[i][MissionNumber][2 * j + 1]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        double[] EndTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        cal.setTime(Time_Point[0]);
                        cal.add(Calendar.HOUR_OF_DAY, -8);
                        StringTime = sdf.format(cal.getTime());
                        double[] ZeroTime = {Double.parseDouble(StringTime.substring(0, 4)),
                                Double.parseDouble(StringTime.substring(5, 7)),
                                Double.parseDouble(StringTime.substring(8, 10)),
                                Double.parseDouble(StringTime.substring(11, 13)),
                                Double.parseDouble(StringTime.substring(14, 16)),
                                Double.parseDouble(StringTime.substring(17, 19))};
                        VisibilityTimePeriod[i][MissionNumber][2 * j] = (int) ((JD(StarTime) - JD(ZeroTime)) * (24 * 60 * 60));
                        VisibilityTimePeriod[i][MissionNumber][2 * j + 1] = (int) ((JD(EndTime) - JD(ZeroTime)) * (24 * 60 * 60));
                    }
                }

                MissionStareTime[MissionNumber] = Double.parseDouble(document.getString("min_stare_time"));
                MissionPriority[MissionNumber] = Integer.parseInt(document.getString("priority"));
                MissionSerialNumber[MissionNumber] = document.getString("mission_number");
                if (document.getString("image_mode").equals("常规")) {
                    MissionImagingMode[MissionNumber] = 1;
                    MissionStareTime[MissionNumber]=10;
                } else if (document.getString("image_mode").equals("凝视")) {
                    MissionImagingMode[MissionNumber] = 2;
                    MissionStareTime[MissionNumber] = Double.parseDouble(document.getString("min_stare_time"));
                } else if (document.getString("image_mode").equals("定标")) {
                    MissionImagingMode[MissionNumber] = 3;
                    MissionStareTime[MissionNumber]=10;
                }
                if (document.getString("image_type").equals("Point")) {
                    MissionTargetType[MissionNumber] = 1;
                } else if (document.getString("image_type").equals("Polygon")) {
                    MissionTargetType[MissionNumber] = 2;
                }

                //读取订单编号
                ArrayList<String> MissionForOrderNumbers_i=new ArrayList<>();
                MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");
                MissionForOrderNumbers.add(MissionNumber,MissionForOrderNumbers_i);
                MissionNumber = MissionNumber + 1;
            }
        }

        //为任务分配载荷
        int[][] VisibilityTimePeriodAll = new int[MissionNumber][400];
        int[] TimePeriodNumAll = new int[MissionNumber];
        int[][] VisibilityLoadType = new int[MissionNumber][200];
        double[][] VisibilityAttitude = new double[MissionNumber][200];
        for (int i = 0; i < MissionNumber; i++) {
            //目标点位置经纬度，区域目标取中心点
            double[] TargetPosition_LLA = {0, 0, 0};
            if (MissionTargetType[i] == 1) {
                TargetPosition_LLA[0] = MissionTargetArea[i][0];
                TargetPosition_LLA[1] = MissionTargetArea[i][1];
            } else {
                for (int j = 0; j < MissionTargetNum[i]; j++) {
                    TargetPosition_LLA[0] = TargetPosition_LLA[0] + MissionTargetArea[i][2 * j];
                    TargetPosition_LLA[1] = TargetPosition_LLA[1] + MissionTargetArea[i][2 * j + 1];
                }
                TargetPosition_LLA[0] = TargetPosition_LLA[0] / MissionTargetNum[i];
                TargetPosition_LLA[1] = TargetPosition_LLA[1] / MissionTargetNum[i];
            }
            double[] TargetPosition_ECEF = new double[3];
            LLAToECEF(TargetPosition_LLA, TargetPosition_ECEF);
            //将所有载荷的可见弧段存在一个数组中
            TimePeriodNumAll[i] = 0;
            for (int j = 0; j < LoadNumber; j++) {
                if (MissionLoadType[i][j] == 1) {
                    for (int k = 0; k < TimePeriodNum[j][i]; k++) {
                        //定义弧段是否满足成像时长，姿态机动需求标志
                        int StareTimeFlag = 1;
                        int AttitudeFlag = 1;
                        //判定可见弧段长度是否满足成像需求
                        double[] VisibilityStarTime = {Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][0],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][1],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][2],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][3],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][4],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k]][5]};
                        double[] VisibilityEndTime = {Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][0],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][1],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][2],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][3],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][4],
                                Orbital_Time[VisibilityTimePeriod[j][i][2 * k + 1]][5]};
                        double VisibilityStareTime = (JD(VisibilityEndTime) - JD(VisibilityStarTime)) * (24 * 60 * 60);
                        if (VisibilityStareTime >= MissionStareTime[i]) {
                            StareTimeFlag = 1;
                        } else {
                            StareTimeFlag = 0;
                        }
                        //判定该弧段是否满足凝视机动需求
                        double[] ViewInstall = LoadInstall[j];
                        if (MissionImagingMode[i] == 2 || MissionImagingMode[i] == 3) {
                            int VisibilityStarPeriod = VisibilityTimePeriod[j][i][2 * k];
                            int VisibilityEndPeriod = VisibilityTimePeriod[j][i][2 * k + 1];
                            int VisibilityPeriod = VisibilityEndPeriod - VisibilityStarPeriod + 1;
                            double[][] SatAttitudeAng = new double[VisibilityPeriod][3];
                            double[][] SatAttitudeVel = new double[VisibilityPeriod - 1][3];
                            for (int l = VisibilityStarPeriod; l < VisibilityEndPeriod; l++) {
                                double[] SatPosition_GEI = Orbital_SatPosition[l];
                                double[] SatVelocity_GEI = Orbital_SatVelocity[l];
                                double[] NowTime = Orbital_Time[l];
                                AttitudeCalculation(SatPosition_GEI, SatVelocity_GEI, TargetPosition_LLA, NowTime, ViewInstall, SatAttitudeAng[l - VisibilityStarPeriod]);
                                if (l > VisibilityStarPeriod) {
                                    //姿态角变化率转化为角速度
                                    double[][] AngRaid = {{(SatAttitudeAng[l - VisibilityStarPeriod][0] - SatAttitudeAng[l - VisibilityStarPeriod - 1][0]) / Step},
                                            {(SatAttitudeAng[l - VisibilityStarPeriod][1] - SatAttitudeAng[l - VisibilityStarPeriod - 1][1]) / Step},
                                            {(SatAttitudeAng[l - VisibilityStarPeriod][2] - SatAttitudeAng[l - VisibilityStarPeriod - 1][2]) / Step}};
                                    double theta1 = SatAttitudeAng[l - VisibilityStarPeriod][0];
                                    double theta2 = SatAttitudeAng[l - VisibilityStarPeriod][1];
                                    double theta3 = SatAttitudeAng[l - VisibilityStarPeriod][2];
                                    double[][] Tran = {{1, 0, -sin(theta2)},
                                            {0, cos(theta1), sin(theta1) * cos(theta2)},
                                            {0, -sin(theta1), cos(theta1) * cos(theta2)}};
                                    double[][] Vel = MatrixMultiplication(Tran, AngRaid);
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][0] = Vel[0][0];
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][1] = Vel[1][0];
                                    SatAttitudeVel[l - VisibilityStarPeriod - 1][2] = Vel[2][0];
                                    if (Vel[0][0] > SatelliteManeuverVelocity[0] || Vel[1][0] > SatelliteManeuverVelocity[1]) {
                                        AttitudeFlag = 0;
                                        break;
                                    }
                                }

                            }
                        } else {
                            AttitudeFlag = 1;
                        }
                        if (StareTimeFlag == 1 && AttitudeFlag == 1) {
                            //可见弧段
                            VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]] = VisibilityTimePeriod[j][i][2 * k];
                            VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i] + 1] = VisibilityTimePeriod[j][i][2 * k + 1];
                            //载荷号
                            VisibilityLoadType[i][TimePeriodNumAll[i]] = j + 1;
                            //弧段开始时刻卫星位置
                            double[] SatllitePosition_GEI = {Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatPosition[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double[] SatlliteVelocity_GEI = {Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatVelocity[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double[] SatlliteTime = {Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][3],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][4],
                                    Orbital_Time[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][5]};
                            double[] SatllitePosition_LLA = {Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][0],
                                    Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][1],
                                    Orbital_SatPositionLLA[VisibilityTimePeriodAll[i][2 * TimePeriodNumAll[i]]][2]};
                            double SatlliteTime_JD = JD(SatlliteTime);
                            double[] TargetPosition_GEI = new double[3];
                            ECEFToICRS(SatlliteTime_JD, TargetPosition_ECEF, TargetPosition_GEI);
                            double[] ErrorSatToTarget_GEI = {TargetPosition_GEI[0] - SatllitePosition_GEI[0],
                                    TargetPosition_GEI[1] - SatllitePosition_GEI[1],
                                    TargetPosition_GEI[2] - SatllitePosition_GEI[2]};
                            double[] ErrorSatToTarget_ORF = new double[3];
                            GEIToORF(SatllitePosition_GEI, SatlliteVelocity_GEI, ErrorSatToTarget_GEI, ErrorSatToTarget_ORF);
                            //卫星位置到目标点矢量与轨道系z轴夹角
                            double theta_yz = atan2(ErrorSatToTarget_ORF[1], ErrorSatToTarget_ORF[2]);
                            double LoadTheta_yz = atan2(cos(LoadInstall[j][1]), cos(LoadInstall[j][2]));
                            VisibilityAttitude[i][TimePeriodNumAll[i]] = abs(theta_yz - LoadTheta_yz);
                            //弧段数加1
                            TimePeriodNumAll[i] = TimePeriodNumAll[i] + 1;
                        }
                    }
                }
            }
            //按照光轴指向目标点的姿态转动大小为可见弧段排序
            for (int j = 0; j < TimePeriodNumAll[i]; j++) {
                for (int k = j + 1; k < TimePeriodNumAll[i]; k++) {
                    int a, b, c;
                    if (VisibilityAttitude[i][k] < VisibilityAttitude[i][j]) {
                        a = VisibilityTimePeriodAll[i][2 * j];
                        b = VisibilityTimePeriodAll[i][2 * j + 1];
                        c = VisibilityLoadType[i][j];
                        VisibilityTimePeriodAll[i][2 * j] = VisibilityTimePeriodAll[i][2 * k];
                        VisibilityTimePeriodAll[i][2 * j + 1] = VisibilityTimePeriodAll[i][2 * k + 1];
                        VisibilityLoadType[i][j] = VisibilityLoadType[i][k];
                        VisibilityTimePeriodAll[i][2 * k] = a;
                        VisibilityTimePeriodAll[i][2 * k + 1] = b;
                        VisibilityLoadType[i][k] = c;
                    }
                }
            }
        }

        //将任务按照优先级降序，可见弧段升序排列
        int[] MissionSequence = new int[MissionNumber];
        for (int i = 0; i < MissionNumber; i++) {
            MissionSequence[i] = i;
        }
        for (int i = 0; i < MissionNumber; i++) {
            for (int j = i + 1; j < MissionNumber; j++) {
                if (MissionPriority[MissionSequence[j]] > MissionPriority[MissionSequence[i]]) {
                    int a = MissionSequence[i];
                    MissionSequence[i] = MissionSequence[j];
                    MissionSequence[j] = a;
                } else if (MissionPriority[MissionSequence[j]] == MissionPriority[MissionSequence[i]]) {
                    if (TimePeriodNumAll[MissionSequence[j]] < TimePeriodNumAll[MissionSequence[i]]) {
                        int a = MissionSequence[i];
                        MissionSequence[i] = MissionSequence[j];
                        MissionSequence[j] = a;
                    }
                }
            }
        }

        //分配标致
        //0表示该轨道点无分配任务，
        // 100表示该轨道点进行阳光规避，
        // 100<i表示该轨道点进行数传任务，大小表示第i-100个地面站
        //i>0表示该轨道点进行成像任务，大小表示第i个任务
        int[] PlanningFlag = new int[(int) OrbitDataCount];
        for (int i = 0; i < (int) OrbitDataCount; i++) {
            PlanningFlag[i] = 0;
        }

        //首先分配阳光规避弧段
        //模块内计算阳光规避弧段
        int SunFlag_tBefore = 0;
        int SunAvoid_Flag = 0;
        int SunFlag_t = 0;
        SunAvoidTimePeriodNum = 0;
        for (int i = 0; i < OrbitalDataNum; i++) {
            double Time_JD = JD(Orbital_Time[i]);
            double[] r_sun = new double[3];//地心惯性坐标系下太阳位置
            double[] su = new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun = Sun(Time_JD, r_sun, su);
            double a = r_sun[0] * Orbital_SatPosition[i][0] + r_sun[1] * Orbital_SatPosition[i][1] + r_sun[2] * Orbital_SatPosition[i][2];
            double r_Sat = sqrt(Orbital_SatPosition[i][0] * Orbital_SatPosition[i][0] + Orbital_SatPosition[i][1] * Orbital_SatPosition[i][1] + Orbital_SatPosition[i][2] * Orbital_SatPosition[i][2]);
            double theta = acos(a / (rad_sun * r_Sat));

            if (theta >= 175 * PI / 180.0) {
                SunAvoid_Flag = 1;
                SunFlag_tBefore = SunFlag_t;
                SunFlag_t = SunAvoid_Flag;
            } else {
                SunAvoid_Flag = 0;
                SunFlag_tBefore = SunFlag_t;
                SunFlag_t = SunAvoid_Flag;
            }

            //判定开始结束时间
            if (SunFlag_tBefore == 0 && SunFlag_t == 1) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum] = i;
            } else if (SunFlag_tBefore == 1 && SunFlag_t == 0) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum + 1] = i - 1;
                SunAvoidTimePeriodNum = SunAvoidTimePeriodNum + 1;
            }
            if (i == OrbitalDataNum - 1 && SunFlag_t == 1) {
                SunAvoidTimePeriod[2 * SunAvoidTimePeriodNum + 1] = i;
                SunAvoidTimePeriodNum = SunAvoidTimePeriodNum + 1;
            }
        }
        for (int i = 0; i < SunAvoidTimePeriodNum; i++) {
            for (int j = SunAvoidTimePeriod[2 * i]; j <= SunAvoidTimePeriod[2 * i + 1]; j++) {
                PlanningFlag[j] = 100;
            }
        }

        //启发式算法分配任务
        PlanningMissionTimePeriod = new int[MissionNumber][2];
        for (int i = 0; i < MissionNumber; i++) {
            double MissionTime = MissionStareTime[MissionSequence[i]];
            double MissionTimeMin = ImageTimeMin;
            int HalfTime = (int) (MissionTime / 2 + MissionTimeMin / 2);
            //无可见弧段
            if (TimePeriodNumAll[MissionSequence[i]] == 0) {
                PlanningMissionFailReason[MissionSequence[i]] = 2;
            }
            for (int j = 0; j < TimePeriodNumAll[MissionSequence[i]]; j++) {
                int MiddTimePeriod = (VisibilityTimePeriodAll[MissionSequence[i]][2 * j] + VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1]) / 2;
                //成像窗口前移
                for (int k = MiddTimePeriod; k > VisibilityTimePeriodAll[MissionSequence[i]][2 * j] + (int) (MissionTime / 2); k--) {
                    int FlagSum = 0;
                    for (int l = k - HalfTime; l <= k + HalfTime; l++) {
                        FlagSum = FlagSum + PlanningFlag[l];
                    }
                    if (FlagSum == 0) {
                        PlanningMissionFailReason[MissionSequence[i]] = 1;
                        PlanningMissionTimePeriod[MissionSequence[i]][0] = k - (int) (MissionTime / 2);
                        PlanningMissionTimePeriod[MissionSequence[i]][1] = k + (int) (MissionTime / 2);
                        PlanningMissionLoad[MissionSequence[i]] = VisibilityLoadType[MissionSequence[i]][j];
                        for (int l = k - HalfTime; l < k + HalfTime; l++) {
                            PlanningFlag[l] = MissionSequence[i] + 1;
                        }
                        break;
                    }
                }
                if (PlanningMissionFailReason[MissionSequence[i]] == 0) {
                    //成像窗口后移
                    for (int k = MiddTimePeriod; k < VisibilityTimePeriodAll[MissionSequence[i]][2 * j + 1] - (int) (MissionTime / 2); k++) {
                        int FlagSum = 0;
                        for (int l = k - HalfTime; l <= k + HalfTime; l++) {
                            FlagSum = FlagSum + PlanningFlag[l];
                        }
                        if (FlagSum == 0) {
                            PlanningMissionFailReason[MissionSequence[i]] = 1;
                            PlanningMissionTimePeriod[MissionSequence[i]][0] = k - (int) (MissionTime / 2);
                            PlanningMissionTimePeriod[MissionSequence[i]][1] = k + (int) (MissionTime / 2);
                            PlanningMissionLoad[MissionSequence[i]] = VisibilityLoadType[MissionSequence[i]][j];
                            for (int l = k - HalfTime; l < k + HalfTime; l++) {
                                PlanningFlag[l] = MissionSequence[i] + 1;
                            }
                            break;
                        }
                    }
                }
                if (PlanningMissionFailReason[MissionSequence[i]] == 1) {
                    break;
                }
                //冲突
                if (j == TimePeriodNumAll[MissionSequence[i]] - 1 && PlanningMissionFailReason[MissionSequence[i]] == 0) {
                    PlanningMissionFailReason[MissionSequence[i]] = 3;
                }
            }
        }

        //传输任务分配
        ArrayList<int[][]> PlanningTransTimePeriodList=new ArrayList<>();
        ArrayList<Integer> PlanningTransNumList=new ArrayList<>();
        int MissionPeriodAll = 0;
        StationNumber = 1;
        for (int j = 0; j < StationMissionNum; j++) {
            PlanningTransNum = 0;
            int Side_Flag = 0;
            int Flag_tBefore = 0;
            int Visibility_Flag = 0;
            int Flag_t = 0;
            double[] Target_LLA = new double[3];
            double[] Target_ECEF = new double[3];
            Target_LLA[0] = StationPosition[StationNumber][0];
            Target_LLA[1] = StationPosition[StationNumber][1];
            Target_LLA[2] = StationPosition[StationNumber][2];
            LLAToECEF(Target_LLA, Target_ECEF);
            for (int k = 0; k < (int) OrbitDataCount; k++) {
                double Time_JD = JD(Orbital_Time[k]);
                int Flag_StationTime=0;
                double StationStarTime_JD=JD(StationMissionStarTime[j]);
                double StationEndTime_JD=JD(StationMissionEndTime[j]);
                if (Time_JD >= StationStarTime_JD && Time_JD<=StationEndTime_JD) {
                    Flag_StationTime=1;
                }
                if (Flag_StationTime == 1) {
                    double[] Target_GEI = new double[3];
                    ECEFToICRS(Time_JD, Target_ECEF, Target_GEI);
                    double[] SatPositionRe_LLA = new double[3];
                    double[] SatPosition_ECEF = new double[3];
                    SatPositionRe_LLA[0] = Orbital_SatPositionLLA[k][0];
                    SatPositionRe_LLA[1] = Orbital_SatPositionLLA[k][1];
                    SatPositionRe_LLA[2] = Orbital_SatPositionLLA[k][2];
                    LLAToECEF(SatPositionRe_LLA, SatPosition_ECEF);
                    Side_Flag = 1;
                    if (Side_Flag == 1) {
                        Visibility_Flag = StationVisibilityJudge(Target_ECEF, SatPosition_ECEF, StationPitch[StationNumber]);
                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                    } else {
                        Visibility_Flag = 0;
                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                    }
                }else {
                    Visibility_Flag = 0;
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                }

                if (Flag_tBefore == 0 && Flag_t == 1) {
                    PlanningTransTimePeriod[PlanningTransNum][0] = k;
                    PlanningTransStation[PlanningTransNum] = StationNumber;
                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                    PlanningTransTimePeriod[PlanningTransNum][1] = k - 1;
                    PlanningTransNum = PlanningTransNum + 1;
                }
                if (k == (int)OrbitDataCount - 1 && Flag_t == 1) {
                    PlanningTransTimePeriod[PlanningTransNum][1] = k;
                    PlanningTransNum = PlanningTransNum + 1;
                }
            }

            MissionPeriodAll=MissionPeriodAll+PlanningTransNum;
            int[][] PlanningTransTimePeriod_iList=PlanningTransTimePeriod;
            int PlanningTransNum_iList=PlanningTransNum;
            PlanningTransTimePeriodList.add(j,PlanningTransTimePeriod_iList);
            PlanningTransNumList.add(j,PlanningTransNum_iList);
        }

        //数据传出
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        String transmission_number = "tn_" + Instant.now().toEpochMilli();
        //任务规划结果数据传出
        for (int i = 0; i < MissionNumber; i++) {
            ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
            if (PlanningMissionFailReason[i] == 0) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "待规划");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);
            } else if (PlanningMissionFailReason[i] == 1) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", PlanningMissionLoad[i]);
                ImageWindowjsonObject.append("start_time", Time_Point[PlanningMissionTimePeriod[i][0]]);
                ImageWindowjsonObject.append("end_time", Time_Point[PlanningMissionTimePeriod[i][1]]);
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "待执行");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);

                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待执行");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }

            } else if (PlanningMissionFailReason[i] == 2) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "不可见");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);

                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","被退回");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }

            } else if (PlanningMissionFailReason[i] == 3) {
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "任务冲突");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);


                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","被退回");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }
            Document modifiers = new Document();
            modifiers.append("$set", ImageMissionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", ImageMissionjson.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }

        //传输任务数据传出
        if(TransmissionMissionJson != null) {
            ArrayList<Document> TranWindowjsonArry = new ArrayList<>();
            for (int i = 0; i < StationMissionNum; i++) {

                for (int j = 0; j < PlanningTransNumList.get(i); j++) {
                    Document TranWindowjsonObject = new Document();
                    TranWindowjsonObject.append("station_name", StationSerialNumber[1]);
                    TranWindowjsonObject.append("start_time", Time_Point[PlanningTransTimePeriodList.get(i)[j][0]]);
                    TranWindowjsonObject.append("end_time", Time_Point[PlanningTransTimePeriodList.get(i)[j][1]]);
                    TranWindowjsonArry.add(TranWindowjsonObject);
                }
                if (PlanningTransNumList.get(i) == 0) {
                    Document d=StationMissionJson.get(i);
                    if (d.containsKey("_id"))
                        d.remove("_id");
                    d.append("tag","被退回");
                    Document modifiers = new Document();
                    modifiers.append("$set", d);
                    MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                    station_mission.updateOne(new Document("mission_number", d.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                }else{
                    Document d=StationMissionJson.get(i);
                    if (d.containsKey("_id"))
                        d.remove("_id");
                    d.append("tag","待执行");
                    Document modifiers = new Document();
                    modifiers.append("$set", d);
                    MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                    station_mission.updateOne(new Document("mission_number", d.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                }
            }
            if (MissionPeriodAll == 0) {
                Document TranWindowjsonObject = new Document();
                TranWindowjsonObject.append("station_name", "");
                TranWindowjsonObject.append("start_time", "");
                TranWindowjsonObject.append("end_time", "");
                TranWindowjsonArry.add(TranWindowjsonObject);
            }
            //地面站，传输任务更新？？？？
            TransmissionMissionJson.append("transmission_window", TranWindowjsonArry);

            if(TransmissionMissionJson.containsKey("_id"))
                TransmissionMissionJson.remove("_id");
            MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
            Document modifiers = new Document();
            modifiers.append("$set", TransmissionMissionJson);
            transmission_mission.updateOne(new Document("transmission_number", TransmissionMissionJson.getString("transmission_number")), modifiers, new UpdateOptions().upsert(true));

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
        BToO = MatrixMultiplication(BToO, BToS);//BToO=Result

        //欧拉角转序位1-2-3
        sy = sqrt(BToO[0][0] * BToO[0][0] + BToO[1][0] * BToO[1][0]);
        if (sy < pow(10, -6))
            flag = 1;
        else
            flag = 0;
        if (flag == 0) {
            //atan2(X,Y)的含义和atan(X/Y)的含义是一样的。
            x = atan2(BToO[2][1], BToO[2][2]);
            y = atan2(-BToO[2][0], sy);
            z = atan2(BToO[1][0], BToO[0][0]);
        } else {
            x = atan2(-BToO[1][2], BToO[1][1]);
            y = atan2(-BToO[2][0], sy);
            z = 0;
        }

        Attitude = new double[]{x, y, z};
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
        if (a / (r_error * r_Target) >= cos(PI/2-StationPitch))
            return 1;
        else
            return 0;
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
    private static void ECEFToICRSold(double JD, double position_ECEF[], double position_GEI[]) {
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
    //地固坐标系转到惯性坐标系
    private static void ECEFToICRS(double JD, double position_ECEF[], double position_GEI[]) {
        double T = (JD - 2451545.0) / 36525.0;

        //岁差角
        double Zeta_A = 2.5976176 + 2306.0809506*T + 0.3019015*T*T + 0.0179663*T*T*T - 0.0000327*T*T*T*T - 0.0000002*T*T*T*T*T;//秒
        double Theta_A = 2004.1917476*T - 0.4269353*T*T - 0.041825*T*T*T - 0.0000601*T*T*T*T - 0.0000001*T*T*T*T*T;
        double Z_A = -2.5976176 + 2306.0803226*T + 1.094779*T*T + 0.0182273*T*T*T + 0.000047*T*T*T*T - 0.0000003*T*T*T*T*T;
        Zeta_A = Zeta_A/3600.0;//度
        Theta_A = Theta_A/3600.0;
        Z_A = Z_A/3600.0;
        //岁差矩阵
        double[][] R3Z_A={{cos(-Z_A*PI/180.0),sin(-Z_A*PI/180.0),0},
                {-sin(-Z_A*PI/180.0),cos(-Z_A*PI/180.0),0},
                {0,0,1}};
        double[][] R2Theta_A={{cos(Theta_A*PI/180.0),0,-sin(Theta_A*PI/180.0)},
                {0,1,0},
                {sin(Theta_A*PI/180.0),0,cos(Theta_A*PI/180.0)}};
        double[][] R3_Zeta_A={{cos(-Zeta_A*PI/180.0),sin(-Zeta_A*PI/180.0),0},
                {-sin(-Zeta_A*PI/180.0),cos(-Zeta_A*PI/180.0),0},
                {0,0,1}};
        double[][] PR=new double[3][3];
        double[][] PR_mid=new double[3][3];
        PR_mid=MatrixMultiplication(R3Z_A,R2Theta_A);
        PR=MatrixMultiplication(PR_mid,R3_Zeta_A);

        //章动计算
        double Epsilon_A = 84381.448 - 46.8150*T - 0.00059*T*T + 0.001813*T*T*T;
        Epsilon_A = Epsilon_A/3600.0;
        // http://blog.sina.com.cn/s/blog_852e40660100w1m6.html
        double L = 280.4665+36000.7698*T;
        double dL = 218.3165+481267.8813*T;
        double Omega = 125.04452-1934.136261*T;
        double DeltaPsi = -17.20*sin(Omega*PI/180.0)-1.32*sin(2*L*PI/180.0)-0.23*sin(2*dL*PI/180.0)+0.21*sin(2*Omega*PI/180.0);
        double DeltaEpsilon = 9.20*cos(Omega*PI/180.0)+0.57*cos(2*L*PI/180.0)+0.10*cos(2*dL*PI/180.0)-0.09*cos(2*Omega*PI/180.0);
        DeltaPsi = DeltaPsi/3600.0;
        DeltaEpsilon = DeltaEpsilon/3600.0;

        //章动矩阵
        double[][] R1_DEA={{1,0,0},
                {0,cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0),sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0)},
                {0,-sin(-(DeltaEpsilon+Epsilon_A)*PI/180.0),cos(-(DeltaEpsilon+Epsilon_A)*PI/180.0)}};
        double[][] R3_DeltaPsi={{cos(-DeltaPsi*PI/180.0),sin(-DeltaPsi*PI/180.0),0},
                {-sin(-DeltaPsi*PI/180.0),cos(-DeltaPsi*PI/180.0),0},
                {0,0,1}};
        double[][] R1_Epsilon={{1,0,0},
                {0,cos(Epsilon_A*PI/180.0),sin(Epsilon_A*PI/180.0)},
                {0,-sin(Epsilon_A*PI/180.0),cos(Epsilon_A*PI/180.0)}};
        double[][] NR=new double[3][3];
        double[][] NR_mid=new double[3][3];
        NR_mid=MatrixMultiplication(R1_DEA,R3_DeltaPsi);
        NR=MatrixMultiplication(NR_mid,R1_Epsilon);

        //地球自转
        double GMST = 280.46061837 + 360.98564736629*(JD-2451545.0) + 0.000387933*T*T - T*T*T/38710000.0;
        GMST = GMST%360;
        double GAST = GMST + DeltaPsi*cos((DeltaEpsilon + Epsilon_A)*PI/180.0);
        GAST = GAST%360;
        double[][] ER={{cos(GAST*PI/180.0),sin(GAST*PI/180.0),0},
                {-sin(GAST*PI/180.0),cos(GAST*PI/180.0),0},
                {0,0,1}};

        //极移坐标
        //  https://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html
        // https://datacenter.iers.org/data/html/finals.all.html
        double Xp = 0.001674*0.955;
        double Yp = 0.001462*0.955;
        // 极移矩阵
        double[][] R1_YP={{1,0,0},
                {0,cos(-Yp*PI/180.0),sin(-Yp*PI/180.0)},
                {0,-sin(-Yp*PI/180.0),cos(-Yp*PI/180.0)}};
        double[][] R2_XP={{cos(-Xp*PI/180.0),0,-sin(-Xp*PI/180.0)},
                {0,1,0},
                {sin(-Xp*PI/180.0),0,cos(-Xp*PI/180.0)}};
        double[][] EP=new double[3][3];
        EP=MatrixMultiplication(R1_YP,R2_XP);

        // 空固坐标系到地固坐标系的转换矩阵
        double[][] EPER=new double[3][3];
        double[][] EPERNR=new double[3][3];
        double[][] ECEF;
        EPER=MatrixMultiplication(EP,ER);
        EPERNR=MatrixMultiplication(EPER,NR);
        ECEF=MatrixMultiplication(EPERNR,PR);
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
    private static void GEIToORFold(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
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
    private static void GEIToORF(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        //System.out.println(Double.toString(xs[0])+","+Double.toString(xs[1])+","+Double.toString(xs[2]));
        //System.out.println(Double.toString(ys[0])+","+Double.toString(ys[1])+","+Double.toString(ys[2]));
        //System.out.println(Double.toString(zs[0])+","+Double.toString(zs[1])+","+Double.toString(zs[2]));
        double r_ys=sqrt(pow(ys[0],2)+pow(ys[1],2)+pow(ys[2],2));
        ys[0]=ys[0]/r_ys;
        ys[1]=ys[1]/r_ys;
        ys[2]=ys[2]/r_ys;
        xs=VectorCross(ys,zs);
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

    private static double DoublePointToAngle(double[] Satellite_ECEF,double[] Target_ECEF){
        double[] Error=new double[3];
        Error[0]=Satellite_ECEF[0]-Target_ECEF[0];
        Error[1]=Satellite_ECEF[1]-Target_ECEF[1];
        Error[2]=Satellite_ECEF[2]-Target_ECEF[2];

        double a=Error[0]*Target_ECEF[0]+Error[1]*Target_ECEF[1]+Error[2]*Target_ECEF[2];
        double b=sqrt(Error[0]*Error[0]+Error[1]*Error[1]+Error[2]*Error[2]);
        double c=sqrt(Target_ECEF[0]*Target_ECEF[0]+Target_ECEF[1]*Target_ECEF[1]+Target_ECEF[2]*Target_ECEF[2]);

        double Ang=acos(a/(b*c));
        return Ang;
    }

    private static ArrayList<int[]> AvoidSunshineII(ArrayList<double[]> OrbitTimeList,ArrayList<double[]> OrbitSatPositionGEIList,ArrayList<double[]> OrbitSatVelocityGEIList ){
        //读入模板
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res=mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first=sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties=(ArrayList<Document>) first.get("properties");
        double sun_angle_threshold=25*PI/180.0;//阳光夹角门限
        double sun_middle_day_duration=1800;//正午规避时长
        for (Document document:properties){
            if (document.getString("key").equals("sun_angle_threshold")){
                sun_angle_threshold=Double.parseDouble(document.get("value").toString())*PI/180.0;
            }else if (document.getString("key").equals("sun_middle_day_duration")) {
                sun_middle_day_duration=Double.parseDouble(document.get("value").toString());
            }
        }

        //轨道数据
        int OrbitalDataNum_Avoid = OrbitTimeList.size();

        Boolean SatFlyFlag_tBefore=true;//卫星正飞true/倒飞false判定
        Boolean SatFlyFlag_t=true;
        Boolean SatSunFlag_tBefore=false;
        Boolean SatSunFlag_t=false;
        //初始值
        if (OrbitalDataNum_Avoid > 0) {
            Boolean MiddleNightFlag=false;
            //判断当前时刻是白天false/黑夜true
            double Time_JD=JD(OrbitTimeList.get(0));
            double[] r_sun=new double[3];//地心惯性坐标系下太阳位置
            double[] su=new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun=Sun(Time_JD,r_sun,su);
            double a=r_sun[0]* OrbitSatPositionGEIList.get(0)[0]+r_sun[1]*OrbitSatPositionGEIList.get(0)[1]+r_sun[2]*OrbitSatPositionGEIList.get(0)[2];
            double r_Sat=sqrt(OrbitSatPositionGEIList.get(0)[0]*OrbitSatPositionGEIList.get(0)[0]+OrbitSatPositionGEIList.get(0)[1]*OrbitSatPositionGEIList.get(0)[1]+OrbitSatPositionGEIList.get(0)[2]*OrbitSatPositionGEIList.get(0)[2]);
            double r_Sun=sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]);
            double theta=acos(a/(r_Sun*r_Sat));
            if (theta <= PI/2) {
                MiddleNightFlag=false;
            }else {
                MiddleNightFlag=true;
            }
            if (MiddleNightFlag) {
                //午夜规避
                //卫星飞行方向
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(0)[0]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[1]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[2]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=false;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=true;
                    SatFlyFlag_t=true;
                }
                //惯性系下卫星到太阳的矢量
                double[] r_SatToSun_GEI=new double[]{r_sun[0]-OrbitSatPositionGEIList.get(0)[0],r_sun[1]-OrbitSatPositionGEIList.get(0)[1],r_sun[2]-OrbitSatPositionGEIList.get(0)[2]};
                //轨道系下卫星到太阳的矢量
                double[] r_SatToSun_ORF=new double[3];
                GEIToORF_Ellipse(OrbitSatPositionGEIList.get(0), OrbitSatVelocityGEIList.get(0), r_SatToSun_GEI, r_SatToSun_ORF);
                double r_SatToSun=sqrt(r_SatToSun_ORF[0]*r_SatToSun_ORF[0]+r_SatToSun_ORF[1]*r_SatToSun_ORF[1]+r_SatToSun_ORF[2]*r_SatToSun_ORF[2]);
                double[] r_SatToSun_n_ORF=new double[]{r_SatToSun_ORF[0]/r_SatToSun,r_SatToSun_ORF[1]/r_SatToSun,r_SatToSun_ORF[2]/r_SatToSun};
                double Theta_SatToSunxz=atan2(r_SatToSun_n_ORF[0],r_SatToSun_n_ORF[2]);
                if (abs(Theta_SatToSunxz) < sun_angle_threshold) {
                    SatSunFlag_tBefore=true;
                    SatSunFlag_t=true;
                }else {
                    SatSunFlag_tBefore=false;
                    SatSunFlag_t=false;
                }
            }else {
                SatSunFlag_tBefore=false;
                SatSunFlag_t=false;
                //正午规避
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(0)[0]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[1]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2]),
                        OrbitSatVelocityGEIList.get(0)[2]/sqrt(OrbitSatVelocityGEIList.get(0)[0]*OrbitSatVelocityGEIList.get(0)[0]+OrbitSatVelocityGEIList.get(0)[1]*OrbitSatVelocityGEIList.get(0)[1]+OrbitSatVelocityGEIList.get(0)[2]*OrbitSatVelocityGEIList.get(0)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=false;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=true;
                    SatFlyFlag_t=true;
                }
            }
        }

        ArrayList<int[]> SunAvoidTimePeriodList=new ArrayList<>();
        int[] SunAvoidTimePeriodNightListChild=new int[2];
        for (int i = 0; i < OrbitalDataNum_Avoid; i++) {
            Boolean MiddleNightFlag=false;
            if (i>=OrbitTimeList.size() || i>=OrbitSatPositionGEIList.size() || i>OrbitSatVelocityGEIList.size()) {
                break;
            }
            //判断当前时刻是白天false/黑夜true
            double Time_JD=JD(OrbitTimeList.get(i));
            double[] r_sun=new double[3];//地心惯性坐标系下太阳位置
            double[] su=new double[2];//赤经和赤纬
            double rad_sun;//太阳地球的距离
            rad_sun=Sun(Time_JD,r_sun,su);
            double a=r_sun[0]* OrbitSatPositionGEIList.get(i)[0]+r_sun[1]*OrbitSatPositionGEIList.get(i)[1]+r_sun[2]*OrbitSatPositionGEIList.get(i)[2];
            double r_Sat=sqrt(OrbitSatPositionGEIList.get(i)[0]*OrbitSatPositionGEIList.get(i)[0]+OrbitSatPositionGEIList.get(i)[1]*OrbitSatPositionGEIList.get(i)[1]+OrbitSatPositionGEIList.get(i)[2]*OrbitSatPositionGEIList.get(i)[2]);
            double r_Sun=sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]);
            double theta=acos(a/(r_Sun*r_Sat));
            if (theta <= PI/2) {
                MiddleNightFlag=false;
            }else {
                MiddleNightFlag=true;
            }
            if (MiddleNightFlag) {
                //午夜规避
                //卫星飞行方向
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(i)[0]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[1]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[2]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=true;
                }
                //惯性系下卫星到太阳的矢量
                double[] r_SatToSun_GEI=new double[]{r_sun[0]-OrbitSatPositionGEIList.get(i)[0],r_sun[1]-OrbitSatPositionGEIList.get(i)[1],r_sun[2]-OrbitSatPositionGEIList.get(i)[2]};
                //轨道系下卫星到太阳的矢量
                double[] r_SatToSun_ORF=new double[3];
                GEIToORF_Ellipse(OrbitSatPositionGEIList.get(i), OrbitSatVelocityGEIList.get(i), r_SatToSun_GEI, r_SatToSun_ORF);
                double r_SatToSun=sqrt(r_SatToSun_ORF[0]*r_SatToSun_ORF[0]+r_SatToSun_ORF[1]*r_SatToSun_ORF[1]+r_SatToSun_ORF[2]*r_SatToSun_ORF[2]);
                double[] r_SatToSun_n_ORF=new double[]{r_SatToSun_ORF[0]/r_SatToSun,r_SatToSun_ORF[1]/r_SatToSun,r_SatToSun_ORF[2]/r_SatToSun};
                double Theta_SatToSunxz=atan2(r_SatToSun_n_ORF[0],r_SatToSun_n_ORF[2]);
                if (abs(Theta_SatToSunxz) < sun_angle_threshold) {
                    SatSunFlag_tBefore=SatSunFlag_t;
                    SatSunFlag_t=true;
                }else {
                    SatSunFlag_tBefore=SatSunFlag_t;
                    SatSunFlag_t=false;
                }
                if (SatSunFlag_tBefore == false && SatSunFlag_t==true) {
                    SunAvoidTimePeriodNightListChild[0]=i-1;
                    if (SunAvoidTimePeriodNightListChild[0] < 0) {
                        SunAvoidTimePeriodNightListChild[0]=0;
                    }
                }else if (SatSunFlag_tBefore == true && SatSunFlag_t==false) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodList.add(nighttime);
                }
                if (SatSunFlag_tBefore == true && SatSunFlag_t==true && i==OrbitalDataNum_Avoid-1) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodList.add(nighttime);
                }
            }else {
                if (SatSunFlag_tBefore == true && SatSunFlag_t==true) {
                    SunAvoidTimePeriodNightListChild[1]=i;
                    int[] nighttime=new int[]{SunAvoidTimePeriodNightListChild[0],SunAvoidTimePeriodNightListChild[1]};
                    SunAvoidTimePeriodList.add(nighttime);
                }
                SatSunFlag_tBefore=false;
                SatSunFlag_t=false;
                //正午规避
                double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                        r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(i)[0]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[1]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2]),
                        OrbitSatVelocityGEIList.get(i)[2]/sqrt(OrbitSatVelocityGEIList.get(i)[0]*OrbitSatVelocityGEIList.get(i)[0]+OrbitSatVelocityGEIList.get(i)[1]*OrbitSatVelocityGEIList.get(i)[1]+OrbitSatVelocityGEIList.get(i)[2]*OrbitSatVelocityGEIList.get(i)[2])};
                double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                        (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                if (CosTheta_SunVel > 0) {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=false;
                }else {
                    SatFlyFlag_tBefore=SatFlyFlag_t;
                    SatFlyFlag_t=true;
                }

                if ((SatFlyFlag_tBefore==true && SatFlyFlag_t==false)||(SatFlyFlag_tBefore==false && SatFlyFlag_t==true)) {
                    int[] daytime=new int[]{i-(int) (sun_middle_day_duration/2),i+(int) (sun_middle_day_duration/2)};
                    if (daytime[0] < 0) {
                        daytime[0]=0;
                    }
                    if (daytime[1] >= OrbitalDataNum_Avoid) {
                        daytime[1]=OrbitalDataNum_Avoid-1;
                    }
                    SunAvoidTimePeriodList.add(daytime);
                }
            }
        }
        mongoClient.close();

        return SunAvoidTimePeriodList;
    }

    //惯性坐标系转到轨道坐标系，大椭圆轨道
    private static void GEIToORF_Ellipse(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
        double r = Math.sqrt(Math.pow(SatPosition_GEI[0], 2) + Math.pow(SatPosition_GEI[1], 2) + Math.pow(SatPosition_GEI[2], 2));
        double v = Math.sqrt(Math.pow(SatVelocity_GEI[0], 2) + Math.pow(SatVelocity_GEI[1], 2) + Math.pow(SatVelocity_GEI[2], 2));
        double[] zs = {-SatPosition_GEI[0] / r, -SatPosition_GEI[1] / r, -SatPosition_GEI[2] / r};
        double[] xs = {SatVelocity_GEI[0] / v, SatVelocity_GEI[1] / v, SatVelocity_GEI[2] / v};
        double[] ys = new double[3];
        ys = VectorCross(zs, xs);
        double r_ys=sqrt(pow(ys[0],2)+pow(ys[1],2)+pow(ys[2],2));
        ys[0]=ys[0]/r_ys;
        ys[1]=ys[1]/r_ys;
        ys[2]=ys[2]/r_ys;
        xs=VectorCross(ys,zs);
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

}
