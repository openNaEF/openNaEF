package voss.discovery.agent.mib;

public enum PseudoWireMplsType {
    unknown(-1),
    mplsTe(0),
    mplsNonTe(1),
    vcOnly(2);

    private int id;

    private PseudoWireMplsType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static PseudoWireMplsType getById(int id) {
        for (PseudoWireMplsType value : PseudoWireMplsType.values()) {
            if (value.id == id) {
                return value;
            }
        }
        return PseudoWireMplsType.unknown;
    }

}