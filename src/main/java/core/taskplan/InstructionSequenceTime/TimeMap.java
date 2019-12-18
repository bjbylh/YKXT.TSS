package core.taskplan.InstructionSequenceTime;

import java.util.HashMap;

public class TimeMap {

    public HashMap<String, SequenceTime> timeHashMap(){
        HashMap<String, SequenceTime> timemap=new HashMap<>();
        //成像任务
        TCKG01 tckg01=new TCKG01();
        timemap.put("TCKG01",tckg01);
        TCAG01 tcag01=new TCAG01();
        timemap.put("TCAG01",tcag01);
        TCAG02 tcag02=new TCAG02();
        timemap.put("TCAG02",tcag02);
        TCAG03 tcag03=new TCAG03();
        timemap.put("TCAG03",tcag03);
        TCAG05 tcag05=new TCAG05();
        timemap.put("TCAG05",tcag05);
        TCDZG01 tcdzg01=new TCDZG01();
        timemap.put("TCDZG01",tcdzg01);
        TCAG07 tcag07=new TCAG07();
        timemap.put("TCAG07",tcag07);
        K4420 k4420=new K4420();
        timemap.put("K4420",k4420);
        TCGFG01 tcgfg01=new TCGFG01();
        timemap.put("TCGFG01",tcgfg01);
        TCDGG01 tcdgg01=new TCDGG01();
        timemap.put("TCDGG01",tcdgg01);
        K4401 k4401=new K4401();
        timemap.put("K4401",k4401);
        K4425 k4425=new K4425();
        timemap.put("K4425",k4425);
        TCA301 tca301=new TCA301();
        timemap.put("TCA301",tca301);
        TCA302 tca302=new TCA302();
        timemap.put("TCA302",tca302);
        TCGFG02 tcgfg02=new TCGFG02();
        timemap.put("TCGFG02",tcgfg02);
        TCDGG02 tcdgg02=new TCDGG02();
        timemap.put("TCDGG02",tcdgg02);
        TCGFG03 tcgfg03=new TCGFG03();
        timemap.put("TCGFG03",tcgfg03);
        TCDGG03 tcdgg03=new TCDGG03();
        timemap.put("TCDGG03",tcdgg03);
        TCDZG02 tcdzg02=new TCDZG02();
        timemap.put("TCDZG02",tcdzg02);
        TCA305_01 tca305_01=new TCA305_01();
        timemap.put("TCA305_01",tca305_01);
        TCA305_02 tca305_02=new TCA305_02();
        timemap.put("TCA305_02",tca305_02);
        TCKG02 tckg02=new TCKG02();
        timemap.put("TCKG02",tckg02);

        //任务暂停
        TCGFG06 tcgfg06=new TCGFG06();
        timemap.put("TCGFG06",tcgfg06);
        TCDGG06 tcdgg06=new TCDGG06();
        timemap.put("TCDGG06",tcdgg06);

        //任务恢复
        TCGFG07 tcgfg07=new TCGFG07();
        timemap.put("TCGFG07",tcgfg07);
        TCDGG07 tcdgg07=new TCDGG07();
        timemap.put("TCDGG07",tcdgg07);

        //回放任务
        TCAG04 tcag04=new TCAG04();
        timemap.put("TCAG04",tcag04);

        //固存擦除任务
        TCS207 tcs207=new TCS207();
        timemap.put("TCS207",tcs207);
        TCAG06 tcag06=new TCAG06();
        timemap.put("TCAG06",tcag06);
        TCA303 tca303=new TCA303();
        timemap.put("TCA303",tca303);

        return timemap;
    }
}