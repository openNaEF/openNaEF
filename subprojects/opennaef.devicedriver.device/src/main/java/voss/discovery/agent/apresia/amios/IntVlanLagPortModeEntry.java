package voss.discovery.agent.apresia.amios;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;

@SuppressWarnings("serial")
class IntVlanLagPortModeEntry extends IntSnmpEntry {
    public static final int UNKNOWN = 0;
    public static final int EOE = 1;
    public static final int TAGGED = 2;
    public static final int UNTAGGED = 3;
    public static final int FORCE_EOE = 4;
    public static final int FORCE_TAGGED = 5;
    public static final int FORCE_UNTAGGED = 6;
    public static final int B_TAGGED = 7;
    public static final int FORCE_B_TAGGED = 8;

    private int lagIndex;
    private int eoeID;
    private int vlanID;
    private int lagPortMode;

    public IntVlanLagPortModeEntry(String rootOidString, VarBind varbind) {
        super(rootOidString, varbind);
    }

    public void setup() throws UnexpectedVarBindException {
        if (this.oidSuffix.length != 3) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.lagIndex = oidSuffix[0].intValue();
        this.eoeID = oidSuffix[1].intValue();
        this.vlanID = oidSuffix[2].intValue();
        this.lagPortMode = getValueAsBigInteger().intValue();
    }

    public int getLagIndex() {
        return this.lagIndex;
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
        return "hclVlanLagPortMode: LAG[" + this.lagIndex + "]/VLAN[" + this.eoeID + "." + this.vlanID + "=" + this.lagPortMode;
    }

    public boolean isBindAsUntagged() {
        switch (this.lagPortMode) {
            case UNTAGGED:
            case FORCE_UNTAGGED:
                return true;
        }
        return false;
    }

    public boolean isBindAsTagged() {
        switch (this.lagPortMode) {
            case EOE:
            case TAGGED:
            case FORCE_EOE:
            case FORCE_TAGGED:
                return true;
        }
        return false;
    }

    public boolean isBindAsPbb() {
        switch (this.lagPortMode) {
            case B_TAGGED:
            case FORCE_B_TAGGED:
                return true;
        }
        return false;
    }
}