package naef;

import tef.skelton.FormatException;

public class NaefUtils {

    private NaefUtils() {
    }

    /**
     * 文字列を16進のint値として解釈します.
     */
    public static int parseHexInt(String str) {
        int result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result << 4;
            char c = str.charAt(i);
            if ('0' <= c && c <= '9') {
                result += c - '0';
            } else if ('a' <= c && c <= 'f') {
                result += c - 'a' + 10;
            } else {
                throw new RuntimeException();
            }
        }
        return result;
    }

    /**
     * 文字列を16進のlong値として解釈します.
     */
    public static long parseHexLong(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result << 4;
            char c = str.charAt(i);
            if ('0' <= c && c <= '9') {
                result += c - '0';
            } else if ('a' <= c && c <= 'f') {
                result += c - 'a' + 10;
            } else {
                throw new RuntimeException();
            }
        }
        return result;
    }

    /**
     * int値をdotted octet形式(IPv4アドレスの表現に使用される形式)の文字列に変換します.
     */
    public static String formatIntAsDottedOctet(int value) {
        return Integer.toString((value >> 24) & 0xff)
            + "."
            + Integer.toString((value >> 16) & 0xff)
            + "."
            + Integer.toString((value >> 8) & 0xff)
            + "."
            + Integer.toString((value >> 0) & 0xff);
    }

    /**
     * 文字列をdotted octet形式で表現されたint値として解釈します.
     */
    public static int parseIntAsDottedOctet(String str) throws FormatException {
        String[] tokens = str.split("\\.");
        if (tokens.length != 4) {
            throw new FormatException("4octet 形式に適合しません: " + str);
        }

        int value = 0;
        for (int i = 0; i < tokens.length; i++) {
            int octet;
            try {
                octet = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException nfe) {
                throw new FormatException((i + 1) + "番目の数値の形式が不正です: " + str);
            }
            if (octet < 0 || 0xff < octet) {
                throw new FormatException((i + 1) + "番目の数値が不正です: " + str);
            }

            value <<= 8;
            value |= octet;
        }
        return value;
    }
}
