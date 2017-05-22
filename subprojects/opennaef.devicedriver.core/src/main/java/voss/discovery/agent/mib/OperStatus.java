package voss.discovery.agent.mib;

public enum OperStatus {
    UP(1, "UP"),
    DOWN(2, "down"),
    TESTING(3, "testing"),
    UNKNOWN(4, "unknown"),
    DORMANT(5, "dormant"),
    NOT_PRESENT(6, "notPresent"),
    LOWER_LAYER_DOWN(7, "lowerLayerDown"),;
    private final int mibValue;
    private final String caption;

    private OperStatus(int mibValue, String caption) {
        this.mibValue = mibValue;
        this.caption = caption;
    }

    public int getMibValue() {
        return this.mibValue;
    }

    public String getCaption() {
        return this.caption;
    }

    public static OperStatus getByMibValue(int value) {
        for (OperStatus os : OperStatus.values()) {
            if (os.mibValue == value) {
                return os;
            }
        }
        return null;
    }
}