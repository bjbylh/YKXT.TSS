package common.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import common.redis.subscribe.RedisTaskSubscriber;

/**
 * Created by lihan on 2018/11/15.
 */
public class RedisSubscribe extends Thread {
    private JedisPubSub jedisPubSub;

    private String channel = "mychannel";

    public RedisSubscribe(JedisPubSub jedisPubSub,String channel) {
        super("SubThread");
        this.jedisPubSub = jedisPubSub;
        this.channel = channel;
    }

    @Override
    public void run() {
        System.out.println(String.format("subscribe redis, channel %s, thread will be blocked", channel));
        Jedis jedis = null;
        try {
            jedis = RedisConnector.getJedis();   //取出一个连接
            jedis.subscribe(jedisPubSub, channel);    //通过subscribe的api去订阅，入参是订阅者和频道名
        } catch (Exception e) {
            System.out.println(String.format("subsrcribe channel error, %s", e));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void main(String[] args) {
        RedisTaskSubscriber subscriber = new RedisTaskSubscriber();
        RedisSubscribe redisSubscribe = new RedisSubscribe(subscriber, MsgType.NEW_TASK.name());
        redisSubscribe.run();
    }
}
