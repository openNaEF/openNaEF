package voss.discovery.utils;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ListUtil {

    public static List<String> toLines(String content) {
        return toLines(content, false);
    }

    public static List<String> toLines(String content, boolean trim) {
        BufferedReader br = new BufferedReader(new StringReader(content));
        List<String> result = new ArrayList<String>();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                if (trim) {
                    line = line.trim();
                }
                result.add(line);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(ListUtil.class).warn("failed to read line.", e);
        }
        return result;
    }

    public static String toContent(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    public static List<String> removeBlankLine(List<String> lines) {
        List<String> result = new ArrayList<String>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String temp = line.trim();
            if (temp.length() == 0) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    public static List<String> removeCommentLine(List<String> lines, String prefix) {
        List<String> result = new ArrayList<String>();
        for (String line : lines) {
            String temp = line.trim();
            if (temp.startsWith(prefix)) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    public static List<String> head(List<String> lines, int number) {
        List<String> result = new ArrayList<String>();
        int max = (lines.size() > number ? number : lines.size());
        for (int i = 0; i < max; i++) {
            result.add(lines.get(i));
        }
        if (lines.size() > number) {
            result.add("...(" + (lines.size() - number) + " lines skipped)...");
        }
        return result;
    }

    public static List<String> tail(List<String> lines, int number) {
        List<String> result = new ArrayList<String>();
        int startpos = (lines.size() > number ? lines.size() - number : 0);
        if (lines.size() > number) {
            result.add("...(" + (lines.size() - number) + " lines skipped)...");
        }
        for (int i = startpos; i < lines.size(); i++) {
            result.add(lines.get(i));
        }
        return result;
    }

    public static List<String> headAndTail(List<String> lines, int number) {
        if (lines.size() <= number * 2) {
            return lines;
        }
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < number; i++) {
            result.add(lines.get(i));
        }
        result.add("...(" + (lines.size() - number * 2) + " lines skipped)...");
        int startpos = lines.size() - number;
        for (int i = startpos; i < lines.size(); i++) {
            result.add(lines.get(i));
        }
        return result;
    }
}