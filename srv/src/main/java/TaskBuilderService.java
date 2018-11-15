import redis.clients.jedis.JedisPubSub;

/**
 * Created by lihan on 2018/11/15.
 */
public class TaskBuilderService {

    private RedisSubscribe redisSubscribe;

    public TaskBuilderService(JedisPubSub jedisPubSub) {
        redisSubscribe = new RedisSubscribe(jedisPubSub,Topic.NEW_TASK.name());
    }

    public void startup(){
        redisSubscribe.start();
    }
}
