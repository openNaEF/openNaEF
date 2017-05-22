package voss.discovery.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static List<String> loadFile(String fileName) throws IOException {
        File file = new File(fileName);
        return loadFile(file);
    }

    public static List<String> loadFile(File file) throws IOException {
        BufferedReader reader = null;
        if (!file.exists()) {
            throw new IOException("not found: " + file.getAbsolutePath());
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            List<String> lines = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void saveFile(List<String> contents, String fileName) throws IOException {
        File file = new File(fileName);
        saveFile(contents, file);
    }

    public static void saveFile(List<String> contents, File file) throws IOException {
        FileWriter writer = null;
        if (file.exists()) {
            throw new IOException("already exists: " + file.getAbsolutePath());
        }
        try {
            writer = new FileWriter(file);
            for (String content : contents) {
                writer.write(content);
                writer.write("\r\n");
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static synchronized void rename(File from, File to, boolean overwrite) throws IOException {
        boolean success = true;
        if (!from.exists()) {
            throw new IOException("file from=[" + from.getAbsolutePath() + "] not exists.");
        }
        if (to.exists()) {
            if (overwrite) {
                success = to.delete();
                if (!success) {
                    throw new IOException("rename from->temp failed. from=[" + from.getAbsolutePath() + "]");
                }
            } else {
                throw new IOException("file to=" + to.getAbsolutePath() + "] already exists.");
            }
        }

        success = from.renameTo(to);
        if (!success) {
            throw new IOException("rename from->temp failed. from=[" + from.getAbsolutePath() + "]");
        }
    }

    public static long getTimestamp(File file) {
        assert file != null;
        String name = file.getName();
        int index = name.indexOf('.');
        String s = name.substring(0, index);
        try {
            long timestamp = Long.parseLong(s);
            return timestamp;
        } catch (NumberFormatException e) {
            log.warn("getTimestamp(): illegal name " + name);
            return -1;
        }
    }

    public static synchronized List<File> filterFiles(final File dir, final Pattern validFilePattern) {
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                Matcher matcher = validFilePattern.matcher(file
                        .getName());
                if (matcher.matches()) {
                    return true;
                }
                return false;
            }
        };
        List<File> files = Arrays.asList(dir.listFiles(filter));
        return files;
    }

}