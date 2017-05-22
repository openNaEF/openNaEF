package voss.discovery.agent.juniper;

public enum JnxMplsPathType {
    unknown(-1),
    other(1),
    primary(2),
    standby(3),
    secondary(4);

    private final int id;

    private JnxMplsPathType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static JnxMplsPathType getById(int id) {
        for (JnxMplsPathType instance : JnxMplsPathType.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return unknown;
    }

}