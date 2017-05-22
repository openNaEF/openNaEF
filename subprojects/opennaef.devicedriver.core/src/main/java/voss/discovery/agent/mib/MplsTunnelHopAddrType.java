package voss.discovery.agent.mib;

public enum MplsTunnelHopAddrType {
    illegal(-1),
    unknown(0),
    ipv4(1),
    ipv6(2),
    asnumber(3),
    unnum(4),
    lspid(5);

    private int id;

    private MplsTunnelHopAddrType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static MplsTunnelHopAddrType getById(int id) {
        for (MplsTunnelHopAddrType instance : MplsTunnelHopAddrType.values()) {
            if (instance.id == id) {
                return instance;
            }
        }
        return illegal;
    }
}