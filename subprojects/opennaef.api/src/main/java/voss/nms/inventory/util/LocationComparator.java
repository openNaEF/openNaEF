package voss.nms.inventory.util;

import naef.dto.LocationDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.Serializable;
import java.util.Comparator;

public class LocationComparator implements Comparator<LocationDto>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(LocationDto o1, LocationDto o2) {
        String s1 = DtoUtil.getStringOrNull(o1, "ソート順");
        if (s1 == null) {
            s1 = DtoUtil.getString(o1, MPLSNMS_ATTR.CAPTION);
        }
        String s2 = DtoUtil.getStringOrNull(o2, "ソート順");
        if (s2 == null) {
            s2 = DtoUtil.getString(o2, MPLSNMS_ATTR.CAPTION);
        }
        return s1.compareTo(s2);
    }
}