package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.LogConstants;


public class PerfLog {
    private static final Logger log = LoggerFactory.getLogger(LogConstants.LOG_PERFORMANCE);

    public static void error(long start, long end, String... options) {
        String line = log_(start, end, options);
        log.error(line);
    }

    public static void warn(long start, long end, String... options) {
        String line = log_(start, end, options);
        log.warn(line);
    }

    public static void info(long start, long end, String... options) {
        if (log.isInfoEnabled()) {
            String line = log_(start, end, options);
            log.info(line);
        }
    }

    public static void debug(long start, long end, String... options) {
        if (log.isDebugEnabled()) {
            String line = log_(start, end, options);
            log.debug(line);
        }
    }

    public static void trace(long start, long end, String... options) {
        if (log.isTraceEnabled()) {
            String line = log_(start, end, options);
            log.trace(line);
        }
    }

    private static String log_(long start, long end, String... options) {
        StringBuilder sb = new StringBuilder();
        long tid = Thread.currentThread().getId();
        long duration = end - start;
        sb.append(tid).append(" ").append(start).append(" ").append(end).append(" ").append(duration);
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        if (st.length > 3) {
            sb.append(" ");
            StackTraceElement ste = st[3];
            sb.append(ste.getClassName().substring(ste.getClassName().lastIndexOf('.') + 1) + ":" + ste.getLineNumber());
        }
        for (String option : options) {
            sb.append(" ");
            sb.append(option);
        }
        return sb.toString();
    }
}