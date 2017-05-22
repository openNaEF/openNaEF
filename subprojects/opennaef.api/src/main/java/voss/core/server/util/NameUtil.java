package voss.core.server.util;

import naef.dto.*;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto;
import voss.core.server.database.ATTR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NameUtil {
    private static final Logger log = LoggerFactory.getLogger(NameUtil.class);

    public static String getName(EntityDto dto) {
        return getName(dto, false);
    }

    public static String getName(EntityDto dto, boolean outputClassName) {
        if (dto instanceof NaefDto) {
            NaefDto naef = (NaefDto) dto;
            String name = getCaption((NaefDto) dto);
            if (outputClassName) {
                name = name + " (" + naef.getObjectTypeName() + ")";
            }
            return name;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(dto.getClass().getName());
            sb.append("[");
            for (String attrName : dto.getAttributeNames()) {
                String value = DtoUtil.getStringOrNull(dto, attrName);
                sb.append(value).append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static String getCaption(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto instanceof NodeDto) {
            return ((NodeDto) dto).getName();
        } else if (dto instanceof NodeElementDto) {
            NodeElementDto ne = (NodeElementDto) dto;
            return ne.getNode().getName() + ":" + getNodeElementCaption(ne);
        } else if (dto instanceof LinkDto) {
            return getLinkCaption((LinkDto) dto);
        } else if (dto instanceof NetworkDto) {
            NetworkDto network = (NetworkDto) dto;
            return getResourceId(network) + "(" + getPoolName(network) + ")";
        } else if (dto instanceof LocationDto) {
            return "Location " + ((LocationDto) dto).getName();
        } else {
            return "Unknown:" + dto.toString();
        }
    }

    public static String getRelativeCaption(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto instanceof NodeDto) {
            return null;
        } else if (dto instanceof NodeElementDto) {
            NodeElementDto ne = (NodeElementDto) dto;
            return getNodeElementCaption(ne);
        } else if (dto instanceof PseudowireDto) {
            PseudowireDto pw = (PseudowireDto) dto;
            return "VC ID Pool " + getPoolName(pw) + ":" + pw.getLongId();
        } else if (dto instanceof VrfDto) {
            VrfDto vrf = (VrfDto) dto;
            return "VRF ID Pool " + getPoolName(vrf) + ":" + vrf.getIntegerId();
        } else if (dto instanceof VplsDto) {
            VplsDto vpls = (VplsDto) dto;
            return "VPLS ID Pool " + getPoolName(vpls) + ":" + vpls.getIntegerId();
        } else if (dto instanceof VlanDto) {
            VlanDto vlan = (VlanDto) dto;
            return "VLAN ID Pool " + getPoolName(vlan) + ":" + vlan.getVlanId();
        } else if (dto instanceof LocationDto) {
            return "Location " + ((LocationDto) dto).getName();
        } else {
            return "Unknown" + dto.toString();
        }
    }

    public static String getPoolName(NetworkDto network) {
        if (network == null) {
            return "null.";
        } else if (network instanceof PseudowireDto) {
            if (((PseudowireDto) network).getLongIdPool() == null) {
                return "no pool.";
            }
            return ((PseudowireDto) network).getLongIdPool().getName();
        } else if (network instanceof VrfDto) {
            if (((VrfDto) network).getIntegerIdPool() == null) {
                return "no pool.";
            }
            return ((VrfDto) network).getIntegerIdPool().getName();
        } else if (network instanceof VplsDto) {
            if (((VplsDto) network).getIntegerIdPool() == null) {
                return "no pool.";
            }
            return ((VplsDto) network).getIntegerIdPool().getName();
        } else if (network instanceof VlanDto) {
            if (((VlanDto) network).getIdPool() == null) {
                return "no pool.";
            }
            return ((VlanDto) network).getIdPool().getName();
        } else if (network instanceof RsvpLspDto) {
            if (((RsvpLspDto) network).getIdPool() == null) {
                return "no pool.";
            }
            return ((RsvpLspDto) network).getIdPool().getName();
        } else if (network instanceof RsvpLspHopSeriesDto) {
            if (((RsvpLspHopSeriesDto) network).getIdPool() == null) {
                return "no pool.";
            }
            return ((RsvpLspHopSeriesDto) network).getIdPool().getName();
        } else if (network instanceof PathHopDto) {
            return "-";
        } else if (network instanceof L2LinkDto) {
            return "-";
        } else if (network instanceof VlanSegmentDto) {
            return "-";
        } else if (network instanceof IpSubnetDto) {
            if (((IpSubnetDto) network).getNamespace() == null) {
                return "no pool.";
            }
            return ((IpSubnetDto) network).getNamespace().getName();
        } else {
            throw new IllegalStateException("unknown type: (" + network.getClass().getName() + ")" + network.getAbsoluteName());
        }
    }

    public static String getResourceId(NetworkDto network) {
        if (network instanceof PseudowireDto) {
            PseudowireDto pw = (PseudowireDto) network;
            if (pw.getLongId() != null) {
                return "PseudoWire:" + pw.getLongId().toString();
            } else if (pw.getStringId() != null) {
                return "PseudoWire:" + pw.getStringId().toString();
            } else {
                throw new IllegalStateException("no id on pseudowire: " + pw.getAbsoluteName());
            }
        } else if (network instanceof VrfDto) {
            VrfDto vrf = (VrfDto) network;
            if (vrf.getIntegerId() != null) {
                return "VRF:" + vrf.getIntegerId().toString();
            } else if (vrf.getStringId() != null) {
                return "VRF:" + vrf.getStringId().toString();
            } else {
                throw new IllegalStateException("no id on vrf: " + vrf.getAbsoluteName());
            }
        } else if (network instanceof VplsDto) {
            VplsDto vpls = (VplsDto) network;
            if (vpls.getIntegerId() != null) {
                return "VPLS:" + vpls.getIntegerId().toString();
            } else if (vpls.getStringId() != null) {
                return "VPLS:" + vpls.getStringId().toString();
            } else {
                throw new IllegalStateException("no id on vpls: " + vpls.getAbsoluteName());
            }
        } else if (network instanceof VlanDto) {
            return "VLAN:" + ((VlanDto) network).getVlanId().toString();
        } else if (network instanceof RsvpLspDto) {
            return "RSVP-LSP:" + ((RsvpLspDto) network).getName();
        } else if (network instanceof RsvpLspHopSeriesDto) {
            return "Path:" + ((RsvpLspHopSeriesDto) network).getName();
        } else if (network instanceof PathHopDto) {
            return "PathHop:" + ((PathHopDto) network).getAbsoluteName();
        } else if (network instanceof IpSubnetDto) {
            return "IpSubnet" + ((IpSubnetDto) network).getSubnetName();
        } else if (network instanceof L2LinkDto) {
            return "L2Link:" + ((L2LinkDto) network).getAbsoluteName();
        } else if (network instanceof VlanSegmentDto) {
            return "VlanSegment:" + ((VlanSegmentDto) network).getAbsoluteName();
        } else {
            throw new IllegalStateException("unknown type:" + network.getAbsoluteName());
        }
    }

    public static String getNodeElementCaption(NodeElementDto nodeElement) {
        if (nodeElement instanceof SlotDto) {
            SlotDto slot = (SlotDto) nodeElement;
            return "Slot " + getIfName(slot);
        } else if (nodeElement instanceof ModuleDto) {
            ModuleDto slot = (ModuleDto) nodeElement;
            return "Module " + getIfName(slot);
        } else if (nodeElement instanceof JackDto) {
            JackDto jack = (JackDto) nodeElement;
            return "Port " + getIfName(jack);
        } else if (nodeElement instanceof AtmPvcIfDto) {
            return "ATM PVC " + getIfName(nodeElement);
        } else if (nodeElement instanceof FrPvcIfDto) {
            return "FR PVC " + getIfName(nodeElement);
        } else if (nodeElement instanceof VlanIfDto) {
            return "VLAN " + ((VlanIfDto) nodeElement).getVlanId();
        } else if (nodeElement instanceof VrfIfDto) {
            return "VRF " + ((VrfIfDto) nodeElement).getVrfId();
        } else if (nodeElement instanceof VplsIfDto) {
            return "VPLS " + ((VplsIfDto) nodeElement).getVplsId();
        } else if (nodeElement instanceof EthLagIfDto) {
            return "LAG " + ((EthLagIfDto) nodeElement).getName();
        } else if (nodeElement instanceof PortDto) {
            PortDto port = (PortDto) nodeElement;
            return "Port " + getIfName(port);
        }
        return null;
    }

    public static String getNodeLocalCaptionOf(PortDto port) {
        if (port == null) {
            return null;
        }
        NodeElementDto owner = port.getOwner();
        if (owner != null && owner instanceof JackDto) {
            JackDto jack = (JackDto) owner;
            return getIfName(jack);
        }
        return getIfName(port);
    }

    public static String getNetworkPoolName(NetworkDto network) {
        if (network == null) {
            return null;
        }
        if (network instanceof PseudowireDto) {
            PseudowireDto pw = (PseudowireDto) network;
            return "PW ID Pool:" + pw.getLongIdPool().getName();
        } else if (network instanceof VrfDto) {
            return "VRF ID Pool:" + ((VrfDto) network).getIntegerIdPool().getName();
        } else if (network instanceof VplsDto) {
            return "VPLS ID Pool:" + ((VplsDto) network).getIntegerIdPool().getName();
        } else if (network instanceof VlanDto) {
            return "VLAN ID Pool:" + ((VlanDto) network).getIdPool().getName();
        } else {
            return "Unknown:" + network.getAbsoluteName();
        }
    }

    public static String getNetworkName(NetworkDto network) {
        if (network == null) {
            return null;
        }
        if (network instanceof PseudowireDto) {
            PseudowireDto pw = (PseudowireDto) network;
            return pw.getLongId().toString();
        } else if (network instanceof VrfDto) {
            return ((VrfDto) network).getIntegerId().toString();
        } else if (network instanceof VplsDto) {
            return ((VplsDto) network).getIntegerId().toString();
        } else if (network instanceof VlanDto) {
            return ((VlanDto) network).getVlanId().toString();
        } else {
            return "Unknown:" + network.getAbsoluteName();
        }
    }

    public static String getNodeIfName(NodeElementDto element) {
        if (element == null) {
            return null;
        }
        return element.getNode().getName() + ":" + getIfName(element);
    }

    public static String getIfName(NodeElementDto element) {
        if (element == null) {
            return null;
        }

        if (element instanceof PortDto && VlanUtil.isBridgePort((PortDto) element)) {
            element = element.getOwner();
        }
        log.trace("getIfName(): element=" + element.getAbsoluteName());
        String customIfName = getCustomIfName(element);
        if (customIfName != null) {
            return customIfName;
        }
        return getDefaultIfName(element);
    }

    public static String getCustomIfName(NodeElementDto dto) {
        String ifName = DtoUtil.getStringOrNull(dto, DtoUtil.getIfNameAttributeName());
        if (ifName != null) {
            return ifName;
        }
        String hardPortName = getHardPortIfName(dto);
        if (hardPortName != null) {
            ifName = hardPortName;
        }
        String suffix = getSuffixIfName(dto);
        if (suffix != null) {
            if (ifName != null) {
                ifName = ifName + suffix;
            } else {
                ifName = suffix;
            }
        }
        ifName = Util.removeSlashes(ifName);
        return ifName;
    }

    public static String getHardPortIfName(NodeElementDto dto) {
        String ifName = null;
        NodeElementDto parent = dto;
        while (parent != null) {
            if (parent instanceof HardPortDto) {
                ifName = DtoUtil.getStringOrNull(parent, DtoUtil.getIfNameAttributeName());
                break;
            }
            if (parent instanceof NodeDto) {
                break;
            }
            parent = parent.getOwner();
        }
        return Util.stringToNull(ifName);
    }

    public static String getIfNameForSort(NodeElementDto element) {
        if (element == null) {
            return null;
        }

        if (element instanceof PortDto && VlanUtil.isBridgePort((PortDto) element)) {
            element = element.getOwner();
        }
        log.trace("getIfName(): element=" + element.getAbsoluteName());
        String customIfName = DtoUtil.getStringOrNull(element, DtoUtil.getIfNameAttributeName());
        if (customIfName != null) {
            return customIfName;
        }
        if (element instanceof AtmPvpIfDto) {
            AtmPvpIfDto pvp = (AtmPvpIfDto) element;
            PortDto atm = pvp.getPhysicalPort();
            String suffix = DtoUtil.getStringOrNull(pvp, "suffix");
            if (suffix != null) {
                return getIfName(atm) + "/-1/" + suffix;
            }
            String vpi = (pvp.getVpi() != null ? pvp.getVpi().toString() : pvp.getName());
            return getIfName(atm) + "/-1/" + vpi;
        } else if (element instanceof AtmPvcIfDto) {
            AtmPvcIfDto pvc = (AtmPvcIfDto) element;
            PortDto atm = pvc.getPhysicalPort();
            String suffix = DtoUtil.getStringOrNull(pvc, "suffix");
            if (suffix != null) {
                return getIfName(atm) + "/1/" + suffix;
            }
            String vpi = (pvc.getVpi() != null ? pvc.getVpi().toString() : pvc.getName());
            String vci = (pvc.getVci() != null ? pvc.getVci().toString() : pvc.getName());
            return getIfName(atm) + "/" + vpi + "/" + vci;
        }
        return getIfName(element);
    }

    public static String getNodeIfName(PortDto port) {
        if (port == null) {
            return null;
        }
        return port.getNode().getName() + ":" + getIfName(port);
    }

    public static String getDefaultIfName(NodeElementDto dto) {
        String ifName = null;
        String hardPortName = getDefaultHardPortIfName(dto);
        if (hardPortName != null) {
            ifName = hardPortName;
        }
        String suffix = getSuffixIfName(dto);
        if (suffix != null) {
            if (ifName != null) {
                ifName = ifName + suffix;
            } else {
                ifName = suffix;
            }
        }
        ifName = Util.removeSlashes(ifName);
        return ifName;
    }

    public static String getDefaultHardPortIfName(NodeElementDto dto) {
        String ifName = null;
        NodeElementDto parent = dto;
        while (parent != null) {
            if (parent instanceof HardPortDto) {
                ifName = getParentHardPortIfName((HardPortDto) parent);
                break;
            }
            if (parent instanceof NodeDto) {
                break;
            }
            parent = parent.getOwner();
        }
        return Util.stringToNull(ifName);
    }

    public static String getSuffixIfName(NodeElementDto dto) {
        String suffix = DtoUtil.getStringOrNull(dto, ATTR.SUFFIX);
        if (suffix != null) {
            return "." + suffix;
        }

        String name = null;
        NodeElementDto parent = dto;
        while (parent != null) {
            if (parent instanceof HardPortDto) {
                if (Util.stringToNull(name) != null) {
                    return "/" + name;
                }
                break;
            }
            if (parent instanceof NodeDto) {
                break;
            }
            String elementName = parent.getName();
            if (elementName != null) {
                if (Util.stringToNull(name) == null) {
                    name = elementName;
                } else {
                    name = elementName + "/" + name;
                }
            } else {
            }
            parent = parent.getOwner();
        }
        return Util.stringToNull(name);
    }

    public static String getParentHardPortIfName(HardPortDto hardPort) {
        StringBuilder sb = new StringBuilder();
        buildIfName(sb, hardPort);
        String ifName = sb.toString();
        ifName = Util.removeSlashes(ifName);
        String prefix = DtoUtil.getStringOrNull(hardPort, "Prefix");
        if (prefix != null) {
            ifName = prefix + ifName;
        }
        return ifName;
    }

    private static void buildIfName(StringBuilder sb, NodeElementDto dto) {
        if (dto == null) {
            return;
        }
        NodeElementDto owner = dto.getOwner();
        if (owner != null && !(owner instanceof NodeDto)) {
            buildIfName(sb, owner);
        }
        sb.append("/").append(dto.getName());
    }

    public static String getLinkName(NetworkDto link) {
        if (link == null) {
            return null;
        }
        if (VlanSegmentDto.class.isInstance(link)) {
            for (PortDto member : link.getMemberPorts()) {
                if (member != null) {
                    return getNodeElementCaption(member);
                }
            }
            return "Anonymous VLAN Link";
        }
        return link.getAbsoluteName();
    }

    public static String getLinkCaption(NetworkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() == 0) {
            return "Anonymous Link";
        }
        StringBuilder sb = new StringBuilder();
        for (PortDto port : link.getMemberPorts()) {
            if (sb.length() > 0) {
                sb.append("--");
            }
            sb.append(getCaption(port));
        }
        return sb.toString();
    }

    public static String getLinkUsage(LinkDto link) {
        if (link == null) {
            return null;
        }
        log.debug("link=" + link.getShellClassId() + ", " + link.getAbsoluteName());
        List<PortDto> ports = new ArrayList<PortDto>();
        for (PortDto p : link.getMemberPorts()) {
            ports.add(p);
        }
        Collections.sort(ports, new TypeBasedPortComparator());
        List<String> captions = new ArrayList<String>();
        if (link.getUpperLayers() == null) {
            return null;
        }
        for (NetworkDto upper : link.getUpperLayers()) {
            log.debug("upperLayerLink: " + upper.getShellClassId() + ", " + upper.getAbsoluteName());
            String caption = NameUtil.getLinkCaption(upper);
            if (!captions.contains(caption)) {
                captions.add(caption);
            }
        }
        Collections.sort(captions);
        StringBuilder sb = new StringBuilder();
        for (String caption : captions) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(caption);
        }
        if (sb.length() == 0) {
            return null;
        } else {
            return sb.toString();
        }
    }
}