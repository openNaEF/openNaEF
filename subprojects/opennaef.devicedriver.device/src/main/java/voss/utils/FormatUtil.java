package voss.utils;

import voss.FormatConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class FormatUtil {
    public static String formatDate(final Date date) {
        if (null == date) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(
                FormatConstants.DATE_FORMAT_PATTERN);
        return sdf.format(date);
    }

    public static String formatDate(final Calendar calen) {
        if (null == calen) {
            return null;
        } else {
            return formatDate(calen.getTime());
        }
    }

    public static String n2s(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    private FormatUtil() {

    }
}