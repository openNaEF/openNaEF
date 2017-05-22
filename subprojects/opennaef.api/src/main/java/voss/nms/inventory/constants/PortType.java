package voss.nms.inventory.constants;

import voss.core.server.database.ATTR;

import java.util.ArrayList;
import java.util.List;

public enum PortType {
    ETHERNET("Ethernet", ATTR.TYPE_ETH_PORT, false),
    ATM("ATM", ATTR.TYPE_ATM_PORT, false),
    ATM_APS("ATM APS", ATTR.TYPE_ATM_APS, false),
    SERIAL("Serial", ATTR.TYPE_SERIAL_PORT, false),
    POS("POS", ATTR.TYPE_POS_PORT, false),
    POS_APS("POS APS", ATTR.TYPE_POS_APS_PORT, false),
    ISDN("ISDN", ATTR.TYPE_ISDN_PORT, false),
    WDM("WDM", ATTR.TYPE_WDM_PORT, false),
    LOOPBACK("loopback", ATTR.TYPE_IP_PORT, true),
    NODEPIPE("pipe", ATTR.TYPE_INTERCONNECTION_IF, true),
    TUNNEL("tunnel", ATTR.TYPE_IP_PORT, true),
    LAG("EthernetLAG", ATTR.TYPE_LAG_PORT, false),
    EPS("EthernetEPS", ATTR.TYPE_LAG_PORT, false),
    VLANIF("VLAN", ATTR.TYPE_VLAN_IF, false),
    VRFIF("VRF", ATTR.TYPE_VRF_IF, false),
    VPLSIF("VPLS", ATTR.TYPE_VPLS_IF, false),
    ATMPVP("ATM PVP", ATTR.TYPE_ATM_PVP_IF, false),
    ATMPVC("ATM PVC", ATTR.TYPE_ATM_PVC_IF, false),
    FRPVC("FR PVC", ATTR.TYPE_FR_PVC_IF, false),
    ISDNCHANNEL("ISDN Ch.", ATTR.TYPE_ISDN_CHANNEL_IF, false),
    SDHCHANNEL("Channel", ATTR.TYPE_TDM_SERIAL_PORT, false),
    VM_ETHERNET("Virutal NIC", ATTR.TYPE_ETH_PORT, false),;
    private final boolean creatable;
    private final String caption;
    private final String type;

    private PortType(String caption, String type, boolean creatable) {
        this.caption = caption;
        this.type = type;
        this.creatable = creatable;
    }

    public String getCaption() {
        return this.caption;
    }

    public String getType() {
        return this.type;
    }

    public boolean isCreatable() {
        return this.creatable;
    }

    public static PortType getByType(String type) {
        for (PortType value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }

    public static List<PortType> getUserSelectableTypes() {
        List<PortType> result = new ArrayList<PortType>();
        for (PortType value : values()) {
            if (value.creatable) {
                result.add(value);
            }
        }
        return result;
    }
}