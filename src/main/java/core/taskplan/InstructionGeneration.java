package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.FilePathUtil;
import common.mongo.MangoDBConnector;
import core.taskplan.InstructionSequenceTime.SequenceID;
import core.taskplan.InstructionSequenceTime.SequenceTime;
import core.taskplan.InstructionSequenceTime.TimeMap;
import core.taskplan.InstructionSequenceTime.TimeVariable;
import org.bson.Document;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.lang.Math.*;

public class InstructionGeneration {

    private static double[] ZeroTime = {2018, 1, 1, 0, 0, 0};//参考时间
    private static Instant ZeroTimeIns = Instant.parse("2018-01-01T00:00:00.00Z");
    private static double TaskSelectTime = 3600;//秒
    private static int TimeZone = -8;                     //北京时区到世界时-8
    private static double Re = 6371393;                  //地球半径，单位为：米

    public static void InstructionGenerationII(ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson, ArrayList<Document> StationMissionjson, FindIterable<Document> Attitudejson, long AttitudeDataCount, String FilePath, Document CamStatus) {
        //序列时间设置
        TimeVariable timeVariable = new TimeVariable();
        timeVariable.TSC = 6 + 0.25 + 0.125 + 0.125 + 0.125 + 0.125 + 240 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 +
                0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 +
                0.125 + 8 + 0.125 + 0.125 + 0.125 + 8;
        timeVariable.TGF = 600 + 600 + 2 + 2 + 20 + 20 + 2 + 2 + 2 + 2 + 2 + 2 + 90 + 70 + 90 + 600 + 90 + 70 + 90 + 90 + 70 + 90 + 600 + 90 + 70 + 90 + 10 + 10 + 10;
        timeVariable.TGF2 = 4;
        timeVariable.TDG1 = 600 + 2 + 20 + 2 + 20 + 2 + 50 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 +
                2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 +
                2 + 2 + 2 + 32 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 +
                32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 +
                90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 10 + 10;
        timeVariable.TDG2 = 2 + 50 + 2 + 42 + 2 + 2 + 2 + 2 + 2 + 2;
        timeVariable.T4401 = 720;
        timeVariable.P24 = 720;
        timeVariable.P29 = 100;
        timeVariable.TTCAG04 = 6 + 0.25 + 0.125 + 0.125 + 0.125 + 0.125 + 1 + 1 + 1 + 1 + 1 + 1 + 1;

        HashMap<String, SequenceTime> timeHashMap = new TimeMap().timeHashMap();

        //String test01="TCKG01";
        //System.out.println(timeHashMap.get(test01).ExecutionTime(timeVariable,"成像任务"));

        //读入模板
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //指令块模板
        MongoCollection<Document> Data_TaskInstructionjson = mongoDatabase.getCollection("task_instruction");
        FindIterable<Document> D_TaskInstructionjson = Data_TaskInstructionjson.find();
        ArrayList<Document> TaskInstructionjson = new ArrayList<>();
        for (Document document : D_TaskInstructionjson) {
            TaskInstructionjson.add(document);
        }
        //读入指令序列模板
        MongoCollection<Document> Data_SequenceInstructionjson = mongoDatabase.getCollection("sequence_instruction");
        FindIterable<Document> D_SequenceInstructionjson = Data_SequenceInstructionjson.find();
        ArrayList<Document> SequenceInstructionjson = new ArrayList<>();
        for (Document document : D_SequenceInstructionjson) {
            SequenceInstructionjson.add(document);
        }
        //读入指令码模板
        MongoCollection<Document> Data_MetaInstrunctionjson = mongoDatabase.getCollection("meta_instrunction");
        FindIterable<Document> D_MetaInstrunctionjson = Data_MetaInstrunctionjson.find();
        ArrayList<Document> MetaInstrunctionjson = new ArrayList<>();
        for (Document document : D_MetaInstrunctionjson) {
            MetaInstrunctionjson.add(document);
        }
        //读入基准时间
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first = sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");
        String Cam_force_Powoff = "false";
        Instant zerostart = ZeroTimeIns;
        String LoadNumberIni = "0";
        for (Document document : properties) {
            if (document.getString("key").equals("t0")) {
                zerostart = document.getDate("value").toInstant();
                LocalDateTime zerostart0 = LocalDateTime.ofInstant(zerostart, ZoneOffset.UTC);
                ZeroTime[0] = zerostart0.getYear();
                ZeroTime[1] = zerostart0.getMonthValue();
                ZeroTime[2] = zerostart0.getDayOfMonth();
                ZeroTime[3] = zerostart0.getHour();
                ZeroTime[4] = zerostart0.getMinute();
                ZeroTime[5] = zerostart0.getSecond();
            } else if (document.getString("key").equals("cam_force_powoff")) {
                Cam_force_Powoff = document.get("value").toString();
            } else if (document.getString("key").equals("default_cam")) {
                LoadNumberIni = document.get("value").toString();
            }
        }

        //成像任务读入
        //任务读入
        ArrayList<String> MissionNumberArray = new ArrayList<>();
        ArrayList<Object> MissionInstructionArray = new ArrayList<>();
        ArrayList<Object> MissionInstructionAfterArray = new ArrayList<>();
        ArrayList<Boolean> MissionStateArray = new ArrayList<>();
        ArrayList<String> MissionLoadNumberArray = new ArrayList<>();
        ArrayList<Date> MissionStarTimeArray = new ArrayList<>();
        ArrayList<Date> MissionEndTimeArray = new ArrayList<>();
        ArrayList<String> MissionWorkModel = new ArrayList<>();
        ArrayList<String> MissionImageModel = new ArrayList<>();
        ArrayList<ArrayList<double[]>> MissionTargetAreaList = new ArrayList<>();
        ArrayList<Object> MissionInstructionDefautArray = new ArrayList<>();
        ArrayList<int[]> MissionLoadTypeList = new ArrayList<>();
        int MissionNum = 0;
        if (ImageMissionjson != null) {
            for (Document document : ImageMissionjson) {
                try {
                    if (document.containsKey("mission_params")) {
                        MissionInstructionArray.add(document.get("mission_params"));
                        MissionInstructionDefautArray.add(document.get("default_mission_params"));
                        String MissionNumberArray_i = document.get("mission_number").toString();
                        MissionNumberArray.add(MissionNumberArray_i);
                        ArrayList<Document> ImageWindow = (ArrayList<Document>) document.get("image_window");
                        if (ImageWindow != null) {
                            for (Document document1 : ImageWindow) {
                                if (document1.containsKey("load_number") && document1.get("load_number") != null && !document1.get("load_number").equals("")) {
                                    int loadNumber = Integer.parseInt(document1.get("load_number").toString()) - 1;
                                    MissionLoadNumberArray.add(String.valueOf(loadNumber));
                                } else {
                                    MissionLoadNumberArray.add(LoadNumberIni);
                                }
                                MissionStarTimeArray.add((Date) document1.get("start_time"));
                                MissionEndTimeArray.add((Date) document1.get("end_time"));
                                break;
                            }
                        } else {
                            MissionLoadNumberArray.add("1");
                            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
                            String stringTime = (int) ZeroTime[0] + "-" + (int) ZeroTime[1] + "-" + (int) ZeroTime[2];
                            Date date = dateformat.parse(stringTime);
                            MissionStarTimeArray.add(date);
                            MissionEndTimeArray.add(date);
                        }
                        if (document.get("mission_state").equals("待执行")) {
                            MissionStateArray.add(true);
                        } else {
                            MissionStateArray.add(false);
                        }
                        MissionWorkModel.add(document.get("work_mode").toString());
                        MissionImageModel.add(document.get("image_mode").toString());

                        //读取目标区域
                        if (document.containsKey("image_region") && document.get("image_region") != null && !document.get("image_region").equals("")) {
                            Document target_region = (Document) document.get("image_region");
                            ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                            MissionTargetArea_iList = GetRegionPoint(target_region);
                            MissionTargetAreaList.add(MissionNum, MissionTargetArea_iList);
                        } else {
                            ArrayList<double[]> MissionTargetArea_iList = new ArrayList<double[]>();
                            MissionTargetArea_iList.add(new double[]{0, 0});
                            MissionTargetAreaList.add(MissionNum, MissionTargetArea_iList);
                        }
                        int[] MissionLoadType_iList = new int[]{0, 0, 0, 0};
                        Boolean MissionLoadTypeFlag = true;
                        if (document.containsKey("mission_params") && document.get("mission_params") != null) {
                            ArrayList<Document> MissionParamsTemp = (ArrayList<Document>) document.get("mission_params");
                            for (Document document1 : MissionParamsTemp) {
                                if (document1.containsKey("code") && document1.get("code") != null && document1.get("code").equals("P_SATELLITE_USE")) {
                                    if (document1.containsKey("value") && document1.get("value") != null && !document1.get("value").equals("")) {
                                        if (document1.get("value").equals("1")) {
                                            MissionLoadType_iList[0] = 1;
                                            MissionLoadTypeFlag = false;
                                        } else if (document1.get("value").equals("2")) {
                                            MissionLoadType_iList[1] = 1;
                                            MissionLoadTypeFlag = false;
                                        } else if (document1.get("value").equals("3")) {
                                            MissionLoadType_iList[2] = 1;
                                            MissionLoadTypeFlag = false;
                                        } else if (document1.get("value").equals("4")) {
                                            MissionLoadType_iList[3] = 1;
                                            MissionLoadTypeFlag = false;
                                        }
                                    }
                                }
                            }
                        }
                        if (MissionLoadTypeFlag) {
                            if (document.containsKey("default_mission_params") && document.get("default_mission_params") != null) {
                                ArrayList<Document> MissionParamsTemp = (ArrayList<Document>) document.get("default_mission_params");
                                for (Document document1 : MissionParamsTemp) {
                                    if (document1.containsKey("code") && document1.get("code") != null && document1.get("code").equals("P_SATELLITE_USE")) {
                                        if (document1.containsKey("default_value") && document1.get("default_value") != null && !document1.get("default_value").equals("")) {
                                            if (document1.get("default_value").equals("1")) {
                                                MissionLoadType_iList[0] = 1;
                                            } else if (document1.get("default_value").equals("2")) {
                                                MissionLoadType_iList[1] = 1;
                                            } else if (document1.get("default_value").equals("3")) {
                                                MissionLoadType_iList[2] = 1;
                                            } else if (document1.get("default_value").equals("4")) {
                                                MissionLoadType_iList[3] = 1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        MissionLoadTypeList.add(MissionNum, MissionLoadType_iList);

                        MissionNum = MissionNum + 1;
                    } else {
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //判断任务所要匹配的指令块模板
        Map<Integer, ArrayList<String>> MissionTaskModel = new HashMap<>();
        if (Cam_force_Powoff.equals("false")) {
            if (MissionLoadNumberArray.size() > 1) {
                for (int i = 0; i < 4; i++) {
                    ArrayList<Integer> sortResult = new ArrayList<>();
                    for (int j = 0; j < MissionStarTimeArray.size(); j++) {
                        if (MissionLoadTypeList.get(j)[i] == 1) {
                            sortResult.add(j);
                        }
                    }
                    if (sortResult.size() > 1) {
                        for (int j = 0; j < sortResult.size() - 1; j++) {
                            for (int k = 0; k < sortResult.size() - 1 - j; k++) {
                                if (MissionStarTimeArray.get(sortResult.get(k)).after(MissionStarTimeArray.get(sortResult.get(k + 1)))) {
                                    Integer temp = sortResult.get(k);
                                    Integer tempJ = sortResult.get(k + 1);
                                    sortResult.set(k, tempJ);
                                    sortResult.set(k + 1, temp);
                                }
                            }
                        }
                        for (int j = 0; j < sortResult.size(); j++) {
                            if (j == 0) {
                                ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                                double error = MissionStarTimeArray.get(sortResult.get(j + 1)).getTime() / 1000 - MissionEndTimeArray.get(sortResult.get(j)).getTime() / 1000;
                                String CamGFAStatus_num = "CamGFAStatus";
                                if (i == 0) {
                                    CamGFAStatus_num = "CamGFAStatus";
                                } else if (i == 1) {
                                    CamGFAStatus_num = "CamGFBStatus";
                                } else if (i == 2) {
                                    CamGFAStatus_num = "CamDGAStatus";
                                } else if (i == 3) {
                                    CamGFAStatus_num = "CamDGBStatus";
                                }
                                if (error < TaskSelectTime) {
                                    if (CamStatus.get(CamGFAStatus_num).toString().equals("ON")) {
                                        MissionTaskModelChild.add("TASK10");//任务恢复
                                        MissionTaskModelChild.add("TASK09");//任务暂停
                                    } else {
                                        MissionTaskModelChild.add("TASK02");//开机
                                        MissionTaskModelChild.add("TASK09");//任务暂停
                                    }
                                } else {
                                    if (CamStatus.get(CamGFAStatus_num).toString().equals("ON")) {
                                        MissionTaskModelChild.add("TASK10");//任务恢复
                                        MissionTaskModelChild.add("TASK03");//关机
                                    } else {
                                        MissionTaskModelChild.add("TASK01");//常规成像
                                    }
                                }
                                MissionTaskModel.put(sortResult.get(j), MissionTaskModelChild);
                            } else if (j == sortResult.size() - 1) {
                                ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                                double error = MissionStarTimeArray.get(sortResult.get(j)).getTime() / 1000 - MissionEndTimeArray.get(sortResult.get(j - 1)).getTime() / 1000;
                                if (error < TaskSelectTime) {
                                    MissionTaskModelChild.add("TASK10");//任务恢复
                                    MissionTaskModelChild.add("TASK03");//关机
                                } else {
                                    MissionTaskModelChild.add("TASK01");//常规成像
                                }
                                MissionTaskModel.put(sortResult.get(j), MissionTaskModelChild);
                            } else {
                                ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                                double error = MissionStarTimeArray.get(sortResult.get(j)).getTime() / 1000 - MissionEndTimeArray.get(sortResult.get(j - 1)).getTime() / 1000;
                                double error2 = MissionStarTimeArray.get(sortResult.get(j + 1)).getTime() / 1000 - MissionEndTimeArray.get(sortResult.get(j)).getTime() / 1000;
                                if (error < TaskSelectTime && error2 < TaskSelectTime) {
                                    MissionTaskModelChild.add("TASK10");//任务恢复
                                    MissionTaskModelChild.add("TASK09");//任务暂停
                                } else if (error >= TaskSelectTime && error2 < TaskSelectTime) {
                                    MissionTaskModelChild.add("TASK02");//开机
                                    MissionTaskModelChild.add("TASK09");//任务暂停
                                } else if (error < TaskSelectTime && error2 >= TaskSelectTime) {
                                    MissionTaskModelChild.add("TASK10");//任务恢复
                                    MissionTaskModelChild.add("TASK03");//关机
                                } else {
                                    MissionTaskModelChild.add("TASK01");//常规成像
                                }
                                MissionTaskModel.put(sortResult.get(j), MissionTaskModelChild);
                            }
                        }
                    } else if (sortResult.size() == 1) {
                        ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                        String CamGFAStatus_num = "CamGFAStatus";
                        if (i == 0) {
                            CamGFAStatus_num = "CamGFAStatus";
                        } else if (i == 1) {
                            CamGFAStatus_num = "CamGFBStatus";
                        } else if (i == 2) {
                            CamGFAStatus_num = "CamDGAStatus";
                        } else if (i == 3) {
                            CamGFAStatus_num = "CamDGBStatus";
                        }
                        if (CamStatus.get(CamGFAStatus_num).toString().equals("ON")) {
                            MissionTaskModelChild.add("TASK10");//任务恢复
                            MissionTaskModelChild.add("TASK03");//关机
                        } else {
                            MissionTaskModelChild.add("TASK01");//常规成像
                        }
                        MissionTaskModel.put(sortResult.get(0), MissionTaskModelChild);
                    }
                }
            } else {
                ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                if (MissionLoadTypeList.get(0)[0] == 1) {
                    if (CamStatus.get("CamGFAStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK03");//关机
                    } else {
                        MissionTaskModelChild.add("TASK01");//常规成像
                    }
                } else if (MissionLoadTypeList.get(0)[1] == 1) {
                    if (CamStatus.get("CamGFBStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK03");//关机
                    } else {
                        MissionTaskModelChild.add("TASK01");//常规成像
                    }
                } else if (MissionLoadTypeList.get(0)[2] == 1) {
                    if (CamStatus.get("CamDGAStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK03");//关机
                    } else {
                        MissionTaskModelChild.add("TASK01");//常规成像
                    }
                } else if (MissionLoadTypeList.get(0)[3] == 1) {
                    if (CamStatus.get("CamDGBStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK03");//关机
                    } else {
                        MissionTaskModelChild.add("TASK01");//常规成像
                    }
                }
                MissionTaskModel.put(0, MissionTaskModelChild);
            }
        } else {
            for (int i = 0; i < MissionStarTimeArray.size(); i++) {
                int j = i;
                ArrayList<String> MissionTaskModelChild = new ArrayList<>();
                if (MissionLoadTypeList.get(j)[0] == 1) {
                    if (CamStatus.get("CamGFAStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    } else {
                        MissionTaskModelChild.add("TASK02");//开机不关机
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    }
                } else if (MissionLoadTypeList.get(j)[1] == 1) {
                    if (CamStatus.get("CamGFBStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    } else {
                        MissionTaskModelChild.add("TASK02");//开机不关机
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    }
                } else if (MissionLoadTypeList.get(j)[2] == 1) {
                    if (CamStatus.get("CamDGAStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    } else {
                        MissionTaskModelChild.add("TASK02");//开机不关机
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    }
                } else if (MissionLoadTypeList.get(j)[3] == 1) {
                    if (CamStatus.get("CamDGBStatus").toString().equals("ON")) {
                        MissionTaskModelChild.add("TASK10");//任务恢复
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    } else {
                        MissionTaskModelChild.add("TASK02");//开机不关机
                        MissionTaskModelChild.add("TASK09");//任务暂停
                    }
                }
                MissionTaskModel.put(j, MissionTaskModelChild);
            }
        }

        //传输任务，回放任务
        if (TransmissionMissionJson != null && StationMissionjson.size() > 0) {
            Boolean HuifangFlag = InstructionStationMission(TransmissionMissionJson, StationMissionjson, FilePath);
        }

        //姿态数据
        //轨道数据
        ArrayList<Date> AttitudeTimeDateList = new ArrayList<>();
        ArrayList<double[]> AttitudeTimeList = new ArrayList<>();
        ArrayList<double[]> AttitudeList = new ArrayList<>();
        int AttitudeDataNum = 0;
        try {
            for (Document document : Attitudejson) {
                try {
                    //读取姿态时间戳
                    Date time_point = document.getDate("time_point");
                    AttitudeTimeDateList.add(AttitudeDataNum, time_point);
                    double[] OrbitTime_iList = new double[6];
                    OrbitTime_iList = DateToDouble(time_point);
                    AttitudeTimeList.add(AttitudeDataNum, OrbitTime_iList);
                    //读取本体系相对于轨道系的312姿态
                    Document Attitude_OrbitReference_312 = (Document) document.get("Attitude_EastSouthDown_321");
                    double[] SatPositionGEI = new double[3];
                    SatPositionGEI[0] = Double.parseDouble(Attitude_OrbitReference_312.get("roll_angle").toString());
                    SatPositionGEI[1] = Double.parseDouble(Attitude_OrbitReference_312.get("pitch_angle").toString());
                    SatPositionGEI[2] = Double.parseDouble(Attitude_OrbitReference_312.get("yaw_angle").toString());
                    AttitudeList.add(AttitudeDataNum, SatPositionGEI);

                    //姿态个数加1
                    AttitudeDataNum = AttitudeDataNum + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //指令生成
        ArrayList<ArrayList<byte[]>> InstructionArray = new ArrayList<>();
        ArrayList<Integer> ZhiLingIDArray = new ArrayList<>();
        ArrayList<ArrayList<String>> MissionInstructionCode = new ArrayList<>();
        ArrayList<ArrayList<Integer>> MissionInstructionId = new ArrayList<>();
        ArrayList<ArrayList<Date>> MissionInstructionTime = new ArrayList<>();
        ArrayList<ArrayList<String>> MissionInstructionHex = new ArrayList<>();

        Map<Integer, Map<String, String>> insTimeMap = new HashMap<>();

        for (int i = 0; i < MissionNum; i++) {
            //姿态机动角
            double[] FYGDAttitude = new double[]{0, 0};
            //添加指令参数
            ArrayList<Document> MissionInstructionArrayChildTemp = (ArrayList<Document>) MissionInstructionArray.get(i);
            ArrayList<Document> MissionInstructionArrayChild = new ArrayList<>();
            for (Document document : MissionInstructionArrayChildTemp) {
                MissionInstructionArrayChild.add(document);
            }
            ArrayList<Document> MissionInstructionDefautArrayChild = (ArrayList<Document>) MissionInstructionDefautArray.get(i);
            ArrayList<Document> MissionInstructionAfterPara = new ArrayList<>();
            //P07
            boolean ChildIdFlagP07 = true;
            boolean ChildIdFlagP19 = true;
            boolean ChildIdFlagP20 = true;
            boolean ChildIdFlagP21 = true;
            boolean ChildIdFlagP22 = true;
            boolean ChildIdFlagP23 = true;
            boolean ChildIdFlagP24 = true;
            boolean ChildIdFlagP25 = true;
            boolean ChildIdFlagP26 = true;
            boolean ChildIdFlagP27 = true;
            boolean ChildIdFlagP28 = true;
            boolean ChildIdFlagP29 = true;
            boolean ChildIdFlagP32_1 = true;
            boolean ChildIdFlagP32_2 = true;
            for (Document TaskParams : MissionInstructionArrayChild) {
                Document MissionInstructionAfterParaChild = new Document();
                if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P07_1")) {
                    TaskParams.append("value", MissionLoadNumberArray.get(i));
                    ChildIdFlagP07 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P07_1");
                    MissionInstructionAfterParaChild.append("value", MissionLoadNumberArray.get(i));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P19")) {
                    if (MissionImageModel.get(i).equals("凝视")) {
                        TaskParams.append("value", "2");
                        MissionInstructionAfterParaChild.clear();
                        MissionInstructionAfterParaChild.append("code", "P19");
                        MissionInstructionAfterParaChild.append("value", "凝视");
                        MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                    } else {
                        TaskParams.append("value", "1");
                        MissionInstructionAfterParaChild.clear();
                        MissionInstructionAfterParaChild.append("code", "P19");
                        MissionInstructionAfterParaChild.append("value", "常规");
                        MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                    }
                    ChildIdFlagP19 = false;
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P20")) {
                    int CBTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", CBTimeTemp).toUpperCase());
                    ChildIdFlagP20 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P20");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(CBTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P21")) {
                    float FYAttitudeTemp = 0;
                    for (int j = 0; j < AttitudeTimeDateList.size(); j++) {
                        if (Duration.between(MissionStarTimeArray.get(i).toInstant(), AttitudeTimeDateList.get(j).toInstant()).getSeconds() < 0.25) {
                            FYAttitudeTemp = (float) AttitudeList.get(j)[1];
                            break;
                        }
                    }
                    FYGDAttitude[1] = FYAttitudeTemp;
                    String strtemp = Integer.toHexString(Float.floatToIntBits(FYAttitudeTemp));
                    if (strtemp.length() < 8) {
                        for (int j = strtemp.length() + 1; j <= 8; j++) {
                            strtemp = "0" + strtemp;
                        }
                    }
                    TaskParams.append("value", strtemp.toUpperCase());
                    ChildIdFlagP21 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P21");
                    MissionInstructionAfterParaChild.append("value", Float.toString(FYAttitudeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P22")) {
                    float GDAttitudeTemp = 0;
                    for (int j = 0; j < AttitudeTimeDateList.size(); j++) {
                        if (Duration.between(MissionStarTimeArray.get(i).toInstant(), AttitudeTimeDateList.get(j).toInstant()).getSeconds() < 0.25) {
                            GDAttitudeTemp = (float) AttitudeList.get(j)[0];
                            break;
                        }
                    }
                    FYGDAttitude[0] = GDAttitudeTemp;
                    String strtemp = Integer.toHexString(Float.floatToIntBits(GDAttitudeTemp));
                    if (strtemp.length() < 8) {
                        for (int j = strtemp.length() + 1; j <= 8; j++) {
                            strtemp = "0" + strtemp;
                        }
                    }
                    TaskParams.append("value", strtemp.toUpperCase());
                    ChildIdFlagP22 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P22");
                    MissionInstructionAfterParaChild.append("value", Float.toString(GDAttitudeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P23")) {
                    int NSTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP23 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P23");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P24")) {
                    int NSTimeTemp = 720;
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP24 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P24");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P25")) {
                    int NSTimeTemp = (int) (Duration.between(MissionStarTimeArray.get(i).toInstant(), MissionEndTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP25 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P25");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P26")) {
                    float lonTemp = 0;
                    float latTemp = 0;
                    float HTemp = 0;
                    ArrayList<double[]> MissionTargetArea_iList = MissionTargetAreaList.get(i);
                    for (double[] TargetTemp : MissionTargetArea_iList) {
                        lonTemp = (float) (lonTemp + TargetTemp[0]);
                        latTemp = (float) (latTemp + TargetTemp[1]);
                    }
                    lonTemp = lonTemp / MissionTargetArea_iList.size();
                    latTemp = latTemp / MissionTargetArea_iList.size();
                    String lonstr = Integer.toHexString(Float.floatToIntBits(lonTemp)).toUpperCase();
                    String latstr = Integer.toHexString(Float.floatToIntBits(latTemp)).toUpperCase();
                    String Hstr = Integer.toHexString(Float.floatToIntBits(HTemp)).toUpperCase();
                    if (lonstr.length() < 8) {
                        for (int j = lonstr.length() + 1; j <= 8; j++) {
                            lonstr = "0" + lonstr;
                        }
                    }
                    if (latstr.length() < 8) {
                        for (int j = latstr.length() + 1; j <= 8; j++) {
                            latstr = "0" + latstr;
                        }
                    }
                    if (Hstr.length() < 8) {
                        for (int j = Hstr.length() + 1; j <= 8; j++) {
                            Hstr = "0" + Hstr;
                        }
                    }
                    TaskParams.append("value", (lonstr + latstr + Hstr).toUpperCase());
                    ChildIdFlagP26 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P26");
                    MissionInstructionAfterParaChild.append("value", Float.toString(lonTemp) + "," + Float.toString(latTemp) + "," + Float.toString(HTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P27")) {
                    float AccAngTemp = (float) 0.08;
                    String strtemp = Integer.toHexString(Float.floatToIntBits(AccAngTemp));
                    if (strtemp.length() < 8) {
                        for (int j = strtemp.length() + 1; j <= 8; j++) {
                            strtemp = "0" + strtemp;
                        }
                    }
                    TaskParams.append("value", strtemp.toUpperCase());
                    ChildIdFlagP27 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P27");
                    MissionInstructionAfterParaChild.append("value", Float.toString(AccAngTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P28")) {
                    float VelAngTemp = (float) 0.12;
                    String strtemp = Integer.toHexString(Float.floatToIntBits(VelAngTemp));
                    if (strtemp.length() < 8) {
                        for (int j = strtemp.length() + 1; j <= 8; j++) {
                            strtemp = "0" + strtemp;
                        }
                    }
                    TaskParams.append("value", strtemp.toUpperCase());
                    ChildIdFlagP28 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P28");
                    MissionInstructionAfterParaChild.append("value", Float.toString(VelAngTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P29")) {
                    int NSTimeTemp = 100;
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP29 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P29");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P32_1")) {
                    int NSTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP32_1 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P32_1");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P32_2")) {
                    int NSTimeTemp = (int) (Duration.between(zerostart, MissionEndTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                    ChildIdFlagP32_2 = false;
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P32_2");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                }
            }
            if (ChildIdFlagP07) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P07_1");
                TaskParamsTemp.append("value", MissionLoadNumberArray.get(i));
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.clear();
                MissionInstructionAfterParaChild.append("code", "P07_1");
                MissionInstructionAfterParaChild.append("value", MissionLoadNumberArray.get(i));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP19) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P19");
                if (MissionImageModel.get(i).equals("凝视")) {
                    TaskParamsTemp.append("value", "2");
                    Document MissionInstructionAfterParaChild = new Document();
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P19");
                    MissionInstructionAfterParaChild.append("value", "凝视");
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else {
                    TaskParamsTemp.append("value", "1");
                    Document MissionInstructionAfterParaChild = new Document();
                    MissionInstructionAfterParaChild.clear();
                    MissionInstructionAfterParaChild.append("code", "P19");
                    MissionInstructionAfterParaChild.append("value", "常规");
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                }
                MissionInstructionArrayChild.add(TaskParamsTemp);
            }
            if (ChildIdFlagP20) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P20");
                int CBTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", CBTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.clear();
                MissionInstructionAfterParaChild.append("code", "P20");
                MissionInstructionAfterParaChild.append("value", Integer.toString(CBTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP21) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P21");
                float FYAttitudeTemp = 0;
                for (int j = 0; j < AttitudeTimeDateList.size(); j++) {
                    if (Duration.between(MissionStarTimeArray.get(i).toInstant(), AttitudeTimeDateList.get(j).toInstant()).getSeconds() < 0.25) {
                        FYAttitudeTemp = (float) AttitudeList.get(j)[1];
                        break;
                    }
                }
                FYGDAttitude[1] = FYAttitudeTemp;
                String strtemp = Integer.toHexString(Float.floatToIntBits(FYAttitudeTemp));
                if (strtemp.length() < 8) {
                    for (int j = strtemp.length() + 1; j <= 8; j++) {
                        strtemp = "0" + strtemp;
                    }
                }
                TaskParamsTemp.append("value", strtemp.toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.clear();
                MissionInstructionAfterParaChild.append("code", "P21");
                MissionInstructionAfterParaChild.append("value", Float.toString(FYAttitudeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP22) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P22");
                float GDAttitudeTemp = 0;
                for (int j = 0; j < AttitudeTimeDateList.size(); j++) {
                    if (Duration.between(MissionStarTimeArray.get(i).toInstant(), AttitudeTimeDateList.get(j).toInstant()).getSeconds() < 0.25) {
                        GDAttitudeTemp = (float) AttitudeList.get(j)[0];
                        break;
                    }
                }
                FYGDAttitude[0] = GDAttitudeTemp;
                String strtemp = Integer.toHexString(Float.floatToIntBits(GDAttitudeTemp));
                if (strtemp.length() < 8) {
                    for (int j = strtemp.length() + 1; j <= 8; j++) {
                        strtemp = "0" + strtemp;
                    }
                }
                TaskParamsTemp.append("value", strtemp.toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P22");
                MissionInstructionAfterParaChild.append("value", Float.toString(GDAttitudeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP23) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P23");
                int NSTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P23");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP24) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P24");
                int NSTimeTemp = 720;
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P24");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP25) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P25");
                int NSTimeTemp = (int) (Duration.between(MissionStarTimeArray.get(i).toInstant(), MissionEndTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P25");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP26) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P26");
                float lonTemp = 0;
                float latTemp = 0;
                float HTemp = 0;
                ArrayList<double[]> MissionTargetArea_iList = MissionTargetAreaList.get(i);
                for (double[] TargetTemp : MissionTargetArea_iList) {
                    lonTemp = (float) (lonTemp + TargetTemp[0]);
                    latTemp = (float) (latTemp + TargetTemp[1]);
                }
                lonTemp = lonTemp / MissionTargetArea_iList.size();
                latTemp = latTemp / MissionTargetArea_iList.size();
                String lonstr = Integer.toHexString(Float.floatToIntBits(lonTemp)).toUpperCase();
                String latstr = Integer.toHexString(Float.floatToIntBits(latTemp)).toUpperCase();
                String Hstr = Integer.toHexString(Float.floatToIntBits(HTemp)).toUpperCase();
                if (lonstr.length() < 8) {
                    for (int j = lonstr.length() + 1; j <= 8; j++) {
                        lonstr = "0" + lonstr;
                    }
                }
                if (latstr.length() < 8) {
                    for (int j = latstr.length() + 1; j <= 8; j++) {
                        latstr = "0" + latstr;
                    }
                }
                if (Hstr.length() < 8) {
                    for (int j = Hstr.length() + 1; j <= 8; j++) {
                        Hstr = "0" + Hstr;
                    }
                }
                TaskParamsTemp.append("value", (lonstr + latstr + Hstr).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P26");
                MissionInstructionAfterParaChild.append("value", Float.toString(lonTemp) + "," + Float.toString(latTemp) + "," + Float.toString(HTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP27) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P27");
                float AccAngTemp = (float) 0.08;
                String strtemp = Integer.toHexString(Float.floatToIntBits(AccAngTemp));
                if (strtemp.length() < 8) {
                    for (int j = strtemp.length() + 1; j <= 8; j++) {
                        strtemp = "0" + strtemp;
                    }
                }
                TaskParamsTemp.append("value", strtemp.toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P27");
                MissionInstructionAfterParaChild.append("value", Float.toString(AccAngTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP28) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P28");
                float VelAngTemp = (float) 0.12;
                String strtemp = Integer.toHexString(Float.floatToIntBits(VelAngTemp));
                if (strtemp.length() < 8) {
                    for (int j = strtemp.length() + 1; j <= 8; j++) {
                        strtemp = "0" + strtemp;
                    }
                }
                TaskParamsTemp.append("value", strtemp.toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P28");
                MissionInstructionAfterParaChild.append("value", Float.toString(VelAngTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP29) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P29");
                int NSTimeTemp = 100;
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P29");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP32_1) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P32_1");
                int NSTimeTemp = (int) (Duration.between(zerostart, MissionStarTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P32_1");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP32_2) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P32_2");
                int NSTimeTemp = (int) (Duration.between(zerostart, MissionEndTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp).toUpperCase());
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P32_2");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            MissionInstructionAfterArray.add(MissionInstructionAfterPara);

            ArrayList<String> MissionInstructionCodeChild = new ArrayList<>();
            ArrayList<Integer> MissionInstructionIdChild = new ArrayList<>();
            ArrayList<Date> MissionInstructionTimeChild = new ArrayList<>();
            ArrayList<String> MissionInstructionHexChild = new ArrayList<>();
            ArrayList<String> MissionInstructionWorkCode = new ArrayList<>();
            ArrayList<Integer> MissionInstructionTimeUpdateStatus = new ArrayList<>();


            double InstDelta_tAll = 0;
            double InstDelta_tLastAll = 0;
            Date time_point = MissionStarTimeArray.get(i);
            Date time_pointEnd = MissionEndTimeArray.get(i);

            timeVariable.T0 = time2Second(time_point);
            timeVariable.T0d = time2Second(time_pointEnd);
            timeVariable.T1 = time2Second(time_point);
            timeVariable.T1d = time2Second(time_pointEnd);
            timeVariable.T = time2Second(time_point);
            double timeVariableT0K4425 = time2SecondK4425(time_point);
            double timeVariableGazeTimeK4425 = time2SecondK4425(time_pointEnd) - time2SecondK4425(time_point);

            String ZhilingKuai = "";
            ArrayList<String> MissionTaskModelChild = MissionTaskModel.get(i);

            Map<String, String> it = new HashMap<>();

            for (String workcode : MissionTaskModelChild) {
                //System.out.println(workcode);
                for (Document document : TaskInstructionjson) {
                    try {
                        //选择指令块模板
                        if (document.get("code").equals(workcode)) {
                            ArrayList<Document> SequenceArray = (ArrayList<Document>) document.get("sequence");
                            //指令块中包含的指令序列
                            for (Document document1 : SequenceArray) {
                                String YingYongShuJu = "";
                                //判定该序列是否执行
                                Boolean SequenceFlag = false;
                                Boolean AlternativeFlag = document1.containsKey("alternative");
                                if (AlternativeFlag) {
                                    String Related_id = document1.get("related_param_id").toString();
                                    //搜索任务中相应id的值
                                    String TaskParamsValue = "";
                                    Boolean RelatedIdFindFlag = true;
                                    for (Document TaskParams : MissionInstructionArrayChild) {
                                        if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                            if (Related_id.equals("P07")) {
                                                if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                    ArrayList<String> TaskParamsValueTemp = (ArrayList<String>) TaskParams.get("value");
                                                    for (String TaskParamsValueChildTemp : TaskParamsValueTemp) {
                                                        TaskParamsValue = TaskParamsValue + TaskParamsValueChildTemp;
                                                    }
                                                    RelatedIdFindFlag = false;
                                                }
                                            } else {
                                                if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                    TaskParamsValue = TaskParams.get("value").toString();
                                                    RelatedIdFindFlag = false;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                    if (RelatedIdFindFlag) {
                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                                if (Related_id.equals("P07")) {
                                                    if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                        ArrayList<String> TaskParamsValueTemp = (ArrayList<String>) TaskParams.get("default_value");
                                                        for (String TaskParamsValueChildTemp : TaskParamsValueTemp) {
                                                            TaskParamsValue = TaskParamsValue + TaskParamsValueChildTemp;
                                                        }
                                                        RelatedIdFindFlag = false;
                                                    }
                                                } else {
                                                    if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                        TaskParamsValue = TaskParams.get("default_value").toString();
                                                        RelatedIdFindFlag = false;
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    //判定是否执行该序列
                                    if (Related_id.equals("P07")) {
                                        if (document1.containsKey("when")) {
                                            for (int j = 0; j < TaskParamsValue.length(); j++) {
                                                String TaskParamsValueChild = TaskParamsValue.substring(j, j + 1);
                                                if (document1.get("when").toString().equals(TaskParamsValueChild)) {
                                                    SequenceFlag = true;
                                                }
                                            }
                                        } else if (document1.containsKey("when_or")) {
                                            ArrayList<String> MissionSequenceWhenOr = (ArrayList<String>) document1.get("when_or");
                                            String MissionSequenceWhenOrAll = "";
                                            for (String MissionSequenceWhenOrChild : MissionSequenceWhenOr) {
                                                MissionSequenceWhenOrAll = MissionSequenceWhenOrAll + MissionSequenceWhenOrChild;
                                            }
                                            for (int j = 0; j < TaskParamsValue.length(); j++) {
                                                String TaskParamsValueChild = TaskParamsValue.substring(j, j + 1);
                                                if (MissionSequenceWhenOrAll.contains(TaskParamsValueChild)) {
                                                    SequenceFlag = true;
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        if (document1.containsKey("when")) {
                                            if (document1.get("when").toString().equals(TaskParamsValue)) {
                                                SequenceFlag = true;
                                            }
                                        } else if (document1.containsKey("when_or")) {
                                            ArrayList<String> MissionSequenceWhenOr = (ArrayList<String>) document1.get("when_or");
                                            for (String MissionSequenceWhenOrChild : MissionSequenceWhenOr) {
                                                if (MissionSequenceWhenOrChild.equals(TaskParamsValue)) {
                                                    SequenceFlag = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    SequenceFlag = true;
                                }

                                //执行该序列
                                if (SequenceFlag) {
                                    byte ZhilingNum = 0;
                                    String YouXiaoData = "";
                                    InstDelta_tLastAll = 0;
                                    //判定序列是否为选择类型
                                    String sequencecode = "";
                                    if (document1.containsKey("select")) {
                                        String Related_id = document1.get("related_param_id").toString();
                                        //搜索任务中相应id的值
                                        String TaskParamsValue = "";
                                        Boolean RelatedIdFindFlag = true;
                                        for (Document TaskParams : MissionInstructionArrayChild) {
                                            if (TaskParams.containsKey("code") && TaskParams.get("code") != null && TaskParams.get("code").toString().equals(Related_id)) {
                                                if (TaskParams.containsKey("value") && !TaskParams.get("value").equals("")) {
                                                    TaskParamsValue = TaskParams.get("value").toString();
                                                    RelatedIdFindFlag = false;
                                                }
                                                break;
                                            }
                                        }
                                        if (RelatedIdFindFlag) {
                                            for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                                    if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                        TaskParamsValue = TaskParams.get("default_value").toString();
                                                        RelatedIdFindFlag = false;
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        Document sequencemapping = (Document) document1.get("mapping");
                                        sequencecode = sequencemapping.get(TaskParamsValue).toString();
                                    } else {
                                        sequencecode = document1.get("sequence_code").toString();
                                    }
                                    System.out.println("序列：" + sequencecode);
                                    boolean MoreThanFlag = false;
                                    ArrayList<String> YouXiaoshujuList = new ArrayList<>();
                                    ArrayList<Byte> ZhiLingGeshuList = new ArrayList<>();
                                    //选择指令码模板
                                    for (Document document2 : SequenceInstructionjson) {
                                        try {
                                            if (document2.get("code").equals(sequencecode)) {
                                                ArrayList<Document> InstsArray = (ArrayList<Document>) document2.get("inst");
                                                for (Document document3 : InstsArray) {
                                                    String InstCode = "";
                                                    //判断执行哪种指令码
                                                    if (document3.containsKey("alternative")) {
                                                        //是否执行种类
                                                        String MetaRelated_id = document3.get("related_param_id").toString();
                                                        //搜索任务中相应id的值
                                                        String SequenceParamsValue = "";
                                                        Boolean RelatedIdFindFlag = true;
                                                        for (Document TaskParams : MissionInstructionArrayChild) {
                                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaRelated_id)) {
                                                                if (MetaRelated_id.equals("P07")) {
                                                                    if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                                        ArrayList<String> TaskParamsValueTemp = (ArrayList<String>) TaskParams.get("value");
                                                                        for (String TaskParamsValueChildTemp : TaskParamsValueTemp) {
                                                                            SequenceParamsValue = SequenceParamsValue + TaskParamsValueChildTemp;
                                                                        }
                                                                        RelatedIdFindFlag = false;
                                                                    }
                                                                } else {
                                                                    if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                                        SequenceParamsValue = TaskParams.get("value").toString();
                                                                        RelatedIdFindFlag = false;
                                                                    }
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        if (RelatedIdFindFlag) {
                                                            for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaRelated_id)) {
                                                                    if (MetaRelated_id.equals("P07")) {
                                                                        if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                            ArrayList<String> TaskParamsValueTemp = (ArrayList<String>) TaskParams.get("default_value");
                                                                            for (String TaskParamsValueChildTemp : TaskParamsValueTemp) {
                                                                                SequenceParamsValue = SequenceParamsValue + TaskParamsValueChildTemp;
                                                                            }
                                                                            RelatedIdFindFlag = false;
                                                                        }
                                                                    } else {
                                                                        if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                            SequenceParamsValue = TaskParams.get("default_value").toString();
                                                                            RelatedIdFindFlag = false;
                                                                        }
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        if (MetaRelated_id.equals("P07")) {
                                                            if (document3.containsKey("when")) {
                                                                for (int j = 0; j < SequenceParamsValue.length(); j++) {
                                                                    String TaskParamsValueChild = SequenceParamsValue.substring(j, j + 1);
                                                                    if (document3.get("when").toString().equals(TaskParamsValueChild)) {
                                                                        InstCode = document3.get("inst_code").toString();
                                                                    }
                                                                }
                                                            } else if (document3.containsKey("when_or")) {
                                                                ArrayList<String> MissionSequenceWhenOr = (ArrayList<String>) document3.get("when_or");
                                                                String MissionSequenceWhenOrAll = "";
                                                                for (String MissionSequenceWhenOrChild : MissionSequenceWhenOr) {
                                                                    MissionSequenceWhenOrAll = MissionSequenceWhenOrAll + MissionSequenceWhenOrChild;
                                                                }
                                                                for (int j = 0; j < SequenceParamsValue.length(); j++) {
                                                                    String TaskParamsValueChild = SequenceParamsValue.substring(j, j + 1);
                                                                    if (MissionSequenceWhenOrAll.contains(TaskParamsValueChild)) {
                                                                        InstCode = document3.get("inst_code").toString();
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            if (document3.containsKey("when")) {
                                                                if (document3.get("when").toString().equals(SequenceParamsValue)) {
                                                                    InstCode = document3.get("inst_code").toString();
                                                                }
                                                            } else if (document3.containsKey("when_or")) {
                                                                ArrayList<String> MissionCodeWhenOr = (ArrayList<String>) document3.get("when_or");
                                                                for (String MissionCodeWhenOrChild : MissionCodeWhenOr) {
                                                                    if (MissionCodeWhenOrChild.equals(SequenceParamsValue)) {
                                                                        InstCode = document3.get("inst_code").toString();
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else if (document3.containsKey("select")) {
                                                        //列表选择执行种类
                                                        String MetaRelated_id = document3.get("related_param_id").toString();
                                                        //搜索任务中相应id的值
                                                        String SequenceParamsValue = "";
                                                        Boolean RelatedIdFindFlag = true;
                                                        for (Document SequenceParams : MissionInstructionArrayChild) {
                                                            if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                if (SequenceParams.containsKey("value") && SequenceParams.get("value") != null && !SequenceParams.get("value").equals("")) {
                                                                    SequenceParamsValue = SequenceParams.get("value").toString();
                                                                    RelatedIdFindFlag = false;
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        if (RelatedIdFindFlag) {
                                                            for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaRelated_id)) {
                                                                    if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                        SequenceParamsValue = TaskParams.get("default_value").toString();
                                                                        RelatedIdFindFlag = false;
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        Document sequencemapping = (Document) document3.get("mapping");
                                                        if (sequencemapping.containsKey(SequenceParamsValue) && sequencemapping.get(SequenceParamsValue) != null && !sequencemapping.get(SequenceParamsValue).equals("")) {
                                                            InstCode = sequencemapping.get(SequenceParamsValue).toString();
                                                        }
                                                    } else {
                                                        //变参执行种类
                                                        InstCode = document3.get("inst_code").toString();
                                                    }
                                                    //执行间隔
                                                    String InstDelta_t = "0";
                                                    if (document3.get("delta_t").getClass().toString().equals("class java.lang.String")) {
                                                        InstDelta_t = document3.get("delta_t").toString();
                                                    } else {
                                                        Document delta_tDocument = (Document) document3.get("delta_t");
                                                        if (delta_tDocument.containsKey("related_param_id")) {
                                                            String delta_tId = delta_tDocument.get("related_param_id").toString();
                                                            //搜索任务中相应id的值
                                                            String DeltaParamsValue = "";
                                                            Boolean RelatedIdFindFlag = true;
                                                            for (Document SequenceParams : MissionInstructionArrayChild) {
                                                                if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(delta_tId)) {
                                                                    if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                        DeltaParamsValue = SequenceParams.get("value").toString();
                                                                        RelatedIdFindFlag = false;
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                            if (RelatedIdFindFlag) {
                                                                for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                    if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(delta_tId)) {
                                                                        if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                            DeltaParamsValue = TaskParams.get("default_value").toString();
                                                                            RelatedIdFindFlag = false;
                                                                        }
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if (delta_tDocument.containsKey("mapping")) {
                                                                Document delta_tMappingDocument = (Document) delta_tDocument.get("mapping");
                                                                if (delta_tMappingDocument.containsKey(DeltaParamsValue)) {
                                                                    InstDelta_t = delta_tMappingDocument.get(DeltaParamsValue).toString();
                                                                }
                                                            }
                                                        } else {
                                                            InstDelta_t = document3.get("delta_t").toString();
                                                        }
                                                    }
                                                    InstDelta_tLastAll = InstDelta_tLastAll + Double.parseDouble(InstDelta_t);//Integer.parseInt(InstDelta_t)*0.125;
                                                    //执行该指令
                                                    if (InstCode != "") {
//                                                        if (InstCode.equals("TCS205"))
//                                                            System.out.println(InstCode);
//                                                        if (InstCode.equals("TCS297")) {
//                                                            System.out.println(InstCode);
//                                                        }
                                                        for (Document document4 : MetaInstrunctionjson) {
                                                            if (document4.get("code").toString().equals(InstCode)) {
                                                                if (InstCode.contains("NTCY200")) {
                                                                    String MetaHex = document4.get("hex").toString();
                                                                    byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                                    if (document4.containsKey("params") && document4.get("params") != null) {
                                                                        ArrayList<Document> MetaParams = (ArrayList<Document>) document4.get("params");
                                                                        if (MetaParams.size() != 0) {
                                                                            for (Document MetaParamsChild : MetaParams) {
                                                                                //任务参数读取
                                                                                if (MetaParamsChild.containsKey("related_param_id")) {
                                                                                    String MetaParamsId = MetaParamsChild.get("related_param_id").toString();
                                                                                    //搜索任务中相应的id值
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                            if (MissionMetaParamsChildParamsChild.containsKey("value") && !MissionMetaParamsChildParamsChild.get("value").equals("")) {
                                                                                                //System.out.println(MetaParamsId);
                                                                                                float temeratureFloat = Float.parseFloat(MissionMetaParamsChildParamsChild.get("value").toString());
                                                                                                String MetaParamsIdValue = TemperatureFlotToStr(temeratureFloat);
                                                                                                int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                                int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                                byte[] bytevalueHex = hexStringToBytes(MetaParamsIdValue);
                                                                                                for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                    if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                        byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                    }
                                                                                                }
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    MetaHex = bytesToHexString(byteMetaHex);
                                                                    byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                                    String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                                    int ZhiXingJianGeInt = (new Double(Double.parseDouble(InstDelta_t) / 0.125)).intValue();
                                                                    byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt >> 8) & 0xFF), (byte) ((ZhiXingJianGeInt) & 0xFF)};
                                                                    String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                                    //String APIDString = "FFFF";//????????????
                                                                    String APIDString = "0" + document4.get("apid").toString();
                                                                    if ((YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString).length() > 1000) {
                                                                        String youxiaoDataTemp = YouXiaoData;
                                                                        YouXiaoshujuList.add(youxiaoDataTemp);
                                                                        Byte ZhilingNumTemp = ZhilingNum;
                                                                        ZhiLingGeshuList.add(ZhilingNumTemp);
                                                                        MoreThanFlag = true;
                                                                        YouXiaoData = "";
                                                                        ZhilingNum = 0;
                                                                    }
                                                                    YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                                    ZhilingNum = (byte) (ZhilingNum + 1);
                                                                } else if (document4.containsKey("hex")) {
                                                                    String MetaHex = document4.get("hex").toString();
                                                                    byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                                    if (document4.containsKey("params") && document4.get("params") != null) {
                                                                        ArrayList<Document> MetaParams = (ArrayList<Document>) document4.get("params");
                                                                        if (MetaParams.size() != 0) {
                                                                            for (Document MetaParamsChild : MetaParams) {
                                                                                //任务参数读取
                                                                                if (MetaParamsChild.containsKey("id")) {
                                                                                    String MetaParamsId = MetaParamsChild.get("id").toString();
                                                                                    //搜索任务中相应的id值
                                                                                    Boolean RelatedIdFindFlag = true;
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                            if (MissionMetaParamsChildParamsChild.containsKey("value") && !MissionMetaParamsChildParamsChild.get("value").equals("")) {
                                                                                                //System.out.println(MetaParamsId);
                                                                                                String MetaParamsIdValue = MissionMetaParamsChildParamsChild.get("value").toString();
                                                                                                int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                                int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                                byte[] bytevalueHex = hexStringToBytes(MetaParamsIdValue);
                                                                                                for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                    if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                        byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                    }
                                                                                                }
                                                                                                RelatedIdFindFlag = false;
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    if (RelatedIdFindFlag) {
                                                                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaParamsId)) {
                                                                                                if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                                                    //System.out.println(MetaParamsId);
                                                                                                    String MetaParamsIdValue = TaskParams.get("default_value").toString();
                                                                                                    int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                                    int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                                    byte[] bytevalueHex = hexStringToBytes(MetaParamsIdValue);
                                                                                                    for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                        if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                            byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                        }
                                                                                                    }
                                                                                                    RelatedIdFindFlag = false;
                                                                                                    break;
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                } else if (MetaParamsChild.containsKey("related_param_id")) {
                                                                                    String MetaParamsCode = "";
                                                                                    //列表选择执行种类
                                                                                    String MetaRelated_id = MetaParamsChild.get("related_param_id").toString();
                                                                                    //搜索任务中相应id的值
                                                                                    String SequenceParamsValue = "";
                                                                                    Boolean RelatedIdFindFlag = true;
                                                                                    for (Document SequenceParams : MissionInstructionArrayChild) {
                                                                                        if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                                            if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                                                SequenceParamsValue = SequenceParams.get("value").toString();
                                                                                                RelatedIdFindFlag = false;
                                                                                            }
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                    if (RelatedIdFindFlag) {
                                                                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaRelated_id)) {
                                                                                                if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                                                    SequenceParamsValue = TaskParams.get("default_value").toString();
                                                                                                    RelatedIdFindFlag = false;
                                                                                                }
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    if (MetaParamsChild.containsKey("mapping")) {
                                                                                        Document sequencemapping = (Document) MetaParamsChild.get("mapping");
                                                                                        if (sequencemapping.containsKey(SequenceParamsValue) && sequencemapping.get(SequenceParamsValue) != null && !sequencemapping.get(SequenceParamsValue).equals("")) {
                                                                                            MetaParamsCode = sequencemapping.get(SequenceParamsValue).toString();
                                                                                            int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                            int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                            byte[] bytevalueHex = hexStringToBytes(MetaParamsCode);
                                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                    byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    } else {
                                                                                        int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                        int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                        byte[] bytevalueHex = hexStringToBytes(SequenceParamsValue);
                                                                                        for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                            if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                } else {
                                                                                    String MetaParamsCode = MetaParamsChild.get("code").toString();
                                                                                    //搜索任务中相应的code值
                                                                                    Boolean RelatedIdFindFlag = true;
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsCode)) {
                                                                                            String MetaParamsCodeValue = MissionMetaParamsChildParamsChild.get("value").toString();
                                                                                            int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                            int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                            byte[] bytevalueHex = hexStringToBytes(MetaParamsCodeValue);
                                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                    byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                }
                                                                                            }
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                    if (RelatedIdFindFlag) {
                                                                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                                                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(MetaParamsCode)) {
                                                                                                if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                                                                    String MetaParamsCodeValue = TaskParams.get("default_value").toString();
                                                                                                    int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                                    int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                                    byte[] bytevalueHex = hexStringToBytes(MetaParamsCodeValue);
                                                                                                    for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                        if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                            byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                        }
                                                                                                    }
                                                                                                    RelatedIdFindFlag = false;
                                                                                                }
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                    MetaHex = bytesToHexString(byteMetaHex);
                                                                    byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                                    String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                                    int ZhiXingJianGeInt = (new Double(Double.parseDouble(InstDelta_t) / 0.125)).intValue();
                                                                    byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt >> 8) & 0xFF), (byte) ((ZhiXingJianGeInt) & 0xFF)};
                                                                    String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                                    //String APIDString = "FFFF";//????????????
                                                                    String APIDString = "0" + document4.get("apid").toString();
                                                                    if ((YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString).length() > 1000) {
                                                                        String youxiaoDataTemp = YouXiaoData;
                                                                        YouXiaoshujuList.add(youxiaoDataTemp);
                                                                        Byte ZhilingNumTemp = ZhilingNum;
                                                                        ZhiLingGeshuList.add(ZhilingNumTemp);
                                                                        MoreThanFlag = true;
                                                                        YouXiaoData = "";
                                                                        ZhilingNum = 0;
                                                                    }
                                                                    YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                                    ZhilingNum = (byte) (ZhilingNum + 1);
                                                                } else {
                                                                    //添加特殊指令
                                                                    String MetaHex = "";
                                                                    if (InstCode.equals("K4401")) {
                                                                        if (MissionImageModel.get(i).equals("凝视")) {
                                                                            MetaHex = "100280210118";
                                                                            MetaHex = MetaHex + "AA180055";
                                                                            ArrayList<double[]> MissionTargetArea_iList = MissionTargetAreaList.get(i);
                                                                            double lonAll = 0;
                                                                            double latAll = 0;
                                                                            for (double[] lonlat : MissionTargetArea_iList) {
                                                                                lonAll = lonAll + lonlat[0];
                                                                                latAll = latAll + lonlat[1];
                                                                            }
                                                                            lonAll = lonAll / MissionTargetArea_iList.size();
                                                                            latAll = latAll / MissionTargetArea_iList.size();
                                                                            float t_theta = (float) timeVariable.T0;
                                                                            float lon = (float) lonAll;
                                                                            float lat = (float) latAll;
                                                                            float H = 0;
                                                                            float ddAng = (float) 0.2;
                                                                            float dAng = (float) 1.0;
                                                                            int it_theta = (new Float(t_theta)).intValue();
                                                                            byte[] it_thetaByte = new byte[]{(byte) ((it_theta >> 8) & 0xFF), (byte) ((it_theta) & 0xFF)};
                                                                            String st_thetaByte = bytesToHexString(it_thetaByte);
                                                                            String strtemplon = Integer.toHexString(Float.floatToIntBits(lon));
                                                                            if (strtemplon.length() < 8) {
                                                                                for (int j = strtemplon.length() + 1; j <= 8; j++) {
                                                                                    strtemplon = "0" + strtemplon;
                                                                                }
                                                                            }
                                                                            String strtemplat = Integer.toHexString(Float.floatToIntBits(lat));
                                                                            if (strtemplat.length() < 8) {
                                                                                for (int j = strtemplat.length() + 1; j <= 8; j++) {
                                                                                    strtemplat = "0" + strtemplat;
                                                                                }
                                                                            }
                                                                            String strtempH = Integer.toHexString(Float.floatToIntBits(H));
                                                                            if (strtempH.length() < 8) {
                                                                                for (int j = strtempH.length() + 1; j <= 8; j++) {
                                                                                    strtempH = "0" + strtempH;
                                                                                }
                                                                            }
                                                                            String strtempddAng = Integer.toHexString(Float.floatToIntBits(ddAng));
                                                                            if (strtempddAng.length() < 8) {
                                                                                for (int j = strtempddAng.length() + 1; j <= 8; j++) {
                                                                                    strtempddAng = "0" + strtempddAng;
                                                                                }
                                                                            }
                                                                            String strtempdAng = Integer.toHexString(Float.floatToIntBits(dAng));
                                                                            if (strtempdAng.length() < 8) {
                                                                                for (int j = strtempdAng.length() + 1; j <= 8; j++) {
                                                                                    strtempdAng = "0" + strtempdAng;
                                                                                }
                                                                            }
                                                                            MetaHex = MetaHex + st_thetaByte +
                                                                                    strtemplon +
                                                                                    strtemplat +
                                                                                    strtempH +
                                                                                    strtempddAng +
                                                                                    strtempdAng;
                                                                        } else {
                                                                            MetaHex = "100280210118";
                                                                            MetaHex = MetaHex + "AA1800AA";
                                                                            float GDAtt = (float) FYGDAttitude[0];
                                                                            float FYAtt = (float) FYGDAttitude[1];
                                                                            float t_theta = (float) timeVariable.T4401;
                                                                            float ddAng = (float) 0.08;
                                                                            float dAng = (float) 0.12;
                                                                            int it_theta = (new Float(t_theta)).intValue();
                                                                            byte[] it_thetaByte = new byte[]{(byte) ((it_theta >> 8) & 0xFF), (byte) ((it_theta) & 0xFF)};
                                                                            String st_thetaByte = bytesToHexString(it_thetaByte);
                                                                            String strtempGD = Integer.toHexString(Float.floatToIntBits(GDAtt));
                                                                            if (strtempGD.length() < 8) {
                                                                                for (int j = strtempGD.length() + 1; j <= 8; j++) {
                                                                                    strtempGD = "0" + strtempGD;
                                                                                }
                                                                            } else if (strtempGD.length() > 8) {
                                                                                strtempGD = strtempGD.substring(strtempGD.length() - 8);
                                                                            }
                                                                            String strtempFY = Integer.toHexString(Float.floatToIntBits(FYAtt));
                                                                            if (strtempFY.length() < 8) {
                                                                                for (int j = strtempFY.length() + 1; j <= 8; j++) {
                                                                                    strtempFY = "0" + strtempFY;
                                                                                }
                                                                            } else if (strtempFY.length() > 8) {
                                                                                strtempFY = strtempFY.substring(strtempFY.length() - 8);
                                                                            }
                                                                            String strtempddAng = Integer.toHexString(Float.floatToIntBits(ddAng));
                                                                            if (strtempddAng.length() < 8) {
                                                                                for (int j = strtempddAng.length() + 1; j <= 8; j++) {
                                                                                    strtempddAng = "0" + strtempddAng;
                                                                                }
                                                                            } else if (strtempddAng.length() > 8) {
                                                                                strtempddAng = strtempddAng.substring(strtempddAng.length() - 8);
                                                                            }
                                                                            String strtempdAng = Integer.toHexString(Float.floatToIntBits(dAng));
                                                                            if (strtempdAng.length() < 8) {
                                                                                for (int j = strtempdAng.length() + 1; j <= 8; j++) {
                                                                                    strtempdAng = "0" + strtempdAng;
                                                                                }
                                                                            } else if (strtempdAng.length() > 8) {
                                                                                strtempdAng = strtempdAng.substring(strtempdAng.length() - 8);
                                                                            }
                                                                            MetaHex = MetaHex + st_thetaByte +
                                                                                    strtempGD +
                                                                                    strtempFY +
                                                                                    strtempddAng +
                                                                                    strtempdAng;
                                                                        }
                                                                    } else if (InstCode.equals("K4402")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0101";
                                                                    } else if (InstCode.equals("K4403")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0202";
                                                                    } else if (InstCode.equals("K4404")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0303";
                                                                    } else if (InstCode.equals("K4404")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0303";
                                                                    } else if (InstCode.equals("K4405")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0404";
                                                                    } else if (InstCode.equals("K4406")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0505";
                                                                    } else if (InstCode.equals("K4407")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0606";
                                                                    } else if (InstCode.equals("K4408")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0707";
                                                                    } else if (InstCode.equals("K4409")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0808";
                                                                    } else if (InstCode.equals("K4410")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0101";
                                                                    } else if (InstCode.equals("K4411")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0202";
                                                                    } else if (InstCode.equals("K4412")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0303";
                                                                    } else if (InstCode.equals("K4413")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0404";
                                                                    } else if (InstCode.equals("K4414")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0505";
                                                                    } else if (InstCode.equals("K4415")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0606";
                                                                    } else if (InstCode.equals("K4416")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0707";
                                                                        //} else if (InstCode.equals("K4418")) {
                                                                        //    MetaHex = "100280210118";
                                                                        //    MetaHex = MetaHex + "A102";
                                                                        //    MetaHex = MetaHex + "0808";
                                                                    } else if (InstCode.equals("K4419")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A200";
                                                                    } else if (InstCode.equals("K4420")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A400";
                                                                    } else if (InstCode.equals("K4425")) {
                                                                        MetaHex = "100280210118";
                                                                        MetaHex = MetaHex + "A81C";
                                                                        double GazeStartTime = timeVariableT0K4425;
                                                                        String str_GazeStartTime = Long.toHexString(Double.doubleToLongBits(GazeStartTime));
                                                                        if (str_GazeStartTime.length() < 16) {
                                                                            for (int j = str_GazeStartTime.length() + 1; j <= 16; j++) {
                                                                                str_GazeStartTime = "0" + str_GazeStartTime;
                                                                            }
                                                                        } else if (str_GazeStartTime.length() < 16) {
                                                                            str_GazeStartTime = str_GazeStartTime.substring(str_GazeStartTime.length() - 16);
                                                                        }
                                                                        float GazeStartGap = 720 * 1000000;
                                                                        String str_GazeStartGap = Integer.toHexString(Float.floatToIntBits(GazeStartGap));
                                                                        if (str_GazeStartGap.length() < 8) {
                                                                            for (int j = str_GazeStartGap.length() + 1; j <= 8; j++) {
                                                                                str_GazeStartGap = "0" + str_GazeStartGap;
                                                                            }
                                                                        } else if (str_GazeStartGap.length() > 8) {
                                                                            str_GazeStartGap = str_GazeStartGap.substring(str_GazeStartGap.length() - 8);
                                                                        }
                                                                        float GazeTime = (float) timeVariableGazeTimeK4425;
                                                                        String str_GazeTime = Integer.toHexString(Float.floatToIntBits(GazeTime));
                                                                        if (str_GazeTime.length() < 8) {
                                                                            for (int j = str_GazeTime.length() + 1; j <= 8; j++) {
                                                                                str_GazeTime = "0" + str_GazeTime;
                                                                            }
                                                                        } else if (str_GazeTime.length() > 8) {
                                                                            str_GazeTime = str_GazeTime.substring(str_GazeTime.length() - 8);
                                                                        }
                                                                        ArrayList<double[]> MissionTargetArea_iList = MissionTargetAreaList.get(i);
                                                                        double lonAll = 0;
                                                                        double latAll = 0;
                                                                        for (double[] lonlat : MissionTargetArea_iList) {
                                                                            lonAll = lonAll + lonlat[0];
                                                                            latAll = latAll + lonlat[1];
                                                                        }
                                                                        lonAll = lonAll / MissionTargetArea_iList.size();
                                                                        latAll = latAll / MissionTargetArea_iList.size();
                                                                        float lon = (float) lonAll;
                                                                        float lat = (float) latAll;
                                                                        float H = 0;
                                                                        String strtemplon = Integer.toHexString(Float.floatToIntBits(lon));
                                                                        if (strtemplon.length() < 8) {
                                                                            for (int j = strtemplon.length() + 1; j <= 8; j++) {
                                                                                strtemplon = "0" + strtemplon;
                                                                            }
                                                                        } else if (strtemplon.length() > 8) {
                                                                            strtemplon = strtemplon.substring(strtemplon.length() - 8);
                                                                        }
                                                                        String strtemplat = Integer.toHexString(Float.floatToIntBits(lat));
                                                                        if (strtemplat.length() < 8) {
                                                                            for (int j = strtemplat.length() + 1; j <= 8; j++) {
                                                                                strtemplat = "0" + strtemplat;
                                                                            }
                                                                        } else if (strtemplat.length() > 8) {
                                                                            strtemplat = strtemplat.substring(strtemplat.length() - 8);
                                                                        }
                                                                        String strtempH = Integer.toHexString(Float.floatToIntBits(H));
                                                                        if (strtempH.length() < 8) {
                                                                            for (int j = strtempH.length() + 1; j <= 8; j++) {
                                                                                strtempH = "0" + strtempH;
                                                                            }
                                                                        } else if (strtempH.length() > 8) {
                                                                            strtempH = strtempH.substring(strtempH.length() - 8);
                                                                        }
                                                                        MetaHex = MetaHex + GazeStartTime +
                                                                                GazeStartGap +
                                                                                str_GazeTime +
                                                                                strtemplon +
                                                                                strtemplat +
                                                                                strtempH;
                                                                    }
                                                                    /*
                                                                    else if (InstCode.equals("NTCY200")) {
                                                                        MetaHex="10FFFF210114";
                                                                        for (int j = 13; j <= 258; j++) {
                                                                            MetaHex=MetaHex+"00";
                                                                        }
                                                                    }
                                                                     */
                                                                    else {
                                                                        break;
                                                                    }
                                                                    byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                                    byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                                    String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                                    int ZhiXingJianGeInt = (new Double(Double.parseDouble(InstDelta_t) / 0.125)).intValue();
                                                                    byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt >> 8) & 0xFF), (byte) ((ZhiXingJianGeInt) & 0xFF)};
                                                                    String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                                    //String APIDString = "FFFF";//????????????
                                                                    String APIDString = "0" + document4.get("apid").toString();
                                                                    if ((YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString).length() > 1000) {
                                                                        String youxiaoDataTemp = YouXiaoData;
                                                                        YouXiaoshujuList.add(youxiaoDataTemp);
                                                                        Byte ZhilingNumTemp = ZhilingNum;
                                                                        ZhiLingGeshuList.add(ZhilingNumTemp);
                                                                        MoreThanFlag = true;
                                                                        YouXiaoData = "";
                                                                        ZhilingNum = 0;
                                                                    }
                                                                    YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                                    ZhilingNum = (byte) (ZhilingNum + 1);
                                                                }
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            continue;
                                        }
                                    }
                                    if (MoreThanFlag) {
                                        for (int j = 0; j < YouXiaoshujuList.size(); j++) {
                                            if (YouXiaoshujuList.get(j).length() <= 4) {
                                                continue;
                                            }
                                            int ZhiLingIDNum = SequenceID.SequenceId;
                                            SequenceID.SequenceId = SequenceID.SequenceId + 1;
                                            if (SequenceID.SequenceId > 255) {
                                                SequenceID.SequenceId = 0;
                                            }
                                            byte[] ZhiLingXuLieIDByte = new byte[]{(byte) ((ZhiLingIDNum) & 0xFF)};
                                            String ZhiLingXuLieIDString = bytesToHexString(ZhiLingXuLieIDByte);
                                            ZhiLingXuLieIDString = "00" + ZhiLingXuLieIDString;
                                            byte[] ZhiLingGeShu = new byte[]{ZhiLingGeshuList.get(j)};
                                            String ZhiLingGeShuString = bytesToHexString(ZhiLingGeShu);

                                            Instant time_ZhiXing = time_point.toInstant();
                                            time_ZhiXing = time_ZhiXing.plusSeconds((long) (InstDelta_tAll));
                                            Date time_ZhixingDate = Date.from(time_ZhiXing);
                                            //String KaiShiShiJian=time2String(time_ZhixingDate);
                                            //System.out.println(workcode);
                                            //System.out.println(sequencecode);
                                            //System.out.println(sequencecode);
                                            //System.out.println(Integer.parseInt(KaiShiShiJian,16));
                                            //最后一条指令时间间隔
                                            String YouXiaoDataTemp;
                                            if (YouXiaoshujuList.get(j).length() <= 4) {
                                                continue;
                                            } else {
                                                YouXiaoDataTemp = YouXiaoshujuList.get(j).substring(0, YouXiaoshujuList.get(j).length() - 4);
                                            }
                                            YingYongShuJu = ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoDataTemp;
                                            ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                            String YingYongShuJuTemp = YingYongShuJu;
                                            MissionInstructionHexChild.add(YingYongShuJuTemp);
                                            MissionInstructionCodeChild.add(sequencecode);
                                            MissionInstructionIdChild.add(ZhiLingIDNum);
                                            MissionInstructionTimeChild.add(time_ZhixingDate);
                                            MissionInstructionWorkCode.add(workcode);
                                        }
                                    }
                                    if (YouXiaoData.length() <= 4) {
                                        continue;
                                    }
                                    int ZhiLingIDNum = SequenceID.SequenceId;
                                    SequenceID.SequenceId = SequenceID.SequenceId + 1;
                                    if (SequenceID.SequenceId > 255) {
                                        SequenceID.SequenceId = 0;
                                    }
                                    byte[] ZhiLingXuLieIDByte = new byte[]{(byte) ((ZhiLingIDNum) & 0xFF)};
                                    String ZhiLingXuLieIDString = bytesToHexString(ZhiLingXuLieIDByte);
                                    ZhiLingXuLieIDString = "00" + ZhiLingXuLieIDString;
                                    byte[] ZhiLingGeShu = new byte[]{ZhilingNum};
                                    String ZhiLingGeShuString = bytesToHexString(ZhiLingGeShu);

                                    Instant time_ZhiXing = time_point.toInstant();
                                    time_ZhiXing = time_ZhiXing.plusSeconds((long) (InstDelta_tAll));
                                    Date time_ZhixingDate = Date.from(time_ZhiXing);
                                    //String KaiShiShiJian=time2String(time_ZhixingDate);
                                    //System.out.println(workcode);
                                    //System.out.println(sequencecode);
                                    //System.out.println(sequencecode);
                                    //System.out.println(Integer.parseInt(KaiShiShiJian,16));
                                    //最后一条指令时间间隔
                                    if (YouXiaoData.length() <= 4) {
                                        continue;
                                    } else {
                                        YouXiaoData = YouXiaoData.substring(0, YouXiaoData.length() - 4);
                                    }
                                    YingYongShuJu = ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoData;
                                    ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                    MissionInstructionHexChild.add(YingYongShuJu);
                                    MissionInstructionCodeChild.add(sequencecode);
                                    MissionInstructionIdChild.add(ZhiLingIDNum);
                                    MissionInstructionTimeChild.add(time_ZhixingDate);
                                    MissionInstructionWorkCode.add(workcode);
                                    InstDelta_tAll = InstDelta_tAll + InstDelta_tLastAll;

                                    //更新时间
                                    if (sequencecode.contains("TCAG01")) {
                                        timeVariable.TSC = InstDelta_tLastAll;
                                    } else if (sequencecode.contains("TCGFG01")) {
                                        timeVariable.TGF = InstDelta_tLastAll;
                                    } else if (sequencecode.contains("TCDGG01")) {
                                        timeVariable.TDG1 = InstDelta_tLastAll;
                                    } else if (sequencecode.contains("TCDGG02")) {
                                        timeVariable.TDG2 = InstDelta_tLastAll;
                                    } else if (sequencecode.contains("TCAG04")) {
                                        timeVariable.TTCAG04 = InstDelta_tLastAll;
                                    } else if (sequencecode.contains("TCGFG02")) {
                                        timeVariable.TGF2 = InstDelta_tLastAll;
                                    }
                                }
                            }
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }


                //更新执行时间
                for (int j = 0; j < MissionInstructionHexChild.size(); j++) {

                    String YingYongShuJuTemp = MissionInstructionHexChild.get(j);
                    if (!MissionInstructionTimeUpdateStatus.contains(j)) {
                        String KaiShiShiJian = timeHashMap.get(MissionInstructionCodeChild.get(j)).ExecutionTime(timeVariable, MissionInstructionWorkCode.get(j)).toUpperCase();
                        if (KaiShiShiJian.length() < 8) {
                            for (int i_id = KaiShiShiJian.length(); i_id < 8; i_id++) {
                                KaiShiShiJian = "0" + KaiShiShiJian;
                            }
                        } else if (KaiShiShiJian.length() > 8) {
                            KaiShiShiJian = KaiShiShiJian.substring(KaiShiShiJian.length() - 8);
                        }
                        MissionInstructionHexChild.set(j, KaiShiShiJian + YingYongShuJuTemp);
                        Instant time_ZhiXing = zerostart;
                        time_ZhiXing = time_ZhiXing.plusSeconds((long) (Integer.parseInt(KaiShiShiJian, 16)));
                        Date time_ZhixingDate = Date.from(time_ZhiXing);
                        MissionInstructionTimeChild.set(j, time_ZhixingDate);
                        System.out.println("序列号：" + MissionInstructionCodeChild.get(j));
                        System.out.println("执行时间：" + Integer.parseInt(KaiShiShiJian, 16));

                        it.put(MissionInstructionCodeChild.get(j) + "_" + Instant.now().toEpochMilli(), time_ZhixingDate.toString());
                        MissionInstructionTimeUpdateStatus.add(j);
                    }

                }

                insTimeMap.put(i, it);
            }

            MissionInstructionTimeUpdateStatus.clear();

            ArrayList<byte[]> InstructionArrayChild = new ArrayList<>();
            for (String ZhilingKuai_02 : MissionInstructionHexChild) {
                //String YingYongShuJu = KaiShiShiJian + ZhiLingID + ZhiLingNum + YouXiaoData;
                String ShuJuQuTou = "100B8021";
                int BaoChang = (ShuJuQuTou + ZhilingKuai_02).length() / 2 + 2 - 1;
                String BaoChangstr = String.format("%04X", BaoChang);
                int BaoXuLieIDNum = SequenceID.PackageId;
                SequenceID.PackageId = SequenceID.PackageId + 1;
                if (SequenceID.PackageId > 16383) {
                    SequenceID.PackageId = 0;
                }
                String BaoXuLieIDStr = Integer.toBinaryString(BaoXuLieIDNum);
                if (BaoXuLieIDStr.length() < 14) {
                    for (int i_id = BaoXuLieIDStr.length(); i_id < 14; i_id++) {
                        BaoXuLieIDStr = "0" + BaoXuLieIDStr;
                    }
                } else {
                    BaoXuLieIDStr = BaoXuLieIDStr.substring(BaoXuLieIDStr.length() - 14);
                }
                BaoXuLieIDStr = "11" + BaoXuLieIDStr;
                BaoXuLieIDStr = Integer.toHexString(Integer.parseInt(BaoXuLieIDStr, 2)).toUpperCase();
                String BaoZhuDaoTou = "1C11" + BaoXuLieIDStr + BaoChangstr;
                String total = BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai_02 + ISO(BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai_02);

                int fangshizi = -1;
                //添加填充域
                if (total.length() <= 62 * 2) {
                    for (int j = total.length() / 2; j < 62; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 0;
                } else if (total.length() > 62 * 2 && total.length() <= 126 * 2) {
                    for (int j = total.length() / 2; j < 126; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 1;
                } else if (total.length() > 126 * 2 && total.length() <= 254 * 2) {
                    for (int j = total.length() / 2; j < 254; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 2;
                } else if (total.length() > 254 * 2 && total.length() <= 510 * 2) {
                    for (int j = total.length() / 2; j < 510; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 3;
                } else {
                    //分两包
                }

                byte[] MainBuff = hexStringToBytes(total);
                int a = getCRC_0xFFFF(MainBuff, MainBuff.length);
                String CRCCode = String.format("%04X", a).toUpperCase();
                if (CRCCode.length() > 4) {
                    CRCCode = CRCCode.substring(CRCCode.length() - 4);
                } else if (CRCCode.length() < 4) {
                    for (int j = CRCCode.length(); j < 4; j++) {
                        CRCCode = "0" + CRCCode;
                    }
                }

                if (fangshizi == -1 || fangshizi == 0)
                    total = "EB90762599" + total + CRCCode;
                else if (fangshizi == 1)
                    total = "EB90762569" + total + CRCCode;
                else if (fangshizi == 2)
                    total = "EB907625A5" + total + CRCCode;
                else
                    total = "EB907625C3" + total + CRCCode;


                byte[] bytes = hexStringToBytes(total);
                InstructionArrayChild.add(bytes);
            }
            InstructionArray.add(i, InstructionArrayChild);
            MissionInstructionCode.add(i, MissionInstructionCodeChild);
            MissionInstructionId.add(i, MissionInstructionIdChild);
            MissionInstructionTime.add(i, MissionInstructionTimeChild);
            MissionInstructionHex.add(i, MissionInstructionHexChild);
        }

        //指令输出
        //??????文件夹以任务编号，文件夹内文件：指令序列编号-时间-任务编号-序列ID
        for (int i = 0; i < MissionNum; i++) {
            //指令输出
            String FileFolder = FilePath + "\\" + MissionNumberArray.get(i);
            FileFolder = FilePathUtil.getRealFilePath(FileFolder);
            File file = new File(FileFolder);
            if (!file.exists()) {
                //如果文件夹不存在，新建
                file.mkdirs();
            } else {
                //如果文件夹存在，删除文件夹内所有文件
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        f.delete();
                    }
                }
            }
            for (int j = 0; j < InstructionArray.get(i).size(); j++) {
                //指令文件命名
                Date cal = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String StringTime = sdf.format(cal.getTime());

                String DateString = StringTime.substring(0, 4) + StringTime.substring(5, 7) + StringTime.substring(8, 10) + StringTime.substring(11, 13) + StringTime.substring(14, 16);
                String FileName = MissionInstructionId.get(i).get(j) + "-" + MissionInstructionCode.get(i).get(j) + "-" + DateString + "-" + MissionNumberArray.get(i);
                String realPath = FilePathUtil.getRealFilePath(FileFolder + "\\" + FileName);
                bytesTotxt(InstructionArray.get(i).get(j), realPath);
            }

            String InsTimeFileName = "Ins-Time-" + Instant.now().toEpochMilli() + ".txt";
            String InsTimeFileNameRealPath = FilePathUtil.getRealFilePath(FileFolder + "\\" + InsTimeFileName);

            StringToFile(insTimeMap.get(i), InsTimeFileNameRealPath);
            //数据库传出
            ArrayList<Document> InstructionInfojsonArry = new ArrayList<>();
            if (MissionInstructionCode.get(i).size() > 0) {
                for (int j = 0; j < MissionInstructionCode.get(i).size(); j++) {
                    Document InstructionInfojsonObject = new Document();
                    InstructionInfojsonObject.append("sequence_code", MissionInstructionCode.get(i).get(j));
                    InstructionInfojsonObject.append("sequence_id", MissionInstructionId.get(i).get(j));
                    InstructionInfojsonObject.append("execution_time", MissionInstructionTime.get(i).get(j));
                    InstructionInfojsonObject.append("valid", true);
                    InstructionInfojsonArry.add(InstructionInfojsonObject);
                }
            } else {
                Document InstructionInfojsonObject = new Document();
                InstructionInfojsonObject.append("sequence_code", "");
                InstructionInfojsonObject.append("sequence_id", "");
                InstructionInfojsonObject.append("execution_time", "");
                InstructionInfojsonObject.append("valid", "");
                InstructionInfojsonArry.add(InstructionInfojsonObject);
            }
            ImageMissionjson.get(i).append("instruction_info", InstructionInfojsonArry);
            ImageMissionjson.get(i).append("mission_params_after_planning", MissionInstructionAfterArray.get(i));
            Document modifiers = new Document();
            modifiers.append("$set", ImageMissionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", ImageMissionjson.get(i).get("mission_number").toString()), modifiers, new UpdateOptions().upsert(true));

            InstructionManager instructionManager = new InstructionManager();
            instructionManager.addInstrctionInfo(InstructionInfojsonArry, ImageMissionjson.get(i).get("mission_number").toString(), ImageMissionjson.get(i).get("name").toString());
            instructionManager.close();
        }
        mongoClient.close();
    }

    private static Boolean InstructionStationMission(Document TransmissionMissionJson, ArrayList<Document> StationMissionjson, String FilePath) {
        //序列时间设置
        TimeVariable timeVariable = new TimeVariable();
        HashMap<String, SequenceTime> timeHashMap = new TimeMap().timeHashMap();

        //String test01="TCKG01";
        //System.out.println(timeHashMap.get(test01).ExecutionTime(timeVariable,"成像任务"));

        //读入模板
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //指令块模板
        MongoCollection<Document> Data_TaskInstructionjson = mongoDatabase.getCollection("task_instruction");
        FindIterable<Document> D_TaskInstructionjson = Data_TaskInstructionjson.find();
        ArrayList<Document> TaskInstructionjson = new ArrayList<>();
        for (Document document : D_TaskInstructionjson) {
            TaskInstructionjson.add(document);
        }
        //读入指令序列模板
        MongoCollection<Document> Data_SequenceInstructionjson = mongoDatabase.getCollection("sequence_instruction");
        FindIterable<Document> D_SequenceInstructionjson = Data_SequenceInstructionjson.find();
        ArrayList<Document> SequenceInstructionjson = new ArrayList<>();
        for (Document document : D_SequenceInstructionjson) {
            SequenceInstructionjson.add(document);
        }
        //读入指令码模板
        MongoCollection<Document> Data_MetaInstrunctionjson = mongoDatabase.getCollection("meta_instrunction");
        FindIterable<Document> D_MetaInstrunctionjson = Data_MetaInstrunctionjson.find();
        ArrayList<Document> MetaInstrunctionjson = new ArrayList<>();
        for (Document document : D_MetaInstrunctionjson) {
            MetaInstrunctionjson.add(document);
        }
        //读入基准时间
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first = sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");
        Instant zerostart = ZeroTimeIns;
        for (Document document : properties) {
            if (document.getString("key").equals("t0")) {
                zerostart = document.getDate("value").toInstant();
                LocalDateTime zerostart0 = LocalDateTime.ofInstant(zerostart, ZoneOffset.UTC);
                ZeroTime[0] = zerostart0.getYear();
                ZeroTime[1] = zerostart0.getMonthValue();
                ZeroTime[2] = zerostart0.getDayOfMonth();
                ZeroTime[3] = zerostart0.getHour();
                ZeroTime[4] = zerostart0.getMinute();
                ZeroTime[5] = zerostart0.getSecond();
                break;
            }
        }

        //传输任务读入
        ArrayList<String> TransMissionStationNameArray = new ArrayList<>();
        ArrayList<Date> TransMissionStarTimeArray = new ArrayList<>();
        ArrayList<Date> TransMissionEndTimeArray = new ArrayList<>();
        ArrayList<String> TransForStationMissionNumber = new ArrayList<>();
        ArrayList<String> TransMissionNumbers = new ArrayList<>();

        ArrayList<Object> MissionInstructionDefautArray = new ArrayList<>();

        if (TransmissionMissionJson != null) {
            try {
                if (TransmissionMissionJson.get("fail_reason").equals("不可见")) {
                } else {
                    if (TransmissionMissionJson.containsKey("mission_numbers") && TransmissionMissionJson.get("mission_numbers") != null) {
                        ArrayList<String> MissionNumber = (ArrayList<String>) TransmissionMissionJson.get("mission_numbers");
                        for (String MissionNumberChild : MissionNumber) {
                            String MissionNumberTemp = MissionNumberChild;
                            TransForStationMissionNumber.add(MissionNumberTemp);
                        }
                    }
                    if (TransmissionMissionJson.containsKey("transmission_number") && TransmissionMissionJson.get("transmission_number") != null) {
                        TransMissionNumbers.add(TransmissionMissionJson.get("transmission_number").toString());
                    }
                    if (TransmissionMissionJson.containsKey("transmission_window")) {
                        ArrayList<Document> TransMissionWindow = (ArrayList<Document>) TransmissionMissionJson.get("transmission_window");
                        if (TransMissionWindow != null) {
                            for (Document document : TransMissionWindow) {
                                TransMissionStationNameArray.add(document.get("station_name").toString());
                                TransMissionStarTimeArray.add((Date) document.get("start_time"));
                                TransMissionEndTimeArray.add((Date) document.get("end_time"));
                            }
                        }
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //地面站任务读入
        ArrayList StationMissionRecordFileNoArray = new ArrayList();
        ArrayList<Object> MissionInstructionArray = new ArrayList<>();
        if (StationMissionjson != null) {
            for (String MissionNumber : TransForStationMissionNumber) {
                for (Document document : StationMissionjson) {
                    if (document.get("mission_number").equals(MissionNumber)) {
                        try {
                            if (document.containsKey("record_file_no")) {
                                StationMissionRecordFileNoArray.add(document.get("record_file_no"));
                            }
                            if (document.containsKey("mission_params") && document.get("mission_params") != null) {
                                MissionInstructionArray.add(document.get("mission_params"));
                            }
                            if (document.containsKey("default_mission_params") && document.get("default_mission_params") != null) {
                                MissionInstructionDefautArray.add(document.get("default_mission_params"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }
                break;
            }
        }

        if (MissionInstructionArray.size() == 0) {
            return false;
        }

        //指令生成
        ArrayList<Object> MissionInstructionAfterArray = new ArrayList<>();
        ArrayList<ArrayList<byte[]>> InstructionArray = new ArrayList<>();
        ArrayList<Integer> ZhiLingIDArray = new ArrayList<>();
        ArrayList<ArrayList<String>> MissionInstructionCode = new ArrayList<>();
        ArrayList<ArrayList<Integer>> MissionInstructionId = new ArrayList<>();
        ArrayList<ArrayList<Date>> MissionInstructionTime = new ArrayList<>();
        ArrayList<ArrayList<String>> MissionInstructionTimeString = new ArrayList<>();
        ArrayList<ArrayList<String>> MissionInstructionHex = new ArrayList<>();

        ArrayList<Document> MissionInstructionDefautArrayChild = (ArrayList<Document>) MissionInstructionDefautArray.get(0);

        Map<Integer, Map<String, String>> insTimeMap = new HashMap<>();

        for (int i = 0; i < TransMissionStarTimeArray.size(); i++) {
            //添加指令参数
            ArrayList<Document> MissionInstructionArrayChildTemp = (ArrayList<Document>) MissionInstructionArray.get(0);
            ArrayList<Document> MissionInstructionArrayChild = new ArrayList<>();
            for (Document document : MissionInstructionArrayChildTemp) {
                MissionInstructionArrayChild.add(document);
            }

            ArrayList<Document> MissionInstructionAfterPara = new ArrayList<>();
            boolean ChildIdFlagP33_1 = true;
            boolean ChildIdFlagP33_2 = true;
            for (Document TaskParams : MissionInstructionArrayChild) {
                if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P33_1")) {
                    int NSTimeTemp = (int) (Duration.between(zerostart, TransMissionStarTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp));
                    ChildIdFlagP33_1 = false;
                    Document MissionInstructionAfterParaChild = new Document();
                    MissionInstructionAfterParaChild.append("code", "P33_1");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                } else if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals("P33_2")) {
                    int NSTimeTemp = (int) (Duration.between(zerostart, TransMissionEndTimeArray.get(i).toInstant()).getSeconds());
                    TaskParams.append("value", String.format("%04X", NSTimeTemp));
                    ChildIdFlagP33_2 = false;
                    Document MissionInstructionAfterParaChild = new Document();
                    MissionInstructionAfterParaChild.append("code", "P33_2");
                    MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                    MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
                }
            }

            if (ChildIdFlagP33_1) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P33_1");
                int NSTimeTemp = (int) (Duration.between(zerostart, TransMissionStarTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp));
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P33_1");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }
            if (ChildIdFlagP33_2) {
                Document TaskParamsTemp = new Document();
                TaskParamsTemp.append("code", "P33_2");
                int NSTimeTemp = (int) (Duration.between(zerostart, TransMissionEndTimeArray.get(i).toInstant()).getSeconds());
                TaskParamsTemp.append("value", String.format("%04X", NSTimeTemp));
                MissionInstructionArrayChild.add(TaskParamsTemp);
                Document MissionInstructionAfterParaChild = new Document();
                MissionInstructionAfterParaChild.append("code", "P33_2");
                MissionInstructionAfterParaChild.append("value", Integer.toString(NSTimeTemp));
                MissionInstructionAfterPara.add(MissionInstructionAfterParaChild);
            }

            MissionInstructionAfterArray.add(MissionInstructionAfterPara);


            ArrayList<String> MissionInstructionCodeChild = new ArrayList<>();
            ArrayList<Integer> MissionInstructionIdChild = new ArrayList<>();
            ArrayList<Date> MissionInstructionTimeChild = new ArrayList<>();
            ArrayList<String> MissionInstructionTimeStringChild = new ArrayList<>();
            ArrayList<String> MissionInstructionHexChild = new ArrayList<>();
            double InstDelta_tAll = 0;
            double InstDelta_tLastAll = 0;
            Date time_point = TransMissionStarTimeArray.get(i);
            Date time_pointEnd = TransMissionEndTimeArray.get(i);

            timeVariable.T1 = time2Second(time_point);
            timeVariable.T1d = time2Second(time_pointEnd);

            String ZhilingKuai = "";
            String workcode = "TASK04";
            for (Document document : TaskInstructionjson) {
                try {
                    //选择指令块模板
                    if (document.get("code").equals(workcode)) {
                        ArrayList<Document> SequenceArray = (ArrayList<Document>) document.get("sequence");
                        //指令块中包含的指令序列
                        for (Document document1 : SequenceArray) {
                            String YingYongShuJu = "";
                            //判定该序列是否执行
                            Boolean SequenceFlag = false;
                            Boolean AlternativeFlag = document1.containsKey("alternative");
                            if (AlternativeFlag) {
                                String Related_id = document1.get("related_param_id").toString();
                                //搜索任务中相应id的值
                                String TaskParamsValue = "";
                                ArrayList<String> TaskParamsValueP07 = new ArrayList<>();
                                if (Related_id.equals("P07")) {
                                    boolean findP07 = false;
                                    for (Document TaskParams : MissionInstructionArrayChild) {
                                        if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                            if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                TaskParamsValueP07 = (ArrayList<String>) TaskParams.get("value");
                                                findP07 = true;
                                            }
                                            break;
                                        }
                                    }

                                    if (!findP07) {
                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                                if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("value").equals("")) {
                                                    TaskParamsValueP07 = (ArrayList<String>) TaskParams.get("default_value");
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    //判定是否执行该序列
                                    if (document1.containsKey("when")) {
                                        for (int j = 0; j < TaskParamsValueP07.size(); j++) {
                                            if (document1.get("when").toString().equals(TaskParamsValueP07.get(j))) {
                                                SequenceFlag = true;
                                                break;
                                            }
                                        }
                                    } else if (document1.containsKey("when_or")) {
                                        ArrayList<String> MissionSequenceWhenOr = (ArrayList<String>) document1.get("when_or");
                                        for (String MissionSequenceWhenOrChild : MissionSequenceWhenOr) {
                                            for (int j = 0; j < TaskParamsValueP07.size(); j++) {
                                                if (MissionSequenceWhenOrChild.equals(TaskParamsValueP07.get(j))) {
                                                    SequenceFlag = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } else {

                                    boolean findInsArray = false;
                                    for (Document TaskParams : MissionInstructionArrayChild) {
                                        if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                            if (TaskParams.containsKey("value") && TaskParams.get("value") != null && !TaskParams.get("value").equals("")) {
                                                TaskParamsValue = TaskParams.get("value").toString();
                                                findInsArray = true;
                                            }
                                            break;
                                        }
                                    }
                                    if (!findInsArray) {
                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                            if (TaskParams.containsKey("code") && TaskParams.get("code").toString().equals(Related_id)) {
                                                if (TaskParams.containsKey("default_value") && TaskParams.get("default_value") != null && !TaskParams.get("default_value").equals("")) {
                                                    TaskParamsValue = TaskParams.get("default_value").toString();
                                                    findInsArray = true;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    //判定是否执行该序列
                                    if (document1.containsKey("when")) {
                                        if (document1.get("when").toString().equals(TaskParamsValue)) {
                                            SequenceFlag = true;
                                        }
                                    } else if (document1.containsKey("when_or")) {
                                        ArrayList<String> MissionSequenceWhenOr = (ArrayList<String>) document1.get("when_or");
                                        for (String MissionSequenceWhenOrChild : MissionSequenceWhenOr) {
                                            if (MissionSequenceWhenOrChild.equals(TaskParamsValue)) {
                                                SequenceFlag = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                SequenceFlag = true;
                            }

                            //执行该序列
                            if (SequenceFlag) {
                                byte ZhilingNum = 0;
                                String YouXiaoData = "";
                                InstDelta_tLastAll = 0;
                                //判定序列是否为选择类型
                                String sequencecode = "";
                                if (document1.containsKey("select")) {
                                    String Related_id = document1.get("related_param_id").toString();
                                    //搜索任务中相应id的值
                                    String TaskParamsValue = "";
                                    boolean findInsArray = false;
                                    for (Document TaskParams : MissionInstructionArrayChild) {
                                        if (TaskParams.containsKey("code") && TaskParams.get("code") != null && TaskParams.get("code").toString().equals(Related_id)) {
                                            if (TaskParams.containsKey("value") && !TaskParams.get("value").equals("")) {
                                                TaskParamsValue = TaskParams.get("value").toString();
                                                findInsArray = true;
                                            }
                                            break;
                                        }
                                    }
                                    if (!findInsArray) {
                                        for (Document TaskParams : MissionInstructionDefautArrayChild) {
                                            if (TaskParams.containsKey("code") && TaskParams.get("code") != null && TaskParams.get("code").toString().equals(Related_id)) {
                                                if (TaskParams.containsKey("default_value") && !TaskParams.get("default_value").equals("")) {
                                                    TaskParamsValue = TaskParams.get("default_value").toString();
                                                    findInsArray = true;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    Document sequencemapping = (Document) document1.get("mapping");
                                    sequencecode = sequencemapping.get(TaskParamsValue).toString();
                                } else {
                                    sequencecode = document1.get("sequence_code").toString();
                                }
                                //选择指令码模板
                                boolean MoreThanFlag = false;
                                ArrayList<String> YouXiaoshujuList = new ArrayList<>();
                                ArrayList<Byte> ZhiLingGeshuList = new ArrayList<>();
                                for (Document document2 : SequenceInstructionjson) {
                                    try {
                                        if (document2.get("code").equals(sequencecode)) {
                                            ArrayList<Document> InstsArray = (ArrayList<Document>) document2.get("inst");
                                            for (Document document3 : InstsArray) {
                                                String InstCode = "";
                                                //判断执行哪种指令码
                                                if (document3.containsKey("alternative")) {
                                                    //是否执行种类
                                                    String MetaRelated_id = document3.get("related_param_id").toString();
                                                    boolean findInsArray = false;
                                                    //搜索任务中相应id的值
                                                    String SequenceParamsValue = "";
                                                    for (Document SequenceParams : MissionInstructionArrayChild) {
                                                        if (SequenceParams.containsKey("code") && SequenceParams.get("code") != null && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                            if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                SequenceParamsValue = SequenceParams.get("value").toString();
                                                                findInsArray = true;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                    if (!findInsArray) {
                                                        for (Document SequenceParams : MissionInstructionArrayChild) {
                                                            if (SequenceParams.containsKey("code") && SequenceParams.get("code") != null && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                if (SequenceParams.containsKey("default_value") && !SequenceParams.get("default_value").equals("")) {
                                                                    SequenceParamsValue = SequenceParams.get("default_value").toString();
                                                                    findInsArray = true;
                                                                }
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (document3.containsKey("when")) {
                                                        if (document3.get("when").toString().equals(SequenceParamsValue)) {
                                                            InstCode = document3.get("inst_code").toString();
                                                        }
                                                    } else if (document3.containsKey("when_or")) {
                                                        ArrayList<String> MissionCodeWhenOr = (ArrayList<String>) document3.get("when_or");
                                                        for (String MissionCodeWhenOrChild : MissionCodeWhenOr) {
                                                            if (MissionCodeWhenOrChild.equals(SequenceParamsValue)) {
                                                                InstCode = document3.get("inst_code").toString();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                } else if (document3.containsKey("select")) {
                                                    //列表选择执行种类
                                                    String MetaRelated_id = document3.get("related_param_id").toString();
                                                    //搜索任务中相应id的值
                                                    String SequenceParamsValue = "";
                                                    boolean findInsArray = false;
                                                    for (Document SequenceParams : MissionInstructionArrayChild) {
                                                        if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                            if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                SequenceParamsValue = SequenceParams.get("value").toString();
                                                                findInsArray = true;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                    if (!findInsArray) {
                                                        for (Document SequenceParams : MissionInstructionDefautArrayChild) {
                                                            if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                if (SequenceParams.containsKey("default_value") && !SequenceParams.get("default_value").equals("")) {
                                                                    SequenceParamsValue = SequenceParams.get("default_value").toString();
                                                                }
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    Document sequencemapping = (Document) document3.get("mapping");
                                                    if (sequencemapping.containsKey(SequenceParamsValue) && sequencemapping.get(SequenceParamsValue) != null && !sequencemapping.get(SequenceParamsValue).equals("")) {
                                                        InstCode = sequencemapping.get(SequenceParamsValue).toString();
                                                    }
                                                } else {
                                                    //变参执行种类
                                                    InstCode = document3.get("inst_code").toString();
                                                }
                                                //执行间隔
                                                String InstDelta_t = "0";
                                                if (document3.get("delta_t").getClass().toString().equals("class java.lang.String")) {
                                                    InstDelta_t = document3.get("delta_t").toString();
                                                } else {
                                                    Document delta_tDocument = (Document) document3.get("delta_t");
                                                    if (delta_tDocument.containsKey("related_param_id")) {
                                                        String delta_tId = delta_tDocument.get("related_param_id").toString();
                                                        //搜索任务中相应id的值
                                                        String DeltaParamsValue = "";
                                                        boolean findInsArray = false;
                                                        for (Document SequenceParams : MissionInstructionArrayChild) {
                                                            if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(delta_tId)) {
                                                                if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                    DeltaParamsValue = SequenceParams.get("value").toString();
                                                                    findInsArray = true;
                                                                }
                                                                break;
                                                            }
                                                        }
                                                        if (!findInsArray) {//todo
                                                            for (Document SequenceParams : MissionInstructionDefautArrayChild) {
                                                                if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(delta_tId)) {
                                                                    if (SequenceParams.containsKey("default_value") && !SequenceParams.get("default_value").equals("")) {
                                                                        DeltaParamsValue = SequenceParams.get("default_value").toString();
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        if (delta_tDocument.containsKey("mapping")) {
                                                            Document delta_tMappingDocument = (Document) delta_tDocument.get("mapping");
                                                            if (delta_tMappingDocument.containsKey(DeltaParamsValue)) {
                                                                InstDelta_t = delta_tMappingDocument.get(DeltaParamsValue).toString();
                                                            }
                                                        }
                                                    } else {
                                                        InstDelta_t = document3.get("delta_t").toString();
                                                    }
                                                }
                                                InstDelta_tLastAll = InstDelta_tLastAll + Double.parseDouble(InstDelta_t);//Integer.parseInt(InstDelta_t)*0.125;
                                                //执行该指令
                                                if (InstCode != "") {
//                                                    if (InstCode.equals("TCS205"))
//                                                        System.out.println(InstCode);
                                                    for (Document document4 : MetaInstrunctionjson) {
                                                        if (document4.get("code").toString().equals(InstCode)) {
                                                            if (document4.containsKey("hex")) {
                                                                String MetaHex = document4.get("hex").toString();
                                                                byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                                if (document4.containsKey("params") && document4.get("params") != null) {
                                                                    ArrayList<Document> MetaParams = (ArrayList<Document>) document4.get("params");
                                                                    if (MetaParams.size() != 0) {
                                                                        for (Document MetaParamsChild : MetaParams) {
                                                                            //任务参数读取
                                                                            if (MetaParamsChild.containsKey("id")) {
                                                                                String MetaParamsId = MetaParamsChild.get("id").toString();
                                                                                //搜索任务中相应的id值
                                                                                boolean findInsArray = false;
                                                                                for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                    if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                        if (MissionMetaParamsChildParamsChild.containsKey("value") && !MissionMetaParamsChildParamsChild.get("value").equals("")) {
                                                                                            findInsArray = true;
                                                                                            //System.out.println(MetaParamsId);
                                                                                            String MetaParamsIdValue = MissionMetaParamsChildParamsChild.get("value").toString();
                                                                                            int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                            int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                            byte[] bytevalueHex = hexStringToBytes(MetaParamsIdValue);
                                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                    byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                }
                                                                                            }
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }

                                                                                if (!findInsArray) {
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionDefautArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                            if (MissionMetaParamsChildParamsChild.containsKey("default_value") && !MissionMetaParamsChildParamsChild.get("default_value").equals("")) {
                                                                                                findInsArray = true;
                                                                                                //System.out.println(MetaParamsId);
                                                                                                String MetaParamsIdValue = MissionMetaParamsChildParamsChild.get("default_value").toString();
                                                                                                int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                                int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                                byte[] bytevalueHex = hexStringToBytes(MetaParamsIdValue);
                                                                                                for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                    if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                        byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                    }
                                                                                                }
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } else if (MetaParamsChild.containsKey("related_param_id")) {
                                                                                String MetaParamsCode = "";
                                                                                boolean findInsArray = false;

                                                                                //列表选择执行种类
                                                                                String MetaRelated_id = MetaParamsChild.get("related_param_id").toString();
                                                                                //搜索任务中相应id的值
                                                                                String SequenceParamsValue = "";
                                                                                for (Document SequenceParams : MissionInstructionArrayChild) {
                                                                                    if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                                        if (SequenceParams.containsKey("value") && !SequenceParams.get("value").equals("")) {
                                                                                            SequenceParamsValue = SequenceParams.get("value").toString();
                                                                                            findInsArray = true;
                                                                                        }
                                                                                        break;
                                                                                    }
                                                                                }
                                                                                if (!findInsArray) {
                                                                                    for (Document SequenceParams : MissionInstructionDefautArrayChild) {
                                                                                        if (SequenceParams.containsKey("code") && SequenceParams.get("code").toString().equals(MetaRelated_id)) {
                                                                                            if (SequenceParams.containsKey("default_value") && !SequenceParams.get("default_value").equals("")) {
                                                                                                SequenceParamsValue = SequenceParams.get("default_value").toString();
                                                                                                findInsArray = true;
                                                                                            }
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                                if (MetaParamsChild.containsKey("mapping")) {
                                                                                    Document sequencemapping = (Document) MetaParamsChild.get("mapping");
                                                                                    if (sequencemapping.containsKey(SequenceParamsValue) && sequencemapping.get(SequenceParamsValue) != null && !sequencemapping.get(SequenceParamsValue).equals("")) {
                                                                                        MetaParamsCode = sequencemapping.get(SequenceParamsValue).toString();
                                                                                        int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                        int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                        byte[] bytevalueHex = hexStringToBytes(MetaParamsCode);
                                                                                        for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                            if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                } else {
                                                                                    int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                    int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                    byte[] bytevalueHex = hexStringToBytes(SequenceParamsValue);
                                                                                    for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                        if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                            byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                        }
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                String MetaParamsCode = MetaParamsChild.get("code").toString();
                                                                                boolean findInsArray = false;

                                                                                //搜索任务中相应的code值
                                                                                for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                    if (MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsCode)) {
                                                                                        String MetaParamsCodeValue = MissionMetaParamsChildParamsChild.get("value").toString();
                                                                                        findInsArray = true;
                                                                                        int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                        int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                        byte[] bytevalueHex = hexStringToBytes(MetaParamsCodeValue);
                                                                                        for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                            if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                            }
                                                                                        }
                                                                                        break;
                                                                                    }
                                                                                }

                                                                                if (!findInsArray) {
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionDefautArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsCode)) {
                                                                                            String MetaParamsCodeValue = MissionMetaParamsChildParamsChild.get("default_value").toString();
                                                                                            findInsArray = true;
                                                                                            int byteIndex = MetaParamsChild.getInteger("byte_index") - 7;
                                                                                            int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                                            byte[] bytevalueHex = hexStringToBytes(MetaParamsCodeValue);
                                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                                if (j < byteMetaHex.length && j - byteIndex < bytevalueHex.length) {
                                                                                                    byteMetaHex[j] = bytevalueHex[j - byteIndex];
                                                                                                }
                                                                                            }
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                MetaHex = bytesToHexString(byteMetaHex);
                                                                byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                                String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                                int ZhiXingJianGeInt = (new Double(Double.parseDouble(InstDelta_t) / 0.125)).intValue();
                                                                byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt >> 8) & 0xFF), (byte) ((ZhiXingJianGeInt) & 0xFF)};
                                                                String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                                //String APIDString = "FFFF";//????????????
                                                                String APIDString = "0" + document4.get("apid").toString();
                                                                if ((YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString).length() > 1000) {
                                                                    String youxiaoDataTemp = YouXiaoData;
                                                                    YouXiaoshujuList.add(youxiaoDataTemp);
                                                                    Byte ZhilingNumTemp = ZhilingNum;
                                                                    ZhiLingGeshuList.add(ZhilingNumTemp);
                                                                    MoreThanFlag = true;
                                                                    YouXiaoData = "";
                                                                    ZhilingNum = 0;
                                                                }
                                                                YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                                ZhilingNum = (byte) (ZhilingNum + 1);
                                                            } else {
                                                                //添加特殊指令
                                                                String MetaHex = "";
                                                                if (InstCode.equals("K4402")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0101";
                                                                } else if (InstCode.equals("K4403")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0202";
                                                                } else if (InstCode.equals("K4404")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0303";
                                                                } else if (InstCode.equals("K4405")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0404";
                                                                } else if (InstCode.equals("K4406")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0505";
                                                                } else if (InstCode.equals("K4407")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0606";
                                                                } else if (InstCode.equals("K4408")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0707";
                                                                } else if (InstCode.equals("K4409")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A002";
                                                                    MetaHex = MetaHex + "0808";
                                                                } else if (InstCode.equals("K4410")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0101";
                                                                } else if (InstCode.equals("K4411")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0202";
                                                                } else if (InstCode.equals("K4412")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0303";
                                                                } else if (InstCode.equals("K4413")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0404";
                                                                } else if (InstCode.equals("K4414")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0505";
                                                                } else if (InstCode.equals("K4415")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0606";
                                                                } else if (InstCode.equals("K4416")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0707";
                                                                } else if (InstCode.equals("K4417")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A102";
                                                                    MetaHex = MetaHex + "0808";
                                                                } else if (InstCode.equals("K4419")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A200";
                                                                } else if (InstCode.equals("K4420")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A400";
                                                                } else if (InstCode.equals("K4425")) {
                                                                    MetaHex = "100280210118";
                                                                    MetaHex = MetaHex + "A81C";
                                                                } else if (InstCode.equals("NTCY200")) {
                                                                    MetaHex = "10FFFF210114";
                                                                    for (int j = 13; j <= 258; j++) {
                                                                        MetaHex = MetaHex + "00";
                                                                    }
                                                                } else {
                                                                    break;
                                                                }
                                                                byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                                byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                                String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                                int ZhiXingJianGeInt = (new Double(Double.parseDouble(InstDelta_t) / 0.125)).intValue();
                                                                byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt >> 8) & 0xFF), (byte) ((ZhiXingJianGeInt) & 0xFF)};
                                                                String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                                //String APIDString = "FFFF";//????????????
                                                                String APIDString = "0" + document4.get("apid").toString();
                                                                if ((YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString).length() > 1000) {
                                                                    String youxiaoDataTemp = YouXiaoData;
                                                                    YouXiaoshujuList.add(youxiaoDataTemp);
                                                                    Byte ZhilingNumTemp = ZhilingNum;
                                                                    ZhiLingGeshuList.add(ZhilingNumTemp);
                                                                    MoreThanFlag = true;
                                                                    YouXiaoData = "";
                                                                    ZhilingNum = 0;
                                                                }
                                                                YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                                ZhilingNum = (byte) (ZhilingNum + 1);
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        continue;
                                    }
                                }
                                if (MoreThanFlag) {
                                    for (int j = 0; j < YouXiaoshujuList.size(); j++) {
                                        if (YouXiaoshujuList.get(j).length() <= 4) {
                                            continue;
                                        }
                                        int ZhiLingIDNum = SequenceID.SequenceId;
                                        SequenceID.SequenceId = SequenceID.SequenceId + 1;
                                        if (SequenceID.SequenceId > 255) {
                                            SequenceID.SequenceId = 0;
                                        }
                                        byte[] ZhiLingXuLieIDByte = new byte[]{(byte) ((ZhiLingIDNum) & 0xFF)};
                                        String ZhiLingXuLieIDString = bytesToHexString(ZhiLingXuLieIDByte);
                                        ZhiLingXuLieIDString = "00" + ZhiLingXuLieIDString;
                                        byte[] ZhiLingGeShu = new byte[]{ZhiLingGeshuList.get(j)};
                                        String ZhiLingGeShuString = bytesToHexString(ZhiLingGeShu);

                                        Instant time_ZhiXing = time_point.toInstant();
                                        time_ZhiXing = time_ZhiXing.plusSeconds((long) (InstDelta_tAll));
                                        Date time_ZhixingDate = Date.from(time_ZhiXing);
                                        //String KaiShiShiJian=time2String(time_ZhixingDate);
                                        //System.out.println(workcode);
                                        //System.out.println(sequencecode);
                                        //System.out.println(sequencecode);
                                        //System.out.println(Integer.parseInt(KaiShiShiJian,16));
                                        //最后一条指令时间间隔
                                        String YouXiaoDataTemp;
                                        if (YouXiaoshujuList.get(j).length() <= 4) {
                                            continue;
                                        } else {
                                            YouXiaoDataTemp = YouXiaoshujuList.get(j).substring(0, YouXiaoshujuList.get(j).length() - 4);
                                        }
                                        YingYongShuJu = ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoDataTemp;
                                        ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                        String YingYongShuJuTemp = YingYongShuJu;
                                        MissionInstructionHexChild.add(YingYongShuJuTemp);
                                        MissionInstructionCodeChild.add(sequencecode);
                                        MissionInstructionIdChild.add(ZhiLingIDNum);
                                        MissionInstructionTimeChild.add(time_ZhixingDate);
                                    }
                                }
                                if (YouXiaoData.length() <= 4) {
                                    continue;
                                }
                                int ZhiLingIDNum = SequenceID.SequenceId;
                                SequenceID.SequenceId = SequenceID.SequenceId + 1;
                                if (SequenceID.SequenceId > 255) {
                                    SequenceID.SequenceId = 0;
                                }
                                byte[] ZhiLingXuLieIDByte = new byte[]{(byte) ((ZhiLingIDNum) & 0xFF)};
                                String ZhiLingXuLieIDString = bytesToHexString(ZhiLingXuLieIDByte);
                                ZhiLingXuLieIDString = "00" + ZhiLingXuLieIDString;
                                byte[] ZhiLingGeShu = new byte[]{ZhilingNum};
                                String ZhiLingGeShuString = bytesToHexString(ZhiLingGeShu);

                                Instant time_ZhiXing = time_point.toInstant();
                                time_ZhiXing = time_ZhiXing.plusSeconds((long) (InstDelta_tAll));
                                Date time_ZhixingDate = Date.from(time_ZhiXing);
                                //String KaiShiShiJian=time2String(time_ZhixingDate);
                                //String KaiShiShiJian=timeHashMap.get(sequencecode).ExecutionTime(timeVariable,workcode).toUpperCase();
                                //最后一条指令时间间隔
                                //System.out.println(sequencecode);
                                if (YouXiaoData.length() <= 4) {
                                    continue;
                                } else {
                                    YouXiaoData = YouXiaoData.substring(0, YouXiaoData.length() - 4);
                                }
                                YingYongShuJu = ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoData;
                                ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                MissionInstructionHexChild.add(YingYongShuJu);
                                MissionInstructionCodeChild.add(sequencecode);
                                MissionInstructionIdChild.add(ZhiLingIDNum);
                                MissionInstructionTimeChild.add(time_ZhixingDate);
                                InstDelta_tAll = InstDelta_tAll + InstDelta_tLastAll;
                            }
                        }
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }

            Map<String, String> it = new HashMap<>();
            //更新执行时间
            for (int j = 0; j < MissionInstructionHexChild.size(); j++) {
                String KaiShiShiJian = timeHashMap.get(MissionInstructionCodeChild.get(j)).ExecutionTime(timeVariable, workcode).toUpperCase();
                if (KaiShiShiJian.length() < 8) {
                    for (int k = KaiShiShiJian.length(); k < 8; k++) {
                        KaiShiShiJian = "0" + KaiShiShiJian;
                    }
                } else if (KaiShiShiJian.length() > 8) {
                    KaiShiShiJian = KaiShiShiJian.substring(KaiShiShiJian.length() - 8);
                }
                String YingYongShuJuTemp = MissionInstructionHexChild.get(j);
                MissionInstructionHexChild.set(j, KaiShiShiJian + YingYongShuJuTemp);
                Instant time_ZhiXing = zerostart;
                time_ZhiXing = time_ZhiXing.plusSeconds((long) (Integer.parseInt(KaiShiShiJian, 16)));
                Date time_ZhixingDate = Date.from(time_ZhiXing);
                MissionInstructionTimeChild.set(j, time_ZhixingDate);
                MissionInstructionTimeStringChild.add(j, KaiShiShiJian);
                //System.out.println("序列号：" + MissionInstructionCodeChild.get(j));
                //System.out.println("执行时间：" + Integer.parseInt(KaiShiShiJian, 16));

                //it.put(MissionInstructionCodeChild.get(j) + "_" + Instant.now().toEpochMilli(), time_ZhixingDate.toString());
            }
            //insTimeMap.put(i, it);

            ArrayList<byte[]> InstructionArrayChild = new ArrayList<>();
            for (String ZhilingKuai_02 : MissionInstructionHexChild) {
                //String YingYongShuJu = KaiShiShiJian + ZhiLingID + ZhiLingNum + YouXiaoData;
                String ShuJuQuTou = "100B8021";
                int BaoChang = (ShuJuQuTou + ZhilingKuai_02).length() / 2 + 2 - 1;
                String BaoChangstr = String.format("%04X", BaoChang);
                int BaoXuLieIDNum = SequenceID.PackageId;
                SequenceID.PackageId = SequenceID.PackageId + 1;
                if (SequenceID.PackageId > 16383) {
                    SequenceID.PackageId = 0;
                }
                String BaoXuLieIDStr = Integer.toBinaryString(BaoXuLieIDNum);
                if (BaoXuLieIDStr.length() < 14) {
                    for (int i_id = BaoXuLieIDStr.length(); i_id < 14; i_id++) {
                        BaoXuLieIDStr = "0" + BaoXuLieIDStr;
                    }
                } else {
                    BaoXuLieIDStr = BaoXuLieIDStr.substring(BaoXuLieIDStr.length() - 14);
                }
                BaoXuLieIDStr = "11" + BaoXuLieIDStr;
                BaoXuLieIDStr = Integer.toHexString(Integer.parseInt(BaoXuLieIDStr, 2)).toUpperCase();
                String BaoZhuDaoTou = "1C11" + BaoXuLieIDStr + BaoChangstr;
                String total = BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai_02 + ISO(BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai_02);

                int fangshizi = -1;
                //添加填充域
                if (total.length() <= 62 * 2) {
                    for (int j = total.length() / 2; j < 62; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 0;
                } else if (total.length() > 62 * 2 && total.length() <= 126 * 2) {
                    for (int j = total.length() / 2; j < 126; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 1;
                } else if (total.length() > 126 * 2 && total.length() <= 254 * 2) {
                    for (int j = total.length() / 2; j < 254; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 2;
                } else if (total.length() > 254 * 2 && total.length() <= 510 * 2) {
                    for (int j = total.length() / 2; j < 510; j++) {
                        total = total + "A5";
                    }
                    fangshizi = 3;
                } else {
                    //分两包
                }

                byte[] MainBuff = hexStringToBytes(total);
                int a = getCRC_0xFFFF(MainBuff, MainBuff.length);
                String CRCCode = String.format("%04X", a).toUpperCase();
                if (CRCCode.length() > 4) {
                    CRCCode = CRCCode.substring(CRCCode.length() - 4);
                } else if (CRCCode.length() < 4) {
                    for (int j = CRCCode.length(); j < 4; j++) {
                        CRCCode = "0" + CRCCode;
                    }
                }

                if (fangshizi == -1 || fangshizi == 0)
                    total = "EB90762599" + total + CRCCode;
                else if (fangshizi == 1)
                    total = "EB90762569" + total + CRCCode;
                else if (fangshizi == 2)
                    total = "EB907625A5" + total + CRCCode;
                else
                    total = "EB907625C3" + total + CRCCode;
                byte[] bytes = hexStringToBytes(total);
                InstructionArrayChild.add(bytes);
            }
            InstructionArray.add(i, InstructionArrayChild);

            MissionInstructionCode.add(i, MissionInstructionCodeChild);
            MissionInstructionId.add(i, MissionInstructionIdChild);
            MissionInstructionTime.add(i, MissionInstructionTimeChild);
            MissionInstructionHex.add(i, MissionInstructionHexChild);
            MissionInstructionTimeString.add(i, MissionInstructionTimeStringChild);
        }

        //指令输出
        //??????文件夹以任务编号，文件夹内文件：指令序列编号-时间-任务编号-序列ID
        ArrayList<Document> InstructionInfojsonArry = new ArrayList<>();
        for (int i = 0; i < MissionInstructionCode.size(); i++) {
            //指令输出
            String FileFolder = FilePath + "\\" + TransMissionNumbers.get(0);
            FileFolder = FilePathUtil.getRealFilePath(FileFolder);
            File file = new File(FileFolder);
            if (i == 0) {
                if (!file.exists()) {
                    //如果文件夹不存在，新建
                    file.mkdirs();
                } else {
                    //如果文件夹存在，删除文件夹内所有文件
                    if (file.isDirectory()) {
                        File[] files = file.listFiles();
                        for (File f : files) {
                            f.delete();
                        }
                    }
                }
            }
            Map<String, String> it = new HashMap<>();
            for (int j = 0; j < InstructionArray.get(i).size(); j++) {
                //指令文件命名
                Date cal = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String StringTime = sdf.format(cal.getTime());

                String DateString = StringTime.substring(0, 4) + StringTime.substring(5, 7) + StringTime.substring(8, 10) + StringTime.substring(11, 13) + StringTime.substring(14, 16);
                String FileName = MissionInstructionId.get(i).get(j) + "-" + MissionInstructionCode.get(i).get(j) + "-" + DateString + "-" + TransMissionNumbers.get(0);
                String realPath = FilePathUtil.getRealFilePath(FileFolder + "\\" + FileName);
                bytesTotxt(InstructionArray.get(i).get(j), realPath);

                System.out.println("序列号：" + MissionInstructionCode.get(i).get(j));
                System.out.println("执行时间：" + Integer.parseInt(MissionInstructionTimeString.get(i).get(j), 16));

                it.put(MissionInstructionId.get(i).get(j) + "_" + MissionInstructionCode.get(i).get(j) + "_" + Instant.now().toEpochMilli(), MissionInstructionTime.get(i).get(j).toString());
            }
            insTimeMap.put(i, it);

            String InsTimeFileName = "Ins-Time-" + Instant.now().toEpochMilli() + ".txt";
            String InsTimeFileNameRealPath = FilePathUtil.getRealFilePath(FileFolder + "\\" + InsTimeFileName);

            StringToFile(insTimeMap.get(i), InsTimeFileNameRealPath);

            //数据库传出
            if (MissionInstructionCode.get(i).size() > 0) {
                for (int j = 0; j < MissionInstructionCode.get(i).size(); j++) {
                    Document InstructionInfojsonObject = new Document();
                    InstructionInfojsonObject.append("sequence_code", MissionInstructionCode.get(i).get(j));
                    InstructionInfojsonObject.append("sequence_id", MissionInstructionId.get(i).get(j));
                    InstructionInfojsonObject.append("execution_time", MissionInstructionTime.get(i).get(j));
                    InstructionInfojsonObject.append("valid", true);
                    InstructionInfojsonArry.add(InstructionInfojsonObject);
                }
            } else {
                Document InstructionInfojsonObject = new Document();
                InstructionInfojsonObject.append("sequence_code", "");
                InstructionInfojsonObject.append("sequence_id", "");
                InstructionInfojsonObject.append("execution_time", "");
                InstructionInfojsonObject.append("valid", "");
                InstructionInfojsonArry.add(InstructionInfojsonObject);
            }
        }
        TransmissionMissionJson.append("instruction_info", InstructionInfojsonArry);
        TransmissionMissionJson.append("mission_params_after_planning", MissionInstructionAfterArray);
        Document modifiers = new Document();
        modifiers.append("$set", TransmissionMissionJson);
        MongoCollection<Document> image_mission = mongoDatabase.getCollection("transmission_mission");
        image_mission.updateOne(new Document("transmission_number", TransmissionMissionJson.get("transmission_number").toString()), modifiers, new UpdateOptions().upsert(true));

        InstructionManager instructionManager = new InstructionManager();
        instructionManager.addInstrctionInfo(InstructionInfojsonArry, TransmissionMissionJson.get("transmission_number").toString(), "数传任务");
        instructionManager.close();

        return true;
    }

    //byte型数组按二进制输出到txt文件
    private static void bytesTotxt(byte[] bytes, String FilePath) {
        File target = new File(FilePath);
        //如果文件存在，删除
        if (target.exists() && target.isFile()) {
            boolean flag = target.delete();
        }
        try {
            if (target.createNewFile()) {
                DataOutputStream out = new DataOutputStream(new FileOutputStream(FilePath, true));
                out.write(bytes);
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void StringToFile(Map<String, String> it, String FilePath) {
        File target = new File(FilePath);
        //如果文件存在，删除
        if (target.exists() && target.isFile()) {
            boolean flag = target.delete();
        }

        try {
            if (target.createNewFile()) {

                Writer out = new FileWriter(FilePath);

                for (String s : it.keySet()) {
                    out.write("ins:");
                    out.write(s);
                    out.write("   time:");
                    out.write(it.get(s));
                    out.write("\r\n");
                }
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //byte型数组转化为16进制字符串，0101010
    private static String bytesToHexStringold(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toBinaryString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    //byte型数组转化为16进制字符串
    private static String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < src.length; i++) {
            if ((src[i] & 0xFF) < 0x10)//0~F前面补零
                hexString.append("0");
            hexString.append(Integer.toHexString(0xFF & src[i]).toUpperCase());
        }
        return hexString.toString().toUpperCase();
    }

    //16进制字符串转化为byte[]数组
    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    //char字符数据类型转化为byte字节数据类型
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    //double型数据转化为byte数组，长度为4个字节
    private static byte[] double2Bytes(double d) {
        float dd = (float) d;
        int value = Float.floatToRawIntBits(dd);
        byte[] byteRet = new byte[4];
        for (int i = 0; i < 4; i++) {
            byteRet[i] = (byte) ((value >> 8 * i) & 0xFF);
        }
        return byteRet;
    }

    //byte数组转化为double型数据，长度为四个字节
    private static double bytes2Double(byte[] arr) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((int) (arr[i] & 0xFF)) << (8 * i);
        }
        return (double) (Float.intBitsToFloat(value));
    }

    //时间转化为开始时间字符串
    private static String time2String(Date time_point) {
        //时间转换为doubule型
        String StringTime;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time_point);
        cal.add(Calendar.HOUR_OF_DAY, -8);
        StringTime = sdf.format(cal.getTime());
        double[] TimeStarTime = new double[6];
        TimeStarTime[0] = Double.parseDouble(StringTime.substring(0, 4));
        TimeStarTime[1] = Double.parseDouble(StringTime.substring(5, 7));
        TimeStarTime[2] = Double.parseDouble(StringTime.substring(8, 10));
        TimeStarTime[3] = Double.parseDouble(StringTime.substring(11, 13));
        TimeStarTime[4] = Double.parseDouble(StringTime.substring(14, 16));
        TimeStarTime[5] = Double.parseDouble(StringTime.substring(17, 19));
        int TimeMiddle = new Double((JD(TimeStarTime) - JD(ZeroTime)) * 24 * 60 * 60).intValue();
        String KaiShiShiJian = Integer.toHexString(TimeMiddle);

        return KaiShiShiJian;
    }

    //计算当前时间和基准时间的时间间隔的秒数
    private static double time2SecondK4425(Date time_point) {
        //时间转换为doubule型
        String StringTime;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time_point);
        cal.add(Calendar.HOUR_OF_DAY, -8);
        StringTime = sdf.format(cal.getTime());
        double[] TimeStarTime = new double[6];
        TimeStarTime[0] = Double.parseDouble(StringTime.substring(0, 4));
        TimeStarTime[1] = Double.parseDouble(StringTime.substring(5, 7));
        TimeStarTime[2] = Double.parseDouble(StringTime.substring(8, 10));
        TimeStarTime[3] = Double.parseDouble(StringTime.substring(11, 13));
        TimeStarTime[4] = Double.parseDouble(StringTime.substring(14, 16));
        TimeStarTime[5] = Double.parseDouble(StringTime.substring(17, 19));
        double[] ZeroTimeK4425 = {2018, 1, 1, 0, 0, 0};//参考时间
        double TimeMiddle = (JD(TimeStarTime) - JD(ZeroTimeK4425)) * 24 * 60 * 60 * 1000000;

        return TimeMiddle;
    }

    //计算当前时间和基准时间的时间间隔的秒数
    private static double time2Second(Date time_point) {
        //时间转换为doubule型
        String StringTime;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time_point);
        cal.add(Calendar.HOUR_OF_DAY, -8);
        StringTime = sdf.format(cal.getTime());
        double[] TimeStarTime = new double[6];
        TimeStarTime[0] = Double.parseDouble(StringTime.substring(0, 4));
        TimeStarTime[1] = Double.parseDouble(StringTime.substring(5, 7));
        TimeStarTime[2] = Double.parseDouble(StringTime.substring(8, 10));
        TimeStarTime[3] = Double.parseDouble(StringTime.substring(11, 13));
        TimeStarTime[4] = Double.parseDouble(StringTime.substring(14, 16));
        TimeStarTime[5] = Double.parseDouble(StringTime.substring(17, 19));
        double TimeMiddle = (JD(TimeStarTime) - JD(ZeroTime)) * 24 * 60 * 60;

        return TimeMiddle;
    }

    //读取csv文件
    private static String[] CSVRead(String CSVFilePath) {
        String[] item = {};
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CSVFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String info[] = line.split(",");
                int iteml = item.length;
                int infol = info.length;
                item = Arrays.copyOf(item, iteml + infol);//填充
                System.arraycopy(info, 0, item, iteml, infol);//填充数组
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    //ISO和校验算法
    private static String ISO(String Frame) {
        int C0 = 0;
        int C1 = 0;
        for (int i = 0; i < Frame.length(); i = i + 2) {
            int B = Integer.parseInt(Frame.substring(i, i + 2), 16);
            C0 = (C0 + B) % 255;
            C1 = (C1 + C0) % 255;
        }
//        int CK1 = 0 - ((C0 + C1) % 255);
        int CK1 = (-(C0 + C1)) % 255;
        if (CK1 < 0) {
            CK1 = CK1 + 255;
        }
        int CK2 = C1;
        String CK1tot = String.format("%02X", CK1).toUpperCase();
        String CK2tot = String.format("%02X", CK2).toUpperCase();
        String CK1str = CK1tot;
        String CK2str = CK2tot;
        if (CK1tot.length() > 2) {
            CK1str = CK1tot.substring(CK1tot.length() - 2);
        }
        if (CK2tot.length() > 2) {
            CK2str = CK2tot.substring(CK2tot.length() - 2);
        }
        if (CK1str.equals("00")) {
            CK1str = "FF";
        }
        if (CK2str.equals("00")) {
            CK2str = "FF";
        }
        return CK1str + CK2str;
    }


    //CRC校验
    private static int getCRC_0xFFFFold(byte[] data, int len)        //CRC校验
    {
        byte hbit = 0;
        int crc = 0xffff;
        for (int i = 0; i < len; i++) {
            hbit = (byte) ((crc & 0xff00) >> 8);
            crc <<= 8;
            int index_i = bytesToInt((byte) (hbit ^ data[i]));
            crc ^= crc0xFFFF_table_[index_i];
        }
        return crc;
    }

    private static int getCRC_0xFFFF(byte[] buffer, int len)        //CRC校验
    {
        int wCRCin = 0xffff;
        int wCPoly = 0x8408;
        for (byte b : buffer) {
            wCRCin ^= ((int) b & 0x00ff);
            for (int j = 0; j < 8; j++) {
                if ((wCRCin & 0x0001) != 0) {
                    wCRCin >>= 1;
                    wCRCin ^= wCPoly;
                } else {
                    wCRCin >>= 1;
                }
            }
        }
        return wCRCin ^= 0xffff;
    }

    //byte数据转化为Int型整型
    private static int bytesToInt(byte bytes) {

        int int1 = bytes & 0xff;
        int int2 = 0xff;
        int int3 = 0xff;
        int int4 = 0xff;
        return int1 | int2 | int3 | int4;
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

    private static int[] crc0xFFFF_table_ =
            {
                    0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
                    0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
                    0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
                    0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
                    0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
                    0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
                    0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
                    0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
                    0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
                    0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
                    0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
                    0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
                    0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
                    0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
                    0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
                    0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
                    0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
                    0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
                    0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
                    0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
                    0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
                    0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
                    0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
                    0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
                    0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
                    0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
                    0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
                    0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
                    0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
                    0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
                    0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
                    0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
            };

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

//    private static ArrayList<double[]> GetGeometryPoint(Document geometry) {
//        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
//        if (geometry.get("type").equals("Point")) {
//            ArrayList<Double> coordinates = (ArrayList<Double>) geometry.get("coordinates");
//            double[] Target = new double[2];
//            Target[0] = coordinates.get(0);
//            Target[1] = coordinates.get(1);
//            CoordinatesList.add(Target);
//        } else if (geometry.get("type").equals("LineString")) {
//            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
//            for (ArrayList<Double> document : coordinates) {
//                double[] Target = new double[2];
//                Target[0] = document.get(0);
//                Target[1] = document.get(1);
//                CoordinatesList.add(Target);
//            }
//        } else if (geometry.get("type").equals("Polygon")) {
//            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
//            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
//                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
//                    double[] Target = new double[2];
//                    Target[0] = subsubcoordinates.get(0);
//                    Target[1] = subsubcoordinates.get(1);
//                    CoordinatesList.add(Target);
//                }
//            }
//        } else if (geometry.get("type").equals("MultiPoint")) {
//            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
//            for (ArrayList<Double> document : coordinates) {
//                double[] Target = new double[2];
//                Target[0] = document.get(0);
//                Target[1] = document.get(1);
//                CoordinatesList.add(Target);
//            }
//        } else if (geometry.get("type").equals("MultiLineString")) {
//            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
//            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
//                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
//                    double[] Target = new double[2];
//                    Target[0] = subsubcoordinates.get(0);
//                    Target[1] = subsubcoordinates.get(1);
//                    CoordinatesList.add(Target);
//                }
//            }
//        } else if (geometry.get("type").equals("MultiPolygon")) {
//            ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> coordinates = (ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>) geometry.get("coordinates");
//            for (ArrayList<ArrayList<ArrayList<Double>>> subcoordinates : coordinates) {
//                for (ArrayList<ArrayList<Double>> subsubcoordinates : subcoordinates) {
//                    for (ArrayList<Double> subsubsubcoordinates : subsubcoordinates) {
//                        double[] Target = new double[2];
//                        Target[0] = subsubsubcoordinates.get(0);
//                        Target[1] = subsubsubcoordinates.get(1);
//                        CoordinatesList.add(Target);
//                    }
//                }
//            }
//        }
//
//        return CoordinatesList;
//    }

    private static ArrayList<double[]> GetGeometryPoint(Document geometry) {
        ArrayList<double[]> CoordinatesList = new ArrayList<double[]>();
        if (geometry.get("type").equals("Point")) {
            ArrayList<Object> coordinates = (ArrayList<Object>) geometry.get("coordinates");
            double[] Target = new double[2];
            if (coordinates.get(0).getClass().getName().equals("java.lang.Double")) {
                Target[0] = (Double) coordinates.get(0);
            } else {
                Target[0] = (double) (Integer) coordinates.get(0);
            }
            if (coordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                Target[1] = (Double) coordinates.get(1);
            } else {
                Target[1] = (double) (Integer) coordinates.get(1);
            }
            CoordinatesList.add(Target);
        } else if (geometry.get("type").equals("LineString")) {
            ArrayList<ArrayList<Object>> coordinates = (ArrayList<ArrayList<Object>>) geometry.get("coordinates");
            for (ArrayList<Object> document : coordinates) {
                double[] Target = new double[2];
                if (document.get(0).getClass().getName().equals("java.lang.Double")) {
                    Target[0] = (Double) document.get(0);
                } else {
                    Target[0] = (double) (Integer) document.get(0);
                }
                if (document.get(1).getClass().getName().equals("java.lang.Double")) {
                    Target[1] = (Double) document.get(1);
                } else {
                    Target[1] = (double) (Integer) document.get(1);
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
                    } else {
                        Target[0] = (double) (Integer) subsubcoordinates.get(0);
                    }
                    if (subsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                        Target[1] = (Double) subsubcoordinates.get(1);
                    } else {
                        Target[1] = (double) (Integer) subsubcoordinates.get(1);
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
                } else {
                    Target[0] = (double) (Integer) document.get(0);
                }
                if (document.get(1).getClass().getName().equals("java.lang.Double")) {
                    Target[1] = (Double) document.get(1);
                } else {
                    Target[1] = (double) (Integer) document.get(1);
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
                    } else {
                        Target[0] = (double) (Integer) subsubcoordinates.get(0);
                    }
                    if (subsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                        Target[1] = (Double) subsubcoordinates.get(1);
                    } else {
                        Target[1] = (double) (Integer) subsubcoordinates.get(1);
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
                        } else {
                            Target[0] = (double) (Integer) subsubsubcoordinates.get(0);
                        }
                        if (subsubsubcoordinates.get(1).getClass().getName().equals("java.lang.Double")) {
                            Target[1] = (Double) subsubsubcoordinates.get(1);
                        } else {
                            Target[1] = (double) (Integer) subsubsubcoordinates.get(1);
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

    //地固直角坐标系转换为地心地固坐标系
    private static void LLAToECEF(double Position_LLA[], double Position_ECEF[]) {
        double L = Position_LLA[0] * Math.PI / 180.0;
        double B = Position_LLA[1] * Math.PI / 180.0;
        double H = Position_LLA[2];

        Position_ECEF[0] = (Re + H) * Math.cos(B) * Math.cos(L);
        Position_ECEF[1] = (Re + H) * Math.cos(B) * Math.sin(L);
        Position_ECEF[2] = (Re + H) * Math.sin(B);
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

    //控温阈值计算公式
    private static String TemperatureFlotToStr(float tem) {
        String temstr = Integer.toHexString(Float.floatToIntBits(tem));
        if (temstr.length() < 4) {
            for (int j = temstr.length() + 1; j <= 4; j++) {
                temstr = "0" + temstr;
            }
        } else if (temstr.length() > 4) {
            temstr = temstr.substring(temstr.length() - 4);
        }
        return temstr;
    }

    //private static double TCKG01(){
    //return 10;
    //}

    public static void main(String[] args) {
        //连接数据库
        String str = "1C11C0000019100B80210405A40800300101E30A10028021010F40020002";
        String iso = ISO(str);
        System.out.println(iso);
    }
}
