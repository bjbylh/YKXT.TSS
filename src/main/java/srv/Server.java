package srv;

import common.redis.subscribe.NewTaskSubscriber;
import common.redis.TaskBuilderService;
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
        NewTaskSubscriber newTaskSubscriber = new NewTaskSubscriber();
        TaskBuilderService taskBuilderService = new TaskBuilderService(newTaskSubscriber);
        taskBuilderService.startup();
    }
}
