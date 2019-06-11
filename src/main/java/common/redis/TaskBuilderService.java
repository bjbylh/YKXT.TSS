package common.redis;

import redis.clients.jedis.JedisPubSub;

/**
 * Created by lihan on 2018/11/15.
 */
public class TaskBuilderService {

    private RedisSubscribe redisSubscribe;

    public TaskBuilderService(JedisPubSub jedisPubSub, String topic) {
        redisSubscribe = new RedisSubscribe(jedisPubSub, topic);
    }

    public void startup() {
        redisSubscribe.start();
    }
}
