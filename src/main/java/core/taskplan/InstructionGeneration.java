package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import common.FilePathUtil;
import common.mongo.MangoDBConnector;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

//import common.FilePathUtil;
//import common.mongo.DbDefine;
//import common.mongo.MangoDBConnector;

public class InstructionGeneration {

    private static double[] ZeroTime = {2000, 1, 1, 0, 0, 0};//参考时间

    public static void InstructionGenerationII(ArrayList<Document> ImageMissionjson, Document TransmissionMissionJson, ArrayList<Document> StationMissionjson, String FilePath) {

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


        //成像任务读入
        //任务读入
        ArrayList<String> MissionNumberArray = new ArrayList<>();
        ArrayList<Object> MissionInstructionArray = new ArrayList<>();
        ArrayList<Boolean> MissionStateArray = new ArrayList<>();
        ArrayList<String> MissionLoadNumberArray = new ArrayList<>();
        ArrayList<Date> MissionStarTimeArray = new ArrayList<>();
        ArrayList<Date> MissionEndTimeArray = new ArrayList<>();
        ArrayList<String> MissionWorkModel = new ArrayList<>();
        int MissionNum = 0;
        if (ImageMissionjson != null) {
            for (Document document : ImageMissionjson) {
                try {
                    if (document.containsKey("mission_params")) {
                        MissionInstructionArray.add(document.get("mission_params"));
                        String MissionNumberArray_i = document.get("mission_number").toString();
                        MissionNumberArray.add(MissionNumberArray_i);
                        ArrayList<Document> ImageWindow = (ArrayList<Document>) document.get("image_window");
                        if (ImageWindow != null) {
                            for (Document document1 : ImageWindow) {
                                MissionLoadNumberArray.add(document1.get("load_number").toString());
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
                        MissionNum = MissionNum + 1;
                    }else {
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //传输任务读入
        ArrayList<String> TransMissionStationNameArray = new ArrayList<>();
        ArrayList<Date> TransMissionStarTimeArray = new ArrayList<>();
        ArrayList<Date> TransMissionEndTimeArray = new ArrayList<>();
        if (TransmissionMissionJson != null) {
            try {
                if (TransmissionMissionJson.get("fail_reason").equals("不可见")) {
                } else {
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
        if (StationMissionjson != null) {
            for (Document document : StationMissionjson) {
                try {
                    if (document.containsKey("record_file_no")) {
                        StationMissionRecordFileNoArray.add(document.get("record_file_no"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //指令生成
        ArrayList<byte[]> InstructionArray = new ArrayList<>();
        ArrayList<Byte> ZhiLingIDArray = new ArrayList<Byte>();
        ArrayList<ArrayList<String>> MissionInstructionCode=new ArrayList<>();
        ArrayList<ArrayList<Byte>> MissionInstructionId=new ArrayList<>();
        ArrayList<ArrayList<Date>> MissionInstructionTime=new ArrayList<>();
        for (int i = 0; i < MissionNum; i++) {
            ArrayList<String> MissionInstructionCodeChild=new ArrayList<>();
            ArrayList<Byte> MissionInstructionIdChild=new ArrayList<>();
            ArrayList<Date> MissionInstructionTimeChild=new ArrayList<>();
            double InstDelta_tAll=0;
            double InstDelta_tLastAll=0;
            Date time_point = MissionStarTimeArray.get(i);

            String ZhilingKuai = "";
            byte ZhiLingIDNum = 0;

            String workmodel = MissionWorkModel.get(i);
            workmodel = workmodel + "模式";
            for (Document document : TaskInstructionjson) {
                try {
                    //选择指令块模板
                    if (document.get("name").equals(workmodel)) {
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
                                Document MissionParams = (Document) MissionInstructionArray.get(i);
                                ArrayList<Document> MissionSequenceParams = (ArrayList<Document>) MissionParams.get("task_params");
                                for (Document TaskParams : MissionSequenceParams) {
                                    if (TaskParams.get("id").toString().equals(Related_id)) {
                                        TaskParamsValue = TaskParams.get("value").toString();
                                        break;
                                    }
                                }
                                //判定是否执行该序列
                                if ( document1.containsKey("when")) {
                                    if (document1.get("when").toString().equals(TaskParamsValue)) {
                                        SequenceFlag = true;
                                    }                           
                                }else if (document1.containsKey("when_or")) {
                                    ArrayList<String> MissionSequenceWhenOr= (ArrayList<String>) document1.get("when_or");
                                    for (String MissionSequenceWhenOrChild:MissionSequenceWhenOr) {
                                        if (MissionSequenceWhenOrChild.equals(TaskParamsValue)) {
                                            SequenceFlag = true;
                                            break;
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
                                InstDelta_tLastAll=0;

                                String sequencecode = document1.get("sequence_code").toString();
                                //选择指令码模板
                                for (Document document2 : SequenceInstructionjson) {
                                    try {
                                        if (document2.get("code").equals(sequencecode)) {
                                            ArrayList<Document> InstsArray = new ArrayList<>();
                                            InstsArray = (ArrayList<Document>) document2.get("inst");
                                            for (Document document3 : InstsArray) {
                                                String InstCode = "";
                                                //判断执行哪种指令码
                                                if (document3.containsKey("alternative")) {
                                                    //是否执行种类
                                                    String MetaRelated_id = document3.get("related_param_id").toString();
                                                    //搜索任务中相应id的值
                                                    String SequenceParamsValue = "";
                                                    Document MissionParams = (Document) MissionInstructionArray.get(i);
                                                    Document MissionSequenceParams = (Document) MissionParams.get("sequence_params");
                                                    Document MissionSequenceParams_TCG = (Document) MissionSequenceParams.get(sequencecode);
                                                    ArrayList<Document> MissionSequenceParamsChild = (ArrayList<Document>) MissionSequenceParams_TCG.get("sequence_params");
                                                    for (Document SequenceParams : MissionSequenceParamsChild) {
                                                        if (SequenceParams.get("id").toString().equals(MetaRelated_id)) {
                                                            SequenceParamsValue = SequenceParams.get("value").toString();
                                                            break;
                                                        }
                                                    }
                                                    if ( document3.containsKey("when")) {
                                                        if (document3.get("when").toString().equals(SequenceParamsValue)) {
                                                            InstCode = document3.get("inst_code").toString();
                                                        }
                                                    }else if (document3.containsKey("when_or")) {
                                                        ArrayList<String> MissionCodeWhenOr= (ArrayList<String>) document3.get("when_or");
                                                        for (String MissionCodeWhenOrChild:MissionCodeWhenOr) {
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
                                                    Document MissionParams = (Document) MissionInstructionArray.get(i);
                                                    Document MissionSequenceParams = (Document) MissionParams.get("sequence_params");
                                                    Document MissionSequenceParams_TCG = (Document) MissionSequenceParams.get(sequencecode);
                                                    ArrayList<Document> MissionSequenceParamsChild = (ArrayList<Document>) MissionSequenceParams_TCG.get("sequence_params");
                                                    for (Document SequenceParams : MissionSequenceParamsChild) {
                                                        if (SequenceParams.get("id").toString().equals(MetaRelated_id)) {
                                                            SequenceParamsValue = SequenceParams.get("value").toString();
                                                            break;
                                                        }
                                                    }
                                                    InstCode = SequenceParamsValue;
                                                } else {
                                                    //变参执行种类
                                                    InstCode = document3.get("inst_code").toString();
                                                }
                                                //执行间隔
                                                String InstDelta_t="0";
                                                Document delta_tDocument= (Document) document3.get("delta_t");
                                                if (delta_tDocument.containsKey("related_param_id")) {
                                                    String delta_tId=delta_tDocument.get("related_param_id").toString();
                                                    //搜索任务中相应id的值
                                                    String DeltaParamsValue = "";
                                                    Document MissionParams = (Document) MissionInstructionArray.get(i);
                                                    Document MissionSequenceParams = (Document) MissionParams.get("sequence_params");
                                                    Document MissionSequenceParams_TCG = (Document) MissionSequenceParams.get(sequencecode);
                                                    ArrayList<Document> MissionSequenceParamsChild = (ArrayList<Document>) MissionSequenceParams_TCG.get("sequence_params");
                                                    for (Document SequenceParams : MissionSequenceParamsChild) {
                                                        if (SequenceParams.get("id").toString().equals(delta_tId)) {
                                                            DeltaParamsValue = SequenceParams.get("value").toString();
                                                            break;
                                                        }
                                                    }
                                                    if (delta_tDocument.containsKey("mapping")) {
                                                        Document delta_tMappingDocument= (Document) delta_tDocument.get("mapping");
                                                        if (delta_tMappingDocument.containsKey(DeltaParamsValue)) {
                                                            InstDelta_t=delta_tMappingDocument.get(DeltaParamsValue).toString();
                                                        }
                                                    }
                                                }else {
                                                    InstDelta_t = document3.get("delta_t").toString();
                                                }
                                                InstDelta_tLastAll=InstDelta_tLastAll+Integer.parseInt(InstDelta_t)*0.125;
                                                //执行该指令
                                                if (InstCode != "") {
                                                    for (Document document4 : MetaInstrunctionjson) {
                                                        if (document4.get("code").toString().equals(InstCode)) {
                                                            String MetaHex = document4.get("hex").toString();
                                                            byte[] byteMetaHex = hexStringToBytes(MetaHex);
                                                            ArrayList<Document> MetaParams = (ArrayList<Document>) document4.get("params");
                                                            for (Document MetaParamsChild : MetaParams) {
                                                                if (MetaParamsChild.get("tag").toString().equals("ui")) {
                                                                    //任务参数读取
                                                                    String MetaParamsCode = MetaParamsChild.get("code").toString();
                                                                    //搜索任务中相应的code值
                                                                    Document MissionParams = (Document) MissionInstructionArray.get(i);
                                                                    Document MissionSequenceParams = (Document) MissionParams.get("sequence_params");
                                                                    Document MissionSequenceParams_TCG = (Document) MissionSequenceParams.get(sequencecode);
                                                                    Document MissionMetaParams = (Document) MissionSequenceParams_TCG.get("meta_inst_params");
                                                                    Document MissionMetaParamsChild = (Document) MissionMetaParams.get(InstCode);
                                                                    ArrayList<Document> MissionMetaParamsChildParams = (ArrayList<Document>) MissionMetaParamsChild.get("params");
                                                                    for (Document MissionMetaParamsChildParamsChild : MissionMetaParamsChildParams) {
                                                                        if (MissionMetaParamsChildParamsChild.get("code").toString().equals(MetaParamsCode)) {
                                                                            String MetaParamsCodeValue = MissionMetaParamsChildParamsChild.get("value").toString();
                                                                            int byteIndex = MetaParamsChild.getInteger("byte_index");
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
                                                                } else {
                                                                    //规划参数读取
                                                                    String MetaParamsLabel = MetaParamsChild.get("label").toString();
                                                                    int byteIndex = MetaParamsChild.getInteger("byte_index");
                                                                    int byteLength = MetaParamsChild.getInteger("byte_length");
                                                                    double attitudeCode_x = 0;
                                                                    double attitudeCode_y = 0;
                                                                    double attitudeCode_z = 0;
                                                                    //读取姿态表中姿态数据
                                                                    try {
                                                                        //指令块模板
                                                                        MongoCollection<Document> Data_NormalAttitudejson = mongoDatabase.getCollection("normal_attitude");
                                                                        Date Find_time_point=time_point;
                                                                        Bson queryBsonAttitude= Filters.eq("time_point",Find_time_point);
                                                                        FindIterable<Document> D_NormalAttitudejson = Data_TaskInstructionjson.find(queryBsonAttitude);
                                                                        ArrayList<Document> NormalAttitudejson = new ArrayList<>();
                                                                        for (Document D_NormalAttitudejsonChild : D_NormalAttitudejson) {
                                                                            NormalAttitudejson.add(D_NormalAttitudejsonChild);
                                                                            Document AttitudeJson= (Document) D_NormalAttitudejsonChild.get("Attitude_EastSouthDown_312");
                                                                            attitudeCode_x=AttitudeJson.getDouble("roll_angle");
                                                                            attitudeCode_y=AttitudeJson.getDouble("pitch_angle");
                                                                            attitudeCode_z=AttitudeJson.getDouble("yaw_angle");

                                                                        }

                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    attitudeCode_x = 0;
                                                                    attitudeCode_y = 0;
                                                                    attitudeCode_z = 0;
                                                                    byte[] attitudeCodeByte_x = new byte[4];
                                                                    byte[] attitudeCodeByte_y = new byte[4];
                                                                    byte[] attitudeCodeByte_z = new byte[4];
                                                                    attitudeCodeByte_x = double2Bytes(attitudeCode_x);
                                                                    attitudeCodeByte_y = double2Bytes(attitudeCode_y);
                                                                    attitudeCodeByte_z = double2Bytes(attitudeCode_z);
                                                                    if (MetaParamsLabel.equals("偏航角")) {
                                                                        //z轴
                                                                        if (byteLength == 4) {
                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                if (j < byteMetaHex.length && j - byteIndex < attitudeCodeByte_z.length) {
                                                                                    byteMetaHex[j] = attitudeCodeByte_z[j - byteIndex];
                                                                                }
                                                                            }
                                                                        }
                                                                    } else if (MetaParamsLabel.equals("俯仰角")) {
                                                                        //y轴
                                                                        if (byteLength == 4) {
                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                if (j < byteMetaHex.length && j - byteIndex < attitudeCodeByte_z.length) {
                                                                                    byteMetaHex[j] = attitudeCodeByte_y[j - byteIndex];
                                                                                }
                                                                            }
                                                                        }
                                                                    } else if (MetaParamsLabel.equals("滚转角")) {
                                                                        //x轴
                                                                        if (byteLength == 4) {
                                                                            for (int j = byteIndex; j < byteIndex + byteLength; j++) {
                                                                                if (j < byteMetaHex.length && j - byteIndex < attitudeCodeByte_z.length) {
                                                                                    byteMetaHex[j] = attitudeCodeByte_x[j - byteIndex];
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            MetaHex = bytesToHexString(byteMetaHex);
                                                            byte[] YouXiaoChangDuByte = new byte[]{(byte) (byteMetaHex.length)};
                                                            String YouXiaoChangDuString = bytesToHexString(YouXiaoChangDuByte);
                                                            int ZhiXingJianGeInt = Integer.parseInt(InstDelta_t);
                                                            byte[] ZhiXingJianGeByte = new byte[]{(byte) ((ZhiXingJianGeInt) & 0xFF), (byte) ((ZhiXingJianGeInt >> 8) & 0xFF)};
                                                            String ZhiXingJianGeString = bytesToHexString(ZhiXingJianGeByte);
                                                            String APIDString = "FFFF";//????????????
                                                            YouXiaoData = YouXiaoData + APIDString + YouXiaoChangDuString + MetaHex + ZhiXingJianGeString;
                                                            ZhilingNum = (byte) (ZhilingNum + 1);
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
                                ZhiLingIDNum = (byte) (ZhiLingIDNum + 1);
                                byte[] ZhiLingXuLieIDByte = new byte[]{ZhiLingIDNum};
                                String ZhiLingXuLieIDString = bytesToHexString(ZhiLingXuLieIDByte);
                                byte[] ZhiLingGeShu = new byte[]{ZhilingNum};
                                String ZhiLingGeShuString = bytesToHexString(ZhiLingGeShu);
                                Instant time_ZhiXing= time_point.toInstant();
                                time_ZhiXing=time_ZhiXing.plusMillis((long)(InstDelta_tAll*1000));
                                Date time_ZhixingDate=Date.from(time_ZhiXing);
                                String KaiShiShiJian=time2String(time_ZhixingDate);
                                YingYongShuJu = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + YouXiaoData;
                                ZhilingKuai = ZhilingKuai + YingYongShuJu;

                                MissionInstructionCodeChild.add(sequencecode);
                                MissionInstructionIdChild.add(ZhiLingIDNum);
                                MissionInstructionTimeChild.add(time_ZhixingDate);

                                InstDelta_tAll=InstDelta_tAll+InstDelta_tLastAll;
                            }
                        }

                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }


            //String YingYongShuJu = KaiShiShiJian + ZhiLingID + ZhiLingNum + YouXiaoData;
            String ShuJuQuTou = "10562347";
            long BaoChang = (ShuJuQuTou + ZhilingKuai).length() / 2 + 2;
            String BaoChangstr = Long.toHexString(BaoChang);
            for (int j = 0; j < 4; j++) {
                BaoChangstr = "0" + BaoChangstr;
            }
            String BaoZhuDaoTou = "1D81C001" + BaoChang;
            String total = BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai + ISO(BaoZhuDaoTou + ShuJuQuTou + ZhilingKuai);
            int count = total.length();
            for (int j = total.length(); j < 126 * 2; j++) {
                total = total + "A";
            }

            byte[] MainBuff = hexStringToBytes(total);
            int a = getCRC_0xFFFF(MainBuff, MainBuff.length);
            String CRCCode = Integer.toHexString(a).toUpperCase();
            for (int j = CRCCode.length(); j < 4; i++) {
                CRCCode = "0" + CRCCode;
            }
            total = "EB90762569" + total + CRCCode;
            byte[] bytes = hexStringToBytes(total);

            ZhiLingIDArray.add(i, ZhiLingIDNum);
            InstructionArray.add(i, bytes);

            MissionInstructionCode.add(i,MissionInstructionCodeChild);
            MissionInstructionId.add(i,MissionInstructionIdChild);
            MissionInstructionTime.add(i,MissionInstructionTimeChild);
        }

        //指令输出
        for (int i = 0; i < MissionNumberArray.size(); i++) {
            String FileFolder = FilePath + MissionNumberArray.get(i);
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
            //指令文件命名
            Date cal = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String StringTime = sdf.format(cal.getTime());

            String DateString = StringTime.substring(0, 4) + StringTime.substring(5, 7) + StringTime.substring(8, 10) + StringTime.substring(11, 13) + StringTime.substring(14, 16);
            String FileName = MissionNumberArray.get(i) + "\\" + DateString + "-1.txt";

            String realPath = FilePathUtil.getRealFilePath(FilePath + FileName);
            bytesTotxt(InstructionArray.get(i), realPath);


            //数据库传出
            ArrayList<Document> InstructionInfojsonArry = new ArrayList<>();
            if (MissionInstructionCode.get(i).size()>0) {
                for (int j = 0; j < MissionInstructionCode.get(i).size(); j++) {
                    Document InstructionInfojsonObject = new Document();
                    InstructionInfojsonObject.append("sequence_code",MissionInstructionCode.get(i).get(j));
                    InstructionInfojsonObject.append("sequence_id", MissionInstructionId.get(i).get(j));
                    InstructionInfojsonObject.append("execution_time", MissionInstructionTime.get(i).get(j));
                    InstructionInfojsonObject.append("valid",true);
                    InstructionInfojsonArry.add(InstructionInfojsonObject);
                }
            }else {
                Document InstructionInfojsonObject = new Document();
                InstructionInfojsonObject.append("sequence_code","");
                InstructionInfojsonObject.append("sequence_id", "");
                InstructionInfojsonObject.append("execution_time","");
                InstructionInfojsonObject.append("valid","");
                InstructionInfojsonArry.add(InstructionInfojsonObject);
            }
            ImageMissionjson.get(i).append("instruction_info", InstructionInfojsonArry);
            Document modifiers = new Document();
            modifiers.append("$set", ImageMissionjson.get(i));
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", ImageMissionjson.get(i).get("mission_number").toString()), modifiers, new UpdateOptions().upsert(true));
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
    private static String time2String(Date time_point){
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

    //????
    private static String ISO(String Frame) {
        int C0 = 0;
        int C1 = 0;
        for (int i = 0; i < Frame.length(); i = i + 2) {
            int B = Integer.parseInt(Frame.substring(i, i + 2), 16);
            C0 = C0 + B;
            C1 = C1 + C0;
        }
        int CK1 = -(C0 + C1);
        int CK2 = C1;
        String CK1tot = Integer.toHexString(CK1);
        String CK2tot = Integer.toHexString(CK2);
        if (CK1tot.length() % 2 == 1) {
            CK1tot = "0" + CK1tot;
        }
        if (CK2tot.length() % 2 == 1) {
            CK2tot = "0" + CK2tot;
        }
        String CK1str = CK1tot.substring(0, 2);
        String CK2str = CK2tot.substring(CK2tot.length() - 2, CK2tot.length());
        if (CK1str == "00") {
            CK1str = "FF";
        }
        if (CK2str == "00") {
            CK2str = "FF";
        }
        return CK1str + CK2str;
    }

    //CRC校验
    private static int getCRC_0xFFFF(byte[] data, int len)        //CRC校验
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

    public static void main(String[] args) {
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase database = mongoClient.getDatabase("temp");
        FindIterable<Document> image_mission = database.getCollection("image_mission").find();
        ArrayList<Document> input = new ArrayList<>();

        for (Document d : image_mission) {
            if (d.getString("mission_number") != null)
                input.add(d);
        }
        InstructionGenerationII(input, null, null, "C:\\test\\");
    }
}
