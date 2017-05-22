package voss.discovery.agent.apresia.apware;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;

@SuppressWarnings("serial")
final class SlotType extends IntSnmpEntry {

    private final int index;
    private final int type;

    public SlotType(String oid, VarBind varbind) {
        super(oid, varbind);
        this.index = getOIDSuffixLast();
        this.type = intValue();
    }

    public int getSlotIndex() {
        return index;
    }

    public String getSlotName() {
        return getSlotName(index);
    }

    public static boolean isManagementInterface(int slotIndex) {
        return 100 < slotIndex;
    }

    public static String getManagementInterfaceNamePrefix() {
        return "Manage";
    }

    public static String getSlotName(int slotIndex) {
        if (isManagementInterface(slotIndex)) {
            return getManagementInterfaceNamePrefix() + " (" + (slotIndex % 100) + ")";
        } else {
            return Integer.toString(slotIndex);
        }
    }

    public String getModuleName() {
        switch (type) {
            case 1:
                return "unknown(1)";
            case 2:
            case 3:
                return null;
            case 21:
                return "a8U-MM1(21)";
            case 22:
                return "a8L-XG8001c(22)";
            case 23:
                return "a8L-G8010c(23)";
            case 24:
                return "a8L-G8012Tc(24)";
            case 25:
                return "a8L-FE8048c(25)";
            case 26:
                return "a8L-FE8148c(26)";
            case 27:
                return "a8L-G8110c(27)";
            case 28:
                return "a8L-G8112c(28)";
            case 29:
                return "a8L-XG8102c(29)";
            case 30:
                return "a8L-G8210c(30)";
            case 31:
                return "a8L-G8212c(31)";
            case 32:
                return "a8L-XG8202c(32)";
            case 43:
                return "a8L-G8312c(43)";

            case 37:
                return "ARB";
            case 38:
                return "MM";
            case 39:
                return "SWF";
            case 40:
                return "XG18002c";

            default:
                return "unknown(" + type + ")";
        }
    }
}