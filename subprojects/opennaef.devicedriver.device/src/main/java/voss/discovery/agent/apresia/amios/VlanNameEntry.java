package voss.discovery.agent.apresia.amios;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;


@SuppressWarnings("serial")
class VlanNameEntry extends StringSnmpEntry {

    private int eoeID;
    private int vlanID;
    private String vlanName;

    public VlanNameEntry(String rootOidString, VarBind varbind) {
        super(rootOidString, varbind);
    }

    public void setup() throws UnexpectedVarBindException {
        if (oidSuffix.length != 2) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.eoeID = oidSuffix[0].intValue();
        this.vlanID = oidSuffix[1].intValue();
        this.vlanName = getValue();
    }

    public int getEoeID() {
        return this.eoeID;
    }

    public int getVlanID() {
        return this.vlanID;
    }

    public int getVlanIfIndex() {
        return this.vlanID + ApresiaAmiosMib.VLAN_BASE_IFINDEX + this.eoeID * ApresiaAmiosMib.EOE_MULTIPLIER;
    }

    public String toString() {
        return "vlan id: " + this.eoeID + "." + this.vlanID + " name:" + vlanName;
    }

}