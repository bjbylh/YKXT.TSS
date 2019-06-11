package srv.task;

import common.redis.RedisPublish;

/**
 * Created by lihan on 2018/12/25.
 */
public class HeartBeatService {
    private static HeartBeatService ourInstance = new HeartBeatService();

    public static HeartBeatService getInstance() {
        return ourInstance;
    }

    private HeartBeatService() {
    }
    public void startup() throws InterruptedException {
        HeartBeatService.DoWork doWork = new HeartBeatService.DoWork();
        doWork.start();
    }


    class DoWork extends Thread {
        public void run() {
            while (true) {
                try {
                    report();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void report(){
            RedisPublish.heartbeat();
        }
    }
}
