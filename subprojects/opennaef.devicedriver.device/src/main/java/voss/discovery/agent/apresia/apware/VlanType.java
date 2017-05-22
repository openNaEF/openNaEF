package voss.discovery.agent.apresia.apware;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;


@SuppressWarnings("serial")
class VlanType extends IntSnmpEntry {

    private int vlanId;
    private int ifIndex;
    private int vlanType;

    public VlanType(String rootOidString, VarBind varbind) {
        super(rootOidString, varbind);
    }

    public void setup() throws UnexpectedVarBindException {
        if (oidSuffix.length != 2) {
            throw new UnexpectedVarBindException(varbind);
        }
        vlanId = oidSuffix[0].intValue();
        ifIndex = oidSuffix[1].intValue();
        vlanType = intValue();
    }

    public int getVlanID() {
        return vlanId;
    }

    public int getVlanIfIndex() {
        return vlanId + ApresiaApwareMib.VLAN_BASE_IFINDEX;
    }

    public int getPortIfIndex() {
        return ifIndex;
    }

    public boolean isVdrRelation() {
        return ifIndex > ApresiaApwareMib.VDR_BASE_IFINDEX
                && ifIndex < ApresiaApwareMib.MMRP_BASE_IFINDEX;
    }

    public int getVdrId() {
        return (ifIndex - ApresiaApwareMib.VDR_BASE_IFINDEX) / 100;
    }

    public boolean isLagRelation() {
        return ifIndex > ApresiaApwareMib.LAG_BASE_IFINDEX
                && ifIndex < ApresiaApwareMib.SUPER_LAG_BASE_IFINDEX;
    }

    public int getLagId() {
        return ifIndex - ApresiaApwareMib.LAG_BASE_IFINDEX;
    }

    public boolean isTagged() {
        return vlanType == 1 || vlanType == 3;
    }

    public boolean isUntagged() {
        return vlanType == 2;
    }

    public String toString() {
        return "vlanId:" + vlanId + " portIfIndex:" + ifIndex + " type:" + vlanType;
    }

}