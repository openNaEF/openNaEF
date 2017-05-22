package voss.multilayernms.inventory.web.link;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.serial.TdmSerialIfDto;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.nms.inventory.util.NodeUtil;

public class L3LinkUtil {

    public static PortDto getL3Neighbor(PortDto port) {
        IpIfDto thisIP = NodeUtil.getIpOn(port);
        IpSubnetDto subnet = NodeUtil.getLayer3Link(thisIP);
        if (subnet == null) {
            return null;
        }
        for (PortDto member : subnet.getMemberIpifs()) {
            if (DtoUtil.isSameMvoEntity(member, thisIP)) {
                continue;
            }
            IpIfDto ip = NodeUtil.toIpIfDto(member);
            if (ip == null) {
                continue;
            } else if (ip.getAssociatedPorts().size() == 0) {
                return null;
            } else if (ip.getAssociatedPorts().size() == 1) {
                return ip.getAssociatedPorts().iterator().next();
            } else {
                return null;
            }
        }
        return null;
    }

    public static boolean isL3LinkCapablePort(PortDto port) {
        if (port instanceof IpIfDto) {
            return !NodeUtil.isLoopback(port);
        } else if (port instanceof TdmSerialIfDto) {
            return false;
        }
        return true;
    }

    public static boolean isApproved(IpSubnetDto subnet) {
        return LinkRenderer.isLinkApproved(subnet);
    }

}