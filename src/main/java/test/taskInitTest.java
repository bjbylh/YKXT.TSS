package test;

import common.def.Topic;
import common.redis.RedisConnector;
import common.redis.RedisDataEntity;
import redis.clients.jedis.Jedis;

/**
 * Created by lihan on 2018/11/19.
 */
public class taskInitTest {
    public static void main(String[] args) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.CMD, RedisDataEntity.GenNewTask());
    }
}
