package voss.multilayernms.inventory.constants;

public enum LinkFacilityStatus {
    IN_USE("IN USE"),

    RESERVED("RESERVED"),

    UNAPPROVED("UNAPPROVED"),;

    private final String displayString;

    private LinkFacilityStatus(String s) {
        this.displayString = s;
    }

    public String getDisplayString() {
        return this.displayString;
    }

    public static LinkFacilityStatus getByDisplayString(String s) {
        for (LinkFacilityStatus st : LinkFacilityStatus.values()) {
            if (st.displayString.equals(s)) {
                return st;
            }
        }
        return null;
    }
}