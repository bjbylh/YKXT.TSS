package core.taskplan;


import com.mongodb.client.FindIterable;
import org.bson.Document;

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
     * @param isClearAll 是否清空所有文件
     * @param filenos    isClearAll 为true时，该字段被忽略，为false时，表示要清空的文件编号（0-63）
     * @param FilePath   指令生成路径
     * @return 指令文件存储路径（全路径）
     */
    String FileClearInsGenInf(Boolean isClearAll, HashSet<Integer> filenos, String FilePath);

    /**
     * 星上指令清空接口
     *
     * @param insno    待清空的指令序号
     * @param FilePath 指令生成路径
     * @return 指令文件存储路径（全路径）
     */
    String InsClearInsGenInf(HashSet<Integer> insno, String FilePath);
}
