package naef.mvo.vlan;

public enum VlanType {

    DOT1Q(".1q") {

        @Override public boolean isIdInRange(int vlanid) {
            return 1 <= vlanid && vlanid <= 4094;
        }
    };

    private final String typeName_;

    VlanType(String typeName) {
        typeName_ = typeName;
    }

    public String getTypeName() {
        return typeName_;
    }

    abstract public boolean isIdInRange(int vlanid);
}
