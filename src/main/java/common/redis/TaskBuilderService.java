package common.redis;

import redis.clients.jedis.JedisPubSub;

/**
 * Created by lihan on 2018/11/15.
 */
public class TaskBuilderService {

    private RedisSubscribe redisSubscribe;

    public TaskBuilderService(JedisPubSub jedisPubSub, Topic topic) {
        redisSubscribe = new RedisSubscribe(jedisPubSub, topic.name());
    }

    public void startup() {
        redisSubscribe.start();
    }
}
