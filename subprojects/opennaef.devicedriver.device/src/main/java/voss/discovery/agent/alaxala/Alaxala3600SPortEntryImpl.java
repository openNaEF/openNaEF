package voss.discovery.agent.alaxala;

public class Alaxala3600SPortEntryImpl implements AlaxalaPortEntry {
    private int ifIndex;
    private int portId;
    private final static int portIfIndexOffset = 10;
    private final static String prefix = "0/";

    public Alaxala3600SPortEntryImpl(int _portId) {
        if (_portId < 1) {
            throw new IllegalArgumentException();
        }
        this.portId = _portId;
        this.ifIndex = getIfIndex(_portId);
    }

    public static int getIfIndex(int _portId) {
        return _portId - 1 + portIfIndexOffset;
    }

    public int getIfIndex() {
        return this.ifIndex;
    }

    public int getRpId() {
        return -1;
    }

    public int getSlotId() {
        return 0;
    }

    public int getPortId() {
        return this.portId;
    }

    public String getIfName() {
        return prefix + this.portId;
    }

}