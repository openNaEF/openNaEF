package opennaef.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日付のフォーマットと解析を行う
 * 形式: "yyyy-MM-dd'T'HH:mm:ss.SSS"
 * <p>
 * SimpleDateFormatを同期化している
 */
public class DateFormat {
    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static String format(Date date) {
        synchronized (_sdf) {
            return _sdf.format(date);
        }
    }

    public static Date parse(String dateStr) throws ParseException {
        synchronized (_sdf) {
            return _sdf.parse(dateStr);
        }
    }
}
