package common.redis;

/**
 * Created by lihan on 2018/11/15.
 */
public enum MsgType {
    DB_REFRESH,
    NEW_TASK,
    TASK_STATUS_CHANGE,
    KEEP_ALIVE
}
