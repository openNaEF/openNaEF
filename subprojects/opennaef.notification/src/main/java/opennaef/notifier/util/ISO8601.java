package opennaef.notifier.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 時間を ISO 8601 形式に変換する
 * format: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
 */
public class ISO8601 {
    public static final String ISO8601_EXTENDED = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final SimpleDateFormat _format = new SimpleDateFormat(ISO8601_EXTENDED);

    public static String format(long timeMillis) {
        synchronized (_format) {
            return _format.format(new Date(timeMillis));
        }
    }

}
