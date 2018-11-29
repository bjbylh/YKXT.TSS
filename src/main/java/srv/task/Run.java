package srv.task;

import java.io.*;

/**
 * @ClassName: srv.task.Run
 * @Description: TODO
 * @author: wang
 */
public class Run {

    public static Boolean Exec(String path, String exeName, String param) {
        new Thread(() -> {
            Process p;
            try {
//                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "java", "-jar", "core-1.0-SNAPSHOT.jar", id);
                ProcessBuilder builder = new ProcessBuilder(path + exeName, param);

                builder.directory(new File(path));
                builder.redirectErrorStream(true);
                p = builder.start();
                //
                InputStream fis = p.getInputStream();
                //
                InputStreamReader isr = new InputStreamReader(fis);
                // 顢�
                BufferedReader br = new BufferedReader(isr);
                String line;
                // 顒�
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

    public static Boolean ExecJar(String path, String param) {
        new Thread(() -> {
            Process p;
            try {
                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "java", "-jar", path + "core.jar", param);
//                ProcessBuilder builder = new ProcessBuilder(path + exeName, param);

                builder.directory(new File(path));
                builder.redirectErrorStream(true);
                p = builder.start();

                InputStream fis = p.getInputStream();

                InputStreamReader isr = new InputStreamReader(fis);

                BufferedReader br = new BufferedReader(isr);
                String line;

                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }
}
