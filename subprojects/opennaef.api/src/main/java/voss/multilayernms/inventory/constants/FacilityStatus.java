package voss.multilayernms.inventory.constants;

public enum FacilityStatus {
    UNKNOWN("UNKNOWN"),

    LOST("LOST"),

    RESERVED("RESERVED"),

    CONFIGURED("CONFIGURED"),

    TESTED("TESTED"),

    IN_OPERATION("IN OPERATION"),

    NOT_MONITOR("NOT MONITOR"),

    END_OPERATION("END OPERATION"),

    WAITING_FOR_REVOKE("WAITING FOR REVOKE"),

    REVOKED("REVOKED"),;

    private final String displayString;

    private FacilityStatus(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return this.displayString;
    }

    public static FacilityStatus getByDisplayString(String s) {
        for (FacilityStatus value : FacilityStatus.values()) {
            if (value.displayString.equals(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException(s);
    }
}