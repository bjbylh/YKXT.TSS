package srv;

import common.def.Topic;
import common.redis.TaskBuilderService;
import common.redis.subscribe.GuidanceTaskSubscriber;
import common.redis.subscribe.GuidanceTaskSubscriber2;
import common.redis.subscribe.RedisTaskSubscriber;
import srv.task.*;

import java.io.IOException;

/**
 * Created by lihan on 2018/10/24.
 */
public class Server {
    public static void main(String[] args) throws InterruptedException, IOException {
        InitHistoryInstruction.getInstance().init();
        //启动任务监视线程
        TaskMonitor.getInstance().startup();

        //启动新任务监听线程
        RedisTaskSubscriber newTaskSubscriber = new RedisTaskSubscriber();
        TaskBuilderService newTaskBuilderService = new TaskBuilderService(newTaskSubscriber, Topic.CMD_RECV);
        newTaskBuilderService.startup();

        //引导信息任务
        GuidanceTaskSubscriber guidanceTaskSubscriber1 = new GuidanceTaskSubscriber();
        TaskBuilderService newTaskBuilderService1 = new TaskBuilderService(guidanceTaskSubscriber1, Topic.CMD);
        newTaskBuilderService1.startup();

        //引导信息任务2
        GuidanceTaskSubscriber2 guidanceTaskSubscriber2 = new GuidanceTaskSubscriber2();
        TaskBuilderService newTaskBuilderService2 = new TaskBuilderService(guidanceTaskSubscriber2, Topic.CMD2);
        newTaskBuilderService2.startup();

        //轨道外推
        OrbitCalcTaskLisener.getInstance().startup();

        //指令状态及内存占用计算线程
        InsAndFlashMontor.getInstance().startup();

        //文件占用情况统计服务
        FileOccupancyStatus.getInstance().startup();

        //启动控制线程
        //TaskStatusSubscriber taskStatusSubscriber = new TaskStatusSubscriber();
        //TaskBuilderService taskStatusBuilderService = new TaskBuilderService(taskStatusSubscriber, Topic.CMD);
        //taskStatusBuilderService.startup();

        //心跳服务
        //HeartBeatService.getInstance().startup();
    }
}
