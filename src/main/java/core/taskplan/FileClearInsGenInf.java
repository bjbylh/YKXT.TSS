package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import common.FilePathUtil;
import common.mongo.MangoDBConnector;
import core.taskplan.InstructionSequenceTime.SequenceID;
import org.bson.Document;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

//固存擦除
public class FileClearInsGenInf {

    private static double[] ZeroTime = {2018, 1, 1, 0, 0, 0};//参考时间
    private static Instant ZeroTimeIns=Instant.parse("2018-01-01T00:00:00.00Z");

    public static String FileClearInsGenInfII(Document Mission, String FilePath) {
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //读入基准时间
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res=mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first=sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties=(ArrayList<Document>) first.get("properties");
        Instant zerostart=ZeroTimeIns;

        for (Document document:properties){
            if (document.get("key").toString().equals("t0")){
                zerostart=document.getDate("value").toInstant();
                LocalDateTime zerostart0=LocalDateTime.ofInstant(zerostart, ZoneOffset.UTC);
                ZeroTime[0]=zerostart0.getYear();
                ZeroTime[1]=zerostart0.getMonthValue();
                ZeroTime[2]=zerostart0.getDayOfMonth();
                ZeroTime[3]=zerostart0.getHour();
                ZeroTime[4]=zerostart0.getMinute();
                ZeroTime[5]=zerostart0.getSecond();
                break;
            }
        }
        HashMap<String,String> FileClear=new HashMap<>();
        HashMap<String,Integer> FileSequenID=new HashMap<>();
        HashMap<String,Date> MissionInstructionTime=new HashMap<>();
        String FileFolder="";
        if (Mission != null) {
            Instant exetime=ZeroTimeIns;
            String MissionNumber="";
            try {
                exetime=Mission.getDate("expected_start_time").toInstant();
                MissionNumber=Mission.get("mission_number").toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //
            String TCS207="100E8121D0";
            String LengtTCS207="05";
            String APIDTCS207="0412";
            //序列
            int ZhiLingIDNum = SequenceID.SequenceId;
            SequenceID.SequenceId=SequenceID.SequenceId+1;
            if (SequenceID.SequenceId > 255) {
                SequenceID.SequenceId=0;
            }
            String ZhiLingXuLieIDString = String.format("%02X",ZhiLingIDNum);
            String ZhiLingGeShuString = "01";
            int exetimeint= (int) (exetime.getEpochSecond()-zerostart.getEpochSecond());
            String KaiShiShiJian=String.format("%08X",exetimeint);
            String str = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + APIDTCS207+LengtTCS207+TCS207;
            FileClear.put("TCS207",str);
            int ZhiLingIDNumTCS207=ZhiLingIDNum;
            FileSequenID.put("TCS207",ZhiLingIDNumTCS207);
            Date TCS207Time= Date.from(exetime);
            MissionInstructionTime.put("TCS207",TCS207Time);

            //
            String TCAG06="";
            String TCA01="100201210111400131A5";
            String APIDTCA01="0411";
            String LengTCA01="0A";
            String dert_tTCA01="0030";
            TCAG06=TCAG06+APIDTCA01+LengTCA01+TCA01+dert_tTCA01;
            //
            String TCA11="10028021010F3700";
            String APIDTCA11="01E3";
            String LengTCA11="08";
            TCAG06=TCAG06+APIDTCA11+LengTCA11+TCA11;
            //序列
            ZhiLingIDNum = SequenceID.SequenceId;
            SequenceID.SequenceId=SequenceID.SequenceId+1;
            if (SequenceID.SequenceId > 255) {
                SequenceID.SequenceId=0;
            }
            ZhiLingXuLieIDString = String.format("%02X",ZhiLingIDNum);
            ZhiLingGeShuString = "02";
            exetimeint= (int) (exetime.getEpochSecond()-zerostart.getEpochSecond());
            KaiShiShiJian=String.format("%08X",exetimeint);
            TCAG06 = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + TCAG06;
            FileClear.put("TCAG06",TCAG06);
            int ZhiLingIDNumTCAG06=ZhiLingIDNum;
            FileSequenID.put("TCAG06",ZhiLingIDNumTCAG06);
            Date TCAG06Time= Date.from(exetime);
            MissionInstructionTime.put("TCAG06",TCAG06Time);

            //
            String TCA303="";
            String APIDTCA303="0411";
            try {
                if (Mission.get("clear_all").toString().equals("true")) {
                    TCA303="10028021010F3E0A00010000000000000000";
                }else {
                    TCA303="10028021010F3E0A0002";
                    ArrayList<String> filenos= (ArrayList<String>) Mission.get("clear_filenos");
                    String TCA303Child="";
                    for (String string:filenos) {
                        if (!string.equals("")) {
                            int fileId=Integer.parseInt(string);
                            TCA303Child=TCA303Child+String.format("%02X",fileId);
                        }
                    }
                    TCA303=TCA303+TCA303Child;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return FilePath;
            }
            String LengTCA303=String.format("%02X",(int)TCA303.length()/2);
            TCA303=APIDTCA303+LengTCA303+TCA303;
            //序列
            ZhiLingIDNum = SequenceID.SequenceId;
            SequenceID.SequenceId=SequenceID.SequenceId+1;
            if (SequenceID.SequenceId > 255) {
                SequenceID.SequenceId=0;
            }
            ZhiLingXuLieIDString = String.format("%02X",ZhiLingIDNum);
            ZhiLingGeShuString = "01";
            exetimeint= (int) (exetime.getEpochSecond()-zerostart.getEpochSecond());
            KaiShiShiJian=String.format("%08X",exetimeint);
            TCA303 = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + TCA303;
            FileClear.put("TCA303",TCA303);
            int ZhiLingIDNumTCA303=ZhiLingIDNum;
            FileSequenID.put("TCA303",ZhiLingIDNumTCA303);
            Date TCA303Time= Date.from(exetime);
            MissionInstructionTime.put("TCA303",TCA303Time);

            //
            String TCS205="100E812196";
            String LengtTCS205="05";
            String APIDTCS205="0412";
            //序列
            ZhiLingIDNum = SequenceID.SequenceId;
            SequenceID.SequenceId=SequenceID.SequenceId+1;
            if (SequenceID.SequenceId > 255) {
                SequenceID.SequenceId=0;
            }
            ZhiLingXuLieIDString = String.format("%02X",ZhiLingIDNum);
            ZhiLingGeShuString = "01";
            exetimeint= (int) (exetime.getEpochSecond()-zerostart.getEpochSecond());
            KaiShiShiJian=String.format("%08X",exetimeint);
            TCS205 = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + APIDTCS205+LengtTCS205+TCS205;
            FileClear.put("TCS205",TCS205);
            int ZhiLingIDNumTCS205=ZhiLingIDNum;
            FileSequenID.put("TCS205",ZhiLingIDNumTCS205);
            Date TCS205Time= Date.from(exetime);
            MissionInstructionTime.put("TCS205",TCS205Time);

            HashMap<String,byte[]> InstructionArrayChild=new HashMap<>();
            for (Map.Entry<String,String> entry:FileClear.entrySet()) {
                String ShuJuQuTou = "10562347";
                int BaoChang = (ShuJuQuTou + entry.getValue()).length() / 2 + 2;
                String BaoChangstr = String.format("%04X",BaoChang);
                String BaoZhuDaoTou = "1D81C001" + BaoChangstr;
                String total = BaoZhuDaoTou + ShuJuQuTou + entry.getValue() + ISO(BaoZhuDaoTou + ShuJuQuTou + entry.getValue());

                //添加填充域
                if (total.length()<=62*2) {
                    for (int j = total.length()/2; j < 62; j++) {
                        total=total+"A5";
                    }
                }else if (total.length()>62*2 && total.length()<=126*2) {
                    for (int j = total.length()/2; j < 126; j++) {
                        total=total+"A5";
                    }
                }else if (total.length()>126*2 && total.length()<=254*2){
                    for (int j = total.length()/2; j < 254; j++) {
                        total=total+"A5";
                    }
                }else if (total.length()>254*2 && total.length()<=510*2){
                    for (int j = total.length()/2; j < 510; j++) {
                        total=total+"A5";
                    }
                }else{
                    //分两包
                }

                byte[] MainBuff = hexStringToBytes(total);
                int a = getCRC_0xFFFF(MainBuff, MainBuff.length);
                String CRCCode = Integer.toHexString(a).toUpperCase();
                for (int j = CRCCode.length(); j < 4; j++) {
                    CRCCode = "0" + CRCCode;
                }
                total = "EB90762569" + total + CRCCode;
                byte[] bytes = hexStringToBytes(total);
                InstructionArrayChild.put(entry.getKey(),bytes);
            }

            //指令输出
            FileFolder = FilePathUtil.getRealFilePath(FilePath + "\\"+ MissionNumber);
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
            for (Map.Entry<String,byte[]> Instruction:InstructionArrayChild.entrySet()) {
                //指令文件命名
                Date cal = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String StringTime = sdf.format(cal.getTime());

                String DateString = StringTime.substring(0, 4) + StringTime.substring(5, 7) + StringTime.substring(8, 10) + StringTime.substring(11, 13) + StringTime.substring(14, 16);
                String FileName = FileSequenID.get(Instruction.getKey())+"-"+Instruction.getKey()+"-"+DateString + "-"+MissionNumber;
                String realPath = FilePathUtil.getRealFilePath(FileFolder+"\\" + FileName);
                bytesTotxt(Instruction.getValue(), realPath);
            }

            //数据库传出
            ArrayList<Document> InstructionInfojsonArry = new ArrayList<>();
            if (!InstructionArrayChild.isEmpty()) {
                for (Map.Entry<String,byte[]> Instruction:InstructionArrayChild.entrySet()) {
                    Document InstructionInfojsonObject = new Document();
                    InstructionInfojsonObject.append("sequence_code",Instruction.getKey());
                    InstructionInfojsonObject.append("sequence_id", FileSequenID.get(Instruction.getKey()));
                    InstructionInfojsonObject.append("execution_time", MissionInstructionTime.get(Instruction.getKey()));
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
            Mission.append("instruction_info", InstructionInfojsonArry);
            Document modifiers = new Document();
            modifiers.append("$set", Mission);
            MongoCollection<Document> image_mission = mongoDatabase.getCollection("image_mission");
            image_mission.updateOne(new Document("mission_number", Mission.get("mission_number").toString()), modifiers, new UpdateOptions().upsert(true));
        }

        mongoClient.close();
        //返回加文件名的路径
        return FileFolder;
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

    //计算当前时间和基准时间的时间间隔的秒数
    private static double time2Second(Date time_point){
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
            ArrayList<Double> coordinates = (ArrayList<Double>) geometry.get("coordinates");
            double[] Target = new double[2];
            Target[0] = coordinates.get(0);
            Target[1] = coordinates.get(1);
            CoordinatesList.add(Target);
        } else if (geometry.get("type").equals("LineString")) {
            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
            for (ArrayList<Double> document : coordinates) {
                double[] Target = new double[2];
                Target[0] = document.get(0);
                Target[1] = document.get(1);
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("Polygon")) {
            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    Target[0] = subsubcoordinates.get(0);
                    Target[1] = subsubcoordinates.get(1);
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPoint")) {
            ArrayList<ArrayList<Double>> coordinates = (ArrayList<ArrayList<Double>>) geometry.get("coordinates");
            for (ArrayList<Double> document : coordinates) {
                double[] Target = new double[2];
                Target[0] = document.get(0);
                Target[1] = document.get(1);
                CoordinatesList.add(Target);
            }
        } else if (geometry.get("type").equals("MultiLineString")) {
            ArrayList<ArrayList<ArrayList<Double>>> coordinates = (ArrayList<ArrayList<ArrayList<Double>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<Double>> subcoordinates : coordinates) {
                for (ArrayList<Double> subsubcoordinates : subcoordinates) {
                    double[] Target = new double[2];
                    Target[0] = subsubcoordinates.get(0);
                    Target[1] = subsubcoordinates.get(1);
                    CoordinatesList.add(Target);
                }
            }
        } else if (geometry.get("type").equals("MultiPolygon")) {
            ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> coordinates = (ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>) geometry.get("coordinates");
            for (ArrayList<ArrayList<ArrayList<Double>>> subcoordinates : coordinates) {
                for (ArrayList<ArrayList<Double>> subsubcoordinates : subcoordinates) {
                    for (ArrayList<Double> subsubsubcoordinates : subsubcoordinates) {
                        double[] Target = new double[2];
                        Target[0] = subsubsubcoordinates.get(0);
                        Target[1] = subsubsubcoordinates.get(1);
                        CoordinatesList.add(Target);
                    }
                }
            }
        }

        return CoordinatesList;
    }
}
