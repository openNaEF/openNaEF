package tef.ui;

import tef.DateTimeFormat;

import java.util.Date;

public class TefDateFormatter {

    public static String formatWithRawValue(long time) {
        return formatWithRawValue(new Date(time));
    }

    public static String formatWithRawValue(Date date) {
        return (date.getTime() % 1000 != 0
                ? DateTimeFormat.YMDHMSS_DOT.format(date)
                : DateTimeFormat.YMDHMS_DOT.format(date))
                + "/" + date.getTime();
    }

    public static String getTimestamp() {
        return DateTimeFormat.YMDHMSS_DOT.format(System.currentTimeMillis());
    }
}
