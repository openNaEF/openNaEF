package voss.discovery.iolib.simpletelnet;


public class VossTelnetOption {
    public static final byte OPTION_TIMING_MARK = (byte) 6;
    public static final byte SUBOPTION_END = (byte) 240;
    public static final byte SUBOPTION_BEGIN = (byte) 250;
    public static final byte WILL = (byte) 251;
    public static final byte WONT = (byte) 252;
    public static final byte DO = (byte) 253;
    public static final byte DONT = (byte) 254;
    public static final byte IAC = (byte) 255;
    public static final byte OPTION_ECHO = (byte) 1;
    public static final byte OPTION_STATUS = (byte) 5;
    public static final byte OPTION_TERMINAL_TYPE = (byte) 24;
    public static final byte OPTION_WINDOW_SIZE = (byte) 31;
    public static final byte OPTION_TERMINAL_SPEED = (byte) 32;
    public static final byte OPTION_LINEMODE = (byte) 34;
    public static final byte OPTION_ENVIRONMENT_VARIABLE = (byte) 36;

    public static String getOptionName(byte b) {
        switch (b) {
            case SUBOPTION_BEGIN:
                return "SUBOPTION_BEGIN";
            case SUBOPTION_END:
                return "SUBOPTION_END";
            case DO:
                return "DO";
            case DONT:
                return "DONT";
            case WILL:
                return "WILL";
            case WONT:
                return "WONT";
            case IAC:
                return "IAC";

            case OPTION_ECHO:
                return "OPTION_ECHO";
            case OPTION_STATUS:
                return "OPTION_STATUS";
            case OPTION_TIMING_MARK:
                return "OPTION_TIMING_MARK";
            case OPTION_LINEMODE:
                return "OPTION_LINEMODE";
            case OPTION_TERMINAL_SPEED:
                return "OPTION_TERMINAL_SPEED";
            case OPTION_WINDOW_SIZE:
                return "OPTION_WINDOW_SIZE";
            case OPTION_TERMINAL_TYPE:
                return "OPTION_TERMINAL_TYPE";

            default:
                return "0x" + Integer.toHexString(b);
        }
    }

    public static String getCodeOrCharacter(byte b) {
        if (0x20 <= b && b <= 0x7e) {
            return String.valueOf((char) b);
        } else {
            return "0x" + Integer.toHexString(b);
        }
    }

    public static final byte[] ASK_TERMINAL_TYPE
            = {IAC, DO, OPTION_TERMINAL_TYPE};

    public static final byte[] WILL_TERMINAL_TYPE
            = {IAC, WILL, OPTION_TERMINAL_TYPE};

    public static final byte[] ASK_TERMINAL_SUBOPTION
            = {IAC, SUBOPTION_BEGIN, OPTION_TERMINAL_TYPE, 0x01, IAC, SUBOPTION_END};

    public static final byte[] BEGIN_TERMINAL_SUBOPTION_VT100
            = {IAC, SUBOPTION_BEGIN, OPTION_TERMINAL_TYPE, 0x00, 'V', 'T', '1', '0', '0'};

    public static final byte[] END_SUBOPTION = {IAC, SUBOPTION_END};

    public static String decodeNegotiation(byte[] nego) {
        assert nego.length >= 3;

        StringBuffer sb = new StringBuffer();
        int code_length = (nego.length >= 3 ? 3 : 2);
        for (int i = 0; i < code_length; i++) {
            sb.append(getOptionName(nego[i])).append(" ");
        }
        if (nego.length > 3) {
            for (int i = 3; i < nego.length; i++) {
                sb.append(getCodeOrCharacter(nego[i])).append(" ");
            }
        }
        return sb.toString();
    }

    public static boolean matches(byte[] target, byte[] litmus) {
        if (target.length != litmus.length) {
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if (target[i] != litmus[i]) {
                return false;
            }
        }
        return true;
    }


}