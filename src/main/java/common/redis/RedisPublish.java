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
        jedis.publish(Topic.CMD_RECV, "{\"Head\":{\"id\":\"MAG@1576633544521\",\"time\":1576633544521,\"type\":\"TRANSMISSION_EXPORT\",\"from\":\"MAG\",\"to\":\"TSS\"},\"Data\":{\"content\":\"tn_1576577953868\"}}");//jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
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