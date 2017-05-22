package voss.discovery.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.util.VossMiscUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ByteArrayUtil {
    private final static Logger log = LoggerFactory.getLogger(ByteArrayUtil.class);
    private final static byte NULL_ZERO = 0x00;

    public static boolean mayBeContains(byte[] target, byte[] litmusPaper) {
        if (litmusPaper.length == 0 || target.length == 0) {
            return false;
        }
        if (target.length < litmusPaper.length) {
            return false;
        }

        if (litmusPaper[litmusPaper.length - 1] == NULL_ZERO || litmusPaper[0] == NULL_ZERO) {
            throw new IllegalArgumentException("Last byte must be other than '\0'");
        }

        int target_position = target.length - 1;
        int litmus_position = litmusPaper.length - 1;
        while (litmus_position >= 0) {
            log.trace("compare: " +
                    "litmus[" + litmus_position + "]: 0x" + Integer.toHexString(litmusPaper[litmus_position]) +
                    "/target[" + target_position + "]: 0x" + Integer.toHexString(target[target_position]));
            if (isWildcard(litmusPaper[litmus_position])) {
                litmus_position--;
                if (isWildcard(litmusPaper[litmus_position])) {
                    throw new IllegalArgumentException(
                            "wild card may not be continuous: "
                                    + byteArrayToHexString(litmusPaper));
                }
                WILD_CARD_EXECUTION:
                while (target_position > 0) {
                    log.trace("wild card compare: " +
                            "litmus[" + litmus_position + "]: 0x" + Integer.toHexString(litmusPaper[litmus_position]) +
                            "/target[" + target_position + "]: 0x" + Integer.toHexString(target[target_position]));
                    if (target[target_position] != litmusPaper[litmus_position]) {
                        target_position--;
                    } else {
                        target_position--;
                        litmus_position--;
                        break WILD_CARD_EXECUTION;
                    }
                }
            } else {
                if (target[target_position] == litmusPaper[litmus_position]) {
                    target_position--;
                    litmus_position--;
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isWildcard(byte b) {
        return b == 0x00;
    }

    public static String byteArrayToHexString(byte[] array) {
        assert array != null;

        if (array.length == 0) {
            return null;
        }

        StringBuffer sb = null;
        for (byte b : array) {
            if (sb == null) {
                sb = new StringBuffer();
            } else {
                sb.append(":");
            }
            sb.append("0x").append(Integer.toHexString(b & 0xff));
        }
        return sb.toString();
    }

    public static boolean contains(byte[] target, byte[] litmusPaper) {
        if (target.length < litmusPaper.length) {
            return false;
        }
        for (int i = 0; i <= target.length - litmusPaper.length; i++) {
            if (target[i] == litmusPaper[0]) {
                if (ByteArrayUtil.startsWith(target, i, litmusPaper)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean matches(byte[] targetInByte, byte[] litmusPaper) {
        String litmus = new String(litmusPaper).intern();
        BufferedReader br = new BufferedReader(new StringReader(new String(targetInByte).trim().intern()));
        String target = null;
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                target = line;
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception.", e);
        }
        if (target == null) {
            if (log.isTraceEnabled()) {
                log.trace("no target, litmus=[" + litmus + "]");
            }
            return false;
        } else if (target.charAt(0) == 0x00) {
            target = target.substring(1);
        }
        if (log.isTraceEnabled()) {
            log.trace("target=[" + VossMiscUtility.showDetail(target) + "], litmus=[" + litmus + "]");
        }
        Pattern p = Pattern.compile(litmus);
        Matcher matcher = p.matcher(target);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }

    public static boolean startsWith(byte[] target, int offset, byte[] litmusPaper) {
        if ((target.length - offset) < litmusPaper.length) {
            return false;
        }
        for (int i = offset, j = 0; j < litmusPaper.length; i++, j++) {
            if (target[i] != litmusPaper[j]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        int i = 0;
        for (; i < a.length; i++) {
            result[i] = a[i];
        }
        for (int j = 0; i < a.length + b.length; i++, j++) {
            result[i] = b[j];
        }
        return result;
    }
}