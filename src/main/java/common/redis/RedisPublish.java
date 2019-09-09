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
        jedis.publish(Topic.CMD_DB_REFRESH, RedisDataEntity.GenDbRefreshData(taskid));
        jedis.close();
    }

    public static void heartbeat(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_KEEP_ALIVE, RedisDataEntity.GenHeartBeatData());
        jedis.close();
    }

    public static void newRTOrbitGorecastTask(){
        Jedis jedis = RedisConnector.getJedis();
//        jedis.publish(Topic.CMD_RECV, "{\"Head\":{\"id\":\"MAG@1567477226774\",\"time\":1567477226775,\"type\":\"CHECK_QUERY\",\"from\":\"MAG\",\"to\":\"TSS\"},\"Data\":{\"name\":\"应急规划可见性检查指令\",\"tasktype\":\"REALTIME\",\"content\":\"20190903095709744\"}}");
        jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void newCronOrbitGorecastTask(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RECV, RedisDataEntity.GenNewTask());
        jedis.close();
    }

    public static void checkResult(Map<String,Boolean> trueorfasle){
        System.out.println("Send a message...");
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD_RET, RedisDataEntity.GenCheckResult(trueorfasle));
        jedis.close();
    }

    public static void main(String[] args) {
        RedisPublish.newRTOrbitGorecastTask();
    }

}