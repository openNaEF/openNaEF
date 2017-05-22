package voss.multilayernms.inventory.database;

import java.util.ArrayList;
import java.util.List;

public enum LocationType {
    ROOT("root"),
    AREA("Area"),
    COUNTRY("Country"),
    CITY("City"),
    BUILDING("Building"),
    FLOOR("Floor"),
    RACK("Rack"),
    TRASH("Trash"),;

    private String caption;

    private LocationType(String s) {
        this.caption = s;
    }

    public String getCaption() {
        return this.caption;
    }

    public static LocationType getByCaption(String s) {
        if (s == null) {
            return null;
        }
        for (LocationType value : values()) {
            if (value.caption.equals(s)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> getNames() {
        List<String> result = new ArrayList<String>();
        for (LocationType type : values()) {
            result.add(type.caption);
        }
        return result;
    }
}