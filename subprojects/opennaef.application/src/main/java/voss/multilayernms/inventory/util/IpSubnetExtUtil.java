package voss.multilayernms.inventory.util;

import naef.dto.ip.IpSubnetDto;
import voss.multilayernms.inventory.renderer.LinkRenderer;

public class IpSubnetExtUtil {

    public static boolean isApproved(IpSubnetDto subnet) {
        if (subnet == null) {
            return false;
        }
        return LinkRenderer.isLinkApproved(subnet);
    }
}