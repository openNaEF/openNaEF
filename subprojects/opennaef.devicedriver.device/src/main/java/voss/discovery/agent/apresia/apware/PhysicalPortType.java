package voss.discovery.agent.apresia.apware;

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
        return (getIfIndex() - 10000) / 100;
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
                return "other(1)";
            case 2:
                return "invalid(2)";
            case 101:
                return "utp(101)";
            case 201:
                return "sfp-NONE(201)";
            case 202:
                return "sfp-UNKNOWN(202)";
            case 203:
                return "sfp-SX(203)";
            case 204:
                return "sfp-LX(204)";
            case 205:
                return "sfp-ZX(205)";
            case 206:
                return "sfp-T(206)";
            case 215:
                return "sfp-LX40(215)";
            case 301:
                return "xenpak-NONE(301)";
            case 302:
                return "xenpak-UNKNOWN(302)";
            case 303:
                return "xenpak-SR(303)";
            case 304:
                return "xenpak-LR(304)";
            case 305:
                return "xenpak-ER(305)";
            case 306:
                return "xenpak-LX4(306)";
            case 307:
                return "xenpak-SW(307)";
            case 308:
                return "xenpak-LW(308)";
            case 309:
                return "xenpak-EW(309)";
            default:
                return "unknown(" + type + ")";
        }
    }

}