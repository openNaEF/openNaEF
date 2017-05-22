package voss.multilayernms.inventory.constants;

public enum PseudoWireType {
    EPIPE("epipe"),
    CPIPE("cpipe"),
    APIPE("apipe"),;
    private final String typeName;

    private PseudoWireType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return this.typeName;
    }
}