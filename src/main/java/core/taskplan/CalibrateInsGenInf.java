package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.FilePathUtil;
import common.mongo.MangoDBConnector;
import core.taskplan.InstructionSequenceTime.SequenceID;
import core.taskplan.InstructionSequenceTime.SequenceTime;
import core.taskplan.InstructionSequenceTime.TimeMap;
import core.taskplan.InstructionSequenceTime.TimeVariable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class CalibrateInsGenInf {
    private static double[] ZeroTime = {2018, 1, 1, 0, 0, 0};//参考时间
    private static Instant ZeroTimeIns = Instant.parse("2018-01-01T00:00:00.00Z");

    //黑体定标
    public static String CalibrateInsGenInfII(Document CalibrateOrder, String FilePath) {
        if (CalibrateOrder != null && CalibrateOrder.size() > 0) {
            //生成任务
            ArrayList<Document> ImageOrderjson = new ArrayList<>();
            ImageOrderjson.add(CalibrateOrder);
            OrderOverall.OrderOverallII(ImageOrderjson);
        } else {
            return FilePath;
        }

        //序列时间设置
        TimeVariable timeVariable = new TimeVariable();
        timeVariable.TSC = 6 + 0.25 + 0.125 + 0.125 + 0.125 + 0.125 + 240 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 +
                        0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 + 0.125 +
                        0.125 + 8 + 0.125 + 0.125 + 0.125 + 8;
        timeVariable.TGF = 600 + 600 + 2 + 2 + 20 + 20 + 2 + 2 + 2 + 2 + 2 + 2 + 90 + 70 + 90 + 600 + 90 + 70 + 90 + 90 + 70 + 90 + 600 + 90 + 70 + 90 + 10 + 10 + 10;
        timeVariable.TDG1 = 600 + 2 + 20 + 2 + 20 + 2 + 50 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 +
                2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 +
                2 + 2 + 2 + 32 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 + 90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 +
                32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 600 + 2 + 14 +
                90 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 2 + 14 + 2 + 2 + 2 + 2 + 2 + 32 + 90 + 10 + 10;
        timeVariable.TDG2 = 2 + 50 + 2 + 42 + 2 + 2 + 2 + 2 + 2 + 2;
        timeVariable.T4401 = 720;
        timeVariable.P24 = 720;
        timeVariable.P29 = 100;
        timeVariable.TTCAG04 =6 + 0.25 + 0.125 + 0.125 + 0.125 + 0.125 + 1 + 1 + 1 + 1 + 1 + 1 + 1;

        HashMap<String, SequenceTime> timeHashMap = new TimeMap().timeHashMap();
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
        for (Document document : properties) {
            if (document.getString("key").equals("t0")) {
                zerostart = document.getDate("value").toInstant();
                ZeroTimeIns=zerostart;
                LocalDateTime zerostart0 = LocalDateTime.ofInstant(zerostart, ZoneOffset.UTC);
                ZeroTime[0] = zerostart0.getYear();
                ZeroTime[1] = zerostart0.getMonthValue();
                ZeroTime[2] = zerostart0.getDayOfMonth();
                ZeroTime[3] = zerostart0.getHour();
                ZeroTime[4] = zerostart0.getMinute();
                ZeroTime[5] = zerostart0.getSecond();
            } else if (document.getString("key").equals("cam_force_powoff")) {
                Cam_force_Powoff = document.get("value").toString();
            }
        }

        //任务表
        MongoCollection<Document> Data_Missionjson = mongoDatabase.getCollection("image_mission");
        String Find_MissionNumber = CalibrateOrder.get("mission_number").toString();
        Bson queryBsonMission = Filters.eq("mission_number", Find_MissionNumber);
        FindIterable<Document> D_Missionjson = Data_Missionjson.find(queryBsonMission);
        ArrayList<Document> ImageMissionjson = new ArrayList<>();
        for (Document document : D_Missionjson) {
            ImageMissionjson.add(document);
        }

        //成像任务读入
        //任务读入
        ArrayList<String> MissionNumberArray = new ArrayList<>();
        ArrayList<Object> MissionInstructionArray = new ArrayList<>();
        ArrayList<Date> MissionStarTimeArray = new ArrayList<>();
        ArrayList<Date> MissionEndTimeArray = new ArrayList<>();
        ArrayList<String> MissionWorkModel = new ArrayList<>();
        ArrayList<String> MissionImageModel = new ArrayList<>();
        ArrayList<ArrayList<double[]>> MissionTargetAreaList = new ArrayList<>();
        ArrayList<Object> MissionInstructionDefautArray = new ArrayList<>();
        int MissionNum = 0;
        if (ImageMissionjson != null) {
            for (Document document : ImageMissionjson) {
                try {
                    if (document.containsKey("mission_params")) {
                        MissionInstructionArray.add(document.get("mission_params"));
                        MissionInstructionDefautArray.add(document.get("default_mission_params"));
                        String MissionNumberArray_i = document.get("mission_number").toString();
                        MissionNumberArray.add(MissionNumberArray_i);
                        MissionStarTimeArray.add((Date) document.get("expected_start_time"));
                        MissionEndTimeArray.add((Date) document.get("expected_end_time"));
                        MissionWorkModel.add(document.get("work_mode").toString());
                        MissionImageModel.add(document.get("image_mode").toString());

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
        for (int i = 0; i < MissionStarTimeArray.size(); i++) {
            int j = i;
            ArrayList<String> MissionTaskModelChild = new ArrayList<>();
            MissionTaskModelChild.add("TASK06");
            //MissionTaskModelChild.add("TASK08");
            MissionTaskModel.put(j, MissionTaskModelChild);
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
            Map<String, String> it = new HashMap<>();
            //添加指令参数
            ArrayList<Document> MissionInstructionArrayChild = (ArrayList<Document>) MissionInstructionArray.get(i);
            ArrayList<Document> MissionInstructionDefautArrayChild = (ArrayList<Document>) MissionInstructionDefautArray.get(i);


            ArrayList<String> MissionInstructionCodeChild = new ArrayList<>();
            ArrayList<Integer> MissionInstructionIdChild = new ArrayList<>();
            ArrayList<Date> MissionInstructionTimeChild = new ArrayList<>();
            ArrayList<String> MissionInstructionHexChild = new ArrayList<>();
            double InstDelta_tAll = 0;
            double InstDelta_tLastAll = 0;
            Date time_point = MissionStarTimeArray.get(i);
            Date time_pointEnd = MissionEndTimeArray.get(i);

            timeVariable.T0 = time2Second(time_point);
            timeVariable.T0d = time2Second(time_pointEnd);
            timeVariable.T1 = time2Second(time_point);
            timeVariable.T1d = time2Second(time_pointEnd);
            timeVariable.T = time2Second(time_point);

            String ZhilingKuai = "";
            ArrayList<String> MissionTaskModelChild = MissionTaskModel.get(i);
            for (String workcode : MissionTaskModelChild) {
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
                                                                                boolean findIns = false;
                                                                                if (MetaParamsChild.containsKey("related_param_id")) {
                                                                                    String MetaParamsId = MetaParamsChild.get("related_param_id").toString();
                                                                                    //搜索任务中相应的id值
                                                                                    for (Document MissionMetaParamsChildParamsChild : MissionInstructionArrayChild) {
                                                                                        if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                            if (MissionMetaParamsChildParamsChild.containsKey("value") && !MissionMetaParamsChildParamsChild.get("value").equals("")) {
                                                                                                findIns = true;
                                                                                                //System.out.println(MetaParamsId);
//                                                                                                float temeratureFloat = Float.parseFloat(MissionMetaParamsChildParamsChild.get("value").toString());
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

                                                                                    if(!findIns){
                                                                                        for (Document MissionMetaParamsChildParamsChild : MissionInstructionDefautArrayChild) {
                                                                                            if (MissionMetaParamsChildParamsChild.containsKey("code") && MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsId)) {
                                                                                                if (MissionMetaParamsChildParamsChild.containsKey("default_value") && !MissionMetaParamsChildParamsChild.get("default_value").equals("")) {
                                                                                                    //System.out.println(MetaParamsId);
                                                                                                    findIns = true;
//                                                                            float temeratureFloat = Float.parseFloat(MissionMetaParamsChildParamsChild.get("default_value").toString());
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
//                                                                    System.out.println(YouXiaoData.length());
                                                                    ZhilingNum = (byte) (ZhilingNum + 1);
                                                                } else {
                                                                    //添加特殊指令
                                                                    String MetaHex = "";
                                                                    if (InstCode.equals("K4401")) {
                                                                        MetaHex = "100280010118";
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
                                                                    } else if (InstCode.equals("K4402")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0101";
                                                                    } else if (InstCode.equals("K4403")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0202";
                                                                    } else if (InstCode.equals("K4404")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0303";
                                                                    } else if (InstCode.equals("K4405")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0404";
                                                                    } else if (InstCode.equals("K4406")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0505";
                                                                    } else if (InstCode.equals("K4407")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0606";
                                                                    } else if (InstCode.equals("K4408")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0707";
                                                                    } else if (InstCode.equals("K4409")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A002";
                                                                        MetaHex = MetaHex + "0808";
                                                                    } else if (InstCode.equals("K4410")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0101";
                                                                    } else if (InstCode.equals("K4411")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0202";
                                                                    } else if (InstCode.equals("K4412")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0303";
                                                                    } else if (InstCode.equals("K4413")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0404";
                                                                    } else if (InstCode.equals("K4414")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0505";
                                                                    } else if (InstCode.equals("K4415")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0606";
                                                                    } else if (InstCode.equals("K4416")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0707";
                                                                    } else if (InstCode.equals("K4417")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A102";
                                                                        MetaHex = MetaHex + "0808";
                                                                    } else if (InstCode.equals("K4419")) {//todo
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A200";
                                                                    } else if (InstCode.equals("K4420")) {//todo
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A400";
                                                                    } else if (InstCode.equals("K4425")) {
                                                                        MetaHex = "100280010118";
                                                                        MetaHex = MetaHex + "A81C";
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

                                            //String KaiShiShiJian=time2String(time_ZhixingDate);
                                            //System.out.println(workcode);
                                            //System.out.println(sequencecode);
                                            //System.out.println(sequencecode);
                                            //System.out.println(Integer.parseInt(KaiShiShiJian,16));
                                            String KaiShiShiJian = timeHashMap.get(sequencecode).ExecutionTime(timeVariable, workcode).toUpperCase();

                                            if (KaiShiShiJian.length() < 8) {
                                                for (int k = KaiShiShiJian.length(); k < 8; k++) {
                                                    KaiShiShiJian = "0" + KaiShiShiJian;
                                                }
                                            } else if (KaiShiShiJian.length() > 8) {
                                                KaiShiShiJian = KaiShiShiJian.substring(KaiShiShiJian.length() - 8);
                                            }


                                            Instant time_ZhiXing = zerostart;
                                            time_ZhiXing = time_ZhiXing.plusSeconds((long) (Integer.parseInt(KaiShiShiJian, 16)));
                                            Date time_ZhixingDate = Date.from(time_ZhiXing);
                                            //最后一条指令时间间隔
                                            String YouXiaoDataTemp;
                                            if (YouXiaoshujuList.get(j).length() <= 4) {
                                                continue;
                                            } else {
                                                YouXiaoDataTemp = YouXiaoshujuList.get(j).substring(0, YouXiaoshujuList.get(j).length() - 4);
                                            }
                                            YingYongShuJu = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoDataTemp;
                                            ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                            String YingYongShuJuTemp = YingYongShuJu;
                                            MissionInstructionHexChild.add(YingYongShuJuTemp);
                                            MissionInstructionCodeChild.add(sequencecode);
                                            MissionInstructionIdChild.add(ZhiLingIDNum);
                                            MissionInstructionTimeChild.add(time_ZhixingDate);

//                                            System.out.println("序列号：" + sequencecode);
//                                            System.out.println("执行时间：" + Integer.parseInt(KaiShiShiJian, 16));
                                            it.put(sequencecode + "_" + Instant.now().toEpochMilli(), time_ZhixingDate.toString() + " 十六进制：" + KaiShiShiJian + " 十进制：" + (long) (Integer.parseInt(KaiShiShiJian, 16)));

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

                                    //String KaiShiShiJian=time2String(time_ZhixingDate);
                                    //System.out.println(sequencecode);
                                    String KaiShiShiJian = timeHashMap.get(sequencecode).ExecutionTime(timeVariable, workcode).toUpperCase();
                                    if (KaiShiShiJian.length() < 8) {
                                        for (int k = KaiShiShiJian.length(); k < 8; k++) {
                                            KaiShiShiJian = "0" + KaiShiShiJian;
                                        }
                                    } else if (KaiShiShiJian.length() > 8) {
                                        KaiShiShiJian = KaiShiShiJian.substring(KaiShiShiJian.length() - 8);
                                    }
                                    Instant time_ZhiXing = zerostart;
                                    time_ZhiXing = time_ZhiXing.plusSeconds((long) (Integer.parseInt(KaiShiShiJian, 16)));
                                    Date time_ZhixingDate = Date.from(time_ZhiXing);
                                    //最后一条指令时间间隔
                                    //System.out.println(sequencecode);
                                    if (YouXiaoData.length() <= 4) {
                                        continue;
                                    } else {
                                        YouXiaoData = YouXiaoData.substring(0, YouXiaoData.length() - 4);
                                    }
                                    YingYongShuJu = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoData;
                                    ZhilingKuai = ZhilingKuai + YingYongShuJu;
                                    MissionInstructionHexChild.add(YingYongShuJu);
                                    MissionInstructionCodeChild.add(sequencecode);
                                    MissionInstructionIdChild.add(ZhiLingIDNum);
                                    MissionInstructionTimeChild.add(time_ZhixingDate);
                                    InstDelta_tAll = InstDelta_tAll + InstDelta_tLastAll;

//                                    System.out.println("序列号：" + sequencecode);
//                                    System.out.println("执行时间：" + Integer.parseInt(KaiShiShiJian, 16));
                                    it.put(sequencecode + "_" + Instant.now().toEpochMilli(), time_ZhixingDate.toString() + " 十六进制：" + KaiShiShiJian + " 十进制：" + (long) (Integer.parseInt(KaiShiShiJian, 16)));
                                }
                            }
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }

            }
            insTimeMap.put(i, it);

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

                //添加填充域
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
                //int a = getCRC_0xFFFF(MainBuff, MainBuff.length);
                int a=CRC16_CCITT_FALSE(MainBuff);
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

            String InsTimeFileName = "Ins-Time.txt";
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
            Document modifiers = new Document();
            modifiers.append("$set", ImageMissionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", ImageMissionjson.get(i).get("mission_number").toString()), modifiers, new UpdateOptions().upsert(true));

            mongoClient.close();

            InstructionManager instructionManager = new InstructionManager();
            instructionManager.addInstrctionInfo(InstructionInfojsonArry, ImageMissionjson.get(i).get("mission_number").toString(),ImageMissionjson.get(i).get("name").toString());
            instructionManager.close();

            return FileFolder;
        }
        mongoClient.close();
        return FilePath;
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
        //double TimeMiddle = (JD(TimeStarTime) - JD(ZeroTime)) * 24 * 60 * 60;
        double TimeMiddle= Duration.between(ZeroTimeIns, time_point.toInstant()).getSeconds();

        return TimeMiddle;
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

    private static int CRC16_CCITT_FALSE(byte[] buffer) {
        int wCRCin = 0xffff;
        int wCPoly = 0x1021;
        for (byte b : buffer) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((wCRCin >> 15 & 1) == 1);
                wCRCin <<= 1;
                if (c15 ^ bit)
                    wCRCin ^= wCPoly;
            }
        }
        wCRCin &= 0xffff;
        return wCRCin ^= 0x0000;
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
}
