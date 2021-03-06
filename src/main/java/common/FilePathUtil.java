package common;

import java.time.Instant;

/**
 * Created by lihan on 2018/3/1.
 */
public class FilePathUtil {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    //public static final String FILE_SEPARATOR = File.separator;

    public static String getRealFilePath(String path) {
        return path.replace("/", FILE_SEPARATOR).replace("\\", FILE_SEPARATOR);
    }

    public static void main(String[] args) {
        long t = 1584168147305l;
        System.out.println(Instant.ofEpochMilli(t).toString());
    }
}
