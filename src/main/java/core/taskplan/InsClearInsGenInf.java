package core.taskplan;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

//指令删除
public class InsClearInsGenInf {

    private static double[] ZeroTime = {2018, 1, 1, 0, 0, 0};//参考时间
    private static Instant ZeroTimeIns = Instant.parse("2018-01-01T00:00:00.00Z");

    public static String InsClearInsGenInfII(int isTimeSpan, int type, Instant exetime, Instant start, Instant end, HashSet<Integer> insno, String FilePath) {
        //连接数据库
        MongoClient mongoClient = MangoDBConnector.getClient();
        //获取名为"temp"的数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("temp");
        //读入基准时间
        //获取名为“satellite_resource”的表
        MongoCollection<Document> sate_res = mongoDatabase.getCollection("satellite_resource");
        //获取的表存在Document中
        Document first = sate_res.find().first();
        //将表中properties内容存入properties列表中
        ArrayList<Document> properties = (ArrayList<Document>) first.get("properties");
        Instant zerostart = ZeroTimeIns;

        for (Document document : properties) {
            if (document.get("key").toString().equals("t0")) {
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

        String str = "";
        String SequNume = "";
        if (isTimeSpan == 0) {
            //指定ID删除
            String str78910 = "100B812101";
            String strAPID = "0411";
            strAPID = "";
            for (Integer idChild : insno) {
                str78910 = str78910 + String.format("%04X", idChild);
            }
            int IDNum = str78910.length() / 2;
            str = strAPID + String.format("%02X", IDNum) + str78910;
            SequNume = "TCS291";
        } else if (isTimeSpan == 1) {
            //指定时间段删除
            String str78910 = "100B8221";
            String strAPID = "0411";
            strAPID = "";
            int startint = (int) (start.getEpochSecond() - zerostart.getEpochSecond());
            int endint = (int) (end.getEpochSecond() - zerostart.getEpochSecond());
            if (type == 0) {
                //区间左
                str78910 = str78910 + "11" + String.format("%08X", startint) + String.format("%08X", endint);
            } else if (type == 1) {
                //区间右
                str78910 = str78910 + "22" + String.format("%08X", startint) + String.format("%08X", endint);
            } else if (type == 2) {
                //区间中间
                str78910 = str78910 + "33" + String.format("%08X", startint) + String.format("%08X", endint);
            } else if (type == 3) {
                //全部删除
                str78910 = str78910 + "44" + String.format("%08X", startint) + String.format("%08X", endint);
            }
            int IDNum = str78910.length() / 2;
            str = strAPID + String.format("%02X", IDNum) + str78910;
            SequNume = "TCS292";
        } else if (isTimeSpan == 2) {
            //全部删除
            String str78910 = "100B8521";
            String strAPID = "0411";
            strAPID = "";
            int IDNum = 4;
            str = strAPID + String.format("%02X", IDNum) + str78910;
            SequNume = "TCS296";
        }


        //序列
        int ZhiLingIDNum = SequenceID.SequenceId;
        SequenceID.SequenceId = SequenceID.SequenceId + 1;
        if (SequenceID.SequenceId > 255) {
            SequenceID.SequenceId = 0;
        }
        String ZhiLingXuLieIDString = String.format("%02X", ZhiLingIDNum);
        ZhiLingXuLieIDString = "00" + ZhiLingXuLieIDString;
        String ZhiLingGeShuString = "01";
        int exetimeint = (int) (exetime.getEpochSecond() - zerostart.getEpochSecond());
        String KaiShiShiJian = String.format("%08X", exetimeint);

        //str = KaiShiShiJian + ZhiLingXuLieIDString + ZhiLingGeShuString + str;

        //包
        String ShuJuQuTou = "";
        int BaoChang = (ShuJuQuTou + str).length() / 2 + 2 - 1;
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
        String total = BaoZhuDaoTou + ShuJuQuTou + str + ISO(BaoZhuDaoTou + ShuJuQuTou + str);

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

        String FileFolder = FilePathUtil.getRealFilePath(FilePath + "\\" + "InsClear");
        File file = new File(FileFolder);
        if (!file.exists()) {
            //如果文件夹不存在，新建
            file.mkdirs();
        } else {
            //如果文件夹存在，删除文件夹内所有文件
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    //f.delete();
                }
            }
        }
        //指令文件命名
        Date cal = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String StringTime = sdf.format(cal.getTime());

        String DateString = StringTime.substring(0, 4) + StringTime.substring(5, 7) + StringTime.substring(8, 10) + StringTime.substring(11, 13) + StringTime.substring(14, 16);
        String FileName = FileFolder + "\\" + ZhiLingIDNum + "-" + SequNume + "-InsClear-" + DateString;
        FileName = FilePathUtil.getRealFilePath(FileName);
        file = new File(FileName);
        if (file.exists()) {
            file.delete();
        }
        String realPath = FilePathUtil.getRealFilePath(FileName);
        bytesTotxt(bytes, realPath);

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
