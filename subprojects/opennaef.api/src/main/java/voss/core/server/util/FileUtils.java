package voss.core.server.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    private FileUtils() {
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T loadObject(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        if (!file.exists()) {
            return null;
        }
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            Object o = ois.readObject();
            return (T) o;
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    public static synchronized void saveObject(File file, Object o, boolean overwrite) throws IOException {
        ObjectOutputStream oos = null;
        if (file.exists() && !overwrite) {
            throw new IOException("file exists: " + file.getCanonicalPath());
        }
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(o);
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
    }

    public static String loadContents(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String s = null;
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s);
                sb.append("\r\n");
            }
            return sb.toString();
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    public static List<String> loadLines(File file) throws IOException {
        List<String> result = new ArrayList<String>();
        if (file == null) {
            throw new IllegalArgumentException();
        } else if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException();
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String s = null;
            while ((s = br.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("#")) {
                    continue;
                }
                result.add(s);
            }
            return result;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}