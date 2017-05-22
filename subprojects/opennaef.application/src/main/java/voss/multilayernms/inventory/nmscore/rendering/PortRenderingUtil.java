package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.PortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;
import voss.nms.inventory.util.NodeUtil;

public class PortRenderingUtil extends RenderingUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PortRenderingUtil.class);

    public static String rendering(PortDto port, String field) {
        return rendering(port, field, false);
    }

    public static String rendering(PortDto port, String field, boolean forFiltering) {
        return convertNull2ZeroString(renderingStaticLengthField(port, field, forFiltering));
    }


    public static String renderingStaticLengthField(PortDto port, String field, boolean forFiltering) {
        if (field.equals("IfName")) {
            return PortRenderer.getIfName(port);
        } else if (field.equals("ConfigName")) {
            return PortRenderer.getConfigName(port);
        } else if (field.equals("Node")) {
            if (port.getAliases() != null) {
                if (port.getAliases().size() == 1) {
                    for (PortDto prt : port.getAliases()) {
                        port = prt;
                    }
                    return NodeRenderer.getNodeName(port.getNode());
                } else if (port.getAliases().size() == 0) {
                    return NodeRenderer.getNodeName(port.getNode());
                }
            } else {
                return NodeRenderer.getNodeName(port.getNode());
            }
        } else if (field.equals("IPAddress")) {
            return PortRenderer.getIpAddress(port);
        } else if (field.equals("NetMask")) {
            return PortRenderer.getSubnetMask(port);
        } else if (field.equals("IPAddressNamespace")) {
            return PortRenderer.getIpSubnetNamespace(port);
        } else if (field.equals("VLAN ID")) {
            return PortRenderer.getVlanIDByGroup(port);
        } else if (field.equals("VLAN_ID")) {
            return PortRenderer.getVlanID(port);
        } else if (field.equals("VLAN Pool")) {
            return VlanRenderer.getVlanIdPoolName(port);
        } else if (field.equals("Member VLAN ID")) {
            return PortRenderer.getMemberVlanIDOnPort(port);
        } else if (field.equals("VPI")) {
            return PortRenderer.getVpi(port);
        } else if (field.equals("VCI")) {
            return PortRenderer.getVci(port);
        } else if (field.equals("Bandwidth")) {
            if (forFiltering) {
                return PortRenderer.getRawBandwidth(port);
            } else {
                return PortRenderer.getBandwidth(port);
            }
        } else if (field.equals("Operational Status")) {
            return PortRenderer.getOperStatus(port);
        } else if (field.equals("Admin Status")) {
            return PortRenderer.getAdminStatus(port);
        } else if (field.equals("Facility Status")) {
            return PortRenderer.getFacilityStatus(port);
        } else if (field.equals("Member Ports")) {
            return PortRenderer.getMemberPorts(port);
        } else if (field.equals("Fixed RTT")) {
            return PortRenderer.getFixedRoundTripTime(port);
        } else if (field.equals("Neighbor")) {
            return PortRenderer.getNeighbor(port);
        } else if (field.equals("Port Number")) {
            return PortRenderer.getPortNumber(port);
        } else if (field.equals("description")) {
            return PortRenderer.getDescription(port);
        } else if (field.equals("Connection destination node")) {
            return PortRenderer.getP2POppositeNodeName(port);
        } else if (field.equals("Connection destination port")) {
            return PortRenderer.getP2POppositePortName(port);
        } else if (field.equals("PortMode")) {
            return PortRenderer.getPortMode(port);
        } else if (field.equals("SwitchPortMode")) {
            return PortRenderer.getSwitchPortMode(port);
        } else if (field.equals("Speed")) {
            return PortRenderer.getOperationalSpeedAsString(port);
        } else if (field.equals("AdministrativeSpeed")) {
            return PortRenderer.getAdministrativeSpeedAsString(port);
        } else if (field.equals("Duplex")) {
            return PortRenderer.getOperationalDuplex(port);
        } else if (field.equals("AdministrativeDuplex")) {
            return PortRenderer.getAdministrativeDuplex(port);
        } else if (field.equals("AutoNegotiation")) {
            return PortRenderer.getAutoNegotiation(port);
        } else if (field.equals("Notices")) {
            return PortRenderer.getNotices(port);
        } else if (field.equals("Purpose")) {
            return PortRenderer.getPurpose(port);
        } else if (field.equals("EndUser")) {
            return PortRenderer.getEndUser(port);
        } else if (field.equals("vpn prefix")) {
            if (NodeUtil.getIpOn(port) != null) {
                return PortRenderer.getVpnPrefixOnIpIf(NodeUtil.getIpOn(port));
            } else {
                return PortRenderer.getVpnPrefix(port);
            }

        }
        return "";
    }
}