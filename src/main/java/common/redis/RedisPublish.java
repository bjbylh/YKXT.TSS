package common.redis;

import common.def.Topic;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lihan on 2018/11/13.
 */

public class RedisPublish {

    public static void dbRefresh(String taskid) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_DB_REFRESH, RedisDataEntity.GenDbRefreshData(taskid));
        jedis.close();
    }

    public static void heartbeat() {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_KEEP_ALIVE, RedisDataEntity.GenHeartBeatData());
        jedis.close();
    }

    public static void newRTOrbitGorecastTask() {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RECV, "{\"Head\":{\"id\":\"MAG@1568796984537\",\"time\":1568796984537,\"type\":\"NEW_TASK\",\"from\":\"MAG\",\"to\":\"TSS\"},\"Data\":{\"name\":\"新建任务调度任务指令\",\"tasktype\":\"REALTIME\",\"templet\":\"TASK_PLAN\",\"imageorder\":\"20190917175543913\",\"stationmission\":\"2019091817583969\"}}");
                //jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void newCronOrbitGorecastTask() {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void checkResult(String id, Map<String, Boolean> trueorfasle) {
        Jedis jedis = RedisConnector.getJedis();
        String ret = RedisDataEntity.GenCheckResult(id, trueorfasle);
        System.out.println(ret);
        jedis.publish(Topic.CMD_RET, ret);
        jedis.close();
    }

    public static void taskPlanFinished(String id, ArrayList<String> orderlist) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RET, RedisDataEntity.GenFinishedInform(id, orderlist));
        jedis.close();
    }

    public static void main(String[] args) {
        RedisPublish.newRTOrbitGorecastTask();
    }

}