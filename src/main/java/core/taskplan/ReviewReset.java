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
import java.util.*;

import static java.lang.Math.*;

//import common.mongo.DbDefine;

public class ReviewReset {
    private static double AUtokm = 1.49597870e8;
    private static double kmtom = 10e3;
    private static double Re = 6371393;                  //地球半径，单位为：米
    private static double Step = 1;                      //数据步长

    //能源相关变量
    private static double PowerEfficiency = 0.9;          //太阳帆板发电效率
    private static double PowerChargeEfficiency = 0.7;     //蓄电池充电效率
    private static double PowerChargeOutEfficiency = 0.9;     //蓄电池放电效率
    private static double PowerChargeMax = 12;               //蓄电池最大充电电流
    private static double PowerGenerationMax = 100;       //太阳帆板最大充电电流，单位：安
    private static double PowerCapacity = 125;            //蓄电池容量，单位：安*时
    private static double PowerAverage_Standby = 1000;    //待机平均功率，单位：瓦
    private static double PowerAverage_Image = 2000;      //成像平均功率，单位：瓦
    private static double PowerAverage_Playback = 1500;   //回放平均功率，单位：瓦
    private static double PowerRecord_Play = 3000;        //边记边放平均功率，单位：瓦
    private static double PowerGenerationVoltage = 42;      //太阳帆板充电电压，单位：伏
    private static double PowerBatteryVoltage = 42;         //蓄电池供电电压，单位：伏
    private static double PowerBatteryGenerVoltage = 42;    //蓄电池充电电压，单位：伏

    //数据相关变量
    private static double MemoryStorageCapacity = 2 * 1024 * 1024;    //固存，单位：M
    private static double MemoryRecord = 500;                     //记录速度，单位：M/s
    private static double Memoryplayback = 600;                   //回放速度，单位：M/s

    //轨道数据变量
    private static int OrbitalDataNum;
    private static double[][] Orbital_Time;
    private static double[][] Orbital_SatPosition;
    private static double[][] Orbital_SatVelocity;
    private static Date[] Time_Point;
    private static double[][] Orbital_SatPositionLLA;

    //姿态数据变量
    private static double[][] Attitude_EulerAng;        //卫星姿态角
    private static double[][] Attitude_AngVel;          //卫星姿态角速度
    private static int AttitudeDataNum;
    private static double[] Attitude_EulerMax = {10 * PI / 180.0, 10 * PI / 180.0, 10 * PI / 180.0};//卫星最大机动角度
    private static double[] Attitude_AngVelMax = {10 * Math.PI / 180.0, 10 * Math.PI / 180.0, 10 * Math.PI / 180.0};//最大机动角速度

    //卫星状态变量
    private static int[] ImageMissionStatus;            //卫星成像状态
    private static int[] StationMissionStatus;          //卫星传输状态

    //全过程状态
    private static double[] PowerStatus;                //卫星能量状态
    private static double[] DataStatus;                 //卫星数据状态

    //复核结果
    private static Map<String, Boolean> ReviewResult = new TreeMap<>();
    private static int FalseMissionNum = 0;
    private static int[] FalseMission;

    private static Double[] v_records = new Double[4];//存储每个相机的记录速度
    private static String[] sensorCodes = new String[4];//存储相机code

