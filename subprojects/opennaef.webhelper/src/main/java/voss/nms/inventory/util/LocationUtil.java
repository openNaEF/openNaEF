package voss.nms.inventory.util;

import naef.dto.LocationDto;
import org.apache.wicket.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;

public class LocationUtil extends voss.core.server.util.LocationUtil {
    public static final String KEY_LOCATION_ID = "loc";
    public static final String KEY_TOP_CAPTION = "Location";

    @SuppressWarnings("unused")
    private static Logger log() {
        return LoggerFactory.getLogger(LocationUtil.class);
    }

    public static int getInt(PageParameters param, String keyName) {
        Object o = param.getAsInteger(keyName);
        if (o == null) {
            throw new IllegalArgumentException(keyName + " is null.");
        }
        if (o.getClass().isArray()) {
            Object[] arr = (Object[]) o;
            if (arr.length == 0) {
                throw new IllegalArgumentException(keyName + " is array, but has no value.");
            }
            o = arr[0];
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else if (o instanceof String) {
            return Integer.parseInt((String) o);
        } else {
            return Integer.parseInt(o.toString());
        }
    }

    public static PageParameters getParameters(LocationDto location, String sub1, String sub2, String sub3) {
        PageParameters param = new PageParameters();
        param.add(KEY_LOCATION_ID, DtoUtil.getMvoId(location).toString());
        return param;
    }

    public static PageParameters getParameters(LocationDto loc) {
        if (loc == null) {
            return new PageParameters();
        }
        PageParameters param1 = new PageParameters();
        param1.add(KEY_LOCATION_ID, DtoUtil.getMvoId(loc).toString());
        return param1;
    }
}