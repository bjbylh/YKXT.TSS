package common.process;

import java.io.*;

/**
 * @ClassName: common.process.Run
 * @Description: TODO
 * @author: wang
 */
public class Run {

    public static Boolean Exec(String path, String input) {
        new Thread(() -> {
            Process p;
            try {
//                ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "java", "-jar", "core-1.0-SNAPSHOT.jar", id);
                ProcessBuilder builder = new ProcessBuilder(path + "地面运控系统控制计算软件.exe", input);

                builder.directory(new File(path));
                builder.redirectErrorStream(true);
                p = builder.start();
                //
                InputStream fis = p.getInputStream();
                //
                InputStreamReader isr = new InputStreamReader(fis);
                // 顢�
                BufferedReader br = new BufferedReader(isr);

                OutputStream os = p.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);

                String line;
                // 顒�
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }

                bw.write("1");
                bw.newLine();
                bw.flush();
                bw.close();

                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

    public static Boolean ExecJar(String path, String param, String taskType) {
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

    /**
     * 通用的运行exe接口。先启动exeName程序，然后再输入input，类似回车操作。
     *
     * @throws IOException
     */
    public static void callExtExeAndWait(String exeName, String input) {
        Runtime runtime = Runtime.getRuntime();
        BufferedWriter inputW = null;
        try {
            Process process = runtime.exec(new String[]{exeName}, null, null);
            if (input != null) {
                inputW = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                inputW.write(input);
                inputW.flush();
                inputW.close();
            }
//            BufferedReader stdoutReader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
//            BufferedReader stderrReader = new BufferedReader(
//                    new InputStreamReader(process.getErrorStream(), Charset.forName("GBK")));
//            String line;
//            while ((line = stdoutReader.readLine()) != null) {
//                System.out.println(line);
//            }
//            while ((line = stderrReader.readLine()) != null) {
//                System.out.println(line);
//            }
            int exitVal = process.waitFor();
            System.out.println("process exit value is " + exitVal);
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            if (inputW != null) {
                try {
                    inputW.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 通用的运行exe接口。先启动exeName程序，然后再输入input，类似回车操作。
     *
     * @throws IOException
     */
    public static Process callExtExeWithoutWait(String exeName, String input) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(new String[]{exeName}, null, null);
            return process;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
