package voss.multilayernms.inventory.renderer;

import naef.dto.PortDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.util.SubnetListUtil;
import voss.nms.inventory.database.InventoryConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubnetRenderer extends GenericRenderer {
    static Logger log = LoggerFactory.getLogger(SubnetRenderer.class);

    public static String getIpAddress(IpSubnetDto subnet) {
        return getSubnetAddress(subnet) == null ? null : getSubnetAddress(subnet).getAddress().toString();
    }

    public static String getSubnetMask(IpSubnetDto subnet) {
        Integer subnetmask = getSubnetAddress(subnet) == null ? null : getSubnetAddress(subnet).getSubnetMask();
        return subnetmask == null ? null : subnetmask.toString();
    }

    private static IpSubnetAddressDto getSubnetAddress(IpSubnetDto subnet) {
        IpSubnetAddressDto address = subnet.getSubnetAddress();
        return address == null || address.getIdRanges().size() == 0 ? null : address;
    }

    public static String getNetworkAddress(PortDto port) {
        return SubnetListUtil.getNetworkAddress(port);
    }

    public static String getSubnetName(IpSubnetDto subnet) {
        IpSubnetAddressDto address = subnet.getSubnetAddress();
        String subnetName = address.getParent().getName();
        return subnetName == null ? "" : subnetName;
    }

    public static String getVpnPrefix(IpSubnetDto subnet) {
        return DtoUtil.getStringOrNull(subnet, ATTR.VPN_PREFIX);
    }

    public static List<IpSubnetNamespaceDto> getAllIpSubnetNamespace() throws ExternalServiceException {
        List<IpSubnetNamespaceDto> ipSubnetNses = new ArrayList<IpSubnetNamespaceDto>();
        InventoryConnector conn;
        List<IpSubnetNamespaceDto> ipSubnetNamespaces = null;
        try {
            conn = InventoryConnector.getInstance();
            ipSubnetNamespaces = conn.getActiveRootIpSubnetNamespaces();
        } catch (IOException e) {
            log.error(e.toString());
        }
        for (IpSubnetNamespaceDto ipSubnetNamespace : ipSubnetNamespaces) {
            if (ipSubnetNamespace.getName().equals(ModelConstant.IP_SUBNET_NAMESPACE_TRASH_NAME)) {
                continue;
            } else {
                ipSubnetNses.add(ipSubnetNamespace);
                if (ipSubnetNamespace.getChildren().size() > 0) {
                    for (IpSubnetNamespaceDto ipSubnetNseschrd : ipSubnetNamespace.getChildren()) {
                        ipSubnetNses.add(ipSubnetNseschrd);
                        if (ipSubnetNseschrd.getChildren().size() > 0) {
                            for (IpSubnetNamespaceDto ipSubnetNseschrd2nd : ipSubnetNseschrd.getChildren()) {
                                ipSubnetNses.add(ipSubnetNseschrd2nd);
                            }
                        }
                    }
                }
            }
        }

        return ipSubnetNses;
    }


}