package voss.core.server.util;

import naef.dto.LinkDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanLinkDto;
import naef.dto.vlan.VlanSegmentDto;
import voss.core.server.database.ATTR;

public class LinkCoreUtil {

    public static String getLinkType(LinkDto link) {
        if (link == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(link, ATTR.LINK_TYPE);
    }

    public static VlanLinkDto getVlanLink(VlanIfDto vif1, VlanIfDto vif2) {
        if (vif1 == null || vif2 == null) {
            return null;
        }
        NEXT_NETWORK:
        for (VlanSegmentDto vlanLink : vif1.getVlanLinks()) {
            if (!VlanLinkDto.class.isInstance(vlanLink)) {
                continue;
            }
            for (PortDto port : vlanLink.getMemberPorts()) {
                if (DtoUtil.mvoEquals(port, vif1)) {
                    continue;
                } else if (DtoUtil.mvoEquals(port, vif2)) {
                    continue;
                } else {
                    continue NEXT_NETWORK;
                }
            }
            return (VlanLinkDto) vlanLink;
        }
        return null;
    }
}