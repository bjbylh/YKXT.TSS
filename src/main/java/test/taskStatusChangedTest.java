package test;

import common.def.InsType;
import common.redis.RedisConnector;
import common.redis.RedisDataEntity;
import common.redis.MsgType;
import redis.clients.jedis.Jedis;

/**
 * Created by lihan on 2018/11/30.
 */
public class taskStatusChangedTest {
    public static void main(String[] args) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(MsgType.TASK_STATUS_CHANGE.name(), RedisDataEntity.GenTaskStatusChange("", MsgType.TASK_STATUS_CHANGE, InsType.SUSPEND));
    }
}
