package voss.nms.inventory.util;

import java.math.BigDecimal;

public enum SiPrefix {

    NONE(1l, ""),
    KILO(1000l, "K"),
    MEGA(1000000l, "M"),
    GIGA(1000000000l, "G"),
    TERA(1000000000000l, "T");

    public final long value;
    public final String symbol;

    SiPrefix(long value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public String format(long value) {
        return new BigDecimal(value).divide(new BigDecimal(this.value))
                .toPlainString()
                + " "
                + this.symbol;
    }

    public static SiPrefix get(long value, int threshold) {
        value = Math.abs(value);

        SiPrefix result = NONE;
        for (SiPrefix siprefix : SiPrefix.values()) {
            if (siprefix.value * threshold > value) {
                return result;
            }
            result = siprefix;
        }
        return result;
    }
}