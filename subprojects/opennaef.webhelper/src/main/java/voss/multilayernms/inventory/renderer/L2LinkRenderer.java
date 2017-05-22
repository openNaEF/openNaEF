package voss.multilayernms.inventory.renderer;

import naef.dto.LinkDto;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import tef.MVO.MvoId;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.LinkFacilityStatus;
import voss.multilayernms.inventory.util.PortIfNameComparator;
import voss.multilayernms.inventory.util.PseudoWireExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PseudoWireUtil;

import java.util.*;

public class L2LinkRenderer extends GenericRenderer {

    public static String getName(LinkDto link) {
        if (link == null) {
            return null;
        }
        PortDto port1 = getPort1(link);
        PortDto port2 = getPort2(link);
        StringBuilder sb = new StringBuilder();
        sb.append("Physical link: ").append(PortRenderer.getNodeIfName(port1)).append(" ~ ").append(PortRenderer.getNodeIfName(port2));
        return sb.toString();
    }

    public static String getCableName(LinkDto link) {
        if (link == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(link, MPLSNMS_ATTR.LINK_CABLE_NAME);
    }

    public static LinkFacilityStatus getFacilityStatus(LinkDto link) {
        if (link == null) {
            return null;
        }
        boolean onNetwork = DtoUtil.getBoolean(link, MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK);
        boolean approved = DtoUtil.getBoolean(link, MPLSNMS_ATTR.LINK_APPROVED);
        if (approved) {
            if (onNetwork) {
                return LinkFacilityStatus.IN_USE;
            } else {
                return LinkFacilityStatus.RESERVED;
            }
        } else {
            return LinkFacilityStatus.UNAPPROVED;
        }
    }

    public static String getOnNetworkStatus(LinkDto link) {
        boolean onNetwork = DtoUtil.getBoolean(link, MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK);
        if (onNetwork) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public static String getFacilityStatusName(LinkDto link) {
        LinkFacilityStatus status = getFacilityStatus(link);
        if (status == null) {
            return null;
        }
        return status.getDisplayString();
    }

    public static String getOperStatus(LinkDto link) {
        if (link == null) {
            return null;
        }
        for (PortDto member : link.getMemberPorts()) {
            String operStatus = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.OPER_STATUS);
            if (operStatus == null) {
                return null;
            }
            boolean isUp_ = MPLSNMS_ATTR.UP.equals(operStatus);
            if (!isUp_) {
                return MPLSNMS_ATTR.DOWN;
            }
        }
        return MPLSNMS_ATTR.UP;
    }

    public static String getIgpCost(LinkDto link) {
        if (link == null) {
            return null;
        }
        Integer cost = null;
        for (PortDto port : link.getMemberPorts()) {
            Integer cost_ = PortRenderer.getRawIgpCost(port);
            if (cost_ == null) {
                continue;
            } else {
                if (cost == null) {
                    cost = cost_;
                } else {
                    cost = Math.min(cost, cost_.intValue());
                }
            }
        }
        return String.valueOf(cost);
    }

    public static String getOspfAreaID(LinkDto link) {
        if (link == null) {
            return null;
        }
        Set<String> areas = new HashSet<String>();
        for (PortDto port : link.getMemberPorts()) {
            String areaID = PortRenderer.getOspfAreaID(port);
            if (areaID != null) {
                areas.add(areaID);
            }
        }

        if (areas.isEmpty()) {
            return "";
        } else {
            ArrayList<String> areaList = new ArrayList<String>(areas);
            Collections.sort(areaList);
            StringBuilder result = new StringBuilder();
            for (String area : areaList) {
                result.append(area);
                result.append(',');
            }
            return result.substring(0, result.length() - 1);
        }
    }

    public static String getLinkType(LinkDto link) {
        if (link == null) {
            return null;
        }
        String type = DtoUtil.getStringOrNull(link, MPLSNMS_ATTR.LINK_TYPE);
        if (type != null) {
            return type;
        }
        PortDto port = getPort1(link);
        if (port == null) {
            port = getPort2(link);
        }
        if (port == null) {
            return null;
        }
        return PortRenderer.getPortType(port);
    }

    public static String getLinkAccomodationLimit(LinkDto link) {
        if (link == null) {
            return null;
        }
        Integer limit = getRawLinkAccomodationLimit(link);
        if (limit == null) {
            return null;
        }
        return getRawLinkAccomodationLimit(link).intValue() + " %";
    }

    public static Integer getRawLinkAccomodationLimit(LinkDto link) {
        if (link == null) {
            return null;
        }
        Integer limit = DtoUtil.getInteger(link, MPLSNMS_ATTR.LINK_ACCOMMODATION_LIMIT);
        if (limit == null) {
            return null;
        }
        return limit;
    }

    public static PortDto getPort1(LinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(0));
    }

