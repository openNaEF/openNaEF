package voss.multilayernms.inventory.renderer;

import naef.dto.InterconnectionIfDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;

import java.util.Date;
import java.util.Map;

public class PseudoWireRenderer extends GenericRenderer {

    public static String getPseudoWireID(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        if (pw.getLongId() != null) {
            return pw.getLongId().toString();
        } else {
            return pw.getStringId();
        }
    }

    public static String getPseudoWireName(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.PSEUDOWIRE_NAME);
    }

    public static String getPipeName(InterconnectionIfDto pipe) {
        if (pipe == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pipe, MPLSNMS_ATTR.IFNAME);
    }

    public static String getBandwidth(PseudowireDto pw) {
        return BandwidthFormat.format(getBandwidthAsLong(pw), 2);
    }

    public static Long getBandwidthAsLong(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        Long bandwidth = DtoUtil.getLong(pw, MPLSNMS_ATTR.CONTRACT_BANDWIDTH);
        return bandwidth;
    }

    public static String getRawBandwidth(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        Long bandwidth = DtoUtil.getLong(pw, MPLSNMS_ATTR.CONTRACT_BANDWIDTH);
        if (bandwidth == null) {
            return null;
        }
        return bandwidth.toString();
    }

    public static String getPseudoWireType(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.PSEUDOWIRE_TYPE);
    }

    public static String getServiceType(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.SERVICE_TYPE);
    }

    public static String getAccomodationServiceType(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE);
    }

    public static String getLineId(PseudowireDto pw) {
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.USERLINE_ID);
    }

    public static String getOpenedTime(PseudowireDto pw) {
        Date d = getOpenedTimeAsDate(pw);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static Date getOpenedTimeAsDate(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getDate(pw, MPLSNMS_ATTR.SETUP_DATE);
    }

    public static String getOperationBeginTime(PseudowireDto pw) {
        Date d = getOpenedTimeAsDate(pw);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static Date getOperationBeginTimeAsDate(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getDate(pw, MPLSNMS_ATTR.OPERATION_BEGIN_DATE);
    }

    public static String getServiceID(PseudowireDto pw) {
        if (pw == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(pw, MPLSNMS_ATTR.SERVICE_ID);
    }

    public static String getRouteDistinguisher(PortDto acOnEgress) {
        return "N/A";
    }
    public static String getPseudoWireID(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getPseudoWireID(pw.getPseudowireDto());
        }
        return pw.getPipe().getIfname();
    }

    public static String getPseudoWireName(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        }
        if (pw.isPseudoWire()) {
            return getPseudoWireName(pw.getPseudowireDto());
        }
        return pw.getPipe().getIfname();
    }

    public static String getBandwidth(FakePseudoWire pw) {
        return BandwidthFormat.format(getBandwidthAsLong(pw), 2);
    }

    public static Long getBandwidthAsLong(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPipe()) {
            return null;
        }
        return getBandwidthAsLong(pw.getPseudowireDto());
    }

    public static String getRawBandwidth(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getRawBandwidth(pw.getPseudowireDto());
        }
        Long bandwidth = DtoUtil.getLong(pw.getPipe(), MPLSNMS_ATTR.CONTRACT_BANDWIDTH);
        if (bandwidth == null) {
            return null;
        }
        return bandwidth.toString();
    }

    public static String getPseudoWireType(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getPseudoWireType(pw.getPseudowireDto());
        }
        return DtoUtil.getStringOrNull(pw.getPipe(), MPLSNMS_ATTR.PSEUDOWIRE_TYPE);
    }

    public static String getServiceType(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getServiceType(pw.getPseudowireDto());
        }
        return DtoUtil.getStringOrNull(pw.getPipe(), MPLSNMS_ATTR.SERVICE_TYPE);
    }

    public static String getAccomodationServiceType(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getAccomodationServiceType(pw.getPseudowireDto());
        }
        return DtoUtil.getStringOrNull(pw.getPipe(), MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE);
    }

    public static String getLineId(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getLineId(pw.getPseudowireDto());
        }
        return DtoUtil.getStringOrNull(pw.getPipe(), MPLSNMS_ATTR.USERLINE_ID);
    }

    public static String getOpenedTime(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getOpenedTime(pw.getPseudowireDto());
        }
        Date d = DtoUtil.getDate(pw.getPipe(), MPLSNMS_ATTR.SETUP_DATE);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static String getOperationBeginTime(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getOperationBeginTime(pw.getPseudowireDto());
        }
        Date d = DtoUtil.getDate(pw.getPipe(), MPLSNMS_ATTR.OPERATION_BEGIN_DATE);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static String getServiceID(FakePseudoWire pw) {
        if (pw == null) {
            return null;
        } else if (pw.isPseudoWire()) {
            return getServiceID(pw.getPseudowireDto());
        }
        return DtoUtil.getStringOrNull(pw.getPipe(), MPLSNMS_ATTR.SERVICE_ID);
    }

    public static String getFacilityStatus(FakePseudoWire pw) {
        if (pw.isPipe()) return getFacilityStatus(pw.getPipe());
        return getFacilityStatus(pw.getPseudowireDto());
    }

    public static String getOperStatus(FakePseudoWire pw) {
        if (pw.isPipe()) {
            return getOperStatus(pw.getPipe());
        } else {
            return getOperStatus(pw.getPseudowireDto());
        }
    }

    public static String getNote(FakePseudoWire pw) {
        if (pw.isPipe()) {
            return getNote(pw.getPipe());
        } else {
            return getNote(pw.getPseudowireDto());
        }
    }

    public static String getLastEditor(FakePseudoWire pw) {
        if (pw.isPipe()) {
            return getLastEditor(pw.getPipe());
        } else {
            return getLastEditor(pw.getPseudowireDto());
        }
    }

    public static String getLastEditTime(FakePseudoWire pw) {
        if (pw.isPipe()) {
            return getLastEditTime(pw.getPipe());
        } else {
            return getLastEditTime(pw.getPseudowireDto());
        }
    }

    public static String getCustomerName(PseudowireDto pw) {
        return DtoUtil.getStringOrNull(pw, CustomerConstants.CUSTOMER_NAME);
    }

    public static String getApName1(PseudowireDto pw) {
        return DtoUtil.getStringOrNull(pw, CustomerConstants.AP_NAME1);
    }

    public static String getApName2(PseudowireDto pw) {
        return DtoUtil.getStringOrNull(pw, CustomerConstants.AP_NAME2);
    }

    public static Map<String, String> getValues(PseudowireDto pw) {
        return DtoUtil.getValues(pw);
    }
}