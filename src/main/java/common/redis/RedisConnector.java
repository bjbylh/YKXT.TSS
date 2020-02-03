package common.redis;

import common.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by lihan on 2017/12/5.
 */
public class RedisConnector {
    //Redis服务器IP
    private static String RSIP = ConfigManager.getInstance().fetchRedisAddress();
//    private static String RSIP = "192.168.71.211";

    //Redis的端口
    //private static int RSPORT = 12718;
    //访问密码
    //private static String AUTH = "Sts3000P@ssw0rd";
    //可用连接实例的最大数目，默认值为8；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    private static int MAX_ACTIVE = 1024;
    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 200;
    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 10000;
    private static int TIMEOUT = 10000;
    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;
    private static JedisPool jedisPool = null;

    /**
     * 初始化Redis连接池
     */
    static {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            //config.set.setMaxActive(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            //config.setMaxWait(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            int RSPORT = ConfigManager.getInstance().fetchRedisPort();
            String AUTH = ConfigManager.getInstance().fetchRedisAuth();
            if(AUTH.equals(""))
                jedisPool = new JedisPool(config, RSIP, RSPORT, TIMEOUT);
            else
                jedisPool = new JedisPool(config, RSIP, RSPORT, TIMEOUT, AUTH);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Jedis实例
     *
     * @return
     */
    public synchronized static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            String err = e.getMessage();
            return null;
        }
    }

    /**
     * 释放jedis资源
     *
     * @param jedis
     */
    public static void CloseResource(final Jedis jedis) {
        if (jedis != null) {
            jedisPool.returnResource(jedis);
        }
    }

    public static void Close() {
        if (jedisPool != null) {
            jedisPool.destroy();
        }
    }
}
