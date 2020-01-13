package core.taskplan;


import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.time.Instant;
import java.util.HashSet;

/**
 * Created by lihan on 2019/12/18.
 */
public interface DefineInferface {
    /**
     * 指令直接生成接口
     * 处理过程中，需要将成像订单和数传订单转化为任务，image_order --> image_mission  station_mission --> transmission_mission
     *
     * @param imageOrder     成像订单
     * @param stationMission 数传订单
     * @param Orbitjson      轨道数据
     * @param OrbitDataCount 轨道数据条数
     * @param FilePath       指令生成路径
     * @return 指令文件存储路径（全路径）
     */
    String InsGenWithoutTaskPlanInf(Document imageOrder, Document stationMission, FindIterable<Document> Orbitjson, long OrbitDataCount, String FilePath);

    /**
     * 固存擦除指令生成接口
     *
     * @param Mission  任务数据
     * @param FilePath 指令生成路径
     * @return 指令文件存储路径（全路径）
     */
    String FileClearInsGenInf(Document Mission, String FilePath);

    /**
     * @param type       0区间左、1区间右，2区间中间，3全部删除
     * @param start
     * @param end
     * @param insno
     * @param FilePath
     * @param isTimeSpan 0:id,1:时间段,3:表示全部删除
     * @param exetime 指令执行时间
     * @return
     */
    String InsClearInsGenInf(int isTimeSpan, int type, Instant exetime, Instant start, Instant end, HashSet<Integer> insno, String FilePath);
}
