package tef;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class TefFileUtils {

    private TefFileUtils() {
    }

    static PrintStream newFilePrintStream(File file, boolean append)
            throws FileNotFoundException {
        if (!append && file.exists()) {
            throw new IllegalStateException("file exists: " + file.getAbsolutePath());
        }

        return new PrintStream(new BufferedOutputStream(new FileOutputStream(file, append)));
    }

    static List<File> getDirectories(File parentDir) {
        return Arrays
                .asList(parentDir.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                }));
    }

    public static boolean isAbsolutePath(String path) {
        return new File(path).isAbsolute();
    }
}
