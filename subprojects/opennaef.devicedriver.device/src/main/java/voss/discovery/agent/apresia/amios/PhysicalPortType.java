package voss.discovery.agent.apresia.amios;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;


@SuppressWarnings("serial")
class PhysicalPortType extends IntSnmpEntry {

    private int ifIndex;
    private int type;

    public PhysicalPortType(String oid, VarBind varbind) {
        super(oid, varbind);
    }

    public void setupValue() throws UnexpectedVarBindException {
        if (oidSuffix.length != 2) {
            throw new UnexpectedVarBindException(varbind);
        }
        this.ifIndex = oidSuffix[oidSuffix.length - 2].intValue();
        this.type = intValue();
    }

    public int getIfIndex() {
        return ifIndex;
    }

    public int getPortIndex() {
        return getIfIndex() % 100;
    }

    public int getSlotIndex() {
        return (getIfIndex() / 100) * 100;
    }

    public String getIfNameWithSlotIndex() {
        return SlotType.getSlotName(getSlotIndex()) + "/" + getPortIndex();
    }

    public String getIfNameWithoutSlotIndex() {
        if (SlotType.isManagementInterface(getSlotIndex())) {
            return SlotType.getManagementInterfaceNamePrefix() + " " + Integer.toString(getPortIndex());
        } else {
            return Integer.toString(getPortIndex());
        }
    }

    public String getPortType() {
        switch (type) {
            case 1:
                return "other";
            case 2:
                return "invalid";
            case 101:
                return "utp";
            case 201:
                return "sfp-NONE";
            case 202:
                return "sfp-UNKNOWN";
            case 203:
                return "sfp-SX";
            case 204:
                return "sfp-LX";
            case 205:
                return "sfp-ZX";
            case 206:
                return "sfp-T";
            case 215:
                return "sfp-LX40";
            case 218:
                return "sfp-BX20-U";
            case 219:
                return "sfp-BX20-D";
            case 220:
                return "sfp-BX40-U";
            case 221:
                return "sfp-BX40-D";
            case 222:
                return "sfp-LX80";
            case 301:
                return "xenpak-NONE";
            case 302:
                return "xenpak-UNKNOWN";
            case 303:
                return "xenpak-SR";
            case 304:
                return "xenpak-LR";
            case 305:
                return "xenpak-ER";
            case 306:
                return "xenpak-LX)";
            case 307:
                return "xenpak-SW";
            case 308:
                return "xenpak-LW";
            case 309:
                return "xenpak-EW";
            case 601:
                return "sfpPlus-NONE";
            case 602:
                return "sfpPlus-unknown";
            case 603:
                return "sfpPlus-LRM";
            case 604:
                return "sfpPlus-ER";
            case 605:
                return "sfpPlus-LR";
            case 606:
                return "sfpPlus-SR";
            default:
                return "unknown(" + type + ")";
        }
    }
}