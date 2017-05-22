package tef;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public enum DateTimeFormat {

    YMDHMSS_DOT("yyyy.MM.dd-HH:mm:ss.SSS"),
    YMDHMSS_SLASH("yyyy/MM/dd-HH:mm:ss.SSS"),
    YMDHMS_DOT("yyyy.MM.dd-HH:mm:ss"),
    YMDHMS_SLASH("yyyy/MM/dd-HH:mm:ss"),
    YMD_DOT("yyyy.MM.dd"),
    YMD_SLASH("yyyy/MM/dd");

    public static Date parse(String str) throws ParseException {
        if (str == null) {
            return null;
        }

        for (DateTimeFormat format : values()) {
            try {
                synchronized (format.formatter_) {
                    return format.formatter_.parse(str);
                }
            } catch (ParseException pe) {
            }
        }
        throw new ParseException("invalid date-time format: " + str, 0);
    }

    private final DateFormat formatter_;

    DateTimeFormat(String pattern) {
        formatter_ = new SimpleDateFormat(pattern);
        formatter_.setLenient(false);
    }

    public String format(long date) {
        return format(new Date(date));
    }

    public String format(Date date) {
        if (date == null) {
            return null;
        }

        synchronized (formatter_) {
            return formatter_.format(date);
        }
    }
}
