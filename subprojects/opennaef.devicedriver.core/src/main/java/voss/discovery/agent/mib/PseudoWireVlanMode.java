package voss.discovery.agent.mib;

public enum PseudoWireVlanMode {
    unknown(-1),
    other(0),
    portBased(1),
    noChange(2),
    changeVlan(3),
    addVlan(4),
    removeVlan(5);

    private int id;

    private PseudoWireVlanMode(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static PseudoWireVlanMode getById(int id) {
        for (PseudoWireVlanMode value : PseudoWireVlanMode.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return PseudoWireVlanMode.unknown;
    }
}