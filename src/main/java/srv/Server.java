package srv;

import common.redis.TaskBuilderService;
import common.redis.Topic;
import common.redis.subscribe.NewTaskSubscriber;
import common.redis.subscribe.TaskStatusSubscriber;
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
        TaskBuilderService newTaskBuilderService = new TaskBuilderService(newTaskSubscriber, Topic.NEW_TASK);
        newTaskBuilderService.startup();

        //启动控制监视线程
        TaskStatusSubscriber taskStatusSubscriber = new TaskStatusSubscriber();
        TaskBuilderService taskStatusBuilderService = new TaskBuilderService(taskStatusSubscriber, Topic.TASK_STATUS_CHANGE);
        taskStatusBuilderService.startup();
    }
}
