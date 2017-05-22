package voss.utils;

public class MiscUtils {

    public static String null2empty(Object s) {
        if (s == null) {
            return "";
        }
        if (s instanceof String) {
            return (String) s;
        }
        return s.toString();
    }

    public static String empty2null(String s) {
        if (s == null) {
            return s;
        } else if (s.equals("")) {
            return null;
        }
        return s;
    }

    public static boolean equals(String[] array, String key) {
        if (array == null || key == null) {
            return false;
        }
        for (String s : array) {
            if (s == null) {
                continue;
            }
            if (s.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(String[] array, String key) {
        if (array == null || key == null) {
            return false;
        }
        for (String s : array) {
            if (s == null) {
                continue;
            }
            if (s.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(String[] array, String key) {
        if (array == null || key == null) {
            return false;
        }
        for (String s : array) {
            if (s == null) {
                continue;
            }
            if (s.matches(key)) {
                return true;
            }
        }
        return false;
    }

}