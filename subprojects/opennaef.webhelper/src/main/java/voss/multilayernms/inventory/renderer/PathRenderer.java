package voss.multilayernms.inventory.renderer;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;
import voss.nms.inventory.util.RsvpLspUtil;

public class PathRenderer extends GenericRenderer {

    public static String getName(RsvpLspHopSeriesDto path) {
        if (path == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(path, MPLSNMS_ATTR.PATH_NAME);
    }

    public static String getIngressNodeName(RsvpLspHopSeriesDto path) {
        NodeDto ingress = RsvpLspUtil.getIngressNode(path);
        if (ingress != null) {
            return ingress.getName();
        }
        return null;
    }

    public static String getEgressNodeName(RsvpLspHopSeriesDto path) {
        NodeDto egress = RsvpLspUtil.getEgressNode(path);
        if (egress != null) {
            return egress.getName();
        }
        return null;
    }

    public static String getBandwidth(RsvpLspHopSeriesDto path) {
        return BandwidthFormat.format(getBandwidthAsLong(path), 2);
    }

    public static Long getBandwidthAsLong(RsvpLspHopSeriesDto path) {
        Long bandwidth = DtoUtil.getLong(path, MPLSNMS_ATTR.BANDWIDTH);
        return bandwidth;
    }

    public static String getBandwidthForHPSA(RsvpLspHopSeriesDto path) {
        Long bandwidth = getBandwidthAsLong(path);
        if (bandwidth == null) {
            return null;
        }
        long bandwidthInMega = bandwidth.longValue() / BandwidthFormat.MEGA;
        return String.valueOf(bandwidthInMega);
    }
}