    public static PortDto getPort2(LinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(1));
    }

    public static Long getRawBandwidth(LinkDto link) {
        if (link == null) {
            return null;
        }
        Long bandwidth = null;
        for (PortDto port : link.getMemberPorts()) {
            Long band = PortRenderer.getBandwidthAsLong(port);
            if (band == null) {
                return null;
            } else if (bandwidth == null) {
                bandwidth = band;
            } else {
                bandwidth = Math.min(band, bandwidth);
            }
        }
        return bandwidth;
    }

    public static String getBandwidth(LinkDto link) {
        return link == null
                ? null
                : BandwidthFormat.format(getRawBandwidth(link), 2);
    }

    public static Double getRawFixedRoundTripTime(LinkDto link) {
        if (link == null) {
            return null;
        }
        Double latency = null;
        for (PortDto port : link.getMemberPorts()) {
            Double latency_ = PortRenderer.getRawFixedRoundTripTime(port);
            if (latency_ == null) {
                continue;
            } else if (latency == null) {
                latency = latency_;
            } else {
                latency = Math.max(latency_, latency);
            }
        }
        return latency;
    }

    public static String getFixedRoundTripTime(LinkDto link) {
        if (link == null) {
            return null;
        }
        Double latency = getRawFixedRoundTripTime(link);
        if (latency == null) {
            return null;
        }
        return latency.toString();
    }

    public static Double getRawVariableRoundTripTime(LinkDto link) {
        if (link == null) {
            return null;
        }
        Double latency = null;
        for (PortDto port : link.getMemberPorts()) {
            Double latency_ = PortRenderer.getRawVariableRoundTripTime(port);
            if (latency_ == null) {
                continue;
            } else if (latency == null) {
                latency = latency_;
            } else {
                latency = Math.max(latency_, latency);
            }
        }
        return latency;
    }

    public static String getVariableRoundTripTime(LinkDto link) {
        if (link == null) {
            return null;
        }
        Double latency = getRawVariableRoundTripTime(link);
        if (latency == null) {
            return null;
        }
        return latency.toString();
    }

    public static Long getRawCesTotalBandwidth(LinkDto link) {
        if (link == null) {
            return null;
        }
        long bandwidth = 0L;
        Set<MvoId> processed = new HashSet<MvoId>();
        for (PortDto port : link.getMemberPorts()) {
            for (PseudowireDto pw : PseudoWireUtil.getPseudWiresOn(port)) {
                if (pw == null) {
                    continue;
                } else if (processed.contains(DtoUtil.getMvoId(pw))) {
                    continue;
                }
                processed.add(DtoUtil.getMvoId(pw));
                if (!PseudoWireExtUtil.isCpipeType(pw)) {
                    continue;
                }
                Long band = PseudoWireRenderer.getBandwidthAsLong(pw);
                if (band != null) {
                    bandwidth = bandwidth + band.longValue();
                }
            }
        }
        return Long.valueOf(bandwidth);
    }

    public static Long getRawEtherVpnTotalBandwidth(LinkDto link) {
        if (link == null) {
            return null;
        }
        long bandwidth = 0L;
        Set<MvoId> processed = new HashSet<MvoId>();
        for (PortDto port : link.getMemberPorts()) {
            for (PseudowireDto pw : PseudoWireUtil.getPseudWiresOn(port)) {
                if (pw == null) {
                    continue;
                } else if (processed.contains(DtoUtil.getMvoId(pw))) {
                    continue;
                }
                processed.add(DtoUtil.getMvoId(pw));
                if (!PseudoWireExtUtil.isEpipeType(pw)) {
                    continue;
                }
                Long band = PseudoWireRenderer.getBandwidthAsLong(pw);
                if (band != null) {
                    bandwidth = bandwidth + band.longValue();
                }
            }
        }
        return Long.valueOf(bandwidth);
    }

    public static Long getRawBestEffortGuaranteedBandwidth(LinkDto link) {
        if (link == null) {
            return null;
        }
        long result = 0L;
        for (PortDto port : link.getMemberPorts()) {
            Long bestEffortValue = PortRenderer.getRawBestEffortGuaranteedBandwidth(port);
            if (bestEffortValue != null) {
                result = Math.max(result, bestEffortValue.longValue());
            }
        }
        return Long.valueOf(result);
    }

    public static String getBestEffortGuaranteedBandwidth(LinkDto link) {
        Long bandwidth = getRawBestEffortGuaranteedBandwidth(link);
        if (bandwidth == null) {
            return null;
        }
        return BandwidthFormat.format(bandwidth, PortRenderer.BANDWIDTH_FORMAT_THRESHOLD);
    }

    public static PortDto getYoungerIpIf(LinkDto link) {
        if (link == null) {
            return null;
        } else if (link.getMemberPorts().size() == 1) {
            return link.getMemberPorts().iterator().next();
        } else if (link.getMemberPorts().size() > 2) {
            throw new IllegalStateException("3 or more member found:" + link.getAbsoluteName());
        }
        Map<String, PortDto> map = new HashMap<String, PortDto>();
        for (PortDto member : link.getMemberPorts()) {
            map.put(DtoUtil.getMvoId(member).toString(), member);
        }
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        return map.get(keys.get(0));
    }

    public static PortDto getElderPort(LinkDto link) {
        if (link == null) {
            return null;
        } else if (link.getMemberPorts().size() == 1) {
            return null;
        } else if (link.getMemberPorts().size() > 2) {
            throw new IllegalStateException("3 or more member found:" + link.getAbsoluteName());
        }
        Map<String, PortDto> map = new HashMap<String, PortDto>();
        for (PortDto member : link.getMemberPorts()) {
            map.put(DtoUtil.getMvoId(member).toString(), member);
        }
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        return map.get(keys.get(1));
    }
}