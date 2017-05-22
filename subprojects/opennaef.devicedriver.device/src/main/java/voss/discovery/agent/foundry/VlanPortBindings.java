package voss.discovery.agent.foundry;

import voss.discovery.iolib.snmp.SnmpEntry;

public class VlanPortBindings {

    private int ifIndex;
    private int vlanId;
    private int taggingState;

    public VlanPortBindings(SnmpEntry entry) {
        super();
        this.vlanId = entry.oidSuffix[0].intValue();
        this.ifIndex = entry.oidSuffix[1].intValue();
        this.taggingState = entry.getValueAsBigInteger().intValue();
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public int getVlanId() {
        return vlanId;
    }

    public boolean isTagged() {
        return taggingState == 1;
    }

}