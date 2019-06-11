package common.redis;

import common.def.Topic;
import redis.clients.jedis.Jedis;

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

}