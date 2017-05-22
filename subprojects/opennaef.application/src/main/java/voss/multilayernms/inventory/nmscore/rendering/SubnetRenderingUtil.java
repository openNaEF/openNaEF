package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.nms.inventory.util.NodeUtil;

public class SubnetRenderingUtil extends RenderingUtil {
    private final static Logger log = LoggerFactory.getLogger(SubnetRenderingUtil.class);

    public static String rendering(IpSubnetDto subnet, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(subnet, field));
    }

    public static String renderingStaticLengthField(IpSubnetDto subnet, String field) {
        if (field.equals("Network Address")) {
            return SubnetRenderer.getIpAddress(subnet);
        } else if (field.equals("Mask Length")) {
            return SubnetRenderer.getSubnetMask(subnet);
        } else if (field.equals("Subnet")) {
            return SubnetRenderer.getSubnetName(subnet);
        } else if (field.equals("VpnPrefix")) {
            return SubnetRenderer.getVpnPrefix(subnet);
        }
        return "N/A";
    }

    public static String rendering(PortDto port, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(port, field));
    }

    public static String renderingStaticLengthField(PortDto port, String field) {

        if (field.equals("SubnetIP")) {
            return PortRenderer.getIpAddress(port);
        } else if (field.equals("TargetNodeName")) {
            if (port instanceof IpIfDto) {
                if (NodeUtil.getAssociatedPort(port) == null) {
                    return "";
                } else if (NodeUtil.getAssociatedPort(port) != null) {
                    if (NodeUtil.getAssociatedPort(port).getAliases().size() == 0) {
                        return PortRenderer.getNodeName(NodeUtil.getAssociatedPort(port));
                    } else {
                        if (NodeUtil.getAssociatedPort(port).getAliases().size() > 0) {
                            for (PortDto p : NodeUtil.getAssociatedPort(port).getAliases()) {
                                return PortRenderer.getNodeName(p);
                            }
                        }
                    }
                }
            } else if (port.getAliases().size() == 0) {
                return PortRenderer.getNodeName(port);
            } else {
                if (port.getAliases().size() > 0) {
                    for (PortDto p : port.getAliases()) {
                        return PortRenderer.getNodeName(p);
                    }
                }
            }
        } else if (field.equals("TargetIfName")) {
            if (port instanceof IpIfDto) {
                if (NodeUtil.getAssociatedPort(port) == null) {
                    return "";
                }
                if (NodeUtil.getAssociatedPort(port) != null) {
                    if (NodeUtil.getAssociatedPort(port) instanceof VlanIfDto) {
                        VlanIfDto vlanif = (VlanIfDto) NodeUtil.getAssociatedPort(port);
                        return vlanif == null ? null : vlanif.getIfname();

                    } else if (NodeUtil.getAssociatedPort(port) instanceof EthPortDto) {
                        EthPortDto etherport = (EthPortDto) NodeUtil.getAssociatedPort(port);
                        return etherport.getIfname();

                    } else if (NodeUtil.getAssociatedPort(port) instanceof EthLagIfDto) {
                        EthLagIfDto ethlagport = (EthLagIfDto) NodeUtil.getAssociatedPort(port);
                        return ethlagport.getIfname();

                    } else if (NodeUtil.getAssociatedPort(port) instanceof AtmApsIfDto) {
                        AtmApsIfDto atmapsport = (AtmApsIfDto) NodeUtil.getAssociatedPort(port);
                        return atmapsport.getIfname();

                    } else if (NodeUtil.getAssociatedPort(port) instanceof AtmPortDto) {
                        AtmPortDto atmport = (AtmPortDto) NodeUtil.getAssociatedPort(port);
                        return atmport.getIfname();
                    }
                }
            }
            return PortRenderer.getIfName(port);
        }
        if (field.equals(ATTR.VPN_PREFIX)) {
            return PortRenderer.getVpnPrefix(port);
        }
        return "N/A";
    }
}