package voss.multilayernms.inventory.renderer;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import tef.MVO.MvoId;
import voss.core.server.util.DtoUtil;
import voss.mplsnms.MplsnmsAttrs;
import voss.multilayernms.inventory.constants.LinkFacilityStatus;
import voss.multilayernms.inventory.util.PortIfNameComparator;
import voss.multilayernms.inventory.util.PseudoWireExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;
import voss.nms.inventory.util.IpSubnetUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.PseudoWireUtil;

import java.util.*;

public class LinkRenderer extends GenericRenderer {

    public static String getName(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        return subnet.getSubnetName();
    }

    public static String getCableName(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(subnet, MPLSNMS_ATTR.LINK_CABLE_NAME);
    }

    public static String getSRLGValue(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(subnet, MPLSNMS_ATTR.LINK_SRLG_VALUE);
    }

    public static LinkFacilityStatus getFacilityStatus(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        boolean onNetwork = DtoUtil.getBoolean(subnet, MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK);
        boolean approved = DtoUtil.getBoolean(subnet, MPLSNMS_ATTR.LINK_APPROVED);
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

    public static String getOnNetworkStatus(IpSubnetDto subnet) {
        if (getOnNetworkStatusAsBoolean(subnet)) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public static boolean getOnNetworkStatusAsBoolean(IpSubnetDto subnet) {
        return DtoUtil.getBoolean(subnet, MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK);
    }

    public static boolean isLinkApproved(IpSubnetDto subnet) {
        return DtoUtil.getBoolean(subnet, MPLSNMS_ATTR.LINK_APPROVED);
    }

    public static String getFacilityStatusName(IpSubnetDto subnet) {
        LinkFacilityStatus status = getFacilityStatus(subnet);
        if (status == null) {
            return null;
        }
        return status.getDisplayString();
    }

    public static String getOperStatus(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        for (PortDto member : subnet.getMemberIpifs()) {
            if (IpIfDto.class.isInstance(member)) {
                IpIfDto ip = (IpIfDto) member;
                for (PortDto p : ip.getAssociatedPorts()) {
                    String operStatus = DtoUtil.getStringOrNull(p, MPLSNMS_ATTR.OPER_STATUS);
                    if (operStatus == null) {
                        return null;
                    }
                    boolean isUp_ = MPLSNMS_ATTR.UP.equals(operStatus);
                    if (!isUp_) {
                        return MPLSNMS_ATTR.DOWN;
                    }
                }
            } else {
                String operStatus = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.OPER_STATUS);
                if (operStatus == null) {
                    return null;
                }
                boolean isUp_ = MPLSNMS_ATTR.UP.equals(operStatus);
                if (!isUp_) {
                    return MPLSNMS_ATTR.DOWN;
                }
            }
        }
        return MPLSNMS_ATTR.UP;
    }

    public static String getIgpCost(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Integer cost = null;
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static String getOspfAreaID(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Set<String> areas = new HashSet<String>();
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static String getLinkType(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        String type = DtoUtil.getStringOrNull(subnet, MPLSNMS_ATTR.LINK_TYPE);
        if (type != null) {
            return type;
        }
        PortDto port = getPort1(subnet);
        if (port == null) {
            port = getPort2(subnet);
        }
        if (port == null) {
            return null;
        }
        return PortRenderer.getPortType(port);
    }

    public static String getLinkAccomodationLimit(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Integer limit = getRawLinkAccomodationLimit(subnet);
        if (limit == null) {
            return null;
        }
        return getRawLinkAccomodationLimit(subnet).intValue() + " %";
    }

    public static Integer getRawLinkAccomodationLimit(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Integer limit = DtoUtil.getInteger(subnet, MPLSNMS_ATTR.LINK_ACCOMMODATION_LIMIT);
        if (limit == null) {
            return null;
        }
        return limit;
    }

    public static boolean hasDetourLink(IpSubnetDto subnet) {
        if (subnet == null) {
            return false;
        }
        for (PortDto member : subnet.getMemberIpifs()) {
            Map<?, ?> detour = member.get(MplsnmsAttrs.IpIfDtoAttr.UKAIRO);
            if (detour != null && detour.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public static PortDto getPort1(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        if (subnet.getMemberIpifs().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + subnet.getMemberIpifs().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(subnet.getMemberIpifs());
        Collections.sort(members, new PortIfNameComparator());
        PortDto ip = members.get(0);
        PortDto port = NodeUtil.getAssociatedPort(ip);
        if (port != null) {
            return port;
        }
        return ip;
    }

    public static PortDto getPort2(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        if (subnet.getMemberIpifs().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + subnet.getMemberIpifs().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(subnet.getMemberIpifs());
        Collections.sort(members, new PortIfNameComparator());
        PortDto ip = members.get(1);
        PortDto port = NodeUtil.getAssociatedPort(ip);
        if (port != null) {
            return port;
        }
        return ip;
    }

    public static Long getRawBandwidth(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Long bandwidth = null;
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static String getBandwidth(IpSubnetDto subnet) {
        return subnet == null
                ? null
                : BandwidthFormat.format(getRawBandwidth(subnet), 2);
    }

    public static Double getRawFixedRoundTripTime(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Double latency = null;
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static String getFixedRoundTripTime(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Double latency = getRawFixedRoundTripTime(subnet);
        if (latency == null) {
            return null;
        }
        return latency.toString();
    }

    public static Double getRawVariableRoundTripTime(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Double latency = null;
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static String getVariableRoundTripTime(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        Double latency = getRawVariableRoundTripTime(subnet);
        if (latency == null) {
            return null;
        }
        return latency.toString();
    }

    public static Long getRawCesTotalBandwidth(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        long bandwidth = 0L;
        Set<MvoId> processed = new HashSet<MvoId>();
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static Long getRawEtherVpnTotalBandwidth(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        long bandwidth = 0L;
        Set<MvoId> processed = new HashSet<MvoId>();
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
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

    public static Long getRawBestEffortGuaranteedBandwidth(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        long result = 0L;
        for (PortDto port : IpSubnetUtil.getMemberPorts(subnet)) {
            Long bestEffortValue = PortRenderer.getRawBestEffortGuaranteedBandwidth(port);
            if (bestEffortValue != null) {
                result = Math.max(result, bestEffortValue.longValue());
            }
        }
        return Long.valueOf(result);
    }

    public static String getBestEffortGuaranteedBandwidth(IpSubnetDto subnet) {
        Long bandwidth = getRawBestEffortGuaranteedBandwidth(subnet);
        if (bandwidth == null) {
            return null;
        }
        return BandwidthFormat.format(bandwidth, PortRenderer.BANDWIDTH_FORMAT_THRESHOLD);
    }

    public static IpIfDto getYoungerIpIf(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        } else if (subnet.getMemberIpifs().size() == 1) {
            IpIfDto p = NodeUtil.toIpIfDto(subnet.getMemberIpifs().iterator().next());
            if (p != null) {
                return p;
            } else {
                throw new IllegalStateException("non ip-if member port on ip-subnet: " + subnet.getAbsoluteName());
            }
        } else if (subnet.getMemberIpifs().size() > 2) {
            throw new IllegalStateException("3 or more member found:" + subnet.getAbsoluteName());
        }
        Map<String, IpIfDto> map = new HashMap<String, IpIfDto>();
        for (PortDto member : subnet.getMemberIpifs()) {
            IpIfDto ip = NodeUtil.toIpIfDto(member);
            if (ip == null) {
                continue;
            }
            map.put(DtoUtil.getMvoId(ip).toString(), ip);
        }
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        return map.get(keys.get(0));
    }

    public static IpIfDto getElderIpIf(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        } else if (subnet.getMemberIpifs().size() == 1) {
            return null;
        } else if (subnet.getMemberIpifs().size() > 2) {
            throw new IllegalStateException("3 or more member found:" + subnet.getAbsoluteName());
        }
        Map<String, IpIfDto> map = new HashMap<String, IpIfDto>();
        for (PortDto member : subnet.getMemberIpifs()) {
            IpIfDto ip = NodeUtil.toIpIfDto(member);
            if (ip == null) {
                continue;
            }
            map.put(DtoUtil.getMvoId(ip).toString(), ip);
        }
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        return map.get(keys.get(1));
    }

    public static String getLinkDetourDestinationSpecified(IpSubnetDto link) {
        if (link == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        IpIfDto ip1 = getYoungerIpIf(link);
        if (hasDetour(ip1)) {
            sb.append("A");
        }
        IpIfDto ip2 = getElderIpIf(link);
        if (hasDetour(ip2)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("B");
        }
        return sb.toString();
    }

    private static boolean hasDetour(IpIfDto ip) {
        Map<?, ?> o = ip.get(MplsnmsAttrs.IpIfDtoAttr.UKAIRO);
        return o != null && o.size() > 0;
    }
}