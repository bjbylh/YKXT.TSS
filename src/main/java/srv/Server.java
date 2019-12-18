package srv;

import common.def.Topic;
import common.redis.TaskBuilderService;
import common.redis.subscribe.RedisTaskSubscriber;
import srv.task.InsAndFlashMontor;
import srv.task.TaskMonitor;

import java.io.IOException;

/**
 * Created by lihan on 2018/10/24.
 */
public class Server {
    public static void main(String[] args) throws InterruptedException, IOException {
        //启动任务监视线程
        TaskMonitor.getInstance().startup();

        //启动新任务监听线程
        RedisTaskSubscriber newTaskSubscriber = new RedisTaskSubscriber();
        TaskBuilderService newTaskBuilderService = new TaskBuilderService(newTaskSubscriber, Topic.CMD_RECV);
        newTaskBuilderService.startup();

        //启动控制监视线程
//        TaskStatusSubscriber taskStatusSubscriber = new TaskStatusSubscriber();
//        TaskBuilderService taskStatusBuilderService = new TaskBuilderService(taskStatusSubscriber, Topic.CMD);
//        taskStatusBuilderService.startup();

        //心跳服务
        //HeartBeatService.getInstance().startup();

        //指令状态及内存占用计算线程
        InsAndFlashMontor.getInstance().startup();
    }
}
