import redis.clients.jedis.Jedis;

/**
 * Created by lihan on 2018/11/13.
 */
public class RedisPublish {
    public static void dbRefresh(String taskid) {
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.DB_REFRESH.name(), RedisDataEntity.GenDbRefreshData(taskid));
    }

    public static void testInitTask(){
        Jedis jedis = RedisConnector.getJedis();
        jedis.publish(Topic.NEW_TASK.name(), RedisDataEntity.GenNewTask());
    }

    public static void main(String[] args) {
        testInitTask();
    }
}
