package voss.multilayernms.inventory.renderer;

import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vrf.VrfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.BandwidthFormat;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.VlanUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PortRenderer extends GenericRenderer {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PortRenderer.class);

    public static final int BANDWIDTH_FORMAT_THRESHOLD = 2;

    public static String getNodeName(PortDto port) {
        if (port == null) {
            return null;
        }
        NodeDto node = port.getNode();
        return NodeRenderer.getNodeName(node);
    }

    public static String getIfName(PortDto port) {
        if (port == null) {
            return null;
        }
        return NameUtil.getIfName(port);
    }

    public static String getConfigName(PortDto port) {
        if (port == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(port, ATTR.CONFIG_NAME);
    }

    public static String getNodeIfName(PortDto port) {
        if (port == null) {
            return null;
        }
        return getNodeName(port) + ":" + getIfName(port);
    }

    public static String getPortName(PortDto port) {
        if (port == null) {
            return null;
        }
        String ifName = getIfName(port);
        if (ifName != null && ifName.lastIndexOf('/') > -1) {
            String portName = ifName.substring(ifName.lastIndexOf('/') + 1);
            int suffixIndex = portName.indexOf('.');
            if (suffixIndex == -1) {
                suffixIndex = portName.indexOf(':');
            }
            if (suffixIndex == -1) {
                return portName;
            } else {
                return portName.substring(0, suffixIndex);
            }
        }
        if (port.getOwner() != null && port.getOwner() instanceof JackDto) {
            return port.getOwner().getName();
        }
        return "-";
    }

    public static String getVpnPrefix(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(ip, ATTR.VPN_PREFIX);
    }

    public static String getVpnPrefixOnIpIf(IpIfDto ipif) {
        if (ipif == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(ipif, ATTR.VPN_PREFIX);
    }


    public static String getIpAddress(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(ip, MPLSNMS_ATTR.IP_ADDRESS);
    }

    public static String getSubnetMask(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip == null) {
            return null;
        }
        return DtoUtil.getString(ip, MPLSNMS_ATTR.MASK_LENGTH);
    }

    public static String getAddressMask(PortDto port) {
        String ip = getIpAddress(port);
        String mask = getSubnetMask(port);
        if (ip == null) {
            return null;
        } else if (mask == null) {
            return ip;
        }
        return ip + "/" + mask;
    }

    public static String getPortType(PortDto port) {
        if (port == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (port.isAlias()) {
            sb.append("*");
        }
        sb.append(NameUtil.getPortTypeName(port));
        return sb.toString();
    }

    public static String getPhysicalPortOperStatus(PortDto port) {
        if (NodeUtil.isSubInterface(port)) {
            if (port instanceof AtmPvcIfDto) {
                PortDto atm = ((AtmPvcIfDto) port).getPhysicalPort();
                return getOperStatus(atm);
            }
            NodeElementDto owner = port.getOwner();
            if (owner instanceof PortDto) {
                return getOperStatus((PortDto) owner);
            } else {
                return null;
            }
        }
        return getOperStatus(port);
    }

    public static String getSubInterfaceOperStatus(PortDto port) {
        if (NodeUtil.isSubInterface(port)) {
            return getOperStatus(port);
        }
        return null;
    }

    public static String getFacilityStatus(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.FACILITY_STATUS);
    }

    public static String getBandwidth(PortDto port) {
        return BandwidthFormat.format(getBandwidthAsLong(port), 2);
    }

    public static String getRawBandwidth(PortDto port) {
        Long band = getBandwidthAsLong(port);
        if (band == null) {
            return null;
        }
        return band.toString();
    }

    public static Long getBandwidthAsLong(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof EthPortDto) {
            Long speed = DtoUtil.getLong(port, MPLSNMS_ATTR.PORTSPEED_OPER);
            if (speed != null) {
                return speed;
            }
        }

        return DtoUtil.getLong(port, MPLSNMS_ATTR.BANDWIDTH);
    }

    public static String getSuffix(PortDto port) {
        if (port == null) {
            return null;
        }
        String suffix = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.SUFFIX);
        if (suffix == null) {
            String ifName = DtoUtil.getIfName(port);
            if (ifName == null) {
                return null;
            }
            suffix = getSuffixPart(ifName, '.');
            if (suffix == null) {
                suffix = getSuffixPart(ifName, ':');
            }
        }
        return suffix;
    }

    private static String getSuffixPart(String ifName, char delimiter) {
        int index = ifName.indexOf(delimiter);
        if (index > -1) {
            if (index == (ifName.length() - 1)) {
                return null;
            } else {
                return ifName.substring(index + 1);
            }
        }
        return null;
    }

    public static Integer getBandwidthAsInteger(PortDto port) {
        Long band = getBandwidthAsLong(port);
        if (band == null) {
            return null;
        }
        long inMega = band.longValue() / BandwidthFormat.MEGA;
        if (inMega > Integer.MAX_VALUE) {
            throw new IllegalStateException("too large bandwidth. [" + inMega + "]Mbps.");
        }
        return (int) inMega;
    }

    public static String getOspfAreaID(PortDto port) {
        if (port == null) {
            return null;
        }
        return DtoUtil.getString(port, MPLSNMS_ATTR.OSPF_AREA_ID);
    }

    public static Integer getRawIgpCost(PortDto port) {
        if (port == null) {
            return null;
        }
        return DtoUtil.getInteger(port, MPLSNMS_ATTR.IGP_COST);
    }

    public static String getIgpCost(PortDto port) {
        Integer cost = getIgpCostAsInteger(port);
        if (cost == null) {
            return null;
        }
        return cost.toString();
    }

    public static Integer getIgpCostAsInteger(PortDto port) {
        return getRawIgpCost(port);
    }

    public static String getVpi(PortDto port) {
        Integer vpi = getVpiAsInteger(port);
        return vpi != null ? vpi.toString() : null;
    }

    public static Integer getVpiAsInteger(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof AtmPvcIfDto) {
            AtmPvcIfDto pvc = (AtmPvcIfDto) port;
            return pvc.getVpi();
        }
        return null;
    }

    public static String getVci(PortDto port) {
        Integer vci = getVciAsInteger(port);
        return vci != null ? vci.toString() : null;
    }

    public static Integer getVciAsInteger(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof AtmPvcIfDto) {
            AtmPvcIfDto pvc = (AtmPvcIfDto) port;
            return pvc.getVci();
        }
        return null;
    }

    public static String getRouterVlanID(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof VlanIfDto) {
            VlanIfDto vif = (VlanIfDto) port;
            if (vif.getVlanId() != null) {
                return vif.getVlanId().toString();
            }
        }
        return null;
    }

    public static String getVlanID(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof VlanIfDto) {
            VlanIfDto vif = (VlanIfDto) port;
            if (vif.getVlanId() != null) {
                return vif.getVlanId().toString();
            }
        }
        else if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            ArrayList<Integer> vids = new ArrayList<Integer>();
            try {
                Collection<PortDto> xcs = port.getCrossConnections();
                for (PortDto xc : xcs) {
                    if (xc instanceof VlanIfDto) {
                        if (((VlanIfDto) xc).getVlanId() != null) {
                            vids.add(((VlanIfDto) xc).getVlanId());
                        }
                    }
                }
                Collection<PortDto> uls = port.getUpperLayers();
                for (PortDto ul : uls) {
                    if (ul instanceof VlanIfDto) {
                        if (((VlanIfDto) ul).getVlanId() != null) {
                            vids.add(((VlanIfDto) ul).getVlanId());
                        }
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
            Collections.sort(vids);
            StringBuilder sb = new StringBuilder();
            for (Integer vid : vids) {
                sb.append(vid + ",");
            }
            String s;
            if (sb.length() >= 2 && sb.toString().endsWith(",")) {
                s = sb.substring(0, sb.length() - 1);
            } else {
                s = sb.toString();
            }
            return s;
        }
        return null;
    }

    public static String getVlanIDByGroup(PortDto port) {
        String vlanIDsStr = getVlanID(port);
        if (vlanIDsStr == null) {
            return null;
        }
        if (vlanIDsStr.equals("")) {
            return "";
        }

        String[] vlanIDStrs = vlanIDsStr.split(",");

        ArrayList<Integer> vlanIDs = new ArrayList<Integer>();
        for (String vlanIDStr : vlanIDStrs) {
            vlanIDs.add(Integer.decode(vlanIDStr));
        }

        Collections.sort(vlanIDs);

        ArrayList<String> groupedVlanIDs = new ArrayList<String>();
        ArrayList<Integer> buff = new ArrayList<Integer>();
        for (Integer vlanID : vlanIDs) {
            if (buff.isEmpty()) {
                buff.add(vlanID);
            } else {
                if (!buff.contains(vlanID - 1)) {
                    groupedVlanIDs.add(toGroupInteger(buff));
                    buff.clear();
                }
                buff.add(vlanID);
            }
        }

        if (!buff.isEmpty()) {
            groupedVlanIDs.add(toGroupInteger(buff));
        }

        StringBuffer strBuff = new StringBuffer();
        for (String groupedVlanID : groupedVlanIDs) {
            if (strBuff.length() != 0) {
                strBuff.append(",");
            }
            strBuff.append(groupedVlanID);
        }

        return strBuff.toString();
    }

    private static String toGroupInteger(ArrayList<Integer> nums) {
        int min = Collections.min(nums);
        int max = Collections.max(nums);
        if (min == max) {
            return String.valueOf(min);
        } else {
            return String.format("%d-%d", min, max);
        }
    }

    public static String getMemberVlanIDOnPort(PortDto port) {
        if (port == null) {
            return "";
        }
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            ArrayList<Integer> vidlist = new ArrayList<Integer>();
            Collection<PortDto> vifs = port.getUpperLayers();
            for (PortDto vif : vifs) {
                if (vif instanceof VlanIfDto) {
                    vidlist.add(((VlanIfDto) vif).getVlanId());
                }
            }
            Collections.sort(vidlist);
            StringBuilder sb = new StringBuilder();
            for (Integer vid : vidlist) {
                sb.append(vid + ",");
            }
            if (sb.toString().endsWith(",")) {
                sb.toString().substring(0, sb.toString().length() - 1);
            }
            return sb.toString();
        }
        return "";
    }

    public static List<VlanDto> getTaggedVlans(PortDto port) {
        List<VlanDto> vlans = new ArrayList<VlanDto>();
        for (PortDto upper : port.getUpperLayers()) {
            if (!VlanIfDto.class.isInstance(upper)) {
                continue;
            }
            VlanIfDto vif = VlanIfDto.class.cast(upper);
            if (vif.getTrafficDomain() == null) {
                continue;
            }
            vlans.add(vif.getTrafficDomain());
        }
        return vlans;
    }

    public static List<VlanIfDto> getTaggedVlanIfs(PortDto port) {
        List<VlanIfDto> vlans = new ArrayList<VlanIfDto>();
        for (PortDto upper : port.getUpperLayers()) {
            if (!VlanIfDto.class.isInstance(upper)) {
                continue;
            }
            VlanIfDto vif = VlanIfDto.class.cast(upper);
            vlans.add(vif);
        }
        return vlans;
    }

    public static String getTimeSlot(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof TdmSerialIfDto) {
            return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.TIMESLOT);
        }
        return null;
    }

    public static String getChannelGroup(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof TdmSerialIfDto) {
            return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.CHANNEL_GROUP);
        }
        return null;
    }

    public static ModuleDto getModule(PortDto port) {
        ModuleDto module = null;
        NodeElementDto parent = port;
        while (parent != null) {
            if (parent instanceof ModuleDto) {
                module = (ModuleDto) parent;
                break;
            }
            parent = parent.getOwner();
        }
        if (module == null) {
            return null;
        } else {
            return module;
        }
    }

    public static String getModuleTypeName(PortDto port) {
        ModuleDto module = getModule(port);
        if (module == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(module, MPLSNMS_ATTR.MODULE_TYPE);
    }

    public static String getModuleNumber(PortDto port) {
        return null;
    }

    public static SlotDto getSlot(PortDto port) {
        ModuleDto module = getModule(port);
        if (module == null) {
            return null;
        }
        if (module.getOwner() == null) {
            throw new IllegalStateException("module without parent found: " + module.getAbsoluteName());
        } else if (!(module.getOwner() instanceof SlotDto)) {
            throw new IllegalStateException("unexpected parent of module: " + module.getOwner().getAbsoluteName());
        }
        SlotDto slot = (SlotDto) module.getOwner();
        return slot;
    }

    public static String getSlotID(PortDto port) {
        SlotDto slot = getSlot(port);
        if (slot == null) {
            return null;
        }
        return NameUtil.getIfName(slot);
    }

    public static String getSlotType(PortDto port) {
        SlotDto slot = getSlot(port);
        if (slot == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(slot, MPLSNMS_ATTR.SLOT_TYPE);
    }

    public static Long getRawBestEffortGuaranteedBandwidth(PortDto port) {
        if (port == null) {
            return null;
        }
        Long bestEffortValue = DtoUtil.getLong(port, MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH);
        return bestEffortValue;
    }

    public static Double getRawFixedRoundTripTime(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        return DtoUtil.getDouble(ip, MPLSNMS_ATTR.FIXED_RTT);
    }

    public static String getFixedRoundTripTime(PortDto port) {
        Double rtt = getRawFixedRoundTripTime(port);
        if (rtt == null) {
            return null;
        }
        return rtt.toString();
    }

    public static Double getRawVariableRoundTripTime(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        return DtoUtil.getDouble(ip, MPLSNMS_ATTR.VARIABLE_RTT);
    }

    public static String getVariableRoundTripTime(PortDto port) {
        Double rtt = getRawVariableRoundTripTime(port);
        if (rtt == null) {
            return null;
        }
        return rtt.toString();
    }

    public static String getLineId(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.USERLINE_ID);
    }

    public static String getOrangeGuaranteedBandwidth(PortDto port) {
        Long orange = getRawBestEffortGuaranteedBandwidth(port);
        if (orange == null) {
            return null;
        }
        return BandwidthFormat.format(orange, BANDWIDTH_FORMAT_THRESHOLD);
    }

    public static String getSonetMode(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.SONET_MODE);
    }


    public static String getConnected(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.CONNECTED);
    }

    public static String getInterimConnected(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.INTERIM_CONNECTED);
    }

    public static String getMemberPorts(PortDto port) {
        if (port instanceof EthLagIfDto) {
            EthLagIfDto lag = (EthLagIfDto) port;
            StringBuffer sb = new StringBuffer();
            for (EthPortDto ep : lag.getBundlePorts()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ep.getIfname());
            }
            return sb.toString();
        } else if (port instanceof AtmApsIfDto) {
            AtmApsIfDto aps = (AtmApsIfDto) port;
            StringBuffer sb = new StringBuffer();
            for (AtmPortDto ap : aps.getAtmPorts()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ap.getIfname());
            }
            return sb.toString();
        } else if (port instanceof PosApsIfDto) {
            return "";
        } else {
            return "";
        }
    }

    public static List<String> getNetworks(PortDto port) {
        List<String> networks = new ArrayList<String>();
        for (NetworkDto network : port.getNetworks()) {
            if (VlanDto.class.isInstance(network)) {
                VlanDto vlan = VlanDto.class.cast(network);
                String name = "VLAN: " + vlan.getVlanId().toString() + "(" + vlan.getIdPool().getName() + ")";
                networks.add(name);
                continue;
            } else if (VplsDto.class.isInstance(network)) {
                VplsDto vpls = VplsDto.class.cast(network);
                String name = "VPLS: " + vpls.getStringId() + "(" + vpls.getStringIdPool().getName() + ")";
                networks.add(name);
                continue;
            } else if (VrfDto.class.isInstance(network)) {
                VrfDto vrf = VrfDto.class.cast(network);
                String name = "VRF: " + vrf.getStringId() + "(" + vrf.getStringIdPool().getName() + ")";
                networks.add(name);
                continue;
            } else if (PseudowireDto.class.isInstance(network)) {
                PseudowireDto pw = PseudowireDto.class.cast(network);
                if (!isAttachmentPort(port, pw)) {
                    continue;
                }
                String name = "PW: " + pw.getStringId() + "(" + pw.getStringIdPool().getName() + ")";
                networks.add(name);
                continue;
            } else if (RsvpLspHopSeriesDto.class.isInstance(network)) {
                continue;
            } else if (IpSubnetDto.class.isInstance(network)) {
                continue;
            }
        }
        return networks;
    }

    private static boolean isAttachmentPort(PortDto port, PseudowireDto pw) {
        Desc<PortDto> ref = pw.get(PseudowireDto.ExtAttr.AC1);
        if (ref != null && DtoUtil.getMvoId(ref).equals(DtoUtil.getMvoId(port))) {
            return true;
        }
        Desc<PortDto> ref2 = pw.get(PseudowireDto.ExtAttr.AC2);
        if (ref2 != null && DtoUtil.getMvoId(ref2).equals(DtoUtil.getMvoId(port))) {
            return true;
        }
        return false;
    }

    public static String getSwPort(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.SW_PORT);
    }

    public static Integer getMru(PortDto port) {
        return DtoUtil.getInteger(port, CustomerConstants.MRU);
    }

    public static String getAutoNegotiation(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.AUTO_NEGO);
    }

    public static Integer getAdministrativeSpeed(PortDto port) {
        return DtoUtil.getInteger(port, MPLSNMS_ATTR.PORTSPEED_ADMIN);
    }

    public static String getAdministrativeSpeedAsString(PortDto port) {
        if (DtoUtil.getString(port, MPLSNMS_ATTR.PORTSPEED_ADMIN).equals("auto")) {
            return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.PORTSPEED_ADMIN);
        } else if (DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.PORTSPEED_ADMIN) != null) {
            return BandwidthFormat.format(Long.parseLong(DtoUtil.getString(port, MPLSNMS_ATTR.PORTSPEED_ADMIN)), 2);
        } else {
            return null;
        }
    }

    public static Integer getOperationalSpeed(PortDto port) {
        return DtoUtil.getInteger(port, MPLSNMS_ATTR.PORTSPEED_OPER);
    }

    public static String getOperationalSpeedAsString(PortDto port) {
        Integer opSpeed = DtoUtil.getInteger(port, MPLSNMS_ATTR.PORTSPEED_OPER);
        if (opSpeed == null) {
            return null;
        }
        return opSpeed.toString();
    }

    public static String getAdministrativeDuplex(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.DUPLEX_ADMIN);
    }

    public static String getOperationalDuplex(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.DUPLEX_OPER);
    }

    public static Boolean getSviEnabled(PortDto port) {
        return DtoUtil.getBoolean(port, MPLSNMS_ATTR.SVI_ENABLED);
    }

    public static String getQos(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.QOS);
    }

    public static String getPortDefault(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.PORT_DEFAULT);
    }

    public static String getIngressRegen(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.INGRESS_REGEN);
    }

    public static String getUserName(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.USER_NAME);
    }

    public static String getApName(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.AP_NAME);
    }

    public static String getApId(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.AP_ID);
    }

    public static String getOpeningDate(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.OPENING_DATE);
    }

    public static String getAbolitionDate(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.ABOLITION_DATE);
    }

    public static String getNotices(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.NOTICES);
    }

    public static String getSkip(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.SKIP);
    }

    public static String getNeighbor(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.NEIGHBOR);
    }

    public static String getEndUser(PortDto port) {
        return DtoUtil.getStringOrNull(port, CustomerConstants.END_USER);
    }

    public static String getAggregationPortName(PortDto port) {
        if (port == null) {
            return null;
        } else if (port instanceof EthPortDto) {
            EthPortDto eth = (EthPortDto) port;
            EthLagIfDto lag = NodeUtil.getEthLag(eth);
            return getIfName(lag);
        } else if (port instanceof AtmPortDto) {
            AtmPortDto atm = (AtmPortDto) port;
            AtmApsIfDto aps = NodeUtil.getAtmApsIf(atm);
            return getIfName(aps);
        } else if (port instanceof PosPortDto) {
            PosPortDto pos = (PosPortDto) port;
            PosApsIfDto aps = NodeUtil.getPosApsIf(pos);
            return getIfName(aps);
        }
        return null;
    }

    public static String getDescription(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.DESCRIPTION);
    }

    public static String getPortNumber(PortDto port) {
        String portNumber = port.getOwner().getName();
        return portNumber == null ? "" : portNumber;
    }

    public static String getPortMode(PortDto port) {
        return VlanUtil.getPortMode(port);
    }

    public static PortMode getPortModeValue(PortDto port) {
        String val = getPortMode(port);
        if (val == null) {
            return null;
        }
        return PortMode.valueOf(val);
    }

    public static String getSwitchPortMode(PortDto port) {
        return VlanUtil.getSwitchPortMode(port);
    }

    public static SwitchPortMode getSwitchPortModeValue(PortDto port) {
        String val = getSwitchPortMode(port);
        if (val == null) {
            return null;
        }
        return SwitchPortMode.valueOf(val);
    }

    public static String getP2POppositeNodeName(PortDto port) {
        PortDto port2 = NodeUtil.getLayer2Neighbor(port);

        if (port2 == null) {
            return "";
        }
        return getNodeName(port2);
    }

    public static String getP2POppositePortName(PortDto port) {
        PortDto port2 = NodeUtil.getLayer2Neighbor(port);
        if (port2 == null) {
            return "";
        }
        return getIfName(port2);
    }

    public static String getZone(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.ZONE);
    }

    public static String getTagChargerOuterVlanId(PortDto port) {
        return DtoUtil.getStringOrNull(port, ATTR.TAGCHANGER_OUTER_VLAN_ID);
    }

    public static String getParmanent(PortDto port) {
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.PERMANENT);
    }

    public static List<String> getTaggedPortAbsoluteNames(VlanIfDto vlanif) {
        if (vlanif == null) {
            return null;
        }
        List<String> taggedPortAbsoluteNames = new ArrayList<String>();
        Set<PortDto> taggedPorts = vlanif.getTaggedVlans();
        for (PortDto taggedPort : taggedPorts) {
            taggedPortAbsoluteNames.add(taggedPort.getAbsoluteName());
        }
        return taggedPortAbsoluteNames == null ? null : taggedPortAbsoluteNames;
    }

    public static List<String> getTaggedPortIfNames(VlanIfDto vlanif) {
        if (vlanif == null) {
            return null;
        }
        List<String> taggedPortIfNames = new ArrayList<String>();
        Set<PortDto> taggedPorts = vlanif.getTaggedVlans();
        for (PortDto taggedPort : taggedPorts) {
            taggedPortIfNames.add(taggedPort.getIfname());
        }
        return taggedPortIfNames == null ? null : taggedPortIfNames;
    }


    public static List<String> getUnTaggedPortAbsoluteNames(VlanIfDto vlanif) {
        if (vlanif == null) {
            return null;
        }
        List<String> untaggedPortAbsoluteNames = new ArrayList<String>();
        Set<PortDto> untaggedPorts = vlanif.getUntaggedVlans();
        for (PortDto untaggedPort : untaggedPorts) {
            untaggedPortAbsoluteNames.add(untaggedPort.getAbsoluteName());
        }
        return untaggedPortAbsoluteNames == null ? null : untaggedPortAbsoluteNames;
    }

    public static List<String> getUnTaggedPortIfNames(VlanIfDto vlanif) {
        if (vlanif == null) {
            return null;
        }
        List<String> untaggedPortIfNames = new ArrayList<String>();
        Set<PortDto> untaggedPorts = vlanif.getUntaggedVlans();
        for (PortDto untaggedPort : untaggedPorts) {
            untaggedPortIfNames.add(untaggedPort.getIfname());
        }
        return untaggedPortIfNames == null ? null : untaggedPortIfNames;
    }

    public static String getStormControlBroadcastLevel(PortDto port) {
        if (port == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.STORMCONTROL_BROADCAST_LEVEL);
    }

    public static List<String> getStormControlActions(PortDto port) {
        if (port == null) {
            return null;
        }
        return DtoUtil.getStringList(port, MPLSNMS_ATTR.STORMCONTROL_ACTION);
    }

    public static String getIpSubnetNamespace(PortDto port) {
        if (port == null) return null;

        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip == null) return null;

        IpSubnetAddressDto address = ip.getSubnetAddress();
        if (address == null) return null;

        String subnetName = address.getParent().getName();
        return subnetName == null ? null : subnetName;
    }


    public static String getResourcePermission(PortDto port) {
        return DtoUtil.getStringOrNull(port, "resource_permission");
    }
}