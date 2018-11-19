package test;

import common.redis.RedisConnector;
import common.redis.RedisDataEntity;
import common.redis.Topic;
import redis.clients.jedis.Jedis;

/**
 * Created by lihan on 2018/11/19.
 */
public class taskInitTest {
    public static void main(String[] args) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.NEW_TASK.name(), RedisDataEntity.GenNewTask());
    }
}
