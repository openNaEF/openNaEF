package voss.multilayernms.inventory.constants;

public enum PortFacilityStatus {
    RESERVED("RESERVED"),
    CONFIGURED("CONFIGURED"),
    STANDBY("STANDBY"),
    IN_OPERATION("IN OPERATION"),
    NON_MONITORING("NON MONITORING"),
    END_OF_SERVICE("END OF SERVICE"),
    PENDING_DELETE("PENDING DELETE"),
    REVOKED("REVOKED"),
    LOST("LOST"),
    UNKNOWN("UNKNOWN"),;

    private final String displayString;

    private PortFacilityStatus(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return this.displayString;
    }

    public static PortFacilityStatus getByDisplayString(String s) {
        for (PortFacilityStatus value : PortFacilityStatus.values()) {
            if (value.displayString.equals(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException(s);
    }
}