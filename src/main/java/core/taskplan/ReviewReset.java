package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.mongo.DbDefine;
import common.mongo.MangoDBConnector;
import org.bson.Document;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static java.lang.Math.*;

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

    public static void ReviewResetII(Document Satllitejson, FindIterable<Document> Orbitjson, long OrbitDataCount, FindIterable<Document> Attitudejson, long AttitudeDataCount, ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson) {
        //数据初始话
        ImageMissionStatus = new int[(int) OrbitDataCount];
        StationMissionStatus = new int[(int) OrbitDataCount];
        for (int i = 0; i < (int) OrbitDataCount; i++) {
            ImageMissionStatus[i] = 0;
            StationMissionStatus[i] = 0;
        }
        PowerStatus = new double[(int) OrbitDataCount];
        DataStatus = new double[(int) OrbitDataCount];

        //读取卫星资源数据
        ArrayList<Document> properties = (ArrayList<Document>) Satllitejson.get("properties");
        for (Document document : properties) {
            if (document.getString("key").equals("power_efficiency")) {
                PowerEfficiency = Double.parseDouble(document.get("value").toString()) / 100;
            } else if (document.getString("key").equals("power_charge")) {
                PowerChargeEfficiency = Double.parseDouble(document.get("value").toString()) / 100;
            } else if (document.getString("key").equals("sailboard_current")) {
                PowerGenerationMax = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("power_capacity")) {
                PowerCapacity = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("average_power_standby")) {
                PowerAverage_Standby = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("average_power_image")) {
                PowerAverage_Image = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("average_power_playback")) {
                PowerAverage_Playback = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("record_play_power")) {
                PowerRecord_Play = Double.parseDouble(document.get("value").toString());
            } else if (document.getString("key").equals("roll_angle_max")) {
                Attitude_EulerMax[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("pitch_angle_max")) {
                Attitude_EulerMax[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("v_roll_angle")) {
                Attitude_AngVelMax[0] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else if (document.getString("key").equals("v_pitch_angle")) {
                Attitude_AngVelMax[1] = Double.parseDouble(document.getString("value")) * PI / 180.0;
            } else
                continue;
        }

        //轨道数据读入
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

            if(OrbitalDataNum >= OrbitDataCount)
                break;
        }

        //姿态数据读入
        Attitude_EulerAng = new double[(int) AttitudeDataCount][3];
        Attitude_AngVel = new double[(int) AttitudeDataCount][3];
        AttitudeDataNum = 0;
        for (Document document : Attitudejson) {
            Attitude_EulerAng[AttitudeDataNum][0] = Double.parseDouble(document.get("roll_angle").toString());
            Attitude_EulerAng[AttitudeDataNum][1] = Double.parseDouble(document.get("pitch_angle").toString());
            Attitude_EulerAng[AttitudeDataNum][2] = Double.parseDouble(document.get("yaw_angle").toString());
            Attitude_AngVel[AttitudeDataNum][0] = Double.parseDouble(document.get("V_roll_angle").toString());
            Attitude_AngVel[AttitudeDataNum][1] = Double.parseDouble(document.get("V_pitch_angle").toString());
            Attitude_AngVel[AttitudeDataNum][2] = Double.parseDouble(document.get("V_yaw_angle").toString());

            AttitudeDataNum = AttitudeDataNum + 1;

            if(AttitudeDataNum >= AttitudeDataCount)
                break;
        }

        //成像任务读入
        FalseMission = new int[ImageMissionjson.size()];
        int MissionNumber = 0;
        int[][] MissionStarEnd_Number = new int[ImageMissionjson.size()][2];
        int MissionChark_Number = 0;
        String[] MissionName = new String[ImageMissionjson.size()];
        try {
            for (Document document : ImageMissionjson) {
                if (document.getString("mission_state").equals("待执行")) {
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
                        MissionChark_Number = MissionChark_Number + 1;
                        for (int i = MissionStar_Number; i <= MissionEnd_Number; i++) {
                            ImageMissionStatus[i] = MissionNumber + 1;
                        }
                    }
                    ReviewResult.put(document.getString("mission_number"), true);
                    FalseMission[MissionNumber] = 1;
                } else {
                    ReviewResult.put(document.getString("mission_number"), false);
                    FalseMission[MissionNumber] = 0;
                }
                MissionName[MissionNumber] = document.getString("name");
                MissionNumber = MissionNumber + 1;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        //传输任务读入
        int StationMissionNumber=0;
        try {
            StationMissionNumber = 0;
            ArrayList<Document> TransmissionWindowArray = new ArrayList<>();
            TransmissionWindowArray = (ArrayList<Document>) TransmissionMissionJson.get("transmission_window");
            for (Document document : TransmissionWindowArray) {
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
                    StationMissionStatus[i] = MissionNumber;
                }
                StationMissionNumber = StationMissionNumber + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //将能量单位统一为瓦
        double BatteryCapacity = PowerCapacity * 42 * 60 * 60;      //蓄电池电量
        double MemorySpace = MemoryStorageCapacity;           //固存空间
        PowerStatus[0] = BatteryCapacity;
        DataStatus[0] = MemorySpace;
        FalseMissionNum = 0;
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
                ChargeCurrent = ChargeCurrentCalculation(r_sun, Orbital_SatPosition[i], Orbital_SatVelocity[i], Attitude_EulerAng[i], PowerEfficiency, PowerGenerationMax);
            } else {
                //处于阴影区
                ChargeCurrent = 0;
            }

            //能量复核
            if (ImageMissionStatus[i] != 0 && StationMissionStatus[i] != 0) {
                //边记边放
                double SailBoard = ChargeCurrent * PowerGenerationVoltage * PowerEfficiency;
                if (SailBoard >= PowerRecord_Play) {
                    //帆板供电大于载荷需求
                    if (SailBoard - PowerRecord_Play > PowerChargeMax * PowerBatteryGenerVoltage) {
                        //帆板供电大于最大充电电流
                        PowerStatus[j] = PowerStatus[i] + PowerChargeMax * PowerBatteryGenerVoltage * PowerChargeEfficiency;
                    } else {
                        //帆板供电小于最大充电电流
                        PowerStatus[j] = (SailBoard - PowerRecord_Play) * PowerChargeEfficiency;
                    }
                } else {
                    //帆板供电小于载荷需求
                    PowerStatus[j] = PowerStatus[i] - PowerRecord_Play / PowerChargeOutEfficiency;
                }
                //数据
                DataStatus[j] = DataStatus[i] - MemoryRecord + Memoryplayback;
            } else if (ImageMissionStatus[i] != 0 && StationMissionStatus[i] == 0) {
                //成像模式
                double SailBoard = ChargeCurrent * PowerGenerationVoltage * PowerEfficiency;
                if (SailBoard >= PowerAverage_Image) {
                    //帆板供电大于载荷需求
                    if (SailBoard - PowerAverage_Image > PowerChargeMax * PowerBatteryGenerVoltage) {
                        //帆板供电大于最大充电电流
                        PowerStatus[j] = PowerStatus[i] + PowerChargeMax * PowerBatteryGenerVoltage * PowerChargeEfficiency;
                    } else {
                        //帆板供电小于最大充电电流
                        PowerStatus[j] = (SailBoard - PowerAverage_Image) * PowerChargeEfficiency;
                    }
                } else {
                    //帆板供电小于载荷需求
                    PowerStatus[j] = PowerStatus[i] - PowerAverage_Image / PowerChargeOutEfficiency;
                }
                //数据
                DataStatus[j] = DataStatus[i] - MemoryRecord;
            } else if (ImageMissionStatus[i] == 0 && StationMissionStatus[i] != 0) {
                //回放模式
                double SailBoard = ChargeCurrent * PowerGenerationVoltage * PowerEfficiency;
                if (SailBoard >= PowerAverage_Playback) {
                    //帆板供电大于载荷需求
                    if (SailBoard - PowerAverage_Playback > PowerChargeMax * PowerBatteryGenerVoltage) {
                        //帆板供电大于最大充电电流
                        PowerStatus[j] = PowerStatus[i] + PowerChargeMax * PowerBatteryGenerVoltage * PowerChargeEfficiency;
                    } else {
                        //帆板供电小于最大充电电流
                        PowerStatus[j] = (SailBoard - PowerAverage_Playback) * PowerChargeEfficiency;
                    }
                } else {
                    //帆板供电小于载荷需求
                    PowerStatus[j] = PowerStatus[i] - PowerAverage_Playback / PowerChargeOutEfficiency;
                }
                //数据
                DataStatus[j] = DataStatus[i] + Memoryplayback;
            } else {
                //待机模式
                double SailBoard = ChargeCurrent * PowerGenerationVoltage * PowerEfficiency;
                if (SailBoard >= PowerAverage_Standby) {
                    //帆板供电大于载荷需求
                    if (SailBoard - PowerAverage_Standby > PowerChargeMax * PowerBatteryGenerVoltage) {
                        //帆板供电大于最大充电电流
                        PowerStatus[j] = PowerStatus[i] + PowerChargeMax * PowerBatteryGenerVoltage * PowerChargeEfficiency;
                    } else {
                        //帆板供电小于最大充电电流
                        PowerStatus[j] = (SailBoard - PowerAverage_Standby) * PowerChargeEfficiency;
                    }
                } else {
                    //帆板供电小于载荷需求
                    PowerStatus[j] = PowerStatus[i] - PowerAverage_Standby / PowerChargeOutEfficiency;
                }
                //数据
                DataStatus[j] = DataStatus[i];
            }

            if (PowerStatus[j] > PowerCapacity * 42 * 60 * 60) {
                PowerStatus[j] = PowerCapacity * 42 * 60 * 60;
            }
            PowerStatus[j] = PowerCapacity * 42 * 60 * 60;
            if (DataStatus[j] > MemoryStorageCapacity) {
                DataStatus[j] = MemoryStorageCapacity;
            }
            DataStatus[j] = MemoryStorageCapacity;

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
            double PowerCapacityMin = 0.2 * PowerCapacity * 42 * 60 * 60;
            double MemoryStorageMin = 0.05 * MemoryStorageCapacity;
            if (PowerStatus[j] < PowerCapacityMin) {
                for (int k = j; k >= 0; k--) {
                    if (ImageMissionStatus[k] != 0) {
                        MissionFalse = ImageMissionStatus[k];
                        FalseMission[FalseMissionNum] = MissionFalse;
                        FalseMissionNum = FalseMissionNum + 1;
                        break;
                    }
                }
                //任务删除
                for (int k = MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][0]; k < MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][1]; k++) {
                    ImageMissionStatus[k] = 0;
                }
                j = MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][0];
                FalseMission[FalseMission[FalseMissionNum] - 1] = 2;
            }
            if (DataStatus[j] < MemoryStorageMin) {
                for (int k = j; k >= 0; k--) {
                    if (ImageMissionStatus[k] != 0) {
                        MissionFalse = ImageMissionStatus[k];
                        FalseMission[FalseMissionNum] = MissionFalse;
                        FalseMissionNum = FalseMissionNum + 1;
                        break;
                    }
                }
                //删除任务
                for (int k = MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][0]; k < MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][1]; k++) {
                    ImageMissionStatus[k] = 0;
                }
                j = MissionStarEnd_Number[FalseMission[FalseMissionNum] - 1][0];
                FalseMission[FalseMission[FalseMissionNum] - 1] = 3;
            }


        }

        //数据传出
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DbDefine.DB_NAME);
        String transmission_number = "tn_" + Instant.now().toEpochMilli();
        for (int i = 0; i < MissionNumber; i++) {
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
            }
        }
        mongoClient.close();
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
    private static double ChargeCurrentCalculation(double[] r_sun, double[] SatPosition_GEI, double[] SatVelocity_GEI, double[] Euler_BFToORF, double PowerEfficiency, double PowerGenerationMax) {
        double[] r_SatSun = {SatPosition_GEI[0] - r_sun[0], SatPosition_GEI[1] - r_sun[1], SatPosition_GEI[2] - r_sun[2]};
        double[] r_SatSunORF = new double[3];
        double[] r_SatSunBF = new double[3];
        GEIToORF(SatPosition_GEI, SatVelocity_GEI, r_SatSun, r_SatSunORF);
        ORFToBF(Euler_BFToORF, r_SatSunORF, r_SatSunBF);
        double alpha = atan2(r_SatSunBF[1], r_SatSunBF[2]);
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
