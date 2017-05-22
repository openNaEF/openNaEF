package voss.discovery.agent.apresia.amios;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;

@SuppressWarnings("serial")
class IntVlanPortModeEntry extends IntSnmpEntry {
    public static final int UNKNOWN = 0;
    public static final int EOE = 1;
    public static final int TAGGED = 2;
    public static final int UNTAGGED = 3;
    public static final int B_TAGGED = 7;

    private int cardIndex;
    private int portIndex;
    private int eoeID;
    private int vlanID;
    private int portMode;

    public IntVlanPortModeEntry(String rootOidString, VarBind varbind) {
        super(rootOidString, varbind);
    }

    public void setup() throws UnexpectedVarBindException {
        if (oidSuffix.length != 4) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.cardIndex = oidSuffix[0].intValue();
        this.portIndex = oidSuffix[1].intValue();
        this.eoeID = oidSuffix[2].intValue();
        this.vlanID = oidSuffix[3].intValue();
        this.portMode = getValueAsBigInteger().intValue();
    }

    public int getCardIndex() {
        return this.cardIndex;
    }

    public int getPortIndex() {
        return this.portIndex;
    }

    public int getPortIfIndex() {
        return this.cardIndex + this.portIndex;
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
        return "hclIntVlanPortMode: " + this.cardIndex + "/" + this.portIndex + "-" + this.eoeID + "." + this.vlanID + "="
                + this.portMode;
    }

    public boolean isBindAsTagged() {
        switch (this.portMode) {
            case B_TAGGED:
            case TAGGED:
                return true;
        }
        return false;
    }

    public boolean isBindAsUntagged() {
        switch (this.portMode) {
            case UNTAGGED:
                return true;
        }
        return false;
    }
}