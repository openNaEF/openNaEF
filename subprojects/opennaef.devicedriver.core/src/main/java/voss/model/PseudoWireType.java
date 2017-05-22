package voss.model;

public enum PseudoWireType {
    unknown(-1),
    other(0),
    frameRelay(1),
    atmAal5Vcc(2),
    atmTransparent(3),
    ethernetVLAN(4),
    ethernet(5),
    hdlc(6),
    ppp(7),
    cep(8),
    atmVccCell(9),
    atmVpcCell(10),
    ethernetVPLS(11);

    private final int id;

    private PseudoWireType(int id) {
        this.id = id;
    }

    public static PseudoWireType getById(int id) {
        for (PseudoWireType instance : PseudoWireType.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return unknown;
    }

    public int getId() {
        return this.id;
    }
}