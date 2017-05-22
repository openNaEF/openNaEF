package voss.nms.inventory.util;

import voss.core.server.util.*;

public class Comparators {
    public static DtoComparator getDtoComparator() {
        return new DtoComparator();
    }

    public static NodeElementComparator getNodeElementComparator() {
        return new NodeElementComparator();
    }

    public static LocationComparator getLocationComparator() {
        return new LocationComparator();
    }

    public static TypeBasedPortComparator getTypeBasedPortComparator() {
        return new TypeBasedPortComparator();
    }

    public static IfNameBasedPortComparator getIfNameBasedPortComparator() {
        return new IfNameBasedPortComparator();
    }

    public static IfNameComparator getIfNameComparator() {
        return new IfNameComparator();
    }
}