package common.redis;

import common.def.Topic;
import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * Created by lihan on 2018/11/13.
 */

public class RedisPublish {

    public static void dbRefresh(String taskid) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenDbRefreshData(taskid));
        jedis.close();
    }

    public static void heartbeat(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenHeartBeatData());
        jedis.close();
    }

    public static void newRTOrbitGorecastTask(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void newCronOrbitGorecastTask(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void checkResult(Map<String,Boolean> trueorfasle){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenCheckResult(trueorfasle));
        jedis.close();
    }

    public static void main(String[] args) {
        RedisPublish.newRTOrbitGorecastTask();
    }

}