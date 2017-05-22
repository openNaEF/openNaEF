package voss.nms.inventory.util;

import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeElementComparator;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IpSubnetUtil {

    public static IpSubnetDto getIpSubnet(PortDto port) {
        for (NetworkDto network : port.getNetworks()) {
            if (network instanceof IpSubnetDto) {
                return (IpSubnetDto) network;
            }
        }
        IpIfDto ip = NodeUtil.getIpOn(port);
        if (ip != null) {
            return getIpSubnet(ip);
        }
        return null;
    }

    public static IpSubnetDto getIpSubnet(PortDto port1, PortDto port2) {
        IpSubnetDto s1 = getIpSubnet(port1);
        IpSubnetDto s2 = getIpSubnet(port2);
        if (DtoUtil.isSameMvoEntity(s1, s2)) {
            return s1;
        }
        return null;
    }

    public static IpSubnetDto getIpSubnetDto(String id) throws InventoryException, ExternalServiceException, IOException {
        Set<IpSubnetNamespaceDto> pools = InventoryConnector.getInstance().
                getDtoFacade().getRootIdPools(IpSubnetNamespaceDto.class);
        for (IpSubnetNamespaceDto pool : pools) {
            for (IpSubnetDto user : pool.getUsers()) {
                if (id == null && user.getSubnetName() == null) {
                    return user;
                } else if (id != null && id.equals(user.getSubnetName())) {
                    return user;
                }
            }
        }
        return null;
    }

    public static List<PortDto> getMemberPorts(IpSubnetDto subnet) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (subnet == null) {
            return result;
        }
        for (PortDto member : subnet.getMemberIpifs()) {
            IpIfDto ipIf = NodeUtil.toIpIfDto(member);
            if (ipIf == null) {
                continue;
            }
            if (NodeUtil.isLoopback(ipIf)) {
                continue;
            }
            if (ipIf.getAssociatedPorts() == null || ipIf.getAssociatedPorts().size() == 0) {
                continue;
            }
            result.addAll(ipIf.getAssociatedPorts());
        }
        Collections.sort(result, new NodeElementComparator());
        return result;
    }
}