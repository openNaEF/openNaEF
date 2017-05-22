package voss.core.server.constant;

public enum DiffObjectType {
    NODE("Node"),
    CHASSIS("Chassis"),
    SLOT("Slot"),
    MODULE("Module"),
    LOOPBACK("Loopback"),
    PORT("Port"),
    VLAN_SUBIF("VLAN SubI/F"),
    ATM_VP("ATM VP"),
    ATM_PVC("ATM PVC SubI/F"),
    FR_PVC("FrameRelay PVC SubI/F"),
    SERIAL("Serial SubI/F"),
    RSVPLSP("RSVP-LSP"),
    RSVPLSP_PATH("RSVP-LSP Path"),
    PSEUDOWIRE("PseudoWire"),
    TAG_CHANGER("vport"),
    VLANPOOL("VLAN Pool"),
    VLAN("VLAN ID"),
    VLANIF("VLAN"),
    VLAN_LINK("VLAN Link"),
    VPLSPOOL("VPLS Pool"),
    VPLS("VPLS ID"),
    VPLSIF("VPLS"),
    VRFPOOL("VRF Pool"),
    VRF("VRF ID"),
    VRFIF("VRF"),
    L1_LINK("Link(L1)"),
    L2_LINK("Link(L2)"),
    L3_LINK("Link(L3)"),
    SUBNET("IP Subnet"),
    PIPE("Pipe"),

    ALIAS("VM Alias Port"),

    TASK("Task"),
    LOCATION("Location"),
    CUSTOMER_INFO("Customer-info"),

    IP("IP Address"),
    IP_SUBNET("IP Subnet"),
    IP_DELEGATION("IP Subnet Delegation"),

    NODE_GROUP("Node Group"),

    SYSTEM_USER("System User"),

    OTHER("Other"),;

    private final String caption;

    private DiffObjectType(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return this.caption;
    }
}