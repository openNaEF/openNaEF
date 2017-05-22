package voss.model;

public enum PseudoWireOperStatus {
    undefined(-1),
    up(1),
    down(2),
    testing(3),
    unknown(4),
    dormant(5),
    notPresent(6),
    lowerLayerDown(7),

    noLocalInterface(1001),
    disabled(1002),
    encapsulationMismatch(1003),;

    private int id;

    private PseudoWireOperStatus(int id) {
        this.id = id;
    }

    public static PseudoWireOperStatus getById(int id) {
        for (PseudoWireOperStatus instance : PseudoWireOperStatus.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return undefined;
    }

    public int getId() {
        return this.id;
    }

}