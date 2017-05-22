package voss.nms.inventory.util;

public class BandwidthFormat {

    public static final long ONE = 1L;
    public static final long KILO = 1000L;
    public static final long MEGA = 1000L * KILO;
    public static final long GIGA = 1000L * MEGA;
    public static final long TERA = 1000L * GIGA;

    public String format(long value) {
        return format(value, 2);
    }

    public static String format(Long value, int threshold) {
        return value == null
                ? null
                : SiPrefix.get(value, threshold).format(value);
    }
}