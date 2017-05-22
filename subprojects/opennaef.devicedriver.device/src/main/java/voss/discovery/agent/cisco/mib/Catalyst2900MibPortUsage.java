package voss.discovery.agent.cisco.mib;

public enum Catalyst2900MibPortUsage {
    UNDEFINED(-1, "UNDEFINED"),
    standard(1, "standard"),
    security(2, "security"),
    monitor(3, "monitor"),
    portGrouping(4, "portGrouping"),
    network(5, "network"),
    networkGroup(6, "networkGroup"),
    portGroupDest(7, "portGroupDest"),
    protected_(8, "protected");

    public final int id;
    public final String value;

    private Catalyst2900MibPortUsage(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return this.id;
    }

    public static Catalyst2900MibPortUsage valueOf(int id) {
        for (Catalyst2900MibPortUsage instance : Catalyst2900MibPortUsage.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return UNDEFINED;
    }
}