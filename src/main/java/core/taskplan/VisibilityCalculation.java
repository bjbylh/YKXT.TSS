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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static java.lang.Math.*;

public class VisibilityCalculation {
    private static int TimeZone = -8;                     //北京时区到世界时-8
    //private static int TimeZone = 0;                     //北京时区到世界时-8

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
    private static int[] MissionWorkMode;               //传输模式，格式：每行代表一个任务，1：实传

    private static double MissionStarHeighLimt=200000;     //恒星定标模式轨道高度上下范围
    private static double MissionLimbHeighLimt=200000;     //临边观测模式轨道高度上下范围
    private static double MissionLimbAirHeighLimt=200000;   //临边观测模式大气高度范围

    //载荷变量
    // 载荷安装 矩阵，格式：每行代表一个载荷，每行格式[光轴与本体系x轴夹角，光轴与本体系y轴夹角，光轴与本体系z轴夹角]，单位：弧度
    private static int LoadNumber = 5;                    //载荷数量
    private static double[][] LoadInstall = {{90 * Math.PI / 180.0, 86.3 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 93.7 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 85.6 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 94.4 * Math.PI / 180.0, 3.7 * Math.PI / 180.0},
            {90 * Math.PI / 180.0, 90 * Math.PI / 180.0, 0 * Math.PI / 180.0}};
    //载荷视场角，格式：每行代表一个载荷，每行格式[内视角，外视角，上视角，下视角]，单位：弧度
    private static double[][] LoadViewAng = {{3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 / Math.PI / 180.0},
            {3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 / Math.PI / 180.0},
            {0.1 * Math.PI / 180.0, 0.1 * Math.PI / 180.0, 3 * Math.PI / 180.0, 3 / Math.PI / 180.0}};
    //卫星变量
    //卫星最大机动能力，最大机动欧拉角，格式[绕x轴最大机动角度，绕y轴最大机动角度，绕z轴最大机动角度]，单位：弧度
    public static double[] SatelliteManeuverEuler = {5 * Math.PI / 180.0, 5 * Math.PI / 180.0, 5 * Math.PI / 180.0};

    //地面站变量
    private static int StationNumber;                   //地面站数量
    private static String[] StationSerialNumber;        //地面站编号，格式：每行代表一个地面站
    private static double[][] StationPosition;          //地面站位置，格式，每行代表一个地面站，每行格式：[地面站经度，地面站纬度，地面站高度]，高度单位：米
    private static double[] StationPitch;               //地面站最低仰角要求，格式：每行代表一个地面站，每行格式：最小仰角角度，单位：弧度
    private static double[][] StationMissionStar;           //地面站传输任务时间
    private static double[][] StationMissionStop;       //地面站传输任务时间
    private static int StationMissionNum;               //地面站传输任务数量
    private static String[] StationMissionNumber;       //地面站传输任务编号


    //常量
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;


