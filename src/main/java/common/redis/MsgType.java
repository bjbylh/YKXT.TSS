package common.redis;

/**
 * Created by lihan on 2018/11/15.
 */
public enum MsgType {
    DB_REFRESH,
    NEW_TASK,
    TASK_STATUS_CHANGE,
    KEEP_ALIVE,
    CHECK_QUERY,
    CHECK_RESULT,
    TP_FINISHED,
    ORBIT_DATA_IMPORT,
    TRANSMISSION_EXPORT,
    TRANSMISSION_CANCEL,
    INS_CLEAR,
    FILE_CLEAR,
    INS_GEN,
    BLACK_CALI,
    MANUAL_LOOP,
    ORBIT_DATA_IMPORT_FINISHED,
    TRANSMISSION_EXPORT_FINISHED,
    TRANSMISSION_CANCEL_FINISHED,
    INS_CLEAR_FINISHED,
    INS_GEN_FINISHED,
    FILE_CLEAR_FINISHED,
    BLACK_CALI_FINISHED,
    MANUAL_LOOP_FINISHED,
    ORBIT_DATA_EXPORT,
    ORBIT_DATA_EXPORT_FINISHED,
    ENERGY_RESET,
    ENERGY_RESET_FINISHED
}
