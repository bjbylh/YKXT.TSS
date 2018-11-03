/**
 * Copyright2017Wang. All rights reserved.
 *
 * @Title: Run.java
 * @Prject: demo.nuaa.launcher
 * @Package: demo.nuaa.launcher.run
 * @Description: TODO
 * @author: wang
 * @date: 20174:36:28
 * @version: V1.0
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @ClassName: Run
 * @Description: TODO
 * @author: wang
 */
public class Run {

    public static Boolean Exec(String id) {
        new Thread(() -> {
            Process p;
            try {
//                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "java", "-jar", "core-1.0-SNAPSHOT.jar", id);
                ProcessBuilder builder = new ProcessBuilder("C:\\Users\\lihan\\Desktop\\ykxt\\bin\\TSS-CORE\\CORE.exe");

                builder.directory(new File("C:\\Users\\lihan\\Desktop\\ykxt\\bin\\TSS-CORE\\"));
                builder.redirectErrorStream(true);
                p = builder.start();
                //System.out.println("Program:" + exename + " has started!");
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
}
