package voss.utils;

import java.util.Collections;
import java.util.List;

public class ExceptionUtils {
    private ExceptionUtils() {
    }

    public static String getStackTrace(Throwable throwable) {
        String result = throwable.getMessage() + "\r\n";
        StackTraceElement[] stack = throwable.getStackTrace();
        for (StackTraceElement e : stack) {
            result = result + "    " + e.toString() + "\r\n";
        }
        return result;
    }

    public static class PseudoStacktrace {
        private final String message;
        private final List<String> stacktrace;

        public PseudoStacktrace(String msg, List<String> stacktrace) {
            this.message = msg;
            this.stacktrace = stacktrace;
        }

        public String getMessage() {
            return this.message;
        }

        public List<String> getStacktrace() {
            return Collections.unmodifiableList(this.stacktrace);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.message);
            sb.append("\r\n");
            for (String s : this.stacktrace) {
                sb.append("    ");
                sb.append(s);
                sb.append("\r\n");
            }
            return sb.toString();
        }
    }

}