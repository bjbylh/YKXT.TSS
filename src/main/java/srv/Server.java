package srv;

import common.def.Topic;
import common.redis.TaskBuilderService;
import common.redis.subscribe.NewTaskSubscriber;

import java.io.IOException;

/**
 * Created by lihan on 2018/10/24.
 */
public class Server {
    public static void main(String[] args) throws InterruptedException, IOException {
        //启动任务监视线程
        //TaskMonitor.getInstance().startup();

        //启动新任务监听线程
        NewTaskSubscriber newTaskSubscriber = new NewTaskSubscriber();
        TaskBuilderService newTaskBuilderService = new TaskBuilderService(newTaskSubscriber, Topic.CMD);
        newTaskBuilderService.startup();

        //启动控制监视线程
//        TaskStatusSubscriber taskStatusSubscriber = new TaskStatusSubscriber();
//        TaskBuilderService taskStatusBuilderService = new TaskBuilderService(taskStatusSubscriber, Topic.CMD);
//        taskStatusBuilderService.startup();

        //心跳服务
        //HeartBeatService.getInstance().startup();

    }
}