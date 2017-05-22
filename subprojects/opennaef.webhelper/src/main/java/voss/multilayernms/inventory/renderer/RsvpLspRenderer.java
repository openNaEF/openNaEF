package voss.multilayernms.inventory.renderer;

import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import tef.MVO.MvoId;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.EXT_ATTR;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.RsvpLspUtil;

import java.util.*;

public class RsvpLspRenderer extends GenericRenderer {

    public static String getLspName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(lsp, MPLSNMS_ATTR.LSP_NAME);
    }

    public static String getIngressNodeName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
        if (ingress != null) {
            return ingress.getName();
        }
        return null;
    }

    public static String getIngressNodeIpAddress(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
        if (ingress != null) {
            return NodeRenderer.getManagementIpAddress(ingress);
        }
        return null;
    }

    public static String getEgressNodeName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        NodeDto egress = RsvpLspUtil.getEgressNode(lsp);
        if (egress != null) {
            return egress.getName();
        }
        return null;
    }

    public static String getEgressNodeIpAddress(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        NodeDto egress = RsvpLspUtil.getEgressNode(lsp);
        if (egress != null) {
            return NodeRenderer.getManagementIpAddress(egress);
        }
        return null;
    }

    public static String getSdpId(RsvpLspDto lsp) {
        String sdpID = DtoUtil.getStringOrNull(lsp, MPLSNMS_ATTR.LSP_SDP_ID);
        if (sdpID == null) {
            return "100";
        }
        return sdpID;
    }

    public static String getActivePathName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        String activePathName = DtoUtil.getStringOrNull(lsp, EXT_ATTR.LSP_ACTIVE_PATH_NAME);
        if (activePathName != null) {
            return activePathName;
        }
        RsvpLspHopSeriesDto path = lsp.getActiveHopSeries();
        if (path == null) {
            return null;
        }
        return PathRenderer.getName(path);
    }

    public static String getOppositLspName(RsvpLspDto lsp) {
        return getLspName(RsvpLspExtUtil.getOppositLsp(lsp));
    }

    public static String getCESAggregatedBandwidth(RsvpLspDto lsp) {
        return BandwidthFormat.format(getCESAggregatedBandwidthAsLong(lsp), 2);
    }

    public static Long getCESAggregatedBandwidthAsLong(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        RsvpLspRendererCache cache = RsvpLspRendererCache.getCache();
        CacheUnit unit = cache.getCacheUnit(lsp);
        return Long.valueOf(unit.getCesAggregated());
    }

    public static String getEtherVPNAggregatedBandwidth(RsvpLspDto lsp) {
        return BandwidthFormat.format(getEtherVPNAggregatedBandwidthAsLong(lsp), 2);
    }

    public static Long getEtherVPNAggregatedBandwidthAsLong(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        RsvpLspRendererCache cache = RsvpLspRendererCache.getCache();
        CacheUnit unit = cache.getCacheUnit(lsp);
        return Long.valueOf(unit.getEtherVpnAggregated());
    }

    public static List<PseudowireDto> getPseudoWiresOn(RsvpLspDto lsp) {
        List<PseudowireDto> result = new ArrayList<PseudowireDto>();
        if (lsp == null) {
            return result;
        }
        for (PseudowireDto pw : lsp.getPseudowires()) {
            result.add(pw);
        }
        return result;
    }

    public static String getMainPathName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return PathRenderer.getName(lsp.getHopSeries1());
    }

    public static String getBackupPathName(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return PathRenderer.getName(lsp.getHopSeries2());
    }

    public static String getMainPathOperationStatus(RsvpLspDto lsp) {
        if (lsp == null) {
            return "";
        }
        return DtoUtil.getString(lsp, MPLSNMS_ATTR.LSP_PRIMARY_PATH_OPER_STATUS);
    }

    public static String getBackupPathOperationStatus(RsvpLspDto lsp) {
        if (lsp == null) {
            return "";
        }
        return DtoUtil.getString(lsp, MPLSNMS_ATTR.LSP_SECONDARY_PATH_OPER_STATUS);
    }

    public static String getLspId(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return lsp.getName();
    }

    public static String getTunnelId(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(lsp, MPLSNMS_ATTR.LSP_TUNNEL_ID);
    }

    public static List<Integer> getTermNumberList(RsvpLspDto lsp) {
        List<Integer> result = new ArrayList<Integer>();
        if (lsp == null) {
            return result;
        }
        String termDef = DtoUtil.getString(lsp, MPLSNMS_ATTR.LSP_TERM);
        for (String s : termDef.split(":")) {
            if (s == null || s.length() == 0) {
                continue;
            }
            try {
                Integer i = Integer.valueOf(s);
                result.add(i);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public static String getTermNumber(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer i : getTermNumberList(lsp)) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(i.intValue());
        }
        return sb.toString();
    }

    public static boolean hasTermNumber(RsvpLspDto lsp, int num) {
        List<Integer> list = getTermNumberList(lsp);
        return list.contains(Integer.valueOf(num));
    }

    public static String getBandwidth(RsvpLspDto lsp) {
        Long value = getLspBandwidth(lsp);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static Long getLspBandwidth(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        Long value = DtoUtil.getLong(lsp, MPLSNMS_ATTR.BANDWIDTH);
        if (value != null) {
            return value;
        }
        if (lsp.getHopSeries1() != null) {
            value = getPathBandwidthAsLong(lsp.getHopSeries1());
        }
        return value;
    }

    public static String getPathBandwidth(RsvpLspHopSeriesDto path) {
        Long value = getPathBandwidthAsLong(path);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static Long getPathBandwidthAsLong(RsvpLspHopSeriesDto path) {
        if (path == null) {
            return null;
        }
        Long value = DtoUtil.getLong(path, MPLSNMS_ATTR.BANDWIDTH);
        return value;
    }

    public static List<HopUnit> getPrimaryHops(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto hopSeries = lsp.getHopSeries1();
        return getHopUnit(hopSeries);
    }

    private static List<HopUnit> getHopUnit(RsvpLspHopSeriesDto hopSeries) {
        List<HopUnit> result = new ArrayList<HopUnit>();
        if (hopSeries == null) {
            return result;
        }
        int hopcount = 1;
        for (PathHopDto hop : hopSeries.getHops()) {
            PortDto src = NodeUtil.getAssociatedPort(hop.getSrcPort());
            PortDto dst = NodeUtil.getAssociatedPort(hop.getDstPort());
            HopUnit unit = new HopUnit(hopcount,
                    PortRenderer.getNodeIfName(src),
                    PortRenderer.getIpAddress(src),
                    PortRenderer.getNodeIfName(dst),
                    PortRenderer.getIpAddress(dst)
            );
            result.add(unit);
            hopcount++;
        }
        return result;
    }

    public static List<HopUnit> getSecondaryHops(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto hopSeries = lsp.getHopSeries2();
        return getHopUnit(hopSeries);
    }

    public static String getOpenTime(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        Date d = DtoUtil.getDate(lsp, MPLSNMS_ATTR.SETUP_DATE);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static String getOperationBeginTime(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        Date d = DtoUtil.getDate(lsp, MPLSNMS_ATTR.OPERATION_BEGIN_DATE);
        if (d == null) {
            return null;
        }
        return getDateFormatter().format(d);
    }

    public static String getSuspended(RsvpLspDto lsp) {
        boolean aborted = DtoUtil.getBoolean(lsp, MPLSNMS_ATTR.ABORTED);
        return (aborted ? "Aborted" : null);
    }

    public static String getServiceType(RsvpLspDto lsp) {
        if (lsp == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(lsp, MPLSNMS_ATTR.SERVICE_TYPE);
    }

    public static String getMainPathBandwidth(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto path = lsp.getHopSeries1();
        if (path == null) {
            return null;
        }
        Long bandwidth = getPathBandwidthAsLong(path);
        return BandwidthFormat.format(bandwidth, 2);
    }

    public static String getBackupPathBandwidth(RsvpLspDto lsp) {
        RsvpLspHopSeriesDto path = lsp.getHopSeries2();
        if (path == null) {
            return null;
        }
        Long bandwidth = getPathBandwidthAsLong(path);
        return BandwidthFormat.format(bandwidth, 2);
    }

    public static String getPreviousMainPathBandwidth(RsvpLspDto lsp) {
        RsvpLspDto preProvisioningLsp = RsvpLspExtUtil.getPreProvisioningLsp(lsp);
        return preProvisioningLsp == null
                ? null
                : getMainPathBandwidth(preProvisioningLsp);
    }

    public static String getPreviousBackupPathBandwidth(RsvpLspDto lsp) {
        RsvpLspDto preProvisioningLsp = RsvpLspExtUtil.getPreProvisioningLsp(lsp);
        return preProvisioningLsp == null
                ? null
                : getBackupPathBandwidth(preProvisioningLsp);
    }

    public static List<HopUnit> getPreviousPrimaryHops(RsvpLspDto lsp) {
        RsvpLspDto preProvisioningLsp = RsvpLspExtUtil.getPreProvisioningLsp(lsp);
        return preProvisioningLsp == null
                ? Collections.<HopUnit>emptyList()
                : getPrimaryHops(preProvisioningLsp);
    }

    public static List<HopUnit> getPreviousSecondaryHops(RsvpLspDto lsp) {
        RsvpLspDto preProvisioningLsp = RsvpLspExtUtil.getPreProvisioningLsp(lsp);
        return preProvisioningLsp == null
                ? Collections.<HopUnit>emptyList()
                : getSecondaryHops(preProvisioningLsp);
    }


    private static class RsvpLspRendererCache {
        private static final ThreadLocal<RsvpLspRendererCache> threadLocal = new ThreadLocal<RsvpLspRendererCache>() {
            public RsvpLspRendererCache initialValue() {
                return new RsvpLspRendererCache();
            }
        };

        public static RsvpLspRendererCache getCache() {
            return threadLocal.get();
        }

        private Map<MvoId, CacheUnit> units = new HashMap<MvoId, CacheUnit>();

        private RsvpLspRendererCache() {
        }

        public CacheUnit getCacheUnit(RsvpLspDto lsp) {
            if (lsp == null) {
                return null;
            }
            CacheUnit unit = units.get(DtoUtil.getMvoId(lsp));
            if (unit == null) {
                unit = new CacheUnit(lsp);
            }
            return unit;
        }
    }

    public static String getAdminStatus(RsvpLspIdPoolDto pool) {
        return DtoUtil.getString(pool, MPLSNMS_ATTR.ADMIN_STATUS);
    }
}