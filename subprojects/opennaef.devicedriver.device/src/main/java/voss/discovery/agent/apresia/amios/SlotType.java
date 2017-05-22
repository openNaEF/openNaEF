package voss.discovery.agent.apresia.amios;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;

@SuppressWarnings("serial")
final class SlotType extends IntSnmpEntry {

    private final int index;
    private final int mibIndex;
    private final int type;

    public SlotType(String oid, VarBind varbind) {
        super(oid, varbind);
        int suffix = getOIDSuffixLast();
        this.mibIndex = suffix;
        this.index = (suffix % 10000) / 100;
        this.type = intValue();
    }

    public int getSlotIndex() {
        return index;
    }

    public int getMibIndex() {
        return this.mibIndex;
    }

    public String getSlotName() {
        return getSlotName(index);
    }

    public static boolean isManagementInterface(int slotIndex) {
        return 20000 < slotIndex;
    }

    public static String getManagementInterfaceNamePrefix() {
        return "Manage";
    }

    public static String getSlotName(int slotIndex) {
        if (isManagementInterface(slotIndex)) {
            slotIndex = slotIndex - 20000;
            slotIndex = slotIndex / 100;
            return getManagementInterfaceNamePrefix() + " (" + slotIndex + ")";
        } else {
            slotIndex = slotIndex % 10000;
            slotIndex = slotIndex / 100;
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

            case 33:
            case 34:
            case 35:
            case 36:
                return null;
            case 37:
                return "ap18U-MM1";
            case 38:
                return "ap18U-ARB1";
            case 39:
                return "ap18U-SFC1";
            case 40:
                return "ap18U-XG18002c";

            case 101:
            case 102:
            case 103:
                return null;
            case 104:
                return "a16U-MM1";
            case 105:
                return "a16U3-MM1";
            case 106:
                return "a16L-XG16001c";
            case 107:
                return "a16L-G16010c";
            case 112:
                return "a16U-MM1-XL";
            case 113:
                return "a16L-XG16104c";
            case 114:
                return "a16L-XG16104c-XL";
            case 301:
            case 302:
            case 305:
                return null;
            case 303:
                return "a26U-MM1";
            case 304:
                return "a26L-XG26012c";
            case 307:
                return "a26U-SFC1";

            default:
                return "unknown(" + type + ")";
        }
    }
}