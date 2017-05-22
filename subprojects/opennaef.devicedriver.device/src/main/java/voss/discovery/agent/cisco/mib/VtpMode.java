package voss.discovery.agent.cisco.mib;

public enum VtpMode {
    UNDEFINED(-1), CLIENT(1), SERVER(2), TRANSPARENT(3), UNKNOWN(4),;

    public final int id;

    private VtpMode(int id) {
        this.id = id;
    }

    public static VtpMode valueOf(int id) {
        for (VtpMode instance : VtpMode.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return UNDEFINED;
    }
}