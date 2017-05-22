package voss.multilayernms.inventory.renderer;

import naef.dto.LocationDto;
import voss.core.server.constant.ModelConstant;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class LocationRenderer {

    public static String getName(LocationDto location) {
        checkArg(location);
        return DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.CAPTION);
    }

    public static String getLocationType(LocationDto location) {
        checkArg(location);
        return DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.LOCATION_TYPE);
    }

    public static String getPopName(LocationDto location) {
        checkArg(location);
        return DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.POP_NAME);
    }

    public static String getBuildingCode(LocationDto location) {
        checkArg(location);
        return DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.BUILDING_CODE);
    }

    public static String getSortOrder(LocationDto location) {
        checkArg(location);
        return DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.SORT_ORDER);
    }

    public static boolean isVisible(LocationDto location) {
        checkArg(location);
        Boolean isVisible = (Boolean) location.getValue(MPLSNMS_ATTR.VISIBLE_FLAG);
        if (isVisible == null) {
            return true;
        }
        return isVisible;
    }

    private static void checkArg(LocationDto location) {
        if (location == null) {
            throw new IllegalStateException("location is null.");
        }
    }

    public static boolean isTrash(LocationDto location) {
        checkArg(location);
        if (ModelConstant.LOCATION_TRASH_NAME.equals(location.getName())) {
            return true;
        }
        return false;
    }
}