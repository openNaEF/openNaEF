package voss.multilayernms.inventory.renderer;

import naef.dto.NaefDto;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class GenericRenderer {

    public static String getOperStatus(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        return DtoUtil.getString(dto, MPLSNMS_ATTR.OPER_STATUS);
    }

    public static String getAdminStatus(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        return DtoUtil.getString(dto, MPLSNMS_ATTR.ADMIN_STATUS);
    }

    public static String getFacilityStatus(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(dto, MPLSNMS_ATTR.FACILITY_STATUS);
    }

    public static FacilityStatus getFacilityStatusValue(NaefDto dto) {
        String fs = getFacilityStatus(dto);
        if (fs == null) {
            return null;
        } else {
            return FacilityStatus.getByDisplayString(fs);
        }
    }

    public static String getNote(NaefDto dto) {
        return DtoUtil.getStringOrNull(dto, MPLSNMS_ATTR.NOTE);
    }

    public static String getPurpose(NaefDto dto) {
        return DtoUtil.getStringOrNull(dto, MPLSNMS_ATTR.PURPOSE);
    }

    public static String getSource(NaefDto dto) {
        return DtoUtil.getStringOrNull(dto, MPLSNMS_ATTR.SOURCE);
    }

    public static String getLastEditor(NaefDto dto) {
        return DtoUtil.getStringOrNull(dto, ATTR.LAST_EDITOR);
    }

    public static String getLastEditTime(NaefDto dto) {
        Date d = DtoUtil.getDate(dto, ATTR.LAST_EDIT_TIME);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static String getVersion(NaefDto dto) {
        return DtoUtil.getMvoVersionString(dto);
    }

    public static String getRegisteredDate(NaefDto dto) {
        Date d = DtoUtil.getDate(dto, MPLSNMS_ATTR.REGISTERED_DATE);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static SimpleDateFormat getMvoDateFormat() {
        return DtoUtil.getMvoDateFormat();
    }

    protected static DateFormat getDateFormatter() {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return df;
    }
}