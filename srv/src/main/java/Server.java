import java.io.IOException;

/**
 * Created by lihan on 2018/10/24.
 */
public class Server {
    public static void main(String[] args) throws InterruptedException, IOException {
        //启动任务监视进程
        TaskMonitor.getInstance().startup();
    }
}
