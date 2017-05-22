package voss.multilayernms.inventory.renderer;

import naef.dto.CustomerInfoDto;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.ModelConstant;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.VlanUtil;
import voss.multilayernms.inventory.constants.CustomerConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VlanRenderer extends GenericRenderer {
    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(VlanRenderer.class);

    public static List<VlanIdPoolDto> getVlanIdPools() throws ExternalServiceException {
        List<VlanIdPoolDto> vlanPools = new ArrayList<VlanIdPoolDto>();
        List<VlanIdPoolDto> pools = VlanUtil.getAllVlanPools();
        for (VlanIdPoolDto pool : pools) {
            if (!pool.getName().matches(ModelConstant.VLAN_POOL_TRASH_NAME)) {
                vlanPools.add(pool);
            }
        }
        return vlanPools;
    }

    public static List<String> getVlanIdPoolsName() throws ExternalServiceException {
        List<String> vlanPools = new ArrayList<String>();
        List<VlanIdPoolDto> pools = VlanUtil.getAllVlanPools();
        for (VlanIdPoolDto pool : pools) {
            if (!pool.getName().matches(ModelConstant.VLAN_POOL_TRASH_NAME)) {
                vlanPools.add(pool.getName());
            }
        }
        return vlanPools;
    }

    public static String getVlanIdPoolName(VlanDto vlan) {
        if (vlan == null) {
            return null;
        }
        return vlan.getIdPool().getName();
    }

    public static String getVlanIdPoolName(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof VlanIfDto) {
            return VlanUtil.getVlan((VlanIfDto) port).getIdPool().getName();
        }
        return null;
    }


    public static String getVlanIdPoolName(VlanIfDto vlanif) {
        if (vlanif == null) {
            return null;
        }
        return VlanUtil.getVlan(vlanif).getIdPool().getName();
    }

    public static String getVlanId(VlanDto vlan) {
        return vlan.getVlanId().toString();
    }

    public static String getVlanMvoId(VlanDto vlan) {
        return DtoUtil.getMvoId(vlan).toString();
    }

    public static String getSubnetAddress(VlanDto vlan) {
        StringBuilder result = new StringBuilder();
        for (NetworkDto upperNet : vlan.getUpperLayers()) {
            if (!(upperNet instanceof IpSubnetDto)) {
                continue;
            }
            IpSubnetDto subnet = (IpSubnetDto) upperNet;
            IpSubnetAddressDto address = subnet.getSubnetAddress();
            if (address.getIdRanges().size() == 0) {
                continue;
            }

            result.append(result.length() == 0 ? "" : ", ");
            result.append(address.getAddress().toString() + "/" + address.getSubnetMask());
        }
        return result.toString();
    }

    public static String getSubnetName(VlanDto vlan) {
        for (NetworkDto upperNet : vlan.getUpperLayers()) {
            if (upperNet instanceof IpSubnetDto) {
                IpSubnetDto subnet = (IpSubnetDto) upperNet;
                IpSubnetAddressDto address = subnet.getSubnetAddress();
                String subnetName = address.getParent().getName();
                if (!subnetName.equalsIgnoreCase(ModelConstant.IP_SUBNET_ADDRESS_TRASH_NAME)) {
                    return subnetName == null ? "" : subnetName;
                }
            }
        }
        return "";
    }

    public static String getUser(VlanDto vlan) {
        StringBuffer sb = new StringBuffer();
        for (CustomerInfoDto cs : vlan.getCustomerInfos()) {
            sb.append(sb.length() == 0 ? "" : ",");
            sb.append(CustomerInfoRenderer.getCustomerInfoId(cs));
        }
        return sb.toString();
    }

    public static String getNotice(VlanDto vlan) {
        return DtoUtil.getStringOrNull(vlan, CustomerConstants.NOTICES);
    }

    public static String getParentIfName(PortDto port) {
        if (port == null) {
            return null;
        }
        if (VlanUtil.isSwitchVlanIf((VlanIfDto) port)) {
            return "N/A";
        } else if (VlanUtil.isRouterVlanIf(port)) {
            return VlanUtil.getParentPort((VlanIfDto) port).getIfname();
        }
        return "N/A";
    }

    public static String getConnectedTaggedPort(PortDto port) {
        if (port == null) {
            return null;
        }
        VlanIfDto vlanif = (VlanIfDto) port;
        StringBuilder sb = new StringBuilder();
        Set<PortDto> connectedTaggedPorts = vlanif.getTaggedVlans();
        for (PortDto connectedTaggedPort : connectedTaggedPorts) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(connectedTaggedPort.getIfname());
        }
        return sb.length() == 0 ? "N/A" : sb.toString();
    }

    public static String getConnectedUnTaggedPort(PortDto port) {
        if (port == null) {
            return null;
        }
        VlanIfDto vlanif = (VlanIfDto) port;
        StringBuilder sb = new StringBuilder();
        Set<PortDto> connectedUnTaggedPorts = vlanif.getUntaggedVlans();
        for (PortDto connectedUnTaggedPort : connectedUnTaggedPorts) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(connectedUnTaggedPort.getIfname());
        }
        return sb.length() == 0 ? "N/A" : sb.toString();
    }

    public static String getAreaCode(VlanDto vlan) {
        return DtoUtil.getStringOrNull(vlan, CustomerConstants.AREA_CODE);
    }

    public static String getUserCode(VlanDto vlan) {
        return DtoUtil.getStringOrNull(vlan, CustomerConstants.USER_CODE);
    }
}