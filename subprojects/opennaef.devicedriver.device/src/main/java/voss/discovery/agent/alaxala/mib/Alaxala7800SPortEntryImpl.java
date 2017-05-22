package voss.discovery.agent.alaxala.mib;

import voss.discovery.agent.alaxala.AlaxalaPortEntry;

public class Alaxala7800SPortEntryImpl implements AlaxalaPortEntry {
    private int ifIndex;
    private int rpIndex;
    private int slotIndex;
    private int portIndex;

    private final static String ifNamePrefix = "";

    private final int portIndexOffset = 100;
    private final int rpOffsetOnPortIndexOffset = 200;
    private final int slotOffsetOnPortIndexOffset = 100;

    public Alaxala7800SPortEntryImpl(int _ifIndex) {
        assert Alaxala7800SMibImpl.getInterfaceTypeByIfIndex(_ifIndex)
                == Alaxala7800SMibImpl.InterfaceType.PORT;

        this.ifIndex = _ifIndex;
        int slotAndPortIndex = (ifIndex - portIndexOffset) % rpOffsetOnPortIndexOffset;

        this.slotIndex = slotAndPortIndex / slotOffsetOnPortIndexOffset - 1;
        this.portIndex = slotAndPortIndex % slotOffsetOnPortIndexOffset - 1;
    }

    public Alaxala7800SPortEntryImpl(int _ifIndex, int _slotIndex, int _portIndex) {
        this.ifIndex = _ifIndex;
        this.slotIndex = _slotIndex;
        this.portIndex = _portIndex;
        setRpId();
    }

    public void setRpId() {
        int _rpIndex = (this.ifIndex - portIndexOffset) / rpOffsetOnPortIndexOffset - 1;
        this.rpIndex = _rpIndex;
    }

    public int getIfIndex() {
        return this.ifIndex;
    }

    public int getRpId() {
        return this.rpIndex;
    }

    public int getSlotId() {
        return this.slotIndex;
    }

    public int getPortId() {
        return this.portIndex;
    }

    public String getIfName() {
        return ifNamePrefix + this.slotIndex + "/" + this.portIndex;
    }

}