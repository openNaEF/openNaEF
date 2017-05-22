package voss.discovery.agent.cisco.mib;

public enum SpanningTreeType {
    UNDEFINED(-1, "undefined"),
    PVSTplus(1, "PVST+"),
    MISTP(2, "MISTP"),
    MISTP_PVSTplus(3, "MISTP-PVST+"),
    MST(4, "MST"),
    RSTP_PVSTplus(5, "RSTP-PVST+"),;

    private final int id;
    private final String value;

    private SpanningTreeType(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return this.id;
    }

    public String getValue() {
        return this.value;
    }

    public static SpanningTreeType valueOf(int id) {
        for (SpanningTreeType value : SpanningTreeType.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return UNDEFINED;
    }

}