package voss.nms.inventory.builder.conditional;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import naef.ui.NaefDtoFacade;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.naming.naef.IfNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.model.VrfInstance;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.util.VrfUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VrfCreateCommands extends ConditionalCommands<VrfIfDto> {
    private static final long serialVersionUID = 1L;
    private final String vrfName;
    private final String nodeName;
    private final Map<String, String> vpnAddresses = new HashMap<String, String>();
    private final Map<String, String> addressAndMask = new HashMap<String, String>();

    public VrfCreateCommands(VrfInstance vrf, String editorName) {
        super(editorName);
        this.vrfName = vrf.getVrfID();
        this.nodeName = vrf.getDevice().getDeviceName();
    }

    public void addMemberPorts(String ifName, String ipAddress, String subnetMaskLength) {
        if (vpnAddresses.containsKey(ifName)) {
            return;
        }
        this.vpnAddresses.put(ifName, ipAddress);
        this.addressAndMask.put(ipAddress, subnetMaskLength);
    }

    @Override
    public void evaluateDiffInner(ShellCommands cmd) {
        cmd.addCommand("# built by VrfCreateCommands.");
        try {
            String ifAbsoluteName = nodeName + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_VRF_IF
                    + ATTR.NAME_DELIMITER_SECONDARY + vrfName;
            NodeDto node = NodeUtil.getNode(nodeName);
            VrfDto vrf = getNetwork();
            VrfStringIdPoolDto pool = getIdPool();
            if (vrf == null) {
                InventoryBuilder.changeContext(cmd, pool);
                InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_VRF,
                        ATTR.ATTR_VRF_ID_STRING, vrfName,
                        ATTR.ATTR_VRF_POOL_STRING, pool.getName());
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VRF_NAME, vrfName);
                cmd.addLastEditCommands();
                recordChange(MPLSNMS_ATTR.VRF_NAME, null, vrfName);
            }
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
            VrfIfDto vrfIf = VrfUtil.getVrfIf(node, vrfName);
            if (vrfIf == null) {
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_VRF_IF, vrfName);
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, vrfName);
                cmd.addLastEditCommands();
            }
            InventoryBuilder.changeContext(cmd, pool, ATTR.NETWORK_TYPE_ID, vrfName);
            cmd.addLastEditCommands();
            InventoryBuilder.buildBindPortToNetworkCommands(cmd, ifAbsoluteName);

            NaefDtoFacade facade = InventoryConnector.getInstance().getDtoFacade();
            List<PortDto> addedPorts = getAddedPortsByIfName(this.vpnAddresses.keySet(), node, facade);
            InventoryBuilder.changeContext(cmd, ifAbsoluteName);
            for (PortDto added : addedPorts) {
                InventoryBuilder.buildConnectPortToNetworkIfCommands(cmd, ifAbsoluteName, added.getAbsoluteName());
                String ifName = DtoUtil.getStringOrNull(added, MPLSNMS_ATTR.IFNAME);
                recordChange("attachment port", null, ifName);
                String vpnIpAddress = this.vpnAddresses.get(ifName);
                if (vpnIpAddress == null) {
                    continue;
                }
                IpIfDto vpnIpIf = NodeUtil.getVpnIpIfByIfName(vrfIf, vpnIpAddress);
                if (vpnIpIf == null) {
                    InventoryBuilder.changeContext(cmd, AbsoluteNameFactory.getVrfIfAbsoluteName(nodeName, vrfName));
                    SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, vpnIpAddress);
                    String ipifVariableName = "IPIF_" + Integer.toHexString(System.identityHashCode(this));
                    InventoryBuilder.assignVar(cmd, ipifVariableName);
                    InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, IfNameFactory.getVpnIpIfIfName(vrfName, vpnIpAddress));
                    InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IP_ADDRESS, vpnIpAddress);
                    String subnetMask = this.addressAndMask.get(vpnIpAddress);
                    InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MASK_LENGTH, subnetMask);
                    InventoryBuilder.changeContext(cmd, added);
                    InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, "$;" + ipifVariableName);
                } else {
                    InventoryBuilder.changeContext(cmd, added);
                    InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, vpnIpIf.getAbsoluteName());
                }
            }
            clearAssertions();
            addAssertion(pool);
            addAssertion(getNetwork());
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    protected VrfDto getNetwork() throws IOException, InventoryException, ExternalServiceException {
        VrfStringIdPoolDto targetPool = getIdPool();
        if (targetPool == null) {
            return null;
        }
        for (VrfDto vrf : targetPool.getUsers()) {
            if (vrf.getStringId().equals(this.vrfName)) {
                return vrf;
            }
        }
        return null;
    }

    protected VrfStringIdPoolDto getIdPool() throws IOException, InventoryException, ExternalServiceException {
        String vrfDomainName = NetworkDiffUtil.getPolicy().getDefaultVrfPoolName();
        NaefDtoFacade facade = InventoryConnector.getInstance().getDtoFacade();
        Set<VrfStringIdPoolDto> pools = facade.getRootIdPools(VrfStringIdPoolDto.class);
        VrfStringIdPoolDto targetPool = null;
        for (VrfStringIdPoolDto pool : pools) {
            if (pool.getName().equals(vrfDomainName)) {
                targetPool = pool;
                break;
            }
        }
        return targetPool;
    }
}