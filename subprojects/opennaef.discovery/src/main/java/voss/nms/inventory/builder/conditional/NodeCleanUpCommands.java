package voss.nms.inventory.builder.conditional;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpIf;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.builder.SimpleNodeBuilder;

import java.util.Collection;
import java.util.List;

public class NodeCleanUpCommands extends ConditionalCommands<NodeDto> {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;

    public NodeCleanUpCommands(NodeDto dto, String editorName) {
        super(dto, editorName);
        this.node = dto;
    }

    @Override
    protected void evaluateDiffInner(ShellCommands cmd) {
        if (node == null) {
            return;
        }
        try {
            InventoryBuilder.changeContext(cmd, node);
            cmd.addLastEditCommands();
            List<IpIfDto> ipIfs = DtoUtil.getNaefDtoFacade(node).getSubElements(node, IpIf.class, IpIfDto.class);
            for (IpIfDto ipIf : ipIfs) {
                if (NodeUtil.isLoopback(ipIf)) {
                    continue;
                }
                Collection<PortDto> ports = ipIf.getAssociatedPorts();
                if (ports != null && ports.size() > 0) {
                    continue;
                }
                NetworkDto unexpected = getUnexpectedUser(ipIf);
                if (unexpected != null) {
                    LoggerFactory.getLogger(NodeCleanUpCommands.class).debug("ip-if("
                            + DtoUtil.toDebugString(ipIf) + ") is used by network: " +
                            DtoUtil.toDebugString(unexpected));
                    continue;
                }
                unassociateNetwork(cmd, ipIf);
                InventoryBuilder.changeContext(cmd, ipIf.getOwner());
                SimpleNodeBuilder.buildPortDeletionCommands(cmd, ipIf);
            }
            clearAssertions();
            addAssertion(node);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private NetworkDto getUnexpectedUser(IpIfDto ipIf) {
        for (NetworkDto network : ipIf.getNetworks()) {
            if (IpSubnetDto.class.isInstance(network)) {
                continue;
            } else if (IpSubnetNamespaceDto.class.isInstance(network)) {
                continue;
            } else if (IpSubnetAddressDto.class.isInstance(network)) {
                continue;
            } else {
                return network;
            }
        }
        return null;
    }

    private void unassociateNetwork(ShellCommands cmd, IpIfDto ipIf) {
        if (ipIf.getSubnet() != null) {
            IpSubnetDto subnet = ipIf.getSubnet();
            InventoryBuilder.changeContext(cmd, subnet);
            InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, ipIf);
        }
        if (ipIf.getSubnetAddress() != null) {
            InventoryBuilder.changeContext(cmd, ipIf);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_IP_ADDRESS, null);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, null);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_SUBNETMASK_LENGTH, null);
        }
    }
}