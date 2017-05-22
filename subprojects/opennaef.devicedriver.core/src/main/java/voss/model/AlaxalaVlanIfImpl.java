package voss.model;


public class AlaxalaVlanIfImpl extends VlanIfImpl {
    private static final long serialVersionUID = 1L;
    public static final String UNKNOWN_TYPE_VLAN = "UNKNOWN";
    public static final String IEEE802_1Q_VLAN = "Port (802.1q) VLAN";
    public static final String MAC_VLAN = "MAC VLAN";
    public static final String PROTOCOL_VLAN = "Protocol VLAN";
    public static final String[] acceptableVlanTypes =
            {UNKNOWN_TYPE_VLAN, IEEE802_1Q_VLAN, MAC_VLAN, PROTOCOL_VLAN};

    private String vlanType;

    public void setVlanType(String _type) {
        if (!isVlanTypeAcceptable(_type)) {
            throw new IllegalArgumentException("invalid vlan type: " + _type);
        }
        this.vlanType = _type;
    }

    public String getVlanType() {
        return this.vlanType;
    }

    private boolean isVlanTypeAcceptable(String _type) {
        if (_type == null) {
            return false;
        }
        for (int i = 0; i < acceptableVlanTypes.length; i++) {
            if (acceptableVlanTypes[i].equals(_type)) {
                return true;
            }
        }
        return false;
    }

}