    //可见弧段计算
    //VisibilityTimePeriod[k][i][]：第k个载荷第i个任务的可见弧段，两个一组，分别为可见弧段的起始索引和可见弧段的结束索引
    //TimePeriodNum[k][i]：第k个载荷第i个任务的可见弧段数量
    //Time：输入，轨道数据对应的时间
    //SatPosition_GEI：输入：卫星在惯性系下的位置
    //SatVelocity_GEI：输入：卫星在惯性系下的速度
    //SatPosition_LLA：输入：卫星的经纬高
    //Json格式
    public static Document VisibilityCalculationII(Document Satllitejson, FindIterable<Document> Orbitjson, long orbitDataCount, ArrayList<Document> GroundStationjson, ArrayList<Document> Missionjson, ArrayList<Document> StationMissionjson) throws ParseException {
        //成像任务读入
        //ArrayList<Integer> MissionTargetTypeList=new ArrayList<Integer>();
        ArrayList<ArrayList<double[]>> MissionTargetAreaList = new ArrayList<ArrayList<double[]>>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionStarTimeList = new ArrayList<double[]>();
        ArrayList<double[]> MissionStopTimeList = new ArrayList<double[]>();
        ArrayList<String> MissionSerialNumberList = new ArrayList<String>();
        ArrayList<Integer> MissionImagingModeList = new ArrayList<Integer>();
        ArrayList<Integer> MissionWorkModeList = new ArrayList<Integer>();
        ArrayList<double[]> MissionTargetHeightList = new ArrayList<double[]>();
        ArrayList<String> MissionTransferStationList=new ArrayList<String>();
        ArrayList<ArrayList<String>> MissionForOrderNumbers=new ArrayList<>();
        ArrayList<Date[]> MissionStarStopTimeList=new ArrayList<>();
        MissionNumber = 0;

        //恒星定标、临边观测
        ArrayList<Document> MissionStarDocument=new ArrayList<>();
        ArrayList<Document> MissionLimbDocument=new ArrayList<>();
        ArrayList<Document> ImageMissionTemp=new ArrayList<>();
        for (Document document:Missionjson) {
            //将恒星定标和临边单独提出
            if (document.get("image_mode").toString().equals("恒星定标")) {
                MissionStarDocument.add(document);
            }else if (document.get("image_mode").toString().equals("临边观测")){
                MissionLimbDocument.add(document);
            }else {
                ImageMissionTemp.add(document);
            }
        }
        Missionjson.clear();
        for (Document document:ImageMissionTemp) {
            Missionjson.add(document);
        }

        try {
            for (Document document : Missionjson) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                    MissionTargetArea_iList = GetRegionPoint(target_region);
                    MissionTargetAreaList.add(MissionNumber, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);
                    /*//读取指定载荷
                    int[] MissionLoadType_iList = new int[]{0,0,0,0};
                    Boolean MissionLoadTypeFlag=true;
                    if (document.containsKey("mission_params") && document.get("mission_params")!=null) {
                        ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("mission_params");
                        for (Document document1:MissionParamsTemp) {
                            if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                if (document1.containsKey("value") && document1.get("value")!=null && !document1.get("value").equals("")) {
                                    if (document1.get("value").equals("1")) {
                                        MissionLoadType_iList[0]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("2")) {
                                        MissionLoadType_iList[1]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("3")) {
                                        MissionLoadType_iList[2]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("4")) {
                                        MissionLoadType_iList[3]=1;
                                        MissionLoadTypeFlag=false;
                                    }
                                }
                            }
                        }
                    }
                    if (MissionLoadTypeFlag) {
                        if (document.containsKey("default_mission_params") && document.get("default_mission_params")!=null) {
                            ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("default_mission_params");
                            for (Document document1:MissionParamsTemp) {
                                if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                    if (document1.containsKey("default_value") && document1.get("default_value")!=null && !document1.get("default_value").equals("")) {
                                        if (document1.get("default_value").equals("1")) {
                                            MissionLoadType_iList[0]=1;
                                        }else if (document1.get("default_value").equals("2")) {
                                            MissionLoadType_iList[1]=1;
                                        }else if (document1.get("default_value").equals("3")) {
                                            MissionLoadType_iList[2]=1;
                                        }else if (document1.get("default_value").equals("4")) {
                                            MissionLoadType_iList[3]=1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);*/
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = new double[6];
                    MissionStarTime_iList = DateToDouble(expected_start_time);
                    MissionStarTimeList.add(MissionNumber, MissionStarTime_iList);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList = new double[6];
                    MissionStopTime_iList=DateToDouble(expected_stop_time);
                    MissionStopTimeList.add(MissionNumber, MissionStopTime_iList);
                    Date[] MissionStarStopTime_iList=new Date[]{expected_start_time,expected_stop_time};
                    MissionStarStopTimeList.add(MissionNumber,MissionStarStopTime_iList);
                    //读取任务编号
                    String MissionSerialNumber_iList = document.getString("mission_number");
                    MissionSerialNumberList.add(MissionNumber, MissionSerialNumber_iList);
                    //读取成像模式
                    int MissionImagingMode_iList = 1;
                    if (document.getString("image_mode").equals("常规")) {
                        MissionImagingMode_iList = 1;
                    } else if (document.getString("image_mode").equals("凝视")) {
                        MissionImagingMode_iList = 2;
                    } else if (document.getString("image_mode").equals("定标")) {
                        MissionImagingMode_iList = 3;
                    }
                    MissionImagingModeList.add(MissionNumber, MissionImagingMode_iList);
                    //读取是否为实传模式
                    //读取所需地面站
                    String MissionTransferStation_iList=null;
                    int MissionWorkMode_iList = 1;
                    if (document.getString("work_mode").equals("记录")) {
                        MissionWorkMode_iList = 0;
                    } else {
                        MissionWorkMode_iList = 1;
                        MissionTransferStation_iList=document.get("station_number").toString();
                    }
                    MissionWorkModeList.add(MissionNumber, MissionWorkMode_iList);
                    MissionTransferStationList.add(MissionNumber,MissionTransferStation_iList);
                    //读取成像高度约束
                    double[] MissionTargetHeight_iList = new double[2];
                    MissionTargetHeight_iList[0] = Double.parseDouble(document.get("min_height_orbit").toString());
                    MissionTargetHeight_iList[1] = Double.parseDouble(document.get("max_height_orbit").toString());
                    MissionTargetHeightList.add(MissionNumber, MissionTargetHeight_iList);

                    //读取订单编号
                    ArrayList<String> MissionForOrderNumbers_i=new ArrayList<>();
                    MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");
                    MissionForOrderNumbers.add(MissionNumber,MissionForOrderNumbers_i);

                    //任务数量加1
                    MissionNumber = MissionNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //数传任务读入
        ArrayList<String> StationMissionStationNumberList = new ArrayList<String>();
        ArrayList<double[]> StationMissionStarList = new ArrayList<double[]>();
        ArrayList<double[]> StationMissionStopList = new ArrayList<double[]>();
        ArrayList<String> StationMissionSerialNumberList = new ArrayList<String>();
        ArrayList<Object> StationMissionModeList=new ArrayList<>();
        ArrayList<Object> StationMissionRecordFileNoList=new ArrayList<>();
        ArrayList<Object> StationMissionAutoAssignRecordFileList=new ArrayList<>();
        StationMissionNum = 0;
        Date StationstartTimeDateForName=new Date();
        try {
            for (Document document : StationMissionjson) {
                try {
                    //读取数传任务地面站代号
                    String StationMissionStationNumber_iList = document.getString("station_number");
                    StationMissionStationNumberList.add(StationMissionNum, StationMissionStationNumber_iList);
                    //读取数传任务期望时间
                    Date time_point = document.getDate("expected_start_time");
                    StationstartTimeDateForName=time_point;
                    double[] StationMissionStar_iList = new double[6];
                    StationMissionStar_iList = DateToDouble(time_point);
                    StationMissionStarList.add(StationMissionNum, StationMissionStar_iList);
                    time_point = document.getDate("expected_end_time");
                    double[] StationMissionStop_iList = new double[6];
                    StationMissionStop_iList = DateToDouble(time_point);
                    StationMissionStopList.add(StationMissionNum, StationMissionStop_iList);
                    //读取传输任务编号
                    String StationMissionSerialNumber_iList = document.getString("mission_number");
                    StationMissionSerialNumberList.add(StationMissionNum, StationMissionSerialNumber_iList);
                    StationMissionModeList.add(document.get("mode"));
                    StationMissionRecordFileNoList.add(document.get("record_file_no"));
                    StationMissionAutoAssignRecordFileList.add(document.get("auto_assign_record_file"));

                    //任务数量加1
                    StationMissionNum = StationMissionNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //载荷参数更新
        if (Satllitejson.size() == 0) {
            System.out.println("卫星资源数据为空");
        } else {
            try {
                ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //轨道数据
        ArrayList<Date> OrbitTimeDateList = new ArrayList<>();
        ArrayList<double[]> OrbitTimeList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionLLAList = new ArrayList<>();
        ArrayList<double[]> OrbitSatPositionECEFList=new ArrayList<>();

        int OrbitalDataNum = 0;
        try {
            for (Document document : Orbitjson) {
                try {
                    //读取轨道时间戳
                    Date time_point = document.getDate("time_point");
                    OrbitTimeDateList.add(OrbitalDataNum, time_point);
                    double[] OrbitTime_iList = new double[6];
                    OrbitTime_iList = DateToDouble(time_point);
                    OrbitTimeList.add(OrbitalDataNum, OrbitTime_iList);
                    //读取惯性系轨道位置
                    double[] SatPositionGEI = new double[3];
                    SatPositionGEI[0] = Double.parseDouble(document.get("P_x").toString());
                    SatPositionGEI[1] = Double.parseDouble(document.get("P_y").toString());
                    SatPositionGEI[2] = Double.parseDouble(document.get("P_z").toString());
                    OrbitSatPositionGEIList.add(OrbitalDataNum, SatPositionGEI);
                    //读取惯性系轨道速度
                    double[] SatVelocityGEI = new double[3];
                    SatVelocityGEI[0] = Double.parseDouble(document.get("Vx").toString());
                    SatVelocityGEI[1] = Double.parseDouble(document.get("Vy").toString());
                    SatVelocityGEI[2] = Double.parseDouble(document.get("Vz").toString());
                    OrbitSatVelocityGEIList.add(OrbitalDataNum, SatVelocityGEI);
                    //读取卫星经纬高
                    double[] SatPositionLLA = new double[3];
                    SatPositionLLA[0] = Double.parseDouble(document.get("lon").toString());
                    SatPositionLLA[1] = Double.parseDouble(document.get("lat").toString());
                    SatPositionLLA[2] = Double.parseDouble(document.get("H").toString());
                    OrbitSatPositionLLAList.add(OrbitalDataNum, SatPositionLLA);
                    //读取地心地固坐标系卫星位置
                    double[] SatPositionECEF=new double[3];
                    Document PositionECEFTemp= (Document) document.get("Position_EarthCenteredEarthFixed");
                    SatPositionECEF[0] = Double.parseDouble(PositionECEFTemp.get("P_x").toString());
                    SatPositionECEF[1] = Double.parseDouble(PositionECEFTemp.get("P_y").toString());
                    SatPositionECEF[2] = Double.parseDouble(PositionECEFTemp.get("P_z").toString());
                    OrbitSatPositionECEFList.add(OrbitalDataNum, SatPositionECEF);

                    //轨道个数加1
                    OrbitalDataNum = OrbitalDataNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //地面站资源读入
        ArrayList<String> StationSerialNumberList = new ArrayList<String>();
        ArrayList<double[]> StationPositionList = new ArrayList<double[]>();
        ArrayList<Double> StationPitchList = new ArrayList<Double>();
        StationNumber = 0;
        try {
            for (Document document : GroundStationjson) {
                try {
                    //读取地面站代号
                    String StationSerialNumber_iList = document.get("ground_station_code").toString();
                    StationSerialNumberList.add(StationNumber,StationSerialNumber_iList);
                    //读取地面站数据
                    ArrayList<Document> Stationproperties = (ArrayList<Document>) document.get("properties");
                    double[] StationPosition_iList = new double[3];
                    double StationPitch_iList = 0;
                    for (Document document1 : Stationproperties) {
                        if (document1.getString("key").equals("station_name")) {
                            //StationSerialNumber[StationNumber] = document1.getString("value");
                        } else if (document1.getString("key").equals("station_lon")) {
                            StationPosition_iList[0] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("station_lat")) {
                            StationPosition_iList[1] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("altitude")) {
                            StationPosition_iList[2] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("angle_pitch_min")) {
                            StationPitch_iList = Double.parseDouble(document1.getString("value")) * PI / 180.0;
                        }
                    }
                    StationPositionList.add(StationNumber, StationPosition_iList);
                    StationPitchList.add(StationNumber, StationPitch_iList);

                    //地面站数量加1
                    StationNumber = StationNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可见弧段计算
        double[] Target_LLA = new double[3];
        double[] Target_ECEF = new double[3];
        double[] Target_GEI = new double[3];
        double[] SatPositionRe_LLA = new double[3];
        double Time_JD;
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<ArrayList<ArrayList<int[]>>>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<ArrayList<Integer>>();

        //任务循环
        for (int Mission_i = 0; Mission_i < MissionNumber; Mission_i++) {
            ArrayList<ArrayList<int[]>> VisibilityTimePeriod_iList = new ArrayList<ArrayList<int[]>>();
            ArrayList<Integer> TimePeriodNum_iList = new ArrayList<Integer>();

            //定标
            if (MissionImagingModeList.get(Mission_i) == 3) {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        TimePeriodNum_iiList=1;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        VisibilityTimeperiod_iiiList[0]=(int) ((JD(MissionStarTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimeperiod_iiiList[1]=(int) ((JD(MissionStopTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
                VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
                TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
                continue;
            }

            //判断是否为实传任务
            if (MissionWorkModeList.get(Mission_i) == 1) {
                //搜索地面站资源信息
                double[] subStationPosition=new double[3];
                double subStationPitch=0;
                String subMissionTransferStation=MissionTransferStationList.get(Mission_i);
                for (int Station_i = 0; Station_i < StationNumber; Station_i++) {
                    if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                        subStationPosition=StationPositionList.get(Station_i);
                        subStationPitch=StationPitchList.get(Station_i);
                        break;
                    }
                }

                //搜索对应的传输任务
                ArrayList<double[]> subStationMissionStarTime=new ArrayList<double[]>();
                ArrayList<double[]> subStationMissionStopTime=new ArrayList<double[]>();
                /*
                for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
                    if (StationMissionStationNumberList.get(StationMission_i).equals(subMissionTransferStation)) {
                        subStationMissionStarTime.add(StationMissionStarList.get(StationMission_i));
                        subStationMissionStopTime.add(StationMissionStopList.get(StationMission_i));
                    }
                }
                */
                //期望时间
                subStationMissionStarTime.add(MissionStarTimeList.get(Mission_i));
                subStationMissionStopTime.add(MissionStopTimeList.get(Mission_i));
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //传输任务循环
                        for (int StationMission_i = 0; StationMission_i < subStationMissionStarTime.size(); StationMission_i++) {
                            //传输任务期望时间
                            double[] subsubStationMissionStarTime=new double[6];
                            double[] subsubStationMissionStopTime=new double[6];
                            subsubStationMissionStarTime=subStationMissionStarTime.get(StationMission_i);
                            subsubStationMissionStopTime=subStationMissionStopTime.get(StationMission_i);
                            //任务期望时间
                            double[] subMissionStarTime = new double[6];
                            double[] subMissionStopTime = new double[6];
                            ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                            double[] subMissionTargetHeight = new double[2];
                            subMissionStarTime = MissionStarTimeList.get(Mission_i);
                            subMissionStopTime = MissionStopTimeList.get(Mission_i);
                            subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                            subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                            int Flag_tBefore = 0;
                            int Flag_t = 0;
                            int Visibility_Flag = 0;
                            int VisibilityStation_Flag=0;
                            //轨道数据循环
                            int OrbitalStepPlus = 10;
                            int[] VisibilityTimeperiod_iiiList=new int[2];
                            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                                double[] NowTime = new double[6];
                                double[] SatPosition_LLA = new double[3];
                                double[] SatPosition_GEI = new double[3];
                                double[] SatVelocity_GEI = new double[3];
                                double[] SatPosition_ECEF=new double[3];
                                NowTime = OrbitTimeList.get(Orbit_i);
                                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                if (VisibilityStation_Flag == 1) {
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                }else {
                                    Visibility_Flag=0;
                                }

                                Flag_tBefore = Flag_t;
                                Flag_t = Visibility_Flag;
                                //判定开始结束时间，精确判断
                                if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[0] = Orbit_i;
                                } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[0]=l;
                                            break;
                                        }
                                    }
                                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 0) {
                                            VisibilityTimeperiod_iiiList[1]=l-1;
                                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                            break;
                                        }
                                    }
                                } else if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[1]=l;
                                            break;
                                        }
                                    }
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }

                                Orbit_i = Orbit_i + OrbitalStepPlus;
                            }
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            } else {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //任务期望时间
                        double[] subMissionStarTime = new double[6];
                        double[] subMissionStopTime = new double[6];
                        ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                        double[] subMissionTargetHeight = new double[2];
                        subMissionStarTime = MissionStarTimeList.get(Mission_i);
                        subMissionStopTime = MissionStopTimeList.get(Mission_i);
                        subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                        subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                        int Flag_tBefore = 0;
                        int Flag_t = 0;
                        int Visibility_Flag = 0;
                        //轨道数据循环
                        int OrbitalStepPlus = 10;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                            double[] NowTime = new double[6];
                            double[] SatPosition_LLA = new double[3];
                            double[] SatPosition_GEI = new double[3];
                            double[] SatVelocity_GEI = new double[3];
                            double[] SatPosition_ECEF=new double[3];
                            NowTime = OrbitTimeList.get(Orbit_i);
                            SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                            SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                            SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                            SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);

                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);

                            Flag_tBefore = Flag_t;
                            Flag_t = Visibility_Flag;
                            //判定开始结束时间，精确判断
                            if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[0] = Orbit_i;
                            } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[0]=l;
                                        break;
                                    }
                                }
                            } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 0) {
                                        VisibilityTimeperiod_iiiList[1]=l-1;
                                        int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                        VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                        VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                        VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                        TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                        break;
                                    }
                                }
                            } else if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                            }else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[1]=l;
                                        break;
                                    }
                                }
                                int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                            }

                            Orbit_i = Orbit_i + OrbitalStepPlus;
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            }

            VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
            TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
        }

        ArrayList<ArrayList<int[]>> StationVisibilityTimePeriodList = new ArrayList<ArrayList<int[]>>();
        ArrayList<Integer> StationVisibilityTimePeriodNumList = new ArrayList<Integer>();
        //传输任务循环
        for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
            int Side_Flag = 0;
            int Flag_tBefore = 0;
            int Visibility_Flag = 0;
            int Flag_t = 0;
            //传输任务期望时间
            double[] subStationMissionStarTime=new double[6];
            double[] subStationMissionStopTime=new double[6];
            String subMissionTransferStation;
            subStationMissionStarTime=StationMissionStarList.get(StationMission_i);
            subStationMissionStopTime=StationMissionStopList.get(StationMission_i);
            subMissionTransferStation=StationMissionStationNumberList.get(StationMission_i);
            //获取地面站位置
            double[] subStationPosition=new double[3];
            double subStationPitch=0;
            for (int Station_i = 0; Station_i < GroundStationjson.size(); Station_i++) {
                if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                    subStationPosition=StationPositionList.get(Station_i);
                    subStationPitch=StationPitchList.get(Station_i);
                    break;
                }
            }
            int PeriodNum = 0;
            ArrayList<int[]> StationVisibilityTimePeriod_iList=new ArrayList<int[]>();
            int[] StationVisibilityTimePeriod_iiList=new int[2];
            Target_LLA[0] = subStationPosition[0];
            Target_LLA[1] = subStationPosition[1];
            Target_LLA[2] = subStationPosition[2];
            LLAToECEF(Target_LLA, Target_ECEF);
            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; Orbit_i++) {
                double[] NowTime = new double[6];
                double[] SatPosition_LLA = new double[3];
                double[] SatPosition_GEI = new double[3];
                double[] SatVelocity_GEI = new double[3];
                double[] SatPosition_ECEF=new double[3];
                NowTime = OrbitTimeList.get(Orbit_i);
                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                if (Side_Flag == 1) {
                    Visibility_Flag = VisibilityStationMissionJudge(subStationMissionStarTime,subStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                } else {
                    Visibility_Flag = 0;
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                }

                if (Flag_tBefore == 0 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[0] = Orbit_i;
                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i-1;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }else if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }

            }
            StationVisibilityTimePeriodList.add(StationMission_i,StationVisibilityTimePeriod_iList);
            StationVisibilityTimePeriodNumList.add(StationMission_i,PeriodNum);
        }

        //恒星定标可见性计算
        if (MissionStarDocument.size() > 0) {
            StarVisibilityCalculationNew(MissionStarDocument, OrbitTimeDateList, OrbitTimeList, OrbitSatPositionGEIList);
        }
        //临边观测可见性计算
        if (MissionLimbDocument.size() > 0) {
            //StarVisibilityCalculationNew(MissionLimbDocument, OrbitTimeDateList, OrbitTimeList, OrbitSatPositionGEIList);
            LimbVisibilityCalculation( MissionLimbDocument, OrbitTimeDateList, OrbitTimeList, OrbitSatPositionGEIList, OrbitSatVelocityGEIList, OrbitSatPositionLLAList);
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

        //数据传出
        for (int i = 0; i < MissionNumber; i++) {
            int WindowsNum = 0;
            ArrayList<Document> AvailWindowjsonArray = new ArrayList<>();
            for (int j = 0; j < LoadNumber; j++) {
                for (int k = 0; k < TimePeriodNumList.get(i).get(j); k++) {
                    Document AvailWindowjsonObject = new Document();
                    AvailWindowjsonObject.append("load_number", j + 1);
                    AvailWindowjsonObject.append("amount_window", TimePeriodNumList.get(i).get(j));
                    AvailWindowjsonObject.append("window_number", k + 1);
                    if (MissionImagingModeList.get(i) == 3) {
                        AvailWindowjsonObject.append("window_start_time", MissionStarStopTimeList.get(i)[0]);
                        AvailWindowjsonObject.append("window_end_time", MissionStarStopTimeList.get(i)[1]);
                    }else {
                        AvailWindowjsonObject.append("window_start_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[0]));
                        AvailWindowjsonObject.append("window_end_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[1]));
                    }
                    AvailWindowjsonArray.add(AvailWindowjsonObject);
                    WindowsNum = WindowsNum + 1;
                }
                Missionjson.get(i).append("fail_reason", "");
                Missionjson.get(i).append("mission_state", "待规划");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待规划");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }
            if (WindowsNum == 0) {
                Missionjson.get(i).append("fail_reason", "不可见");
                Missionjson.get(i).append("mission_state", "被退回");
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

            } else {
                Missionjson.get(i).append("available_window", AvailWindowjsonArray);
            }
            if (Missionjson.get(i).containsKey("_id"))
                Missionjson.get(i).remove("_id");
            Document modifiers = new Document();
            modifiers.append("$set", Missionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            System.out.println(Missionjson.get(i).toString());
            image_mission.updateOne(new Document("mission_number", Missionjson.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }


        if (StationMissionjson.size() == 0) {
            mongoClient.close();
            return null;
        } else {
            ArrayList<String> mission_numbers = new ArrayList<>();

            SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
            Date startTimeDate= StationstartTimeDateForName;
            String startTimestr=dateFormat.format(startTimeDate);
            String imageModelstr="CS";

            String transmission_number = "tn_" + startTimestr+"_"+imageModelstr+"_"+ Instant.now().toEpochMilli();

            int StationMissionNum_Send=0;
            for (Document d : StationMissionjson) {
                if (d.containsKey("_id"))
                    d.remove("_id");
                String mission_number = d.getString("mission_number");
                mission_numbers.add(mission_number);
                if (StationVisibilityTimePeriodNumList.get(StationMissionNum_Send) >0) {
                    d.append("tag","待规划");
                    d.append("transmission_number", transmission_number);
                }else{
                    d.append("tag","被退回");
                }

                Document modifiers = new Document();
                modifiers.append("$set", d);
                MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                station_mission.updateOne(new Document("mission_number", d.getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
                StationMissionNum_Send=StationMissionNum_Send+1;
            }

            MongoCollection<Document> transmission_misison = mongoDatabase.getCollection("transmission_mission");

            Document Transmissionjson = new Document();
            Transmissionjson.append("transmission_number", transmission_number);
            Transmissionjson.append("mission_numbers", mission_numbers);

            ArrayList<Document> stationInfos = new ArrayList<>();
            int WindowsNum = 0;

            for (int i = 0; i < StationMissionNum; i++) {
                Document stationInfo = new Document();
                stationInfo.append("station_name", StationMissionStationNumberList.get(i));

                ArrayList<Document> StationWindowjsonArray = new ArrayList<>();
                for (int j = 0; j < StationVisibilityTimePeriodNumList.get(i); j++) {
                    Document StationWindowjsonObject = new Document();
                    StationWindowjsonObject.append("amount_window", StationVisibilityTimePeriodNumList.get(i));
                    StationWindowjsonObject.append("window_number", j + 1);
                    StationWindowjsonObject.append("window_start_time", OrbitTimeDateList.get(StationVisibilityTimePeriodList.get(i).get(j)[0]));
                    StationWindowjsonObject.append("window_end_time", OrbitTimeDateList.get(StationVisibilityTimePeriodList.get(i).get(j)[1]));
                    ArrayList<String> StationMissionNumberArray = new ArrayList<>();
                    //for (int k = 0; k < StationMissionNum; k++) {
                    //    StationMissionNumberArray.add(k, StationMissionSerialNumberList.get(k));
                    //}
                    StationMissionNumberArray.add(StationMissionSerialNumberList.get(i));
                    StationWindowjsonObject.append("mission_number", StationMissionNumberArray);
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
            if (StationMissionModeList.size()>0) {
                Transmissionjson.append("mode", StationMissionModeList.get(0));
                Transmissionjson.append("record_file_no", StationMissionRecordFileNoList.get(0));
                Transmissionjson.append("auto_assign_record_file", StationMissionAutoAssignRecordFileList.get(0));
            }
            transmission_misison.insertOne(Transmissionjson);
            mongoClient.close();

            return Transmissionjson;
        }
    }

    //应急任务可见性计算
    public static Map<Integer, Map<String, Boolean>> VisibilityCalculationEmergency(Document Satllitejson, FindIterable<Document> Orbitjson, long orbitDataCount, ArrayList<Document> GroundStationjson, ArrayList<Document> OrderMissionjson, ArrayList<Document> StationMissionjson) throws ParseException {
        //返回值定义
        Map<Integer, Map<String, Boolean>> map = new TreeMap<Integer, Map<String, Boolean>>();

        //成像任务读入
        //恒星定标、临边观测
        ArrayList<Document> MissionStarDocument=new ArrayList<>();
        ArrayList<Document> MissionLimbDocument=new ArrayList<>();
        ArrayList<Document> ImageMissionTemp=new ArrayList<>();
        for (Document document:OrderMissionjson) {
            //将恒星定标和临边单独提出
            if (document.get("image_mode").toString().equals("恒星定标")) {
                MissionStarDocument.add(document);
            }else if (document.get("image_mode").toString().equals("临边观测")){
                MissionLimbDocument.add(document);
            }else {
                ImageMissionTemp.add(document);
            }
        }
        OrderMissionjson.clear();
        for (Document document:ImageMissionTemp) {
            OrderMissionjson.add(document);
        }

        //ArrayList<Integer> MissionTargetTypeList=new ArrayList<Integer>();
        ArrayList<ArrayList<double[]>> MissionTargetAreaList = new ArrayList<ArrayList<double[]>>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionStarTimeList = new ArrayList<double[]>();
        ArrayList<double[]> MissionStopTimeList = new ArrayList<double[]>();
        ArrayList<String> MissionSerialNumberList = new ArrayList<String>();
        ArrayList<Integer> MissionImagingModeList = new ArrayList<Integer>();
        ArrayList<Integer> MissionWorkModeList = new ArrayList<Integer>();
        ArrayList<double[]> MissionTargetHeightList = new ArrayList<double[]>();
        ArrayList<String> MissionTransferStationList=new ArrayList<String>();
        MissionNumber = 0;
        try {
            for (Document document : OrderMissionjson) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                    MissionTargetArea_iList = GetRegionPoint(target_region);
                    MissionTargetAreaList.add(MissionNumber, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);
                    /*//读取指定载荷
                    int[] MissionLoadType_iList = new int[]{0,0,0,0};
                    Boolean MissionLoadTypeFlag=true;
                    if (document.containsKey("mission_params") && document.get("mission_params")!=null) {
                        ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("mission_params");
                        for (Document document1:MissionParamsTemp) {
                            if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                if (document1.containsKey("value") && document1.get("value")!=null && !document1.get("value").equals("")) {
                                    if (document1.get("value").equals("1")) {
                                        MissionLoadType_iList[0]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("2")) {
                                        MissionLoadType_iList[1]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("3")) {
                                        MissionLoadType_iList[2]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("4")) {
                                        MissionLoadType_iList[3]=1;
                                        MissionLoadTypeFlag=false;
                                    }
                                }
                            }
                        }
                    }
                    if (MissionLoadTypeFlag) {
                        if (document.containsKey("default_mission_params") && document.get("default_mission_params")!=null) {
                            ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("default_mission_params");
                            for (Document document1:MissionParamsTemp) {
                                if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                    if (document1.containsKey("default_value") && document1.get("default_value")!=null && !document1.get("default_value").equals("")) {
                                        if (document1.get("default_value").equals("1")) {
                                            MissionLoadType_iList[0]=1;
                                        }else if (document1.get("default_value").equals("2")) {
                                            MissionLoadType_iList[1]=1;
                                        }else if (document1.get("default_value").equals("3")) {
                                            MissionLoadType_iList[2]=1;
                                        }else if (document1.get("default_value").equals("4")) {
                                            MissionLoadType_iList[3]=1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);*/
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = new double[6];
                    MissionStarTime_iList = DateToDouble(expected_start_time);
                    MissionStarTimeList.add(MissionNumber, MissionStarTime_iList);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList = new double[6];
                    MissionStopTime_iList=DateToDouble(expected_stop_time);
                    MissionStopTimeList.add(MissionNumber, MissionStopTime_iList);
                    //读取任务订单编号
                    String MissionSerialNumber_iList = document.getString("order_number");
                    MissionSerialNumberList.add(MissionNumber, MissionSerialNumber_iList);
                    //读取成像模式
                    int MissionImagingMode_iList = 1;
                    if (document.getString("image_mode").equals("常规")) {
                        MissionImagingMode_iList = 1;
                    } else if (document.getString("image_mode").equals("凝视")) {
                        MissionImagingMode_iList = 2;
                    } else if (document.getString("image_mode").equals("定标")) {
                        MissionImagingMode_iList = 3;
                    }
                    MissionImagingModeList.add(MissionNumber, MissionImagingMode_iList);
                    //读取是否为实传模式
                    //读取所需地面站
                    String MissionTransferStation_iList=null;
                    int MissionWorkMode_iList = 1;
                    if (document.getString("work_mode").equals("记录")) {
                        MissionWorkMode_iList = 0;
                    } else {
                        MissionWorkMode_iList = 1;
                        MissionTransferStation_iList=document.get("station_number").toString();
                    }
                    MissionWorkModeList.add(MissionNumber, MissionWorkMode_iList);
                    MissionTransferStationList.add(MissionNumber,MissionTransferStation_iList);
                    //读取成像高度约束
                    double[] MissionTargetHeight_iList = new double[2];
                    MissionTargetHeight_iList[0] = Double.parseDouble(document.get("min_height_orbit").toString());
                    MissionTargetHeight_iList[1] = Double.parseDouble(document.get("max_height_orbit").toString());
                    MissionTargetHeightList.add(MissionNumber, MissionTargetHeight_iList);

                    //初始话输出
                    Map<String, Boolean> mapMission = new TreeMap<String, Boolean>();
                    mapMission.put(MissionSerialNumber_iList, false);
                    map.put(0, mapMission);

                    //任务数量加1
                    MissionNumber = MissionNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //数传任务读入
        ArrayList<String> StationMissionStationNumberList = new ArrayList<String>();
        ArrayList<double[]> StationMissionStarList = new ArrayList<double[]>();
        ArrayList<double[]> StationMissionStopList = new ArrayList<double[]>();
        ArrayList<String> StationMissionSerialNumberList = new ArrayList<String>();
        StationMissionNum = 0;
        if (StationMissionjson != null) {
            try {
                for (Document document : StationMissionjson) {
                    try {
                        //读取数传任务地面站代号
                        String StationMissionStationNumber_iList = document.getString("station_number");
                        StationMissionStationNumberList.add(StationMissionNum, StationMissionStationNumber_iList);
                        //读取数传任务期望时间
                        Date time_point = document.getDate("expected_start_time");
                        double[] StationMissionStar_iList = new double[6];
                        StationMissionStar_iList = DateToDouble(time_point);
                        StationMissionStarList.add(StationMissionNum, StationMissionStar_iList);
                        time_point = document.getDate("expected_end_time");
                        double[] StationMissionStop_iList = new double[6];
                        StationMissionStop_iList = DateToDouble(time_point);
                        StationMissionStopList.add(StationMissionNum, StationMissionStop_iList);
                        //读取传输任务编号
                        String StationMissionSerialNumber_iList = document.getString("mission_number");
                        StationMissionSerialNumberList.add(StationMissionNum, StationMissionSerialNumber_iList);

                        //初始话输出
                        Map<String, Boolean> mapStationMission = new TreeMap<String, Boolean>();
                        mapStationMission.put(StationMissionSerialNumber_iList, false);
                        map.put(1, mapStationMission);

                        //任务数量加1
                        StationMissionNum = StationMissionNum + 1;
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //载荷参数更新
        if (Satllitejson.size() == 0) {
            System.out.println("卫星资源数据为空");
            return map;
        } else {
            try {
                ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
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
            } catch (Exception e) {
                return map;
                //e.printStackTrace();
            }
        }

        //轨道数据
        ArrayList<Date> OrbitTimeDateList = new ArrayList<Date>();
        ArrayList<double[]> OrbitTimeList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionLLAList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionECEFList=new ArrayList<>();

        int OrbitalDataNum = 0;
        try {
            for (Document document : Orbitjson) {
                try {
                    //读取轨道时间戳
                    Date time_point = document.getDate("time_point");
                    OrbitTimeDateList.add(OrbitalDataNum, time_point);
                    double[] OrbitTime_iList = new double[6];
                    OrbitTime_iList = DateToDouble(time_point);
                    OrbitTimeList.add(OrbitalDataNum, OrbitTime_iList);
                    //读取惯性系轨道位置
                    double[] SatPositionGEI = new double[3];
                    SatPositionGEI[0] = Double.parseDouble(document.get("P_x").toString());
                    SatPositionGEI[1] = Double.parseDouble(document.get("P_y").toString());
                    SatPositionGEI[2] = Double.parseDouble(document.get("P_z").toString());
                    OrbitSatPositionGEIList.add(OrbitalDataNum, SatPositionGEI);
                    //读取惯性系轨道速度
                    double[] SatVelocityGEI = new double[3];
                    SatVelocityGEI[0] = Double.parseDouble(document.get("Vx").toString());
                    SatVelocityGEI[1] = Double.parseDouble(document.get("Vy").toString());
                    SatVelocityGEI[2] = Double.parseDouble(document.get("Vz").toString());
                    OrbitSatVelocityGEIList.add(OrbitalDataNum, SatVelocityGEI);
                    //读取卫星经纬高
                    double[] SatPositionLLA = new double[3];
                    SatPositionLLA[0] = Double.parseDouble(document.get("lon").toString());
                    SatPositionLLA[1] = Double.parseDouble(document.get("lat").toString());
                    SatPositionLLA[2] = Double.parseDouble(document.get("H").toString());
                    OrbitSatPositionLLAList.add(OrbitalDataNum, SatPositionLLA);
                    //读取地心地固坐标系卫星位置
                    double[] SatPositionECEF=new double[3];
                    Document PositionECEFTemp= (Document) document.get("Position_EarthCenteredEarthFixed");
                    SatPositionECEF[0] = Double.parseDouble(PositionECEFTemp.get("P_x").toString());
                    SatPositionECEF[1] = Double.parseDouble(PositionECEFTemp.get("P_y").toString());
                    SatPositionECEF[2] = Double.parseDouble(PositionECEFTemp.get("P_z").toString());
                    OrbitSatPositionECEFList.add(OrbitalDataNum, SatPositionECEF);

                    //轨道个数加1
                    OrbitalDataNum = OrbitalDataNum + 1;

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //地面站资源读入
        ArrayList<String> StationSerialNumberList = new ArrayList<String>();
        ArrayList<double[]> StationPositionList = new ArrayList<double[]>();
        ArrayList<Double> StationPitchList = new ArrayList<Double>();
        StationNumber = 0;
        try {
            for (Document document : GroundStationjson) {
                try {
                    //读取地面站代号
                    String StationSerialNumber_iList = document.get("ground_station_code").toString();
                    StationSerialNumberList.add(StationNumber,StationSerialNumber_iList);
                    //读取地面站数据
                    ArrayList<Document> Stationproperties = (ArrayList<Document>) document.get("properties");
                    double[] StationPosition_iList = new double[3];
                    double StationPitch_iList = 0;
                    for (Document document1 : Stationproperties) {
                        if (document1.getString("key").equals("station_name")) {
                            //StationSerialNumber[StationNumber] = document1.getString("value");
                        } else if (document1.getString("key").equals("station_lon")) {
                            StationPosition_iList[0] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("station_lat")) {
                            StationPosition_iList[1] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("altitude")) {
                            StationPosition_iList[2] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("angle_pitch_min")) {
                            StationPitch_iList = Double.parseDouble(document1.getString("value")) * PI / 180.0;
                        }
                    }
                    StationPositionList.add(StationNumber, StationPosition_iList);
                    StationPitchList.add(StationNumber, StationPitch_iList);

                    //地面站数量加1
                    StationNumber = StationNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可见弧段计算
        double[] Target_LLA = new double[3];
        double[] Target_ECEF = new double[3];
        double[] Target_GEI = new double[3];
        double[] SatPositionRe_LLA = new double[3];
        double Time_JD;
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<ArrayList<ArrayList<int[]>>>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<ArrayList<Integer>>();

        //任务循环
        for (int Mission_i = 0; Mission_i < MissionNumber; Mission_i++) {
            ArrayList<ArrayList<int[]>> VisibilityTimePeriod_iList = new ArrayList<ArrayList<int[]>>();
            ArrayList<Integer> TimePeriodNum_iList = new ArrayList<Integer>();

            //定标
            if (MissionImagingModeList.get(Mission_i) == 3) {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        TimePeriodNum_iiList=1;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        VisibilityTimeperiod_iiiList[0]=(int) ((JD(MissionStarTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimeperiod_iiiList[1]=(int) ((JD(MissionStopTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
                VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
                TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
                continue;
            }

            //判断是否为实传任务
            if (MissionWorkModeList.get(Mission_i) == 1) {
                //搜索地面站资源信息
                double[] subStationPosition=new double[3];
                double subStationPitch=0;
                String subMissionTransferStation=MissionTransferStationList.get(Mission_i);
                for (int Station_i = 0; Station_i < StationNumber; Station_i++) {
                    if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                        subStationPosition=StationPositionList.get(Station_i);
                        subStationPitch=StationPitchList.get(Station_i);
                        break;
                    }
                }
                //搜索对应的传输任务
                ArrayList<double[]> subStationMissionStarTime=new ArrayList<double[]>();
                ArrayList<double[]> subStationMissionStopTime=new ArrayList<double[]>();
                /*
                for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
                    if (StationMissionStationNumberList.get(StationMission_i).equals(subMissionTransferStation)) {
                        subStationMissionStarTime.add(StationMissionStarList.get(StationMission_i));
                        subStationMissionStopTime.add(StationMissionStopList.get(StationMission_i));
                    }
                }
                */
                //期望时间
                subStationMissionStarTime.add(MissionStarTimeList.get(Mission_i));
                subStationMissionStopTime.add(MissionStopTimeList.get(Mission_i));
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //传输任务循环
                        for (int StationMission_i = 0; StationMission_i < subStationMissionStarTime.size(); StationMission_i++) {
                            //传输任务期望时间
                            double[] subsubStationMissionStarTime=new double[6];
                            double[] subsubStationMissionStopTime=new double[6];
                            subsubStationMissionStarTime=subStationMissionStarTime.get(StationMission_i);
                            subsubStationMissionStopTime=subStationMissionStopTime.get(StationMission_i);
                            //任务期望时间
                            double[] subMissionStarTime = new double[6];
                            double[] subMissionStopTime = new double[6];
                            ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                            double[] subMissionTargetHeight = new double[2];
                            subMissionStarTime = MissionStarTimeList.get(Mission_i);
                            subMissionStopTime = MissionStopTimeList.get(Mission_i);
                            subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                            subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                            int Flag_tBefore = 0;
                            int Flag_t = 0;
                            int Visibility_Flag = 0;
                            int VisibilityStation_Flag=0;
                            //轨道数据循环
                            int OrbitalStepPlus = 10;
                            int[] VisibilityTimeperiod_iiiList=new int[2];
                            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                                double[] NowTime = new double[6];
                                double[] SatPosition_LLA = new double[3];
                                double[] SatPosition_GEI = new double[3];
                                double[] SatVelocity_GEI = new double[3];
                                double[] SatPosition_ECEF=new double[3];
                                NowTime = OrbitTimeList.get(Orbit_i);
                                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                if (VisibilityStation_Flag == 1) {
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                }else {
                                    Visibility_Flag=0;
                                }

                                Flag_tBefore = Flag_t;
                                Flag_t = Visibility_Flag;
                                //判定开始结束时间，精确判断
                                if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[0] = Orbit_i;
                                } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[0]=l;
                                            break;
                                        }
                                    }
                                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 0) {
                                            VisibilityTimeperiod_iiiList[1]=l-1;
                                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                            break;
                                        }
                                    }
                                } else if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[1]=l;
                                            break;
                                        }
                                    }
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }

                                Orbit_i = Orbit_i + OrbitalStepPlus;
                            }
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            } else {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //任务期望时间
                        double[] subMissionStarTime = new double[6];
                        double[] subMissionStopTime = new double[6];
                        ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                        double[] subMissionTargetHeight = new double[2];
                        subMissionStarTime = MissionStarTimeList.get(Mission_i);
                        subMissionStopTime = MissionStopTimeList.get(Mission_i);
                        subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                        subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                        int Flag_tBefore = 0;
                        int Flag_t = 0;
                        int Visibility_Flag = 0;
                        //轨道数据循环
                        int OrbitalStepPlus = 10;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                            double[] NowTime = new double[6];
                            double[] SatPosition_LLA = new double[3];
                            double[] SatPosition_GEI = new double[3];
                            double[] SatVelocity_GEI = new double[3];
                            double[] SatPosition_ECEF=new double[3];
                            NowTime = OrbitTimeList.get(Orbit_i);
                            SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                            SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                            SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                            SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);

                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);

                            Flag_tBefore = Flag_t;
                            Flag_t = Visibility_Flag;
                            //判定开始结束时间，精确判断
                            if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[0] = Orbit_i;
                            } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[0]=l;
                                        break;
                                    }
                                }
                            } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 0) {
                                        VisibilityTimeperiod_iiiList[1]=l-1;
                                        int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                        VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                        VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                        VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                        TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                        break;
                                    }
                                }
                            } else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[1]=l;
                                        break;
                                    }
                                }
                                int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                            }

                            Orbit_i = Orbit_i + OrbitalStepPlus;
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            }

            VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
            TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
        }

        ArrayList<ArrayList<int[]>> StationVisibilityTimePeriodList = new ArrayList<ArrayList<int[]>>();
        ArrayList<Integer> StationVisibilityTimePeriodNumList = new ArrayList<Integer>();
        //传输任务循环
        for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
            int Side_Flag = 0;
            int Flag_tBefore = 0;
            int Visibility_Flag = 0;
            int Flag_t = 0;
            //传输任务期望时间
            double[] subStationMissionStarTime=new double[6];
            double[] subStationMissionStopTime=new double[6];
            String subMissionTransferStation;
            subStationMissionStarTime=StationMissionStarList.get(StationMission_i);
            subStationMissionStopTime=StationMissionStopList.get(StationMission_i);
            subMissionTransferStation=StationMissionStationNumberList.get(StationMission_i);
            //获取地面站位置
            double[] subStationPosition=new double[3];
            double subStationPitch=0;
            for (int Station_i = 0; Station_i < GroundStationjson.size(); Station_i++) {
                if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                    subStationPosition=StationPositionList.get(Station_i);
                    subStationPitch=StationPitchList.get(Station_i);
                    break;
                }
            }
            int PeriodNum = 0;
            ArrayList<int[]> StationVisibilityTimePeriod_iList=new ArrayList<int[]>();
            int[] StationVisibilityTimePeriod_iiList=new int[2];
            Target_LLA[0] = subStationPosition[0];
            Target_LLA[1] = subStationPosition[1];
            Target_LLA[2] = subStationPosition[2];
            LLAToECEF(Target_LLA, Target_ECEF);
            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; Orbit_i++) {
                double[] NowTime = new double[6];
                double[] SatPosition_LLA = new double[3];
                double[] SatPosition_GEI = new double[3];
                double[] SatVelocity_GEI = new double[3];
                double[] SatPosition_ECEF=new double[3];
                NowTime = OrbitTimeList.get(Orbit_i);
                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                if (Side_Flag == 1) {
                    Visibility_Flag = VisibilityStationMissionJudge(subStationMissionStarTime,subStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                } else {
                    Visibility_Flag = 0;
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                }

                if (Flag_tBefore == 0 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[0] = Orbit_i;
                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i-1;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }
                if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }

            }
            StationVisibilityTimePeriodList.add(StationMission_i,StationVisibilityTimePeriod_iList);
            StationVisibilityTimePeriodNumList.add(StationMission_i,PeriodNum);
        }

        //恒星定标可见性计算
        Map<String, Boolean> Starmap=new HashMap<>();
        Map<String, Boolean> Limbmap=new HashMap<>();
        if (MissionStarDocument.size() > 0) {
            Starmap= EmerStarVisibilityCalculationNew( MissionStarDocument,  OrbitTimeDateList,  OrbitTimeList,  OrbitSatPositionGEIList);
        }
        //临边观测可见性计算
        if (MissionLimbDocument.size() > 0) {
            //Limbmap=EmerStarVisibilityCalculationNew( MissionLimbDocument,  OrbitTimeDateList,  OrbitTimeList,  OrbitSatPositionGEIList);
            Limbmap=EmerLimbVisibilityCalculation( MissionLimbDocument, OrbitTimeDateList, OrbitTimeList, OrbitSatPositionGEIList, OrbitSatVelocityGEIList, OrbitSatPositionLLAList);
        }

        //返回应急任务可见性结果
        //成像任务
        map.clear();
        Map<String, Boolean> map2 = new TreeMap<String, Boolean>();
        for (int Mission_i = 0; Mission_i < MissionNumber; Mission_i++) {
            int TimePeriodNumSum = 0;
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                TimePeriodNumSum = TimePeriodNumSum + TimePeriodNumList.get(Mission_i).get(Load_i);
            }
            if (TimePeriodNumSum >= 1) {
                map2.put(MissionSerialNumberList.get(Mission_i), true);
            } else {
                map2.put(MissionSerialNumberList.get(Mission_i), false);
            }
        }
        //恒星和临边任务
        if (!Starmap.isEmpty()) {
            for (Map.Entry<String,Boolean> entry:Starmap.entrySet()) {
                map2.put(entry.getKey(),entry.getValue());
            }
        }
        if (!Limbmap.isEmpty()) {
            for (Map.Entry<String,Boolean> entry:Limbmap.entrySet()) {
                map2.put(entry.getKey(),entry.getValue());
            }
        }
        map.put(0,map2);
        //传输任务
        Map<String, Boolean> map3 = new TreeMap<String, Boolean>();
        for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
            if (StationVisibilityTimePeriodNumList.get(StationMission_i) >= 1) {
                map3.put(StationMissionSerialNumberList.get(StationMission_i),true);
            }else{
                map3.put(StationMissionSerialNumberList.get(StationMission_i),false);
            }
        }
        map.put(1,map3);

        return map;
    }

    //应急任务可见性计算
    public static Map<Integer, Map<String, ArrayList<Date[]>>> VisibilityCalculationEmergencyII(Document Satllitejson, FindIterable<Document> Orbitjson, long orbitDataCount, ArrayList<Document> GroundStationjson, ArrayList<Document> OrderMissionjson, ArrayList<Document> StationMissionjson) throws ParseException {
        //返回值定义
        Map<Integer, Map<String, ArrayList<Date[]>>> map = new TreeMap<>();

        //成像任务读入
        //恒星定标、临边观测
        ArrayList<Document> MissionStarDocument=new ArrayList<>();
        ArrayList<Document> MissionLimbDocument=new ArrayList<>();
        ArrayList<Document> ImageMissionTemp=new ArrayList<>();
        for (Document document:OrderMissionjson) {
            //将恒星定标和临边单独提出
            if (document.get("image_mode").toString().equals("恒星定标")) {
                MissionStarDocument.add(document);
            }else if (document.get("image_mode").toString().equals("临边观测")){
                MissionLimbDocument.add(document);
            }else {
                ImageMissionTemp.add(document);
            }
        }
        OrderMissionjson.clear();
        for (Document document:ImageMissionTemp) {
            OrderMissionjson.add(document);
        }

        //ArrayList<Integer> MissionTargetTypeList=new ArrayList<Integer>();
        ArrayList<ArrayList<double[]>> MissionTargetAreaList = new ArrayList<ArrayList<double[]>>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionStarTimeList = new ArrayList<double[]>();
        ArrayList<double[]> MissionStopTimeList = new ArrayList<double[]>();
        ArrayList<String> MissionSerialNumberList = new ArrayList<String>();
        ArrayList<Integer> MissionImagingModeList = new ArrayList<Integer>();
        ArrayList<Integer> MissionWorkModeList = new ArrayList<Integer>();
        ArrayList<double[]> MissionTargetHeightList = new ArrayList<double[]>();
        ArrayList<String> MissionTransferStationList=new ArrayList<String>();
        MissionNumber = 0;
        try {
            for (Document document : OrderMissionjson) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                    MissionTargetArea_iList = GetRegionPoint(target_region);
                    MissionTargetAreaList.add(MissionNumber, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);
                    /*//读取指定载荷
                    int[] MissionLoadType_iList = new int[]{0,0,0,0};
                    Boolean MissionLoadTypeFlag=true;
                    if (document.containsKey("mission_params") && document.get("mission_params")!=null) {
                        ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("mission_params");
                        for (Document document1:MissionParamsTemp) {
                            if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                if (document1.containsKey("value") && document1.get("value")!=null && !document1.get("value").equals("")) {
                                    if (document1.get("value").equals("1")) {
                                        MissionLoadType_iList[0]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("2")) {
                                        MissionLoadType_iList[1]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("3")) {
                                        MissionLoadType_iList[2]=1;
                                        MissionLoadTypeFlag=false;
                                    }else if (document1.get("value").equals("4")) {
                                        MissionLoadType_iList[3]=1;
                                        MissionLoadTypeFlag=false;
                                    }
                                }
                            }
                        }
                    }
                    if (MissionLoadTypeFlag) {
                        if (document.containsKey("default_mission_params") && document.get("default_mission_params")!=null) {
                            ArrayList<Document> MissionParamsTemp= (ArrayList<Document>) document.get("default_mission_params");
                            for (Document document1:MissionParamsTemp) {
                                if (document1.containsKey("code") && document1.get("code")!=null && document1.get("code").equals("P_SATELLITE_USE")) {
                                    if (document1.containsKey("default_value") && document1.get("default_value")!=null && !document1.get("default_value").equals("")) {
                                        if (document1.get("default_value").equals("1")) {
                                            MissionLoadType_iList[0]=1;
                                        }else if (document1.get("default_value").equals("2")) {
                                            MissionLoadType_iList[1]=1;
                                        }else if (document1.get("default_value").equals("3")) {
                                            MissionLoadType_iList[2]=1;
                                        }else if (document1.get("default_value").equals("4")) {
                                            MissionLoadType_iList[3]=1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNumber, MissionLoadType_iList);*/
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = new double[6];
                    MissionStarTime_iList = DateToDouble(expected_start_time);
                    MissionStarTimeList.add(MissionNumber, MissionStarTime_iList);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList = new double[6];
                    MissionStopTime_iList=DateToDouble(expected_stop_time);
                    MissionStopTimeList.add(MissionNumber, MissionStopTime_iList);
                    //读取任务订单编号
                    String MissionSerialNumber_iList = document.getString("order_number");
                    MissionSerialNumberList.add(MissionNumber, MissionSerialNumber_iList);
                    //读取成像模式
                    int MissionImagingMode_iList = 1;
                    if (document.getString("image_mode").equals("常规")) {
                        MissionImagingMode_iList = 1;
                    } else if (document.getString("image_mode").equals("凝视")) {
                        MissionImagingMode_iList = 2;
                    } else if (document.getString("image_mode").equals("定标")) {
                        MissionImagingMode_iList = 3;
                    }
                    MissionImagingModeList.add(MissionNumber, MissionImagingMode_iList);
                    //读取是否为实传模式
                    //读取所需地面站
                    String MissionTransferStation_iList=null;
                    int MissionWorkMode_iList = 1;
                    if (document.getString("work_mode").equals("记录")) {
                        MissionWorkMode_iList = 0;
                    } else {
                        MissionWorkMode_iList = 1;
                        MissionTransferStation_iList=document.get("station_number").toString();
                    }
                    MissionWorkModeList.add(MissionNumber, MissionWorkMode_iList);
                    MissionTransferStationList.add(MissionNumber,MissionTransferStation_iList);
                    //读取成像高度约束
                    double[] MissionTargetHeight_iList = new double[2];
                    MissionTargetHeight_iList[0] = Double.parseDouble(document.get("min_height_orbit").toString());
                    MissionTargetHeight_iList[1] = Double.parseDouble(document.get("max_height_orbit").toString());
                    MissionTargetHeightList.add(MissionNumber, MissionTargetHeight_iList);

                    //初始话输出
                    Map<String, ArrayList<Date[]>> mapMission = new TreeMap<>();
                    mapMission.put(MissionSerialNumber_iList, new ArrayList<>());
                    map.put(0, mapMission);

                    //任务数量加1
                    MissionNumber = MissionNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //数传任务读入
        ArrayList<String> StationMissionStationNumberList = new ArrayList<String>();
        ArrayList<double[]> StationMissionStarList = new ArrayList<double[]>();
        ArrayList<double[]> StationMissionStopList = new ArrayList<double[]>();
        ArrayList<String> StationMissionSerialNumberList = new ArrayList<String>();
        StationMissionNum = 0;
        if (StationMissionjson != null) {
            try {
                for (Document document : StationMissionjson) {
                    try {
                        //读取数传任务地面站代号
                        String StationMissionStationNumber_iList = document.getString("station_number");
                        StationMissionStationNumberList.add(StationMissionNum, StationMissionStationNumber_iList);
                        //读取数传任务期望时间
                        Date time_point = document.getDate("expected_start_time");
                        double[] StationMissionStar_iList = new double[6];
                        StationMissionStar_iList = DateToDouble(time_point);
                        StationMissionStarList.add(StationMissionNum, StationMissionStar_iList);
                        time_point = document.getDate("expected_end_time");
                        double[] StationMissionStop_iList = new double[6];
                        StationMissionStop_iList = DateToDouble(time_point);
                        StationMissionStopList.add(StationMissionNum, StationMissionStop_iList);
                        //读取传输任务编号
                        String StationMissionSerialNumber_iList = document.getString("mission_number");
                        StationMissionSerialNumberList.add(StationMissionNum, StationMissionSerialNumber_iList);

                        //初始话输出
                        Map<String, ArrayList<Date[]>> mapStationMission = new TreeMap<>();
                        mapStationMission.put(StationMissionSerialNumber_iList, new ArrayList<>());
                        map.put(1, mapStationMission);

                        //任务数量加1
                        StationMissionNum = StationMissionNum + 1;
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //载荷参数更新
        if (Satllitejson.size() == 0) {
            System.out.println("卫星资源数据为空");
            return map;
        } else {
            try {
                ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
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
            } catch (Exception e) {
                return map;
                //e.printStackTrace();
            }
        }

        //轨道数据
        ArrayList<Date> OrbitTimeDateList = new ArrayList<Date>();
        ArrayList<double[]> OrbitTimeList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionGEIList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatVelocityGEIList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionLLAList = new ArrayList<double[]>();
        ArrayList<double[]> OrbitSatPositionECEFList=new ArrayList<>();

        int OrbitalDataNum = 0;
        try {
            for (Document document : Orbitjson) {
                try {
                    //读取轨道时间戳
                    Date time_point = document.getDate("time_point");
                    OrbitTimeDateList.add(OrbitalDataNum, time_point);
                    double[] OrbitTime_iList = new double[6];
                    OrbitTime_iList = DateToDouble(time_point);
                    OrbitTimeList.add(OrbitalDataNum, OrbitTime_iList);
                    //读取惯性系轨道位置
                    double[] SatPositionGEI = new double[3];
                    SatPositionGEI[0] = Double.parseDouble(document.get("P_x").toString());
                    SatPositionGEI[1] = Double.parseDouble(document.get("P_y").toString());
                    SatPositionGEI[2] = Double.parseDouble(document.get("P_z").toString());
                    OrbitSatPositionGEIList.add(OrbitalDataNum, SatPositionGEI);
                    //读取惯性系轨道速度
                    double[] SatVelocityGEI = new double[3];
                    SatVelocityGEI[0] = Double.parseDouble(document.get("Vx").toString());
                    SatVelocityGEI[1] = Double.parseDouble(document.get("Vy").toString());
                    SatVelocityGEI[2] = Double.parseDouble(document.get("Vz").toString());
                    OrbitSatVelocityGEIList.add(OrbitalDataNum, SatVelocityGEI);
                    //读取卫星经纬高
                    double[] SatPositionLLA = new double[3];
                    SatPositionLLA[0] = Double.parseDouble(document.get("lon").toString());
                    SatPositionLLA[1] = Double.parseDouble(document.get("lat").toString());
                    SatPositionLLA[2] = Double.parseDouble(document.get("H").toString());
                    OrbitSatPositionLLAList.add(OrbitalDataNum, SatPositionLLA);
                    //读取地心地固坐标系卫星位置
                    double[] SatPositionECEF=new double[3];
                    Document PositionECEFTemp= (Document) document.get("Position_EarthCenteredEarthFixed");
                    SatPositionECEF[0] = Double.parseDouble(PositionECEFTemp.get("P_x").toString());
                    SatPositionECEF[1] = Double.parseDouble(PositionECEFTemp.get("P_y").toString());
                    SatPositionECEF[2] = Double.parseDouble(PositionECEFTemp.get("P_z").toString());
                    OrbitSatPositionECEFList.add(OrbitalDataNum, SatPositionECEF);

                    //轨道个数加1
                    OrbitalDataNum = OrbitalDataNum + 1;

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //地面站资源读入
        ArrayList<String> StationSerialNumberList = new ArrayList<String>();
        ArrayList<double[]> StationPositionList = new ArrayList<double[]>();
        ArrayList<Double> StationPitchList = new ArrayList<Double>();
        StationNumber = 0;
        try {
            for (Document document : GroundStationjson) {
                try {
                    //读取地面站代号
                    String StationSerialNumber_iList = document.get("ground_station_code").toString();
                    StationSerialNumberList.add(StationNumber,StationSerialNumber_iList);
                    //读取地面站数据
                    ArrayList<Document> Stationproperties = (ArrayList<Document>) document.get("properties");
                    double[] StationPosition_iList = new double[3];
                    double StationPitch_iList = 0;
                    for (Document document1 : Stationproperties) {
                        if (document1.getString("key").equals("station_name")) {
                            //StationSerialNumber[StationNumber] = document1.getString("value");
                        } else if (document1.getString("key").equals("station_lon")) {
                            StationPosition_iList[0] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("station_lat")) {
                            StationPosition_iList[1] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("altitude")) {
                            StationPosition_iList[2] = Double.parseDouble(document1.getString("value"));
                        } else if (document1.getString("key").equals("angle_pitch_min")) {
                            StationPitch_iList = Double.parseDouble(document1.getString("value")) * PI / 180.0;
                        }
                    }
                    StationPositionList.add(StationNumber, StationPosition_iList);
                    StationPitchList.add(StationNumber, StationPitch_iList);

                    //地面站数量加1
                    StationNumber = StationNumber + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可见弧段计算
        double[] Target_LLA = new double[3];
        double[] Target_ECEF = new double[3];
        double[] Target_GEI = new double[3];
        double[] SatPositionRe_LLA = new double[3];
        double Time_JD;
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<ArrayList<ArrayList<int[]>>>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<ArrayList<Integer>>();

        //任务循环
        for (int Mission_i = 0; Mission_i < MissionNumber; Mission_i++) {
            ArrayList<ArrayList<int[]>> VisibilityTimePeriod_iList = new ArrayList<ArrayList<int[]>>();
            ArrayList<Integer> TimePeriodNum_iList = new ArrayList<Integer>();

            //定标
            if (MissionImagingModeList.get(Mission_i) == 3) {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        TimePeriodNum_iiList=1;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        VisibilityTimeperiod_iiiList[0]=(int) ((JD(MissionStarTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimeperiod_iiiList[1]=(int) ((JD(MissionStopTimeList.get(Mission_i))-JD(OrbitTimeList.get(0)))*24*60*60);
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
                VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
                TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
                continue;
            }

            //判断是否为实传任务
            if (MissionWorkModeList.get(Mission_i) == 1) {
                //搜索地面站资源信息
                double[] subStationPosition=new double[3];
                double subStationPitch=0;
                String subMissionTransferStation=MissionTransferStationList.get(Mission_i);
                for (int Station_i = 0; Station_i < StationNumber; Station_i++) {
                    if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                        subStationPosition=StationPositionList.get(Station_i);
                        subStationPitch=StationPitchList.get(Station_i);
                        break;
                    }
                }
                //搜索对应的传输任务
                ArrayList<double[]> subStationMissionStarTime=new ArrayList<double[]>();
                ArrayList<double[]> subStationMissionStopTime=new ArrayList<double[]>();
                /*
                for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
                    if (StationMissionStationNumberList.get(StationMission_i).equals(subMissionTransferStation)) {
                        subStationMissionStarTime.add(StationMissionStarList.get(StationMission_i));
                        subStationMissionStopTime.add(StationMissionStopList.get(StationMission_i));
                    }
                }
                */
                //期望时间
                subStationMissionStarTime.add(MissionStarTimeList.get(Mission_i));
                subStationMissionStopTime.add(MissionStopTimeList.get(Mission_i));
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //传输任务循环
                        for (int StationMission_i = 0; StationMission_i < subStationMissionStarTime.size(); StationMission_i++) {
                            //传输任务期望时间
                            double[] subsubStationMissionStarTime=new double[6];
                            double[] subsubStationMissionStopTime=new double[6];
                            subsubStationMissionStarTime=subStationMissionStarTime.get(StationMission_i);
                            subsubStationMissionStopTime=subStationMissionStopTime.get(StationMission_i);
                            //任务期望时间
                            double[] subMissionStarTime = new double[6];
                            double[] subMissionStopTime = new double[6];
                            ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                            double[] subMissionTargetHeight = new double[2];
                            subMissionStarTime = MissionStarTimeList.get(Mission_i);
                            subMissionStopTime = MissionStopTimeList.get(Mission_i);
                            subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                            subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                            int Flag_tBefore = 0;
                            int Flag_t = 0;
                            int Visibility_Flag = 0;
                            int VisibilityStation_Flag=0;
                            //轨道数据循环
                            int OrbitalStepPlus = 10;
                            int[] VisibilityTimeperiod_iiiList=new int[2];
                            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                                double[] NowTime = new double[6];
                                double[] SatPosition_LLA = new double[3];
                                double[] SatPosition_GEI = new double[3];
                                double[] SatVelocity_GEI = new double[3];
                                double[] SatPosition_ECEF=new double[3];
                                NowTime = OrbitTimeList.get(Orbit_i);
                                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                if (VisibilityStation_Flag == 1) {
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                }else {
                                    Visibility_Flag=0;
                                }

                                Flag_tBefore = Flag_t;
                                Flag_t = Visibility_Flag;
                                //判定开始结束时间，精确判断
                                if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[0] = Orbit_i;
                                } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[0]=l;
                                            break;
                                        }
                                    }
                                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                    for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        SatPosition_ECEF=OrbitSatPositionECEFList.get(l);
                                        //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                                        VisibilityStation_Flag=VisibilityStationMissionJudge(subsubStationMissionStarTime,subsubStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                                        if (VisibilityStation_Flag == 1) {
                                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        }else {
                                            Visibility_Flag=0;
                                        }
                                        if (Visibility_Flag == 0) {
                                            VisibilityTimeperiod_iiiList[1]=l-1;
                                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                            break;
                                        }
                                    }
                                } else if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                    VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                    for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                        NowTime = OrbitTimeList.get(l);
                                        SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                        SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                        SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                        Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                        if (Visibility_Flag == 1) {
                                            VisibilityTimeperiod_iiiList[1]=l;
                                            break;
                                        }
                                    }
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                }

                                Orbit_i = Orbit_i + OrbitalStepPlus;
                            }
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            } else {
                //载荷循环
                for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                    ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                    int TimePeriodNum_iiList = 0;
                    //判断载荷是否使用
                    if (MissionLoadTypeList.get(Mission_i)[Load_i] == 1) {
                        //任务期望时间
                        double[] subMissionStarTime = new double[6];
                        double[] subMissionStopTime = new double[6];
                        ArrayList<double[]> subMissionTargetArea = new ArrayList<double[]>();
                        double[] subMissionTargetHeight = new double[2];
                        subMissionStarTime = MissionStarTimeList.get(Mission_i);
                        subMissionStopTime = MissionStopTimeList.get(Mission_i);
                        subMissionTargetArea = MissionTargetAreaList.get(Mission_i);
                        subMissionTargetHeight = MissionTargetHeightList.get(Mission_i);

                        int Flag_tBefore = 0;
                        int Flag_t = 0;
                        int Visibility_Flag = 0;
                        //轨道数据循环
                        int OrbitalStepPlus = 10;
                        int[] VisibilityTimeperiod_iiiList=new int[2];
                        for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; ) {
                            double[] NowTime = new double[6];
                            double[] SatPosition_LLA = new double[3];
                            double[] SatPosition_GEI = new double[3];
                            double[] SatVelocity_GEI = new double[3];
                            double[] SatPosition_ECEF=new double[3];
                            NowTime = OrbitTimeList.get(Orbit_i);
                            SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                            SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                            SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                            SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);

                            Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);

                            Flag_tBefore = Flag_t;
                            Flag_t = Visibility_Flag;
                            //判定开始结束时间，精确判断
                            if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[0] = Orbit_i;
                            } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[0]=l;
                                        break;
                                    }
                                }
                            } else if (Flag_tBefore == 1 && Flag_t == 0) {
                                for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 0) {
                                        VisibilityTimeperiod_iiiList[1]=l-1;
                                        int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                        VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                        VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                        VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                        TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                        break;
                                    }
                                }
                            } else if (Orbit_i > OrbitalDataNum- OrbitalStepPlus - 1 && Flag_t == 1) {
                                VisibilityTimeperiod_iiiList[1]=Orbit_i;
                                for (int l = OrbitalDataNum-1; l > Orbit_i; l--) {
                                    NowTime = OrbitTimeList.get(l);
                                    SatPosition_LLA = OrbitSatPositionLLAList.get(l);
                                    SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                    SatVelocity_GEI = OrbitSatVelocityGEIList.get(l);
                                    Visibility_Flag = VisibilityJudgeNormal(subMissionStarTime, subMissionStopTime, subMissionTargetArea, subMissionTargetHeight, Load_i, NowTime, SatPosition_LLA, SatPosition_GEI, SatVelocity_GEI,SatPosition_ECEF);
                                    if (Visibility_Flag == 1) {
                                        VisibilityTimeperiod_iiiList[1]=l;
                                        break;
                                    }
                                }
                                int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                            }

                            Orbit_i = Orbit_i + OrbitalStepPlus;
                        }
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    } else {
                        VisibilityTimePeriod_iList.add(Load_i, VisibilityTimeperiod_iiList);
                        TimePeriodNum_iList.add(Load_i, TimePeriodNum_iiList);
                    }
                }
            }

            VisibilityTimePeriodList.add(Mission_i, VisibilityTimePeriod_iList);
            TimePeriodNumList.add(Mission_i, TimePeriodNum_iList);
        }

        ArrayList<ArrayList<int[]>> StationVisibilityTimePeriodList = new ArrayList<ArrayList<int[]>>();
        ArrayList<Integer> StationVisibilityTimePeriodNumList = new ArrayList<Integer>();
        //传输任务循环
        for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
            int Side_Flag = 0;
            int Flag_tBefore = 0;
            int Visibility_Flag = 0;
            int Flag_t = 0;
            //传输任务期望时间
            double[] subStationMissionStarTime=new double[6];
            double[] subStationMissionStopTime=new double[6];
            String subMissionTransferStation;
            subStationMissionStarTime=StationMissionStarList.get(StationMission_i);
            subStationMissionStopTime=StationMissionStopList.get(StationMission_i);
            subMissionTransferStation=StationMissionStationNumberList.get(StationMission_i);
            //获取地面站位置
            double[] subStationPosition=new double[3];
            double subStationPitch=0;
            for (int Station_i = 0; Station_i < GroundStationjson.size(); Station_i++) {
                if (StationSerialNumberList.get(Station_i).equals(subMissionTransferStation)) {
                    subStationPosition=StationPositionList.get(Station_i);
                    subStationPitch=StationPitchList.get(Station_i);
                    break;
                }
            }
            int PeriodNum = 0;
            ArrayList<int[]> StationVisibilityTimePeriod_iList=new ArrayList<int[]>();
            int[] StationVisibilityTimePeriod_iiList=new int[2];
            Target_LLA[0] = subStationPosition[0];
            Target_LLA[1] = subStationPosition[1];
            Target_LLA[2] = subStationPosition[2];
            LLAToECEF(Target_LLA, Target_ECEF);
            for (int Orbit_i = 0; Orbit_i < OrbitalDataNum; Orbit_i++) {
                double[] NowTime = new double[6];
                double[] SatPosition_LLA = new double[3];
                double[] SatPosition_GEI = new double[3];
                double[] SatVelocity_GEI = new double[3];
                double[] SatPosition_ECEF=new double[3];
                NowTime = OrbitTimeList.get(Orbit_i);
                SatPosition_LLA = OrbitSatPositionLLAList.get(Orbit_i);
                SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);
                SatVelocity_GEI = OrbitSatVelocityGEIList.get(Orbit_i);
                SatPosition_ECEF=OrbitSatPositionECEFList.get(Orbit_i);
                //LLAToECEF(SatPosition_LLA,SatPosition_ECEF);
                Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                if (Side_Flag == 1) {
                    Visibility_Flag = VisibilityStationMissionJudge(subStationMissionStarTime,subStationMissionStopTime,subStationPosition,subStationPitch,  NowTime,SatPosition_ECEF);
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                } else {
                    Visibility_Flag = 0;
                    Flag_tBefore = Flag_t;
                    Flag_t = Visibility_Flag;
                }

                if (Flag_tBefore == 0 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[0] = Orbit_i;
                } else if (Flag_tBefore == 1 && Flag_t == 0) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i-1;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }
                if (Orbit_i == OrbitalDataNum - 1 && Flag_t == 1) {
                    StationVisibilityTimePeriod_iiList[1] = Orbit_i;
                    int[] StationVisibilityTimePeriod_iiListMid=new int[2];
                    StationVisibilityTimePeriod_iiListMid[0]=StationVisibilityTimePeriod_iiList[0];
                    StationVisibilityTimePeriod_iiListMid[1]=StationVisibilityTimePeriod_iiList[1];
                    StationVisibilityTimePeriod_iList.add(StationVisibilityTimePeriod_iiListMid);
                    PeriodNum = PeriodNum + 1;
                }

            }
            StationVisibilityTimePeriodList.add(StationMission_i,StationVisibilityTimePeriod_iList);
            StationVisibilityTimePeriodNumList.add(StationMission_i,PeriodNum);
        }

        //恒星定标可见性计算
        Map<String, ArrayList<Date[]>> Starmap=new HashMap<>();
        Map<String, ArrayList<Date[]>> Limbmap=new HashMap<>();
        if (MissionStarDocument.size() > 0) {
            Starmap= EmerStarVisibilityCalculationNewII( MissionStarDocument,  OrbitTimeDateList,  OrbitTimeList,  OrbitSatPositionGEIList);
        }
        //临边观测可见性计算
        if (MissionLimbDocument.size() > 0) {
            //Limbmap=EmerStarVisibilityCalculationNewII( MissionLimbDocument,  OrbitTimeDateList,  OrbitTimeList,  OrbitSatPositionGEIList);
            Limbmap=EmerLimbVisibilityCalculationII( MissionLimbDocument, OrbitTimeDateList, OrbitTimeList, OrbitSatPositionGEIList, OrbitSatVelocityGEIList, OrbitSatPositionLLAList);
        }

        //返回应急任务可见性结果
        //成像任务
        map.clear();
        Map<String, ArrayList<Date[]>> map2 = new HashMap<>();
        for (int Mission_i = 0; Mission_i < MissionNumber; Mission_i++) {
            int TimePeriodNumSum = 0;
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                TimePeriodNumSum = TimePeriodNumSum + TimePeriodNumList.get(Mission_i).get(Load_i);
            }
            ArrayList<Date[]> dateTemp=new ArrayList<>();
            if (TimePeriodNumSum >= 1) {
                for (int i = 0; i < VisibilityTimePeriodList.get(Mission_i).size(); i++) {
                    for (int j = 0; j < VisibilityTimePeriodList.get(Mission_i).get(i).size(); j++) {
                        Date[] temp=new Date[]{OrbitTimeDateList.get(VisibilityTimePeriodList.get(Mission_i).get(i).get(j)[0]),
                                OrbitTimeDateList.get(VisibilityTimePeriodList.get(Mission_i).get(i).get(j)[1])};
                        dateTemp.add(temp);
                    }
                }

            } else {
                //map2.put(MissionSerialNumberList.get(Mission_i), false);
            }
            map2.put(MissionSerialNumberList.get(Mission_i), dateTemp);
        }
        //恒星和临边任务
        if (!Starmap.isEmpty()) {
            for (Map.Entry<String,ArrayList<Date[]>> entry:Starmap.entrySet()) {
                map2.put(entry.getKey(),entry.getValue());
            }
        }
        if (!Limbmap.isEmpty()) {
            for (Map.Entry<String,ArrayList<Date[]>> entry:Limbmap.entrySet()) {
                map2.put(entry.getKey(),entry.getValue());
            }
        }
        map.put(0,map2);
        //传输任务
        Map<String, ArrayList<Date[]>> map3 = new HashMap<>();
        for (int StationMission_i = 0; StationMission_i < StationMissionNum; StationMission_i++) {
            ArrayList<Date[]> dateTemp=new ArrayList<>();
            if (StationVisibilityTimePeriodNumList.get(StationMission_i) >= 1) {
                for (int i = 0; i < StationVisibilityTimePeriodList.get(StationMission_i).size(); i++) {
                    Date[] temp=new Date[]{OrbitTimeDateList.get(StationVisibilityTimePeriodList.get(StationMission_i).get(i)[0]),
                            OrbitTimeDateList.get(StationVisibilityTimePeriodList.get(StationMission_i).get(i)[1])};
                    dateTemp.add(temp);
                }

            }else{
                //map3.put(StationMissionSerialNumberList.get(StationMission_i),false);
            }
            map3.put(StationMissionSerialNumberList.get(StationMission_i),dateTemp);
        }
        map.put(1,map3);

        return map;
    }

    //非实传任务可见型判定
    private static int VisibilityJudgeNormal(double[] subMissionStarTime, double[] subMissionStopTime, ArrayList<double[]> subMissionTargetArea, double[] subMissionTargetHeight, int k, double[] NowTime, double[] SatPosition_LLA, double[] SatPosition_GEI, double[] SatVelocity_GEI,double[] SatPosition_ECEF) {
        int Visibility_Flag = 0;

        double NowTime_JD = JD(NowTime);
        double MissonStarTime_JD = JD(subMissionStarTime);
        double MissionStopTime_JD = JD(subMissionStopTime);

        if (NowTime_JD < MissonStarTime_JD || NowTime_JD > MissionStopTime_JD) {
            Visibility_Flag = 0;
        } else {
            ArrayList<Integer> Area_Flag = new ArrayList<Integer>();
            for (int t = 0; t < subMissionTargetArea.size(); t++) {
                double[] Target_LLA = new double[3];
                double[] Target_ECEF = new double[3];
                double[] Target_GEI = new double[3];
                double[] SatPositionRe_LLA = new double[3];
                //double[] SatPosition_ECEF = new double[3];

                Target_LLA[0] = subMissionTargetArea.get(t)[0];
                Target_LLA[1] = subMissionTargetArea.get(t)[1];
                Target_LLA[2] = 0;
                LLAToECEF(Target_LLA, Target_ECEF);
                ECEFToICRS(NowTime_JD, Target_ECEF, Target_GEI);
                SatPositionRe_LLA[0] = SatPosition_LLA[0];
                SatPositionRe_LLA[1] = SatPosition_LLA[1];
                SatPositionRe_LLA[2] = SatPosition_LLA[2];
                //LLAToECEF(SatPositionRe_LLA, SatPosition_ECEF);
                int Side_Flag = SideJudge(Target_ECEF, SatPosition_ECEF);
                int High_Flag = HighJudge(SatPosition_LLA, subMissionTargetHeight);
                int AreaFlag_i = 0;
                if (Side_Flag == 1 && High_Flag == 1) {
                    AreaFlag_i = VisibilityJudge(Target_GEI, SatPosition_GEI, SatVelocity_GEI, LoadInstall[k], LoadViewAng[k], SatelliteManeuverEuler);
                    Area_Flag.add(t, AreaFlag_i);
                } else {
                    AreaFlag_i = 0;
                    Area_Flag.add(t, AreaFlag_i);
                }
            }
            int AreaFlagSum = 0;
            for (int l = 0; l < subMissionTargetArea.size(); l++) {
                AreaFlagSum = AreaFlagSum + Area_Flag.get(l);
            }
            if (AreaFlagSum == subMissionTargetArea.size()) {
                Visibility_Flag = 1;
            } else {
                Visibility_Flag = 0;
            }
        }
        return Visibility_Flag;
    }

    //传输任务可见性判定
    private static int VisibilityStationMissionJudge(double[] subsubStationMissionStarTime,double[] subsubStationMissionStopTime,double[] subStationPosition,double subStationPitch, double[] NowTime,double SatPosition_ECEF[]){
        int VisibilityStationMission_Flag=0;

        double NowTime_JD = JD(NowTime);
        double MissonStarTime_JD = JD(subsubStationMissionStarTime);
        double MissionStopTime_JD = JD(subsubStationMissionStopTime);
        if (NowTime_JD < MissonStarTime_JD || NowTime_JD > MissionStopTime_JD) {
            VisibilityStationMission_Flag = 0;
        }else {
            double[] Target_LLA=new double[3];
            Target_LLA[0]=subStationPosition[0];
            Target_LLA[1]=subStationPosition[1];
            Target_LLA[2]=subStationPosition[2];
            double[] Target_ECEF=new double[3];
            LLAToECEF(Target_LLA,Target_ECEF);

            VisibilityStationMission_Flag=StationVisibilityJudge(Target_ECEF, SatPosition_ECEF, subStationPitch);
        }

        return VisibilityStationMission_Flag;
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
                Target_LLA[2] = 0;
                LLAToECEF(Target_LLA, Target_ECEF);
                ECEFToICRS(NowTime_JD, Target_ECEF, Target_GEI);
                SatPositionRe_LLA[0] = SatPosition_LLA[0];
                SatPositionRe_LLA[1] = SatPosition_LLA[1];
                SatPositionRe_LLA[2] = SatPosition_LLA[2];
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

    //恒星观测任务可见型判定
    private static int VisibilityJudgeNormalHXGC(double[] subMissionStarTime, double[] subMissionStopTime, double[] subMissionTargetArea, double[] NowTime, double[] SatPosition_GEI) {
        int Visibility_Flag = 0;

        double NowTime_JD = JD(NowTime);
        double MissonStarTime_JD = JD(subMissionStarTime);
        double MissionStopTime_JD = JD(subMissionStopTime);

        if (NowTime_JD < MissonStarTime_JD || NowTime_JD > MissionStopTime_JD) {
            Visibility_Flag = 0;
        } else {
            double[] error=new double[]{subMissionTargetArea[0]-SatPosition_GEI[0],
                    subMissionTargetArea[1]-SatPosition_GEI[1],
                    subMissionTargetArea[2]-SatPosition_GEI[2]};
            double r_sat=sqrt(SatPosition_GEI[0]*SatPosition_GEI[0]+SatPosition_GEI[1]*SatPosition_GEI[1]+SatPosition_GEI[2]*SatPosition_GEI[2]);
            double[] r_sats=new double[]{-SatPosition_GEI[0]/r_sat,-SatPosition_GEI[1]/r_sat,-SatPosition_GEI[2]/r_sat};
            double theat=acos((error[0]*r_sats[0]+error[1]*r_sats[1]+error[2]*r_sats[2])/(sqrt(error[0]*error[0]+error[1]*error[1]+error[2]*error[2])*sqrt(r_sats[0]*r_sats[0]+r_sats[1]*r_sats[1]+r_sats[2]*r_sats[2])));
            double theateath=asin(Re/r_sat);
            if (theat > theateath) {
                Visibility_Flag=1;
            }else {
                Visibility_Flag=0;
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
        double[] ViewTheta_xz = {OpticalTheta_xz - ViewAngle[2] - Euler_Max[1], OpticalTheta_xz + ViewAngle[2] + Euler_Max[1]};
        double[] ViewTheta_yz = {OpticalTheta_yz - ViewAngle[0] - Euler_Max[0], OpticalTheta_yz + ViewAngle[0] + Euler_Max[0]};
        //double[] ViewTheta_xz = {OpticalTheta_xz - ViewAng_Min - Euler_Max[1], OpticalTheta_xz + ViewAng_Min + Euler_Max[1]};
        //double[] ViewTheta_yz = {OpticalTheta_yz - ViewAng_Min - Euler_Max[0], OpticalTheta_yz + ViewAng_Min + Euler_Max[0]};
        //double[] ViewTheta_xz = {OpticalTheta_xz-   Euler_Max[1], OpticalTheta_xz +  Euler_Max[1]};
        //double[] ViewTheta_yz = {OpticalTheta_yz-   Euler_Max[0], OpticalTheta_yz +  Euler_Max[0]};

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
        //System.out.println(Math.acos(a / (r_error * r_Target))*180/PI);
        if (a / (r_error * r_Target) >= cos(PI/2-StationPitch))
            return 1;
        else
            return 0;
    }

    //恒星任务可见性计算
    private static void StarVisibilityCalculation(ArrayList<Document> MissionStarDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionLLAList){
        ArrayList<Double> MissionStarHeigh=new ArrayList<>();
        ArrayList<double[]> MissionStarStartTime=new ArrayList<>();
        ArrayList<double[]> MissionStarStopTime=new ArrayList<>();
        ArrayList<Double> MissionStarAng=new ArrayList<>();
        ArrayList<String> MissionStarSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionStarForOrderNumbers=new ArrayList<>();
        int MissionNum=0;

        for (Document document:MissionStarDocument) {
            try {
                //读取期望轨道高度
                double MissionStarHeighChild= Double.parseDouble(document.get("scan_height_orbit").toString());
                //读取任务期望时间
                Date expected_start_time = document.getDate("expected_start_time");
                double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                Date expected_stop_time=document.getDate("expected_end_time");
                double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                //读取任务偏角
                double MissionStarAngChild= Double.parseDouble(document.get("scan_roll_bias").toString());
                //读取任务编号
                String MissionSerialNumber_iList = document.get("mission_number").toString();
                //读取订单编号
                ArrayList<String> MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");


                MissionStarHeigh.add(MissionNum,MissionStarHeighChild);
                MissionStarStartTime.add(MissionNum,MissionStarTime_iList);
                MissionStarStopTime.add( MissionNum,MissionStopTime_iList);
                MissionStarAng.add(MissionNum,MissionStarAngChild);
                MissionStarSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                MissionStarForOrderNumbers.add(MissionNum,MissionForOrderNumbers_i);
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //可执行弧段
        ArrayList<ArrayList<int[]>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<Integer> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            double StartTime_JD=JD(MissionStarStartTime.get(i));
            double StopTime_JD=JD(MissionStarStopTime.get(i));
            boolean PeriodFlag_last=false;
            boolean PeriodFlag_Now=false;
            int[] VisibilityTimePeriodChild=new int[2];
            ArrayList<int[]> VisibilityTimePeriodListChild=new ArrayList<>();
            Integer TimePeriodNumListChild=0;
            for (int j = 0; j < OrbitSatPositionLLAList.size(); j++) {
                double NowTime_JD=JD(OrbitTimeList.get(j));
                if (NowTime_JD >= StartTime_JD && NowTime_JD<=StopTime_JD) {
                    if (OrbitSatPositionLLAList.get(j)[2] >= MissionStarHeigh.get(i)-MissionStarHeighLimt && OrbitSatPositionLLAList.get(j)[2] <= MissionStarHeigh.get(i)+MissionStarHeighLimt) {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=true;
                    }else {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                }else {
                    PeriodFlag_last=PeriodFlag_Now;
                    PeriodFlag_Now=false;
                }

                if (PeriodFlag_last == false && PeriodFlag_Now==true) {
                    VisibilityTimePeriodChild[0]=j;
                }else if (PeriodFlag_last == true && PeriodFlag_Now==false) {
                    VisibilityTimePeriodChild[1]=j-1;

                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                    TimePeriodNumListChild++;
                }else if (PeriodFlag_last == true && PeriodFlag_Now==true && j==OrbitSatPositionLLAList.size()-1){
                    VisibilityTimePeriodChild[1]=j;

                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                    TimePeriodNumListChild++;
                }
            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //数据传出
        for (int i = 0; i < MissionNum; i++) {
            if (TimePeriodNumList.get(i) > 0) {
                ArrayList<Document> AvailWindowjsonArray = new ArrayList<>();
                for (int k = 0; k < TimePeriodNumList.get(i); k++) {
                    Document AvailWindowjsonObject = new Document();
                    AvailWindowjsonObject.append("amount_window", TimePeriodNumList.get(i));
                    AvailWindowjsonObject.append("window_number", k + 1);
                    AvailWindowjsonObject.append("window_start_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(k)[0]));
                    AvailWindowjsonObject.append("window_end_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(k)[1]));
                    AvailWindowjsonArray.add(AvailWindowjsonObject);
                }
                MissionStarDocument.get(i).append("available_window", AvailWindowjsonArray);

                MissionStarDocument.get(i).append("fail_reason", "");
                MissionStarDocument.get(i).append("mission_state", "待规划");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionStarForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待规划");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }else {
                MissionStarDocument.get(i).append("fail_reason", "不可见");
                MissionStarDocument.get(i).append("mission_state", "被退回");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionStarForOrderNumbers.get(i);
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

            if (MissionStarDocument.get(i).containsKey("_id"))
                MissionStarDocument.get(i).remove("_id");
            Document modifiers = new Document();
            modifiers.append("$set", MissionStarDocument.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            System.out.println(MissionStarDocument.get(i).toString());
            image_mission.updateOne(new Document("mission_number", MissionStarDocument.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }

        mongoClient.close();
    }

    //恒星任务可见性计算-2020-11-8
    private static void StarVisibilityCalculationNew(ArrayList<Document> MissionStarDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList){
        ArrayList<double[]> MissionStarStartTime=new ArrayList<>();
        ArrayList<double[]> MissionStarStopTime=new ArrayList<>();
        ArrayList<String> MissionStarSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionStarForOrderNumbers=new ArrayList<>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionTargetAreaList=new ArrayList<>();
        int MissionNum=0;

        try {
            for (Document document : MissionStarDocument) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    double[] MissionTargetArea_iList = new double[3];
                    MissionTargetArea_iList[0]=Double.parseDouble(document.get("j2000x").toString());
                    MissionTargetArea_iList[1]=Double.parseDouble(document.get("j2000y").toString());
                    MissionTargetArea_iList[2]=Double.parseDouble(document.get("j2000z").toString());
                    MissionTargetAreaList.add(MissionNum, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNum, MissionLoadType_iList);
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                    //读取任务编号
                    String MissionSerialNumber_iList = document.get("mission_number").toString();
                    //读取订单编号
                    ArrayList<String> MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");

                    MissionStarStartTime.add(MissionNum,MissionStarTime_iList);
                    MissionStarStopTime.add( MissionNum,MissionStopTime_iList);
                    MissionStarSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                    MissionStarForOrderNumbers.add(MissionNum,MissionForOrderNumbers_i);

                    //任务数量加1
                    MissionNum = MissionNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可执行弧段
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            ArrayList<ArrayList<int[]>> VisibilityTimePeriodListChild = new ArrayList<>();
            ArrayList<Integer> TimePeriodNumListChild = new ArrayList<>();
            //载荷循环
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                int TimePeriodNum_iiList = 0;
                //判断载荷是否使用
                if (MissionLoadTypeList.get(i)[Load_i] == 1) {
                    //任务期望时间
                    double[] subMissionStarTime = new double[6];
                    double[] subMissionStopTime = new double[6];
                    double[] subMissionTarget = MissionTargetAreaList.get(i);
                    subMissionStarTime = MissionStarStartTime.get(i);
                    subMissionStopTime = MissionStarStopTime.get(i);

                    int Flag_tBefore = 0;
                    int Flag_t = 0;
                    int Visibility_Flag = 0;
                    //轨道数据循环
                    int OrbitalStepPlus = 10;
                    int[] VisibilityTimeperiod_iiiList=new int[2];
                    for (int Orbit_i = 0; Orbit_i < OrbitTimeDateList.size(); ) {
                        double[] NowTime = new double[6];
                        double[] SatPosition_GEI = new double[3];
                        NowTime = OrbitTimeList.get(Orbit_i);
                        SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);

                        Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);

                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                        //判定开始结束时间，精确判断
                        if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[0] = Orbit_i;
                        } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[0]=l;
                                    break;
                                }
                            }
                        } else if (Flag_tBefore == 1 && Flag_t == 0) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 0) {
                                    VisibilityTimeperiod_iiiList[1]=l-1;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                    break;
                                }
                            }
                        } else if (Orbit_i == OrbitTimeDateList.size() - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }else if (Orbit_i > OrbitTimeDateList.size()- OrbitalStepPlus - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            for (int l = OrbitTimeDateList.size()-1; l > Orbit_i; l--) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[1]=l;
                                    break;
                                }
                            }
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }

                        Orbit_i = Orbit_i + OrbitalStepPlus;
                    }
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                } else {
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                }
            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //数据传出
        for (int i = 0; i < MissionNum; i++) {
            int WindowsNum = 0;
            for (int j = 0; j < LoadNumber; j++) {
                for (int k = 0; k < TimePeriodNumList.get(i).get(j); k++) {
                    WindowsNum = WindowsNum + 1;
                }
            }
            if (WindowsNum > 0) {
                ArrayList<Document> AvailWindowjsonArray = new ArrayList<>();
                for (int j = 0; j < LoadNumber; j++) {
                    for (int k = 0; k < TimePeriodNumList.get(i).get(j); k++) {
                        Document AvailWindowjsonObject = new Document();
                        AvailWindowjsonObject.append("amount_window", TimePeriodNumList.get(i).get(j));
                        AvailWindowjsonObject.append("load_number",j+1);
                        AvailWindowjsonObject.append("window_number", k + 1);
                        AvailWindowjsonObject.append("window_start_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[0]));
                        AvailWindowjsonObject.append("window_end_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[1]));
                        AvailWindowjsonArray.add(AvailWindowjsonObject);
                    }
                }
                MissionStarDocument.get(i).append("available_window", AvailWindowjsonArray);

                MissionStarDocument.get(i).append("fail_reason", "");
                MissionStarDocument.get(i).append("mission_state", "待规划");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionStarForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待规划");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }else {
                MissionStarDocument.get(i).append("fail_reason", "不可见");
                MissionStarDocument.get(i).append("mission_state", "被退回");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionStarForOrderNumbers.get(i);
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

            if (MissionStarDocument.get(i).containsKey("_id"))
                MissionStarDocument.get(i).remove("_id");
            Document modifiers = new Document();
            modifiers.append("$set", MissionStarDocument.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            System.out.println(MissionStarDocument.get(i).toString());
            image_mission.updateOne(new Document("mission_number", MissionStarDocument.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }

        mongoClient.close();
    }

    //临边观测可见性计算
    private static void LimbVisibilityCalculation(ArrayList<Document> MissionLimbDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList, ArrayList<double[]> OrbitSatVelocityGEIList, ArrayList<double[]> OrbitSatPositionLLAList){
        ArrayList<Double> MissionLimbHeigh=new ArrayList<>();
        ArrayList<double[]> MissionLimbStartTime=new ArrayList<>();
        ArrayList<double[]> MissionLimbStopTime=new ArrayList<>();
        ArrayList<String> MissionLimbSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionLimbForOrderNumbers=new ArrayList<>();
        int MissionNum=0;

        for (Document document:MissionLimbDocument) {
            try {
                //读取期望轨道高度
                double MissionLimbHeighChild= Double.parseDouble(document.get("scan_height_orbit").toString());
                //读取任务期望时间
                Date expected_start_time = document.getDate("expected_start_time");
                double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                Date expected_stop_time=document.getDate("expected_end_time");
                double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                //读取任务编号
                String MissionSerialNumber_iList = document.get("mission_number").toString();
                //读取订单编号
                ArrayList<String> MissionForOrderNumbers_i= (ArrayList<String>) document.get("order_numbers");


                MissionLimbHeigh.add(MissionNum,MissionLimbHeighChild);
                MissionLimbStartTime.add(MissionNum,MissionStarTime_iList);
                MissionLimbStopTime.add( MissionNum,MissionStopTime_iList);
                MissionLimbSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                MissionLimbForOrderNumbers.add(MissionNum,MissionForOrderNumbers_i);
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //可执行弧段
        ArrayList<ArrayList<int[]>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<Integer> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            double StartTime_JD=JD(MissionLimbStartTime.get(i));
            double StopTime_JD=JD(MissionLimbStopTime.get(i));
            boolean PeriodFlag_last=false;
            boolean PeriodFlag_Now=false;
            boolean SatelliteAngFlag=false;
            double SatelliteAng=0;
            int[] VisibilityTimePeriodChild=new int[2];
            ArrayList<int[]> VisibilityTimePeriodListChild=new ArrayList<>();
            Integer TimePeriodNumListChild=0;
            for (int j = 0; j < OrbitSatPositionLLAList.size(); j++) {
                double NowTime_JD=JD(OrbitTimeList.get(j));
                if (NowTime_JD >= StartTime_JD && NowTime_JD<=StopTime_JD) {
                    if (PeriodFlag_last == false && PeriodFlag_Now==true) {
                        //太阳矢量
                        double[] r_sun=new double[3];//惯性系下太阳矢量
                        double[] su=new double[2];
                        double rd_sun=Sun(NowTime_JD, r_sun,su);
                        //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                        boolean FlyOrientationFlag=true;
                        double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                        double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(j)[0]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[1]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[2]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2])};
                        double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                                (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                        if (CosTheta_SunVel > 0) {
                            FlyOrientationFlag=false;
                        }else {
                            FlyOrientationFlag=true;
                        }

                        if (FlyOrientationFlag==true && SatelliteAngFlag==false) {
                            //高度、光照满足，计算转角
                            SatelliteAng=LimbAngCalculation(OrbitSatPositionGEIList.get(j));
                            VisibilityTimePeriodChild[0]=j;
                            SatelliteAngFlag=true;
                            continue;
                        }else if (SatelliteAngFlag==true) {
                            if (FlyOrientationFlag) {
                                if (j == 1403) {
                                    System.out.println(j);
                                }
                                boolean LimbVisibility_Flag= LimbVisibilityFlag(OrbitSatPositionGEIList.get(j),SatelliteAng);
                                if (LimbVisibility_Flag) {
                                    if (j == OrbitSatPositionLLAList.size()-1) {
                                        VisibilityTimePeriodChild[1]=j;
                                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                        TimePeriodNumListChild++;
                                        SatelliteAngFlag=false;
                                        PeriodFlag_last=PeriodFlag_Now;
                                        PeriodFlag_Now=false;
                                    }
                                    continue;
                                }else {
                                    VisibilityTimePeriodChild[1]=j-1;
                                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                    TimePeriodNumListChild++;
                                    SatelliteAngFlag=false;
                                    PeriodFlag_last=PeriodFlag_Now;
                                    PeriodFlag_Now=false;
                                    continue;
                                }
                            }else {
                                VisibilityTimePeriodChild[1]=j-1;
                                int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                TimePeriodNumListChild++;
                                SatelliteAngFlag=false;
                                PeriodFlag_last=PeriodFlag_Now;
                                PeriodFlag_Now=false;
                                continue;
                            }
                        }else {
                            PeriodFlag_last=PeriodFlag_Now;
                            PeriodFlag_Now=false;
                            continue;
                        }
                    }

                    if (OrbitSatPositionLLAList.get(j)[2] >= MissionLimbHeigh.get(i)-MissionLimbHeighLimt && OrbitSatPositionLLAList.get(j)[2] <= MissionLimbHeigh.get(i)+MissionLimbHeighLimt) {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=true;
                    }else {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                }else {
                    if (SatelliteAngFlag) {
                        VisibilityTimePeriodChild[1]=j-1;
                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                        TimePeriodNumListChild++;
                        SatelliteAngFlag=false;
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                    PeriodFlag_last=PeriodFlag_Now;
                    PeriodFlag_Now=false;
                }

            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        //MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //数据传出
        for (int i = 0; i < MissionNum; i++) {
            if (TimePeriodNumList.get(i) > 0) {
                ArrayList<Document> AvailWindowjsonArray = new ArrayList<>();
                for (int k = 0; k < TimePeriodNumList.get(i); k++) {
                    Document AvailWindowjsonObject = new Document();
                    AvailWindowjsonObject.append("amount_window", TimePeriodNumList.get(i));
                    AvailWindowjsonObject.append("window_number", k + 1);
                    AvailWindowjsonObject.append("window_start_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(k)[0]));
                    AvailWindowjsonObject.append("window_end_time", OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(k)[1]));
                    AvailWindowjsonArray.add(AvailWindowjsonObject);
                }
                MissionLimbDocument.get(i).append("available_window", AvailWindowjsonArray);
                MissionLimbDocument.get(i).append("fail_reason", "");
                MissionLimbDocument.get(i).append("mission_state", "待规划");

                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionLimbForOrderNumbers.get(i);
                for (String OrderNumber:MissionForOrderNumbers_i) {
                    MongoCollection<Document> Data_ImageOrderjson=mongoDatabase.getCollection("image_order");
                    FindIterable<Document> D_ImageOrderjson=Data_ImageOrderjson.find();
                    ArrayList<Document> ImageOrderjson =new ArrayList<>();
                    for (Document document:D_ImageOrderjson) {
                        if (document.get("order_number").equals(OrderNumber)) {
                            document.append("order_state","待规划");
                            if(document.containsKey("_id"))
                                document.remove("_id");
                            Document modifiers_mid=new Document();
                            modifiers_mid.append("$set",document);
                            Data_ImageOrderjson.updateOne(new Document("order_number",OrderNumber),modifiers_mid,new UpdateOptions().upsert(true));
                        }
                    }
                }
            }else {
                MissionLimbDocument.get(i).append("fail_reason", "不可见");
                MissionLimbDocument.get(i).append("mission_state", "被退回");
                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=MissionLimbForOrderNumbers.get(i);
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

            if (MissionLimbDocument.get(i).containsKey("_id"))
                MissionLimbDocument.get(i).remove("_id");
            Document modifiers = new Document();
            modifiers.append("$set", MissionLimbDocument.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            System.out.println(MissionLimbDocument.get(i).toString());
            image_mission.updateOne(new Document("mission_number", MissionLimbDocument.get(i).getString("mission_number")), modifiers, new UpdateOptions().upsert(true));
        }

        mongoClient.close();
    }

    //应急恒星任务可见性计算
    private static Map<String, Boolean> EmerStarVisibilityCalculation(ArrayList<Document> MissionStarDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionLLAList){
        ArrayList<Double> MissionStarHeigh=new ArrayList<>();
        ArrayList<double[]> MissionStarStartTime=new ArrayList<>();
        ArrayList<double[]> MissionStarStopTime=new ArrayList<>();
        ArrayList<Double> MissionStarAng=new ArrayList<>();
        ArrayList<String> MissionStarSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionStarForOrderNumbers=new ArrayList<>();
        int MissionNum=0;

        for (Document document:MissionStarDocument) {
            try {
                //读取期望轨道高度
                double MissionStarHeighChild= Double.parseDouble(document.get("scan_height_orbit").toString());
                //读取任务期望时间
                Date expected_start_time = document.getDate("expected_start_time");
                double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                Date expected_stop_time=document.getDate("expected_end_time");
                double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                //读取任务偏角
                double MissionStarAngChild= Double.parseDouble(document.get("scan_roll_bias").toString());
                //读取任务编号
                String MissionSerialNumber_iList = document.get("order_number").toString();

                MissionStarHeigh.add(MissionNum,MissionStarHeighChild);
                MissionStarStartTime.add(MissionNum,MissionStarTime_iList);
                MissionStarStopTime.add( MissionNum,MissionStopTime_iList);
                MissionStarAng.add(MissionNum,MissionStarAngChild);
                MissionStarSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //可执行弧段
        ArrayList<ArrayList<int[]>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<Integer> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            double StartTime_JD=JD(MissionStarStartTime.get(i));
            double StopTime_JD=JD(MissionStarStopTime.get(i));
            boolean PeriodFlag_last=false;
            boolean PeriodFlag_Now=false;
            int[] VisibilityTimePeriodChild=new int[2];
            ArrayList<int[]> VisibilityTimePeriodListChild=new ArrayList<>();
            Integer TimePeriodNumListChild=0;
            for (int j = 0; j < OrbitSatPositionLLAList.size(); j++) {
                double NowTime_JD=JD(OrbitTimeList.get(j));
                if (NowTime_JD >= StartTime_JD && NowTime_JD<=StopTime_JD) {
                    if (OrbitSatPositionLLAList.get(j)[2] >= MissionStarHeigh.get(i)-MissionStarHeighLimt && OrbitSatPositionLLAList.get(j)[2] <= MissionStarHeigh.get(i)+MissionStarHeighLimt) {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=true;
                    }else {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                }else {
                    PeriodFlag_last=PeriodFlag_Now;
                    PeriodFlag_Now=false;
                }

                if (PeriodFlag_last == false && PeriodFlag_Now==true) {
                    VisibilityTimePeriodChild[0]=j;
                }else if (PeriodFlag_last == true && PeriodFlag_Now==false) {
                    VisibilityTimePeriodChild[1]=j-1;

                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                    TimePeriodNumListChild++;
                }else if (PeriodFlag_last == true && PeriodFlag_Now==true && j==OrbitSatPositionLLAList.size()-1){
                    VisibilityTimePeriodChild[1]=j;

                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                    TimePeriodNumListChild++;
                }
            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        Map<String, Boolean> starMissionMap=new TreeMap<>();
        for (int i = 0; i < MissionNum; i++) {
            if (VisibilityTimePeriodList.get(i).size() > 0) {
                starMissionMap.put(MissionStarSerialNumber.get(i),true);
            }else {
                starMissionMap.put(MissionStarSerialNumber.get(i),false);
            }
        }
        return starMissionMap;
    }

    //应急恒星定标任务可见性计算-2020-11-3
    private static Map<String, Boolean> EmerStarVisibilityCalculationNew(ArrayList<Document> MissionStarDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList){
        ArrayList<double[]> MissionStarStartTime=new ArrayList<>();
        ArrayList<double[]> MissionStarStopTime=new ArrayList<>();
        ArrayList<String> MissionStarForOrderNumbers=new ArrayList<>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionTargetAreaList=new ArrayList<>();
        int MissionNum=0;

        try {
            for (Document document : MissionStarDocument) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    double[] MissionTargetArea_iList = new double[3];
                    MissionTargetArea_iList[0]=Double.parseDouble(document.get("j2000x").toString());
                    MissionTargetArea_iList[1]=Double.parseDouble(document.get("j2000y").toString());
                    MissionTargetArea_iList[2]=Double.parseDouble(document.get("j2000z").toString());
                    MissionTargetAreaList.add(MissionNum, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNum, MissionLoadType_iList);
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                    //读取订单编号
                    String MissionForOrderNumbers_i= document.get("order_number").toString();

                    MissionStarStartTime.add(MissionNum,MissionStarTime_iList);
                    MissionStarStopTime.add( MissionNum,MissionStopTime_iList);
                    MissionStarForOrderNumbers.add(MissionNum,MissionForOrderNumbers_i);

                    //任务数量加1
                    MissionNum = MissionNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可执行弧段
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            //载荷循环
            ArrayList<ArrayList<int[]>> VisibilityTimePeriodListChild = new ArrayList<>();
            ArrayList<Integer> TimePeriodNumListChild = new ArrayList<>();
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                int TimePeriodNum_iiList = 0;
                //判断载荷是否使用
                if (MissionLoadTypeList.get(i)[Load_i] == 1) {
                    //任务期望时间
                    double[] subMissionStarTime = new double[6];
                    double[] subMissionStopTime = new double[6];
                    double[] subMissionTarget = MissionTargetAreaList.get(i);
                    subMissionStarTime = MissionStarStartTime.get(i);
                    subMissionStopTime = MissionStarStopTime.get(i);

                    int Flag_tBefore = 0;
                    int Flag_t = 0;
                    int Visibility_Flag = 0;
                    //轨道数据循环
                    int OrbitalStepPlus = 10;
                    int[] VisibilityTimeperiod_iiiList=new int[2];
                    for (int Orbit_i = 0; Orbit_i < OrbitTimeDateList.size(); ) {
                        double[] NowTime = new double[6];
                        double[] SatPosition_GEI = new double[3];
                        NowTime = OrbitTimeList.get(Orbit_i);
                        SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);

                        Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);

                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                        //判定开始结束时间，精确判断
                        if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[0] = Orbit_i;
                        } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[0]=l;
                                    break;
                                }
                            }
                        } else if (Flag_tBefore == 1 && Flag_t == 0) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 0) {
                                    VisibilityTimeperiod_iiiList[1]=l-1;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                    break;
                                }
                            }
                        } else if (Orbit_i == OrbitTimeDateList.size() - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }else if (Orbit_i > OrbitTimeDateList.size()- OrbitalStepPlus - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            for (int l = OrbitTimeDateList.size()-1; l > Orbit_i; l--) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[1]=l;
                                    break;
                                }
                            }
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }

                        Orbit_i = Orbit_i + OrbitalStepPlus;
                    }
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                } else {
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                }
            }

            VisibilityTimePeriodList.add(i, VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i, TimePeriodNumListChild);
        }

        Map<String, Boolean> starMissionMap=new TreeMap<>();
        for (int i = 0; i < MissionNum; i++) {
            int TimePeriodNumSum = 0;
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                TimePeriodNumSum = TimePeriodNumSum + TimePeriodNumList.get(i).get(Load_i);
            }
            if (TimePeriodNumSum >= 1) {
                starMissionMap.put(MissionStarForOrderNumbers.get(i),true);
            } else {
                starMissionMap.put(MissionStarForOrderNumbers.get(i),false);
            }
        }
        return starMissionMap;
    }

    //应急恒星定标任务可见性计算-2020-11-3
    private static Map<String, ArrayList<Date[]>> EmerStarVisibilityCalculationNewII(ArrayList<Document> MissionStarDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList){
        ArrayList<double[]> MissionStarStartTime=new ArrayList<>();
        ArrayList<double[]> MissionStarStopTime=new ArrayList<>();
        ArrayList<String> MissionStarForOrderNumbers=new ArrayList<>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<int[]>();
        ArrayList<double[]> MissionTargetAreaList=new ArrayList<>();
        int MissionNum=0;

        try {
            for (Document document : MissionStarDocument) {
                try {
                    Document target_region = (Document) document.get("image_region");
                    //读取目标区域
                    double[] MissionTargetArea_iList = new double[3];
                    MissionTargetArea_iList[0]=Double.parseDouble(document.get("j2000x").toString());
                    MissionTargetArea_iList[1]=Double.parseDouble(document.get("j2000y").toString());
                    MissionTargetArea_iList[2]=Double.parseDouble(document.get("j2000z").toString());
                    MissionTargetAreaList.add(MissionNum, MissionTargetArea_iList);
                    //读取指定载荷
                    ArrayList<Document> expected_cam = (ArrayList<Document>) document.get("expected_cam");
                    int[] MissionLoadType_iList = new int[5];
                    if (expected_cam.size() == 0) {
                        MissionLoadType_iList[0] = 0;
                        MissionLoadType_iList[1] = 0;
                        MissionLoadType_iList[2] = 0;
                        MissionLoadType_iList[3] = 0;
                        MissionLoadType_iList[4] = 0;
                    } else {
                        for (Document document1 : expected_cam) {
                            if (document1 != null) {
                                ArrayList<Document> sensors= (ArrayList<Document>) document1.get("sensors");
                                for (Document document2: sensors){
                                    if (document2.getString("name").equals("高分相机A")) {
                                        MissionLoadType_iList[0] = 1;
                                    } else if (document2.getString("name").equals("高分相机B")) {
                                        MissionLoadType_iList[1] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机A")) {
                                        MissionLoadType_iList[2] = 1;
                                    } else if (document2.getString("name").equals("多光谱相机B")) {
                                        MissionLoadType_iList[3] = 1;
                                    }
                                }
                                if (MissionLoadType_iList[0] == 1 && MissionLoadType_iList[1] == 1) {
                                    MissionLoadType_iList[0] = 0;
                                    MissionLoadType_iList[1] = 0;
                                    MissionLoadType_iList[4] = 1;
                                }
                            }
                        }
                    }
                    MissionLoadTypeList.add(MissionNum, MissionLoadType_iList);
                    //读取任务期望时间
                    Date expected_start_time = document.getDate("expected_start_time");
                    double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                    Date expected_stop_time=document.getDate("expected_end_time");
                    double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                    //读取订单编号
                    String MissionForOrderNumbers_i= document.get("order_number").toString();

                    MissionStarStartTime.add(MissionNum,MissionStarTime_iList);
                    MissionStarStopTime.add( MissionNum,MissionStopTime_iList);
                    MissionStarForOrderNumbers.add(MissionNum,MissionForOrderNumbers_i);

                    //任务数量加1
                    MissionNum = MissionNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //可执行弧段
        ArrayList<ArrayList<ArrayList<int[]>>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            //载荷循环
            ArrayList<ArrayList<int[]>> VisibilityTimePeriodListChild = new ArrayList<>();
            ArrayList<Integer> TimePeriodNumListChild = new ArrayList<>();
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                ArrayList<int[]> VisibilityTimeperiod_iiList = new ArrayList<int[]>();
                int TimePeriodNum_iiList = 0;
                //判断载荷是否使用
                if (MissionLoadTypeList.get(i)[Load_i] == 1) {
                    //任务期望时间
                    double[] subMissionStarTime = new double[6];
                    double[] subMissionStopTime = new double[6];
                    double[] subMissionTarget = MissionTargetAreaList.get(i);
                    subMissionStarTime = MissionStarStartTime.get(i);
                    subMissionStopTime = MissionStarStopTime.get(i);

                    int Flag_tBefore = 0;
                    int Flag_t = 0;
                    int Visibility_Flag = 0;
                    //轨道数据循环
                    int OrbitalStepPlus = 10;
                    int[] VisibilityTimeperiod_iiiList=new int[2];
                    for (int Orbit_i = 0; Orbit_i < OrbitTimeDateList.size(); ) {
                        double[] NowTime = new double[6];
                        double[] SatPosition_GEI = new double[3];
                        NowTime = OrbitTimeList.get(Orbit_i);
                        SatPosition_GEI = OrbitSatPositionGEIList.get(Orbit_i);

                        Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);

                        Flag_tBefore = Flag_t;
                        Flag_t = Visibility_Flag;
                        //判定开始结束时间，精确判断
                        if (Orbit_i == 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[0] = Orbit_i;
                        } else if (Orbit_i != 0 && Flag_tBefore == 0 && Flag_t == 1) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[0]=l;
                                    break;
                                }
                            }
                        } else if (Flag_tBefore == 1 && Flag_t == 0) {
                            for (int l = Orbit_i - OrbitalStepPlus; l <= Orbit_i; l++) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 0) {
                                    VisibilityTimeperiod_iiiList[1]=l-1;
                                    int[] VisibilityTimeperiod_iiiListMid=new int[2];
                                    VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                                    VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                                    VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                                    TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                                    break;
                                }
                            }
                        } else if (Orbit_i == OrbitTimeDateList.size() - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }else if (Orbit_i > OrbitTimeDateList.size()- OrbitalStepPlus - 1 && Flag_t == 1) {
                            VisibilityTimeperiod_iiiList[1]=Orbit_i;
                            for (int l = OrbitTimeDateList.size()-1; l > Orbit_i; l--) {
                                NowTime = OrbitTimeList.get(l);
                                SatPosition_GEI = OrbitSatPositionGEIList.get(l);
                                Visibility_Flag = VisibilityJudgeNormalHXGC(subMissionStarTime, subMissionStopTime, subMissionTarget, NowTime, SatPosition_GEI);
                                if (Visibility_Flag == 1) {
                                    VisibilityTimeperiod_iiiList[1]=l;
                                    break;
                                }
                            }
                            int[] VisibilityTimeperiod_iiiListMid=new int[2];
                            VisibilityTimeperiod_iiiListMid[0]=VisibilityTimeperiod_iiiList[0];
                            VisibilityTimeperiod_iiiListMid[1]=VisibilityTimeperiod_iiiList[1];
                            VisibilityTimeperiod_iiList.add(TimePeriodNum_iiList,VisibilityTimeperiod_iiiListMid);
                            TimePeriodNum_iiList=TimePeriodNum_iiList+1;
                        }

                        Orbit_i = Orbit_i + OrbitalStepPlus;
                    }
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                } else {
                    VisibilityTimePeriodListChild.add(Load_i, VisibilityTimeperiod_iiList);
                    TimePeriodNumListChild.add(Load_i, TimePeriodNum_iiList);
                }
            }

            VisibilityTimePeriodList.add(i, VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i, TimePeriodNumListChild);
        }

        Map<String, ArrayList<Date[]>> starMissionMap=new TreeMap<>();
        for (int i = 0; i < MissionNum; i++) {
            int TimePeriodNumSum = 0;
            for (int Load_i = 0; Load_i < LoadNumber; Load_i++) {
                TimePeriodNumSum = TimePeriodNumSum + TimePeriodNumList.get(i).get(Load_i);
            }
            ArrayList<Date[]> dateTemp=new ArrayList<>();
            if (TimePeriodNumSum >= 1) {
                for (int j = 0; j < VisibilityTimePeriodList.get(i).size(); j++) {
                    for (int k = 0; k < VisibilityTimePeriodList.get(i).get(j).size(); k++) {
                        Date[] temp=new Date[]{OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[0]),
                                OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j).get(k)[1])};
                        dateTemp.add(temp);

                    }
                }

            } else {
                //starMissionMap.put(MissionStarForOrderNumbers.get(i),false);
            }
            starMissionMap.put(MissionStarForOrderNumbers.get(i),dateTemp);
        }
        return starMissionMap;
    }


    //应急临边观测可见性计算
    private static Map<String, Boolean> EmerLimbVisibilityCalculation(ArrayList<Document> MissionLimbDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList, ArrayList<double[]> OrbitSatVelocityGEIList, ArrayList<double[]> OrbitSatPositionLLAList){
        ArrayList<Double> MissionLimbHeigh=new ArrayList<>();
        ArrayList<double[]> MissionLimbStartTime=new ArrayList<>();
        ArrayList<double[]> MissionLimbStopTime=new ArrayList<>();
        ArrayList<String> MissionLimbSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionLimbForOrderNumbers=new ArrayList<>();
        int MissionNum=0;

        for (Document document:MissionLimbDocument) {
            try {
                //读取期望轨道高度
                double MissionLimbHeighChild= Double.parseDouble(document.get("scan_height_orbit").toString());
                //读取任务期望时间
                Date expected_start_time = document.getDate("expected_start_time");
                double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                Date expected_stop_time=document.getDate("expected_end_time");
                double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                //读取任务编号
                String MissionSerialNumber_iList = document.get("order_number").toString();

                MissionLimbHeigh.add(MissionNum,MissionLimbHeighChild);
                MissionLimbStartTime.add(MissionNum,MissionStarTime_iList);
                MissionLimbStopTime.add( MissionNum,MissionStopTime_iList);
                MissionLimbSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //可执行弧段
        ArrayList<ArrayList<int[]>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<Integer> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            double StartTime_JD=JD(MissionLimbStartTime.get(i));
            double StopTime_JD=JD(MissionLimbStopTime.get(i));
            boolean PeriodFlag_last=false;
            boolean PeriodFlag_Now=false;
            boolean SatelliteAngFlag=false;
            double SatelliteAng=0;
            int[] VisibilityTimePeriodChild=new int[2];
            ArrayList<int[]> VisibilityTimePeriodListChild=new ArrayList<>();
            Integer TimePeriodNumListChild=0;
            for (int j = 0; j < OrbitSatPositionLLAList.size(); j++) {
                double NowTime_JD=JD(OrbitTimeList.get(j));
                if (NowTime_JD >= StartTime_JD && NowTime_JD<=StopTime_JD) {
                    if (PeriodFlag_last == false && PeriodFlag_Now==true) {
                        //太阳矢量
                        double[] r_sun=new double[3];//惯性系下太阳矢量
                        double[] su=new double[2];
                        double rd_sun=Sun(NowTime_JD, r_sun,su);
                        //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                        boolean FlyOrientationFlag=true;
                        double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                        double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(j)[0]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[1]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[2]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2])};
                        double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                                (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                        if (CosTheta_SunVel > 0) {
                            FlyOrientationFlag=false;
                        }else {
                            FlyOrientationFlag=true;
                        }

                        if (FlyOrientationFlag==true && SatelliteAngFlag==false) {
                            //高度、光照满足，计算转角
                            SatelliteAng=LimbAngCalculation(OrbitSatPositionGEIList.get(j));
                            VisibilityTimePeriodChild[0]=j;
                            SatelliteAngFlag=true;
                            continue;
                        }else if (SatelliteAngFlag==true) {
                            if (FlyOrientationFlag) {
                                if (j == 1403) {
                                    System.out.println(j);
                                }
                                boolean LimbVisibility_Flag= LimbVisibilityFlag(OrbitSatPositionGEIList.get(j),SatelliteAng);
                                if (LimbVisibility_Flag) {
                                    if (j == OrbitSatPositionLLAList.size()-1) {
                                        VisibilityTimePeriodChild[1]=j;
                                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                        TimePeriodNumListChild++;
                                        SatelliteAngFlag=false;
                                        PeriodFlag_last=PeriodFlag_Now;
                                        PeriodFlag_Now=false;
                                    }
                                    continue;
                                }else {
                                    VisibilityTimePeriodChild[1]=j-1;
                                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                    TimePeriodNumListChild++;
                                    SatelliteAngFlag=false;
                                    PeriodFlag_last=PeriodFlag_Now;
                                    PeriodFlag_Now=false;
                                    continue;
                                }
                            }else {
                                VisibilityTimePeriodChild[1]=j-1;
                                int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                TimePeriodNumListChild++;
                                SatelliteAngFlag=false;
                                PeriodFlag_last=PeriodFlag_Now;
                                PeriodFlag_Now=false;
                                continue;
                            }
                        }else {
                            PeriodFlag_last=PeriodFlag_Now;
                            PeriodFlag_Now=false;
                            continue;
                        }
                    }

                    if (OrbitSatPositionLLAList.get(j)[2] >= MissionLimbHeigh.get(i)-MissionLimbHeighLimt && OrbitSatPositionLLAList.get(j)[2] <= MissionLimbHeigh.get(i)+MissionLimbHeighLimt) {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=true;
                    }else {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                }else {
                    if (SatelliteAngFlag) {
                        VisibilityTimePeriodChild[1]=j-1;
                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                        TimePeriodNumListChild++;
                        SatelliteAngFlag=false;
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                    PeriodFlag_last=PeriodFlag_Now;
                    PeriodFlag_Now=false;
                }

            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        Map<String, Boolean> starMissionMap=new TreeMap<>();
        for (int i = 0; i < MissionNum; i++) {
            if (VisibilityTimePeriodList.get(i).size() > 0) {
                starMissionMap.put(MissionLimbSerialNumber.get(i),true);
            }else {
                starMissionMap.put(MissionLimbSerialNumber.get(i),false);
            }
        }
        return starMissionMap;
    }

    //应急临边观测可见性计算
    private static Map<String, ArrayList<Date[]>> EmerLimbVisibilityCalculationII(ArrayList<Document> MissionLimbDocument, ArrayList<Date> OrbitTimeDateList, ArrayList<double[]> OrbitTimeList, ArrayList<double[]> OrbitSatPositionGEIList, ArrayList<double[]> OrbitSatVelocityGEIList, ArrayList<double[]> OrbitSatPositionLLAList){
        ArrayList<Double> MissionLimbHeigh=new ArrayList<>();
        ArrayList<double[]> MissionLimbStartTime=new ArrayList<>();
        ArrayList<double[]> MissionLimbStopTime=new ArrayList<>();
        ArrayList<String> MissionLimbSerialNumber=new ArrayList<>();
        ArrayList<ArrayList<String>> MissionLimbForOrderNumbers=new ArrayList<>();
        int MissionNum=0;

        for (Document document:MissionLimbDocument) {
            try {
                //读取期望轨道高度
                double MissionLimbHeighChild= Double.parseDouble(document.get("scan_height_orbit").toString());
                //读取任务期望时间
                Date expected_start_time = document.getDate("expected_start_time");
                double[] MissionStarTime_iList = DateToDouble(expected_start_time);
                Date expected_stop_time=document.getDate("expected_end_time");
                double[] MissionStopTime_iList =DateToDouble(expected_stop_time);
                //读取任务编号
                String MissionSerialNumber_iList = document.get("order_number").toString();

                MissionLimbHeigh.add(MissionNum,MissionLimbHeighChild);
                MissionLimbStartTime.add(MissionNum,MissionStarTime_iList);
                MissionLimbStopTime.add( MissionNum,MissionStopTime_iList);
                MissionLimbSerialNumber.add(MissionNum,MissionSerialNumber_iList);
                MissionNum++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        //可执行弧段
        ArrayList<ArrayList<int[]>> VisibilityTimePeriodList = new ArrayList<>();
        ArrayList<Integer> TimePeriodNumList = new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            double StartTime_JD=JD(MissionLimbStartTime.get(i));
            double StopTime_JD=JD(MissionLimbStopTime.get(i));
            boolean PeriodFlag_last=false;
            boolean PeriodFlag_Now=false;
            boolean SatelliteAngFlag=false;
            double SatelliteAng=0;
            int[] VisibilityTimePeriodChild=new int[2];
            ArrayList<int[]> VisibilityTimePeriodListChild=new ArrayList<>();
            Integer TimePeriodNumListChild=0;
            for (int j = 0; j < OrbitSatPositionLLAList.size(); j++) {
                double NowTime_JD=JD(OrbitTimeList.get(j));
                if (NowTime_JD >= StartTime_JD && NowTime_JD<=StopTime_JD) {
                    if (PeriodFlag_last == false && PeriodFlag_Now==true) {
                        //太阳矢量
                        double[] r_sun=new double[3];//惯性系下太阳矢量
                        double[] su=new double[2];
                        double rd_sun=Sun(NowTime_JD, r_sun,su);
                        //卫星飞行方向与本体x轴是否一致，true表示一致，false表示相反
                        boolean FlyOrientationFlag=true;
                        double[] r_sun_n=new double[]{r_sun[0]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[1]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2]),
                                r_sun[2]/sqrt(r_sun[0]*r_sun[0]+r_sun[1]*r_sun[1]+r_sun[2]*r_sun[2])};
                        double[] v_sat_n=new double[]{OrbitSatVelocityGEIList.get(j)[0]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[1]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2]),
                                OrbitSatVelocityGEIList.get(j)[2]/sqrt(OrbitSatVelocityGEIList.get(j)[0]*OrbitSatVelocityGEIList.get(j)[0]+OrbitSatVelocityGEIList.get(j)[1]*OrbitSatVelocityGEIList.get(j)[1]+OrbitSatVelocityGEIList.get(j)[2]*OrbitSatVelocityGEIList.get(j)[2])};
                        double CosTheta_SunVel=(r_sun_n[0]*v_sat_n[0]+r_sun_n[1]*v_sat_n[1]+r_sun_n[2]*v_sat_n[2])/
                                (sqrt(r_sun_n[0]*r_sun_n[0]+r_sun_n[1]*r_sun_n[1]+r_sun_n[2]*r_sun_n[2])*sqrt(v_sat_n[0]*v_sat_n[0]+v_sat_n[1]*v_sat_n[1]+v_sat_n[2]*v_sat_n[2]));
                        if (CosTheta_SunVel > 0) {
                            FlyOrientationFlag=false;
                        }else {
                            FlyOrientationFlag=true;
                        }

                        if (FlyOrientationFlag==true && SatelliteAngFlag==false) {
                            //高度、光照满足，计算转角
                            SatelliteAng=LimbAngCalculation(OrbitSatPositionGEIList.get(j));
                            VisibilityTimePeriodChild[0]=j;
                            SatelliteAngFlag=true;
                            continue;
                        }else if (SatelliteAngFlag==true) {
                            if (FlyOrientationFlag) {
                                if (j == 1403) {
                                    System.out.println(j);
                                }
                                boolean LimbVisibility_Flag= LimbVisibilityFlag(OrbitSatPositionGEIList.get(j),SatelliteAng);
                                if (LimbVisibility_Flag) {
                                    if (j == OrbitSatPositionLLAList.size()-1) {
                                        VisibilityTimePeriodChild[1]=j;
                                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                        TimePeriodNumListChild++;
                                        SatelliteAngFlag=false;
                                        PeriodFlag_last=PeriodFlag_Now;
                                        PeriodFlag_Now=false;
                                    }
                                    continue;
                                }else {
                                    VisibilityTimePeriodChild[1]=j-1;
                                    int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                    VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                    TimePeriodNumListChild++;
                                    SatelliteAngFlag=false;
                                    PeriodFlag_last=PeriodFlag_Now;
                                    PeriodFlag_Now=false;
                                    continue;
                                }
                            }else {
                                VisibilityTimePeriodChild[1]=j-1;
                                int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                                VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                                TimePeriodNumListChild++;
                                SatelliteAngFlag=false;
                                PeriodFlag_last=PeriodFlag_Now;
                                PeriodFlag_Now=false;
                                continue;
                            }
                        }else {
                            PeriodFlag_last=PeriodFlag_Now;
                            PeriodFlag_Now=false;
                            continue;
                        }
                    }

                    if (OrbitSatPositionLLAList.get(j)[2] >= MissionLimbHeigh.get(i)-MissionLimbHeighLimt && OrbitSatPositionLLAList.get(j)[2] <= MissionLimbHeigh.get(i)+MissionLimbHeighLimt) {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=true;
                    }else {
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                }else {
                    if (SatelliteAngFlag) {
                        VisibilityTimePeriodChild[1]=j-1;
                        int[] VisibilityTimePeriodChildTemp=new int[]{VisibilityTimePeriodChild[0],VisibilityTimePeriodChild[1]};
                        VisibilityTimePeriodListChild.add(TimePeriodNumListChild,VisibilityTimePeriodChildTemp);
                        TimePeriodNumListChild++;
                        SatelliteAngFlag=false;
                        PeriodFlag_last=PeriodFlag_Now;
                        PeriodFlag_Now=false;
                    }
                    PeriodFlag_last=PeriodFlag_Now;
                    PeriodFlag_Now=false;
                }

            }
            VisibilityTimePeriodList.add(i,VisibilityTimePeriodListChild);
            TimePeriodNumList.add(i,TimePeriodNumListChild);
        }

        Map<String, ArrayList<Date[]>> starMissionMap=new TreeMap<>();
        for (int i = 0; i < MissionNum; i++) {
            ArrayList<Date[]> datetemp=new ArrayList<>();
            if (VisibilityTimePeriodList.get(i).size() > 0) {
                for (int j = 0; j < VisibilityTimePeriodList.get(i).size(); j++) {
                    Date[] temp=new Date[]{OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j)[0]),
                            OrbitTimeDateList.get(VisibilityTimePeriodList.get(i).get(j)[1])};
                    datetemp.add(temp);
                }
                starMissionMap.put(MissionLimbSerialNumber.get(i),datetemp);
            }else {
                starMissionMap.put(MissionLimbSerialNumber.get(i),datetemp);
            }
        }
        return starMissionMap;
    }


    //临边观测模式下，由卫星位置计算卫星绕y轴向前侧摆的角度
    private static double LimbAngCalculation(double SatPositionGEI[]){
        double Ang;
        double r_Sat=sqrt(SatPositionGEI[0]*SatPositionGEI[0]+SatPositionGEI[1]*SatPositionGEI[1]+SatPositionGEI[2]*SatPositionGEI[2]);
        Ang=Math.asin((Re+MissionLimbAirHeighLimt)/r_Sat);

        return Ang;
    }

    //判定卫星在当前转角，当前位置下是否能临边观测
    private static boolean LimbVisibilityFlag(double SatPositionGEI[],double SatLimbAng){
        double viewAng=8*PI/180.0;
        double r_Sat=sqrt(SatPositionGEI[0]*SatPositionGEI[0]+SatPositionGEI[1]*SatPositionGEI[1]+SatPositionGEI[2]*SatPositionGEI[2]);
        double r_underLine,r_upLine;
        if (SatLimbAng-viewAng < 0) {
            r_underLine=0;
        }else {
            r_underLine=sin(SatLimbAng-viewAng)*r_Sat;
        }
        if (SatLimbAng+viewAng > PI/2) {
            r_upLine=r_Sat;
        }else {
            r_upLine=sin(SatLimbAng+viewAng)*r_Sat;
        }

        if (r_underLine <= Re+MissionLimbAirHeighLimt && r_upLine>Re) {
            return true;
        }else {
            return false;
        }
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

}