    public static void ReviewResetII(Document Satllitejson, FindIterable<Document> Orbitjson, long OrbitDataCount, FindIterable<Document> Attitudejson, long AttitudeDataCount, ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson, double DataStorageCapacitySur, double PowerCapacitySur) {
        //数据初始话
        ImageMissionStatus = new int[(int) OrbitDataCount];
        StationMissionStatus = new int[(int) OrbitDataCount];
        for (int i = 0; i < (int) OrbitDataCount; i++) {
            ImageMissionStatus[i] = 0;
            StationMissionStatus[i] = 0;
        }
        PowerStatus = new double[(int) OrbitDataCount];
        DataStatus = new double[(int) OrbitDataCount];

        Boolean ESDStatus = true;
        String AxisType = "";

        //读取卫星资源数据
        double v_playback = 600;
        double storage_capacity = 2 * 1024 * 1024;
        MemoryStorageCapacity = storage_capacity;
        double storage_threshold = 0.4;
        double average_power_standby = 1849.78;
        double average_power_image_SCHF = 2410.91;
        double average_power_image_SC = 2000;
        double average_power_image_JL = 2000;
        double average_power_playback = 2246.27;
        PowerAverage_Playback = average_power_playback;
        double record_play_power = 2417.33;
        PowerRecord_Play = record_play_power;
        double sailboard_current = 100;
        PowerGenerationMax = sailboard_current;
        double power_efficiency = 0.94;
        PowerEfficiency = power_efficiency;
        double power_capacity = 125;
        PowerCapacity = power_capacity;
        double power_charge = 0.93;
        PowerChargeEfficiency = power_charge;
        double max_discharge_depth = 0.80;
        double max_record_depth = 0.40;
        double v_record_1 = 277.98;
        double v_record_2 = 158.62;
        double v_record_3 = 69.89;
        double v_record_4 = 69.89;
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        for (Document document : properties) {
            try {
                if (document.get("key").toString().equals("v_playback")) {
                    v_playback = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("storage_capacity")) {
                    storage_capacity = Double.parseDouble(document.get("value").toString()) * 1024 * 1024;
                    MemoryStorageCapacity = Double.parseDouble(document.get("value").toString()) * 1024 * 1024;
                } else if (document.get("key").toString().equals("storage_threshold")) {
                    storage_threshold = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("average_power_standby")) {
                    average_power_standby = Double.parseDouble(document.get("value").toString());
                } else if (document.get("name").toString().equals("实传加回放功率")) {
                    average_power_image_SCHF = Double.parseDouble(document.get("value").toString());
                } else if (document.get("name").toString().equals("实传功率")) {
                    average_power_image_SC = Double.parseDouble(document.get("value").toString());
                } else if (document.get("name").toString().equals("记录功率")) {
                    average_power_image_JL = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("max_discharge_depth")) {
                    max_discharge_depth = Double.parseDouble(document.get("value").toString()) / 100;
                } else if (document.get("key").toString().equals("max_record_depth")) {
                    max_record_depth = Double.parseDouble(document.get("value").toString()) / 100;
                } else if (document.get("key").toString().equals("v_record_1")) {
                    v_record_1 = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("v_record_2")) {
                    v_record_2 = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("v_record_3")) {
                    v_record_3 = Double.parseDouble(document.get("value").toString());
                } else if (document.get("key").toString().equals("v_record_4")) {
                    v_record_4 = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("power_efficiency")) {
                    PowerEfficiency = Double.parseDouble(document.get("value").toString()) / 100;
                    power_efficiency = Double.parseDouble(document.get("value").toString()) / 100;
                } else if (document.getString("key").equals("power_charge")) {
                    PowerChargeEfficiency = Double.parseDouble(document.get("value").toString()) / 100;
                    power_charge = Double.parseDouble(document.get("value").toString()) / 100;
                } else if (document.getString("key").equals("sailboard_current")) {
                    PowerGenerationMax = Double.parseDouble(document.get("value").toString());
                    sailboard_current = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("power_capacity")) {
                    PowerCapacity = Double.parseDouble(document.get("value").toString());
                    power_capacity = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("average_power_standby")) {
                    PowerAverage_Standby = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("average_power_image")) {
                    PowerAverage_Image = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("average_power_playback")) {
                    PowerAverage_Playback = Double.parseDouble(document.get("value").toString());
                    average_power_playback = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("record_play_power")) {
                    PowerRecord_Play = Double.parseDouble(document.get("value").toString());
                    record_play_power = Double.parseDouble(document.get("value").toString());
                } else if (document.getString("key").equals("roll_angle_max")) {
                    Attitude_EulerMax[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
                } else if (document.getString("key").equals("pitch_angle_max")) {
                    Attitude_EulerMax[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
                } else if (document.getString("key").equals("v_roll_angle")) {
                    Attitude_AngVelMax[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
                } else if (document.getString("key").equals("v_pitch_angle")) {
                    Attitude_AngVelMax[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
                } else if (document.getString("key").equals("axis")) {
                    AxisType = document.getString("value");
                } else
                    continue;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        if (AxisType.contains("轨道"))
            ESDStatus = false;
        for (Document document : properties) {//读取记录速度
            if (document.getString("key").equals("v_record_1") && document.getString("group").equals("payload1")) {
                v_records[0] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_2") && document.getString("group").equals("payload2")) {
                v_records[1] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_3") && document.getString("group").equals("payload3")) {
                v_records[2] = Double.parseDouble(document.getString("value"));
            } else if (document.getString("key").equals("v_record_4") && document.getString("group").equals("payload4")) {
                v_records[3] = Double.parseDouble(document.getString("value"));
            } else {
            }
        }
        for (Document document : properties) {//获取相机code
            if (document.getString("key").equals("code") && document.getString("group").equals("payload1")) {
                sensorCodes[0] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload2")) {
                sensorCodes[1] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload3")) {
                sensorCodes[2] = document.getString("value");
            } else if (document.getString("key").equals("code") && document.getString("group").equals("payload4")) {
                sensorCodes[3] = document.getString("value");
            } else {
            }
        }

        //轨道数据读入
        //轨道数据读入
        Orbital_Time = new double[(int) OrbitDataCount][6];
        Orbital_SatPosition = new double[(int) OrbitDataCount][3];
        Orbital_SatVelocity = new double[(int) OrbitDataCount][3];
        Time_Point = new Date[(int) OrbitDataCount];
        Orbital_SatPositionLLA = new double[(int) OrbitDataCount][3];
        OrbitalDataNum = 0;
        if (Orbitjson != null) {
            for (Document document : Orbitjson) {
                try {
                    Date time_point = document.getDate("time_point");
                    //时间转换为doubule型
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String StringTime;
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

                    if (OrbitalDataNum >= OrbitDataCount)
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //姿态数据读入
        Attitude_EulerAng = new double[(int) AttitudeDataCount][3];
        Attitude_AngVel = new double[(int) AttitudeDataCount][3];
        AttitudeDataNum = 0;
        if (Attitudejson != null) {
            for (Document document : Attitudejson) {
                try {
                    Attitude_EulerAng[AttitudeDataNum][0] = Double.parseDouble(document.get("roll_angle").toString());
                    Attitude_EulerAng[AttitudeDataNum][1] = Double.parseDouble(document.get("pitch_angle").toString());
                    Attitude_EulerAng[AttitudeDataNum][2] = Double.parseDouble(document.get("yaw_angle").toString());
                    Attitude_AngVel[AttitudeDataNum][0] = Double.parseDouble(document.get("V_roll_angle").toString());
                    Attitude_AngVel[AttitudeDataNum][1] = Double.parseDouble(document.get("V_pitch_angle").toString());
                    Attitude_AngVel[AttitudeDataNum][2] = Double.parseDouble(document.get("V_yaw_angle").toString());
                    AttitudeDataNum = AttitudeDataNum + 1;
                    if (AttitudeDataNum >= AttitudeDataCount)
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //成像任务读入
        FalseMission = new int[ImageMissionjson.size()];
        int MissionNumber = 0;
        int[][] MissionStarEnd_Number = new int[ImageMissionjson.size()][2];
        int MissionChark_Number = 0;
        String[] MissionName = new String[ImageMissionjson.size()];
        ArrayList<Integer> ImageWindowLoad = new ArrayList<>();
        ArrayList<Integer> ImageWorkModel = new ArrayList<>();

        ArrayList<ArrayList<String>> MissionForOrderNumbers = new ArrayList<>();
        ArrayList<Boolean> MissionCheckFlag = new ArrayList<>();
        ArrayList<int[]> MissionStarEndTime = new ArrayList<>();
        ArrayList<Integer> MissionFalseResuFlag = new ArrayList<>();
        ArrayList<Double> MissionV_RecordList = new ArrayList<>();
        ArrayList<Document> ImageMissionjsonNew = new ArrayList<>();

        try {
            if (ImageMissionjson != null) {
                for (Document document : ImageMissionjson) {
                    try {
                        //获取数据写入速率
                        double MissionV_RecordTemp = getSizePerSec(document);
                        MissionV_RecordList.add(MissionNumber, MissionV_RecordTemp);

                        if (document.getString("mission_state").equals("待执行")) {
                            ImageMissionjsonNew.add(MissionNumber, document);
                            ArrayList<Document> available_window = (ArrayList<Document>) document.get("image_window");
                            for (Document document1 : available_window) {
                                Date time_point = document1.getDate("start_time");
                                //时间转换为doubule型
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String StringTime;
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(time_point);
                                cal.add(Calendar.HOUR_OF_DAY, -8);
                                StringTime = sdf.format(cal.getTime());
                                double[] MissionStar_Time = {Double.parseDouble(StringTime.substring(0, 4)),
                                        Double.parseDouble(StringTime.substring(5, 7)),
                                        Double.parseDouble(StringTime.substring(8, 10)),
                                        Double.parseDouble(StringTime.substring(11, 13)),
                                        Double.parseDouble(StringTime.substring(14, 16)),
                                        Double.parseDouble(StringTime.substring(17, 19))};
                                time_point = document1.getDate("end_time");
                                cal.setTime(time_point);
                                cal.add(Calendar.HOUR_OF_DAY, -8);
                                StringTime = sdf.format(cal.getTime());
                                double[] MissionEnd_Time = {Double.parseDouble(StringTime.substring(0, 4)),
                                        Double.parseDouble(StringTime.substring(5, 7)),
                                        Double.parseDouble(StringTime.substring(8, 10)),
                                        Double.parseDouble(StringTime.substring(11, 13)),
                                        Double.parseDouble(StringTime.substring(14, 16)),
                                        Double.parseDouble(StringTime.substring(17, 19))};
                                int MissionStar_Number = (int) ((JD(MissionStar_Time) - JD(Orbital_Time[0])) * (24 * 60 * 60) / Step);
                                int MissionEnd_Number = (int) ((JD(MissionEnd_Time) - JD(Orbital_Time[0])) * (24 * 60 * 60) / Step);
                                MissionStarEnd_Number[MissionNumber][0] = MissionStar_Number;
                                MissionStarEnd_Number[MissionNumber][1] = MissionEnd_Number;
                                int Load_numberTemp = 1;
                                if (document1.containsKey("load_number") && document1.get("load_number") != null) {
                                    Load_numberTemp = Integer.parseInt(document1.get("load_number").toString());
                                }
                                ImageWindowLoad.add(Load_numberTemp);
                                MissionChark_Number = MissionChark_Number + 1;
                                for (int i = MissionStar_Number; i <= MissionEnd_Number; i++) {
                                    ImageMissionStatus[i] = MissionNumber + 1;
                                }
                                int[] MissionStarEndTimeChild = new int[]{MissionStar_Number, MissionEnd_Number};
                                MissionStarEndTime.add(MissionStarEndTimeChild);
                                MissionFalseResuFlag.add(0);
                            }
                            ReviewResult.put(document.getString("mission_number"), true);
                            FalseMission[MissionNumber] = 1;
                        } else {
                            continue;
                            //ReviewResult.put(document.getString("mission_number"), false);
                            //FalseMission[MissionNumber] = 0;
                            //MissionFalseResuFlag.add(0);
                        }
                        MissionName[MissionNumber] = document.getString("name");
                        ArrayList<String> MissionForOrderNumbers_i = new ArrayList<>();
                        MissionForOrderNumbers_i = (ArrayList<String>) document.get("order_numbers");
                        MissionForOrderNumbers.add(MissionNumber, MissionForOrderNumbers_i);
                        String work_mode = document.get("work_mode").toString();
                        int work_modelTemp = 1;
                        if (work_mode.equals("记录")) {
                            work_modelTemp = 1;
                        } else if (work_mode.equals("实传")) {
                            work_modelTemp = 2;
                        } else if (work_mode.equals("实传+回放")) {
                            work_modelTemp = 3;
                        } else if (work_mode.equals("记录+回放")) {
                            work_modelTemp = 4;
                        }
                        ImageWorkModel.add(work_modelTemp);
                        MissionCheckFlag.add(true);
                        MissionNumber = MissionNumber + 1;
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        //传输任务读入
        ArrayList<Boolean> TransmissionCheckFlag = new ArrayList<>();
        ArrayList<int[]> TransmissionStarEndTime = new ArrayList<>();
        ArrayList<Document> TransmissionWindowArrayTemp = new ArrayList<>();
        if (TransmissionMissionJson != null) {
            int StationMissionNumber = 0;
            try {
                StationMissionNumber = 0;
                Boolean fail_reasonFlag = TransmissionMissionJson.containsKey("fail_reason");
                if (fail_reasonFlag) {
                    if (TransmissionMissionJson.get("fail_reason").equals("不可见")) {
                        fail_reasonFlag = true;
                    } else {
                        fail_reasonFlag = false;
                    }
                }
                if (fail_reasonFlag == false) {
                    ArrayList<Document> TransmissionWindowArray = new ArrayList<>();
                    TransmissionWindowArray = (ArrayList<Document>) TransmissionMissionJson.get("transmission_window");
                    for (Document document : TransmissionWindowArray) {
                        try {
                            Date time_point = document.getDate("start_time");
                            //时间转换为doubule型
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String StringTime;
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(time_point);
                            cal.add(Calendar.HOUR_OF_DAY, -8);
                            StringTime = sdf.format(cal.getTime());
                            double[] MissionStar_Time = {Double.parseDouble(StringTime.substring(0, 4)),
                                    Double.parseDouble(StringTime.substring(5, 7)),
                                    Double.parseDouble(StringTime.substring(8, 10)),
                                    Double.parseDouble(StringTime.substring(11, 13)),
                                    Double.parseDouble(StringTime.substring(14, 16)),
                                    Double.parseDouble(StringTime.substring(17, 19))};
                            time_point = document.getDate("end_time");
                            cal.setTime(time_point);
                            cal.add(Calendar.HOUR_OF_DAY, -8);
                            StringTime = sdf.format(cal.getTime());
                            double[] MissionEnd_Time = {Double.parseDouble(StringTime.substring(0, 4)),
                                    Double.parseDouble(StringTime.substring(5, 7)),
                                    Double.parseDouble(StringTime.substring(8, 10)),
                                    Double.parseDouble(StringTime.substring(11, 13)),
                                    Double.parseDouble(StringTime.substring(14, 16)),
                                    Double.parseDouble(StringTime.substring(17, 19))};
                            int MissionStar_Number = (int) ((JD(MissionStar_Time) - JD(Orbital_Time[0])) * (24 * 60 * 60) / Step);
                            int MissionEnd_Number = (int) ((JD(MissionEnd_Time) - JD(Orbital_Time[0])) * (24 * 60 * 60) / Step);
                            for (int i = MissionStar_Number; i <= MissionEnd_Number; i++) {
                                StationMissionStatus[i] = StationMissionNumber + 1;
                            }
                            TransmissionCheckFlag.add(true);
                            int[] TransmissionStarEndTimeChild = new int[]{MissionStar_Number, MissionEnd_Number};
                            TransmissionStarEndTime.add(TransmissionStarEndTimeChild);
                            TransmissionWindowArrayTemp.add(document);
                            StationMissionNumber = StationMissionNumber + 1;
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //将能量单位统一为瓦
        //初始电量和空间
        double BatteryCapacity = PowerCapacity * 42 * 60 * 60;      //蓄电池电量
        double MemorySpace = MemoryStorageCapacity;           //固存空间
        if ((int) OrbitDataCount > 0) {
            //PowerStatus[0] = BatteryCapacity;
            //DataStatus[0] = MemorySpace;
            PowerStatus[0] = PowerCapacitySur * 42 * 60 * 60;
            DataStatus[0] = DataStorageCapacitySur;
        }
        FalseMissionNum = 0;
        Boolean CheckFlag_now = true;
        Boolean CheckFlag_next = true;
        ArrayList<Double> ChargeCurrentArray = new ArrayList<>();
        double ChargeCurrentSum = 0;
        /*
        for (int i = 0; i < OrbitalDataNum; i++) {
            //计算太阳帆板充电量
            double Time_JD = JD(Orbital_Time[i]);
            double[] r_sun = new double[3];
            double[] su = new double[3];
            double rad_sun = Sun(Time_JD, r_sun, su);
            boolean Eclipse_Flag = EarthEclipseStatus(r_sun, Orbital_SatPosition[i]);
            double ChargeCurrent;
            CheckFlag_now=CheckFlag_next;
            if (Eclipse_Flag == true) {
                //处于光照区
                double[] Attitude_EulerAngTemp=new double[]{0,0,0};
                if (i > Attitude_EulerAng.length) {
                    Attitude_EulerAngTemp=new double[]{0,0,0};
                }
                ChargeCurrent = ChargeCurrentCalculation(r_sun, Orbital_SatPosition[i], Orbital_SatVelocity[i], Attitude_EulerAngTemp, PowerChargeEfficiency, PowerGenerationMax);
                CheckFlag_next=true;
            } else {
                //处于阴影区
                ChargeCurrent = 0;
                CheckFlag_next=false;
            }
            ChargeCurrentArray.add(ChargeCurrent);
            ChargeCurrentSum=ChargeCurrentSum+ChargeCurrent;

        }
        System.out.println(ChargeCurrentSum/OrbitalDataNum);
        */

        if (ImageMissionjsonNew.size() > 0 || TransmissionWindowArrayTemp.size() > 0) {
            for (int j = 1; j < OrbitalDataNum; j++) {
                //System.out.println(j);
                //计算太阳帆板充电量
                int i = j - 1;
                double Time_JD = JD(Orbital_Time[i]);
                double[] r_sun = new double[3];
                double[] su = new double[3];
                double rad_sun = Sun(Time_JD, r_sun, su);
                boolean Eclipse_Flag = EarthEclipseStatus(r_sun, Orbital_SatPosition[i]);
                double ChargeCurrent;
                if (Eclipse_Flag == true) {
                    //处于光照区
                    double[] Attitude_EulerAngTemp = new double[]{0, 0, 0};
                    if (i > Attitude_EulerAng.length) {
                        Attitude_EulerAngTemp = new double[]{0, 0, 0};
                    }
                    ChargeCurrent = ChargeCurrentCalculation(Orbital_Time[i], r_sun, Orbital_SatPosition[i], Orbital_SatVelocity[i], Orbital_SatPositionLLA[i], Attitude_EulerAngTemp, PowerChargeEfficiency, PowerGenerationMax, ESDStatus);
                } else {
                    //处于阴影区
                    ChargeCurrent = 0;
                }

                //能量复核
                if (ImageMissionStatus[i] != 0) {
                    //成像时间段
                    double SailBoard = ChargeCurrent;
                    double PowerUse = average_power_image_JL;
                    double DataUse = 0;
                    if (ImageWindowLoad.size() >= ImageMissionStatus[i]) {
                        DataUse = -MissionV_RecordList.get(ImageMissionStatus[i] - 1);
                    }
                    if (ImageWorkModel.size() >= ImageMissionStatus[i]) {
                        if (ImageWorkModel.get(ImageMissionStatus[i] - 1) == 1) {
                            PowerUse = average_power_image_JL;
                        } else if (ImageWorkModel.get(ImageMissionStatus[i] - 1) == 2) {
                            PowerUse = average_power_image_SC;
                            DataUse = 0;
                        } else if (ImageWorkModel.get(ImageMissionStatus[i] - 1) == 3) {
                            PowerUse = average_power_image_SCHF;
                            DataUse = 0;
                        } else if (ImageWorkModel.get(ImageMissionStatus[i] - 1) == 4) {
                            PowerUse = record_play_power;
                            //DataUse=DataUse+v_playback;
                        }
                    }
                    if (SailBoard >= PowerUse) {
                        //帆板供电大于载荷需求
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) * power_charge;
                        if (PowerStatus[j] > BatteryCapacity) {
                            PowerStatus[j] = BatteryCapacity;
                        }
                    } else {
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) / power_efficiency;
                    }
                    //数据
                    DataStatus[j] = DataStatus[i] + DataUse;
                } else if (StationMissionStatus[i] != 0) {
                    //回放功率
                    double SailBoard = ChargeCurrent;
                    double PowerUse = average_power_playback;
                    double DataUse = 0;
                    if (SailBoard >= PowerUse) {
                        //帆板供电大于载荷需求
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) * power_charge;
                        if (PowerStatus[j] > BatteryCapacity) {
                            PowerStatus[j] = BatteryCapacity;
                        }
                    } else {
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) / power_efficiency;
                    }
                    //数据
                    DataStatus[j] = DataStatus[i] + DataUse;
                } else {
                    //待机功率
                    double SailBoard = ChargeCurrent;
                    double PowerUse = average_power_standby;
                    double DataUse = 0;
                    if (SailBoard >= PowerUse) {
                        //帆板供电大于载荷需求
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) * power_charge;
                        if (PowerStatus[j] > BatteryCapacity) {
                            PowerStatus[j] = BatteryCapacity;
                        }
                    } else {
                        PowerStatus[j] = PowerStatus[i] + (SailBoard - PowerUse) / power_efficiency;
                    }
                    //数据
                    DataStatus[j] = DataStatus[i] + DataUse;
                }

                if (PowerStatus[j] > BatteryCapacity) {
                    PowerStatus[j] = BatteryCapacity;
                }
                //PowerStatus[j] = PowerCapacity * 42 * 60 * 60;
                if (DataStatus[j] > MemoryStorageCapacity) {
                    DataStatus[j] = MemoryStorageCapacity;
                }
                //DataStatus[j] = MemoryStorageCapacity;

                //姿态复核
                if (j < AttitudeDataNum) {
                    int AttitudeChack = 1;
                    if (Attitude_EulerAng[j][0] > Attitude_EulerMax[0] || Attitude_EulerAng[j][1] > Attitude_EulerMax[1]) {
                        AttitudeChack = 0;
                    }
                }


                //复核
                //FalseMission[],表示任务状态，0表示规划失败，1表示规划完成，2表示能量不满足，3表示数据不满足，4表示姿态不满足
                int MissionFalse;
                double PowerCapacityMin = (1 - max_discharge_depth) * PowerCapacity * 42 * 60 * 60;
                double MemoryStorageMin = (1 - max_record_depth) * MemoryStorageCapacity;
                Boolean ThisBeforePowerFlag = false;
                Boolean ThisBeforeDataFlag = true;
                if (PowerStatus[j] < PowerCapacityMin) {
                    for (int k = j; k >= 0; k--) {
                        if (StationMissionStatus[k] != 0) {
                            if (TransmissionCheckFlag.size() >= StationMissionStatus[k]) {
                                int index_mission = StationMissionStatus[k] - 1;
                                int index_long = TransmissionStarEndTime.get(index_mission)[1] - TransmissionStarEndTime.get(index_mission)[0];
                                if (index_long > 600) {
                                    int index_long_Temp = (int) (index_long / 4);
                                    Date TranWindowStartTime = TransmissionWindowArrayTemp.get(index_mission).getDate("start_time");
                                    Date TranWindowEndTime = TransmissionWindowArrayTemp.get(index_mission).getDate("end_time");
                                    TranWindowStartTime = new Date(TranWindowStartTime.getTime() + index_long_Temp * 1000);
                                    TranWindowEndTime = new Date(TranWindowEndTime.getTime() - index_long_Temp * 1000);
                                    TransmissionWindowArrayTemp.get(index_mission).append("start_time", TranWindowStartTime);
                                    TransmissionWindowArrayTemp.get(index_mission).append("end_time", TranWindowEndTime);
                                    for (int l = TransmissionStarEndTime.get(index_mission)[0]; l <= TransmissionStarEndTime.get(index_mission)[0] + index_long_Temp; l++) {
                                        StationMissionStatus[l] = 0;
                                    }
                                    for (int l = TransmissionStarEndTime.get(index_mission)[1] - index_long_Temp; l <= TransmissionStarEndTime.get(index_mission)[1]; l++) {
                                        StationMissionStatus[l] = 0;
                                    }
                                    j = TransmissionStarEndTime.get(index_mission)[0];
                                    TransmissionStarEndTime.get(index_mission)[0] = TransmissionStarEndTime.get(index_mission)[0] + index_long_Temp;
                                    TransmissionStarEndTime.get(index_mission)[1] = TransmissionStarEndTime.get(index_mission)[1] - index_long_Temp;

                                    ThisBeforePowerFlag = false;
                                    break;
                                } else {
                                    TransmissionCheckFlag.set(StationMissionStatus[k] - 1, false);
                                    for (int l = TransmissionStarEndTime.get(index_mission)[0]; l <= TransmissionStarEndTime.get(index_mission)[1]; l++) {
                                        StationMissionStatus[l] = 0;
                                    }
                                    j = TransmissionStarEndTime.get(index_mission)[0];

                                    ThisBeforePowerFlag = false;
                                    break;
                                }
                            }
                        } else if (ImageMissionStatus[k] != 0) {
                            if (MissionCheckFlag.size() >= ImageMissionStatus[k]) {
                                MissionCheckFlag.set(ImageMissionStatus[k] - 1, false);
                                int index_mission = ImageMissionStatus[k] - 1;
                                for (int l = MissionStarEndTime.get(index_mission)[0]; l <= MissionStarEndTime.get(index_mission)[1]; l++) {
                                    ImageMissionStatus[l] = 0;
                                }
                                MissionFalseResuFlag.set(index_mission, 2);
                                j = MissionStarEndTime.get(index_mission)[0];

                                ThisBeforePowerFlag = false;
                                break;
                            }
                        }
                    }
                }

                //判定任务是否全为实传任务
                int ShiChuangFlag = 0;
                for (int k = 0; k < MissionCheckFlag.size(); k++) {
                    if (MissionCheckFlag.get(k) && (ImageWorkModel.get(k) == 1 || ImageWorkModel.get(k) == 4)) {
                        ShiChuangFlag = ShiChuangFlag + 1;
                    }
                }
                //数据复核
                if (ShiChuangFlag != 0) {
                    if (DataStatus[j] < MemoryStorageMin) {
                        for (int k = j; k >= 0; k--) {
                            if (ImageMissionStatus[k] != 0) {
                                if (MissionCheckFlag.size() >= ImageMissionStatus[k] && (ImageWorkModel.get(ImageMissionStatus[k] - 1) == 1 || ImageWorkModel.get(ImageMissionStatus[k] - 1) == 4)) {
                                    MissionCheckFlag.set(ImageMissionStatus[k] - 1, false);
                                    int index_mission = ImageMissionStatus[k] - 1;
                                    for (int l = MissionStarEndTime.get(index_mission)[0]; l <= MissionStarEndTime.get(index_mission)[1]; l++) {
                                        ImageMissionStatus[l] = 0;
                                    }
                                    MissionFalseResuFlag.set(index_mission, 3);
                                    j = MissionStarEndTime.get(index_mission)[0];
                                    break;
                                }
                            }
                        }
                    }
                }

                if (ThisBeforePowerFlag) {
                    for (int k = 0; k < MissionCheckFlag.size(); k++) {
                        if (MissionCheckFlag.get(k)) {
                            MissionCheckFlag.set(k, false);
                            MissionFalseResuFlag.set(k, 2);
                        }
                    }
                }

                //全部任务是否已完成复核
                int SumFlag = 0;
                for (int k = 0; k < MissionCheckFlag.size(); k++) {
                    if (MissionCheckFlag.get(k)) {
                        SumFlag = SumFlag + 1;
                    }
                }
                for (int k = 0; k < TransmissionCheckFlag.size(); k++) {
                    if (TransmissionCheckFlag.get(k)) {
                        SumFlag = SumFlag + 1;
                    }
                }
                if (SumFlag == 0) {
                    break;
                }
            }

            //数据传出
            MongoClient mongoClient = MangoDBConnector.getClient();
            //获取名为"temp"的数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
            String transmission_number = "tn_" + Instant.now().toEpochMilli();
            for (int i = 0; i < MissionNumber; i++) {
                if (!MissionCheckFlag.get(i)) {
                    if (MissionFalseResuFlag.get(i) == 2) {
                        ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                        Document ImageWindowjsonObject = new Document();
                        ImageWindowjsonObject.append("load_number", "");
                        ImageWindowjsonObject.append("start_time", "");
                        ImageWindowjsonObject.append("end_time", "");
                        ImageWindowjsonArry.add(ImageWindowjsonObject);
                        ImageMissionjsonNew.get(i).append("mission_state", "被退回");
                        ImageMissionjsonNew.get(i).append("fail_reason", "能量不足");
                        ImageMissionjsonNew.get(i).append("image_window", ImageWindowjsonArry);
                        Document modifiers = new Document();
                        modifiers.append("$set", ImageMissionjsonNew.get(i));
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.updateOne(new Document("_id", ImageMissionjsonNew.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
                    } else if (MissionFalseResuFlag.get(i) == 3) {
                        ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                        Document ImageWindowjsonObject = new Document();
                        ImageWindowjsonObject.append("load_number", "");
                        ImageWindowjsonObject.append("start_time", "");
                        ImageWindowjsonObject.append("end_time", "");
                        ImageWindowjsonArry.add(ImageWindowjsonObject);
                        ImageMissionjsonNew.get(i).append("mission_state", "被退回");
                        ImageMissionjsonNew.get(i).append("fail_reason", "数据存储空间不足");
                        ImageMissionjsonNew.get(i).append("image_window", ImageWindowjsonArry);
                        Document modifiers = new Document();
                        modifiers.append("$set", ImageMissionjsonNew.get(i));
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.updateOne(new Document("_id", ImageMissionjsonNew.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
                    } else {
                        ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                        Document ImageWindowjsonObject = new Document();
                        ImageWindowjsonObject.append("load_number", "");
                        ImageWindowjsonObject.append("start_time", "");
                        ImageWindowjsonObject.append("end_time", "");
                        ImageWindowjsonArry.add(ImageWindowjsonObject);
                        ImageMissionjsonNew.get(i).append("mission_state", "被退回");
                        ImageMissionjsonNew.get(i).append("fail_reason", "");
                        ImageMissionjsonNew.get(i).append("image_window", ImageWindowjsonArry);
                        Document modifiers = new Document();
                        modifiers.append("$set", ImageMissionjsonNew.get(i));
                        MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                        image_mission.updateOne(new Document("_id", ImageMissionjsonNew.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
                    }
                    //回溯订单
                    ArrayList<String> MissionForOrderNumbers_i = new ArrayList<>();
                    MissionForOrderNumbers_i = MissionForOrderNumbers.get(i);
                    for (String OrderNumber : MissionForOrderNumbers_i) {
                        MongoCollection<Document> Data_ImageOrderjson = mongoDatabase.getCollection("image_order");
                        FindIterable<Document> D_ImageOrderjson = Data_ImageOrderjson.find();
                        ArrayList<Document> ImageOrderjson = new ArrayList<>();
                        for (Document document : D_ImageOrderjson) {
                            if (document.get("order_number").equals(OrderNumber)) {
                                document.append("order_state", "被退回");
                                if (document.containsKey("_id"))
                                    document.remove("_id");
                                Document modifiers_mid = new Document();
                                modifiers_mid.append("$set", document);
                                Data_ImageOrderjson.updateOne(new Document("order_number", OrderNumber), modifiers_mid, new UpdateOptions().upsert(true));
                            }
                        }
                    }
                }
            }
            //传输任务
            if (TransmissionMissionJson != null) {
                ArrayList<Document> TranWindowjsonArryAfter = new ArrayList<>();
                for (int j = 0; j < TransmissionCheckFlag.size(); j++) {
                    if (TransmissionCheckFlag.get(j)) {
                        Document TranWindowjsonArryChild = TransmissionWindowArrayTemp.get(j);
                        TranWindowjsonArryAfter.add(TranWindowjsonArryChild);
                    }
                }
                //地面站，传输任务更新？？？？
                if (TransmissionCheckFlag.size() > 0 && TranWindowjsonArryAfter.size() == 0) {
                    TransmissionMissionJson.append("fail_reason", "能量不足");
                }
                TransmissionMissionJson.append("transmission_window", TranWindowjsonArryAfter);
                if (TransmissionMissionJson.containsKey("_id"))
                    TransmissionMissionJson.remove("_id");
                MongoCollection<Document> transmission_mission = mongoDatabase.getCollection("transmission_mission");
                Document modifiers = new Document();
                modifiers.append("$set", TransmissionMissionJson);
                transmission_mission.updateOne(new Document("transmission_number", TransmissionMissionJson.getString("transmission_number")), modifiers, new UpdateOptions().upsert(true));

                if (TransmissionMissionJson.containsKey("mission_numbers")) {
                    try {
                        ArrayList<String> OrderNumbers = (ArrayList<String>) TransmissionMissionJson.get("mission_numbers");
                        MongoCollection<Document> station_mission = mongoDatabase.getCollection("station_mission");
                        FindIterable<Document> station_missions = station_mission.find();
                        for (Document doc : station_missions) {
                            if (OrderNumbers.contains(doc.get("mission_number"))) {
                                if (TransmissionCheckFlag.size() > 0 && TranWindowjsonArryAfter.size() == 0) {
                                    TransmissionMissionJson.append("fail_reason", "能量不足");
                                    doc.append("tag", "被退回");
                                } else
                                    doc.append("tag", "待执行");
                                if (doc.containsKey("_id"))
                                    doc.remove("_id");
                                Document modifiers_mid = new Document();
                                modifiers_mid.append("$set", doc);
                                station_mission.updateOne(new Document("mission_number", doc.get("mission_number")), modifiers_mid, new UpdateOptions().upsert(true));
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            mongoClient.close();
        }
    }

    /*
    if (FalseMission[i] == 0) {
                ReviewResult.put(MissionName[i], false);
            } else if (FalseMission[i] == 1) {
                ReviewResult.put(MissionName[i], true);
            } else if (FalseMission[i] == 2) {
                ReviewResult.put(MissionName[i], false);

                ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "能量不足");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);
                Document modifiers = new Document();
                modifiers.append("$set", ImageMissionjson.get(i));
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.updateOne(new Document("_id", ImageMissionjson.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));
            } else if (FalseMission[i] == 3) {
                ReviewResult.put(MissionName[i], false);

                ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "数据存储空间不足");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);
                Document modifiers = new Document();
                modifiers.append("$set", ImageMissionjson.get(i));
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.updateOne(new Document("_id", ImageMissionjson.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));

                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=new ArrayList<>();
                MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
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



            } else if (FalseMission[i] == 4) {
                ReviewResult.put(MissionName[i], false);

                ArrayList<Document> ImageWindowjsonArry = new ArrayList<>();
                Document ImageWindowjsonObject = new Document();
                ImageWindowjsonObject.append("load_number", "");
                ImageWindowjsonObject.append("start_time", "");
                ImageWindowjsonObject.append("end_time", "");
                ImageWindowjsonArry.add(ImageWindowjsonObject);
                ImageMissionjson.get(i).append("mission_state", "被退回");
                ImageMissionjson.get(i).append("fail_reason", "姿态机动能力不足");
                ImageMissionjson.get(i).append("image_window", ImageWindowjsonArry);
                Document modifiers = new Document();
                modifiers.append("$set", ImageMissionjson.get(i));
                MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
                image_mission.updateOne(new Document("_id", ImageMissionjson.get(i).getObjectId("_id")), modifiers, new UpdateOptions().upsert(true));



                //回溯订单
                ArrayList<String> MissionForOrderNumbers_i=new ArrayList<>();
                MissionForOrderNumbers_i=MissionForOrderNumbers.get(i);
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
     */

    private static double getSizePerSec(Document imageMisson) {
        try {
            double ret = 0.0;
            Boolean[] camEnables = new Boolean[4];
            Double[] frameP = new Double[4];

            for (int i = 0; i < camEnables.length; i++) {//初始化四个相机的状态，开始时为关机
                camEnables[i] = false;
            }

            //检查哪个相机开机
            ArrayList<Document> expected_cam = (ArrayList<Document>) imageMisson.get("expected_cam");

            if (expected_cam == null)
                return ret;

            if (expected_cam.size() == 0)
                return ret;

            Document sat = expected_cam.get(0);

            ArrayList<Document> sensors = (ArrayList<Document>) sat.get("sensors");

            for (Document sensor : sensors) {
                String code = sensor.getString("code");
                int i = 0;
                for (String sensorCode : sensorCodes) {
                    if (code.equals(sensorCode)) {
                        camEnables[i] = true;
                        break;
                    }
                    i++;
                }
            }

            //计算压缩比
            double compressRate = 1.5;//无压缩则按1.5处理
            String P02 = "";

            ArrayList<Document> mission_params = (ArrayList<Document>) imageMisson.get("mission_params");

            for (Document mission_param : mission_params) {
                if (mission_param.getString("code").equals("P02")) {
                    P02 = mission_param.getString("value");
                }
            }

            if (P02.equals("")) {
                ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                for (Document default_mission_param : default_mission_params) {
                    if (default_mission_param.getString("code").equals("P02")) {
                        P02 = default_mission_param.getString("default_value");
                    }
                }
            }

            if (!P02.equals("")) {
                if (P02.equals("1"))
                    compressRate = 2.0;
            }
            //压缩比计算完成

            //计算高分相机帧频，按照a开机、b开机、ab均开机分别计算
            if (camEnables[0] && camEnables[1]) {//ab均开机
                String P08_AB = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P08_AB")) {
                        P08_AB = mission_param.getString("value");
                    }
                }

                if (P08_AB.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P08_AB")) {
                            P08_AB = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P08_AB.equals("1")) {
                    frameP[0] = 0.5;
                    frameP[1] = 0.5;
                } else {
                    frameP[0] = 1.0;
                    frameP[1] = 1.0;
                }
            } else if (camEnables[0] && !camEnables[1]) {//a开b关
                String P08_A = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P08_A")) {
                        P08_A = mission_param.getString("value");
                    }
                }

                if (P08_A.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P08_A")) {
                            P08_A = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P08_A.equals("1")) {
                    frameP[0] = 0.5;
                } else {
                    frameP[0] = 1.0;
                }

            } else if (!camEnables[0] && camEnables[1]) {
                String P08_B = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P08_B")) {
                        P08_B = mission_param.getString("value");
                    }
                }

                if (P08_B.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P08_B")) {
                            P08_B = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P08_B.equals("0")) {
                    frameP[1] = 1.0;
                } else if (P08_B.equals("2")) {
                    frameP[1] = 3.0;
                } else if (P08_B.equals("3")) {
                    frameP[1] = 4.0;
                } else if (P08_B.equals("4")) {
                    frameP[1] = 5.0;
                } else if (P08_B.equals("5")) {
                    frameP[1] = 0.5;
                } else if (P08_B.equals("6")) {
                    frameP[1] = 0.2;
                } else if (P08_B.equals("7")) {
                    frameP[1] = 0.1;
                } else {
                    frameP[1] = 1.0;
                }
            } else {//都关机
            }

            //计算多光谱相机帧频，按照a开机、b开机、ab均开机分别计算
            if (camEnables[2] && camEnables[3]) {
                String P09_AB = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P09_AB")) {
                        P09_AB = mission_param.getString("value");
                    }
                }

                if (P09_AB.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P08_AB")) {
                            P09_AB = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P09_AB.equals("1")) {
                    frameP[2] = 0.5;
                    frameP[3] = 0.5;
                } else {
                    frameP[2] = 1.0;
                    frameP[3] = 1.0;
                }
            } else if (camEnables[2] && !camEnables[3]) {
                String P09_A = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P09_A")) {
                        P09_A = mission_param.getString("value");
                    }
                }

                if (P09_A.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P09_A")) {
                            P09_A = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P09_A.equals("1")) {
                    frameP[2] = 0.5;
                } else if (P09_A.equals("2")) {
                    frameP[2] = 0.2;
                } else if (P09_A.equals("3")) {
                    frameP[2] = 0.1;
                } else {
                    frameP[2] = 1.0;
                }

            } else if (!camEnables[2] && camEnables[3]) {
                String P09_B = "";

                for (Document mission_param : mission_params) {
                    if (mission_param.getString("code").equals("P09_B")) {
                        P09_B = mission_param.getString("value");
                    }
                }

                if (P09_B.equals("")) {
                    ArrayList<Document> default_mission_params = (ArrayList<Document>) imageMisson.get("default_mission_params");

                    for (Document default_mission_param : default_mission_params) {
                        if (default_mission_param.getString("code").equals("P09_B")) {
                            P09_B = default_mission_param.getString("default_value");
                        }
                    }
                }

                if (P09_B.equals("1")) {
                    frameP[3] = 0.5;
                } else if (P09_B.equals("2")) {
                    frameP[3] = 0.2;
                } else if (P09_B.equals("3")) {
                    frameP[3] = 0.1;
                } else {
                    frameP[3] = 1.0;
                }
            } else {
            }
            //帧频计算完成

            for (int i = 0; i < 4; i++) {
                if (camEnables[i]) {
                    ret += v_records[i] * frameP[i] / compressRate;
                }
            }
            return ret;
        } catch (Exception e) {
            return 0.0;
        }
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

    //判定卫星是否处于地影区域
    //地影模型采用柱形地影模型
    private static boolean EarthEclipseStatus(double[] r_sun, double[] SatPosition_GEI) {
        double res = r_sun[0] * SatPosition_GEI[0] + r_sun[1] * SatPosition_GEI[1] + r_sun[2] * SatPosition_GEI[2];
        double rsu = sqrt(r_sun[0] * r_sun[0] + r_sun[1] * r_sun[1] + r_sun[2] * r_sun[2]);
        double rsa = sqrt(SatPosition_GEI[0] * SatPosition_GEI[0] + SatPosition_GEI[1] * SatPosition_GEI[1] + SatPosition_GEI[2] * SatPosition_GEI[2]);
        double theta = acos(res / (rsu * rsa));
        if (theta >= PI / 2 && sin(theta) * rsa <= Re) {
            return false;
        } else {
            return true;
        }
    }

    //太阳帆板充电电流
    private static double ChargeCurrentCalculation(double Time[], double[] r_sun, double[] SatPosition_GEI, double[] SatVelocity_GEI, double[] SatPosition_LLA, double[] Euler_BFToORF, double PowerEfficiency, double PowerGenerationMax, Boolean ESDStatus) {
        double[] r_SatSun = {SatPosition_GEI[0] - r_sun[0], SatPosition_GEI[1] - r_sun[1], SatPosition_GEI[2] - r_sun[2]};
        double[] r_SatSun_Axis = new double[3];
        if (ESDStatus) {
            //东南系
            double[] r_sun_sat_ECEF = new double[3];
            ICRSToECEF(Time, r_SatSun, r_sun_sat_ECEF);
            ECEFToESD(SatPosition_LLA, r_sun_sat_ECEF, r_SatSun_Axis);
        } else {
            //轨道系
            GEIToORF_Ellipse(SatPosition_GEI, SatVelocity_GEI, r_SatSun, r_SatSun_Axis);
        }

        double[] r_SatSun_Axis_n = new double[]{r_SatSun_Axis[0] / sqrt(r_SatSun_Axis[0] * r_SatSun_Axis[0] + r_SatSun_Axis[1] * r_SatSun_Axis[1] + r_SatSun_Axis[2] * r_SatSun_Axis[2]),
                r_SatSun_Axis[1] / sqrt(r_SatSun_Axis[0] * r_SatSun_Axis[0] + r_SatSun_Axis[1] * r_SatSun_Axis[1] + r_SatSun_Axis[2] * r_SatSun_Axis[2]),
                r_SatSun_Axis[2] / sqrt(r_SatSun_Axis[0] * r_SatSun_Axis[0] + r_SatSun_Axis[1] * r_SatSun_Axis[1] + r_SatSun_Axis[2] * r_SatSun_Axis[2])};
        double alpha = asin(r_SatSun_Axis_n[1]);
        double ChargeCurrent = PowerGenerationMax * abs(cos(alpha)) * PowerEfficiency;
        return ChargeCurrent;
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

    //惯性坐标系转到轨道坐标系
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

    //轨道坐标系转到本体坐标系
    //卫星本体系相对于轨道系的姿态采用欧拉角1-2-3转序表示
    private static void ORFToBF(double[] Euler_BFToORF, double[] Position_ORF, double[] Position_BF) {
        double[][] Rx = {{1, 0, 0},
                {0, cos(-Euler_BFToORF[0]), -sin(-Euler_BFToORF[0])},
                {0, sin(-Euler_BFToORF[0]), cos(-Euler_BFToORF[0])}};
        double[][] Ry = {{cos(-Euler_BFToORF[1]), 0, sin(-Euler_BFToORF[1])},
                {0, 1, 0},
                {-sin(-Euler_BFToORF[1]), 0, cos(-Euler_BFToORF[1])}};
        double[][] Rz = {{cos(-Euler_BFToORF[2]), -sin(-Euler_BFToORF[2]), 0},
                {sin(-Euler_BFToORF[2]), cos(-Euler_BFToORF[2]), 0},
                {0, 0, 1}};
        double[][] R = MatrixMultiplication(MatrixMultiplication(Rx, Ry), Rz);
        double[][] A = {{Position_ORF[0]}, {Position_ORF[1]}, {Position_ORF[2]}};
        double[][] B = MatrixMultiplication(R, A);

        Position_BF[0] = B[0][0];
        Position_BF[1] = B[1][0];
        Position_BF[2] = B[2][0];
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

    //地固坐标系到卫星东南地坐标系
    private static void ECEFToESD(double[] Satellite_LLA, double[] Target_ECEF, double[] Target_ESD) {
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

    //地固直角坐标系转换为地心地固坐标系
    private static void LLAToECEF(double Position_LLA[], double Position_ECEF[]) {
        double L = Position_LLA[0] * Math.PI / 180.0;
        double B = Position_LLA[1] * Math.PI / 180.0;
        double H = Position_LLA[2];

        Position_ECEF[0] = (Re + H) * Math.cos(B) * Math.cos(L);
        Position_ECEF[1] = (Re + H) * Math.cos(B) * Math.sin(L);
        Position_ECEF[2] = (Re + H) * Math.sin(B);
    }

    //惯性坐标系转到轨道坐标系，大椭圆轨道
    private static void GEIToORF_Ellipse(double SatPosition_GEI[], double SatVelocity_GEI[], double Position_GEI[], double Position_ORF[]) {
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
        double[][] pS_GEI = {{Position_GEI[0]}, {Position_GEI[1]}, {Position_GEI[2]}};
        double[][] pS_ORF = new double[3][1];
        pS_ORF = MatrixMultiplication(OR, pS_GEI);
        Position_ORF[0] = pS_ORF[0][0];
        Position_ORF[1] = pS_ORF[1][0];
        Position_ORF[2] = pS_ORF[2][0];
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
