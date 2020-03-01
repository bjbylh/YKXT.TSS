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
        jedis.publish(Topic.CMD_RECV, "{\"Head\":{\"id\":\"MAG@1582527916630\",\"time\":1582527916630,\"type\":\"MANUAL_LOOP\",\"from\":\"MAG\",\"to\":\"TSS\"},\"Data\":{\"content\":\"20200224153648219\"}}");//jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void newCronOrbitGorecastTask() {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void checkResult(String id, Map<Integer, Map<String, Boolean>> trueorfasle) {
        Jedis jedis = RedisConnector.getJedis();
        String ret = RedisDataEntity.GenCheckResult(id, trueorfasle);
        System.out.println(ret);
        jedis.publish(Topic.CMD_RET, ret);
        jedis.close();
    }

    public static void CommonReturn(String id, Boolean isOK, String details, MsgType msgType) {
        Jedis jedis = RedisConnector.getJedis();
        String ret = RedisDataEntity.GenCommonRet(id, isOK, details, msgType);
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