package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.MvoDtoSet;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SimpleIpSubnetCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final IpSubnetNamespaceDto pool;
    private final IpSubnetDto subnet;
    private String id;
    private String originalId;
    private final MvoDtoSet<IpIfDto> ports = new MvoDtoSet<IpIfDto>();

    public SimpleIpSubnetCommandBuilder(IpSubnetDto target, String editorName) {
        super(IpSubnetDto.class, target, editorName);
        if (target == null) {
            throw new IllegalArgumentException("");
        }
        setConstraint(IpSubnetDto.class);
        this.id = target.getSubnetName();
        this.subnet = target;
        for (PortDto member : subnet.getMemberIpifs()) {
            this.ports.add((IpIfDto) member);
        }
        this.cmd.addVersionCheckTarget(subnet);
        this.pool = this.subnet.getNamespace();
    }

    public SimpleIpSubnetCommandBuilder(String id, IpSubnetNamespaceDto pool, String editorName) {
        super(IpSubnetDto.class, null, editorName);
        if (id == null) {
            throw new IllegalArgumentException("no id.");
        } else if (pool == null) {
            throw new IllegalArgumentException("no pool.");
        }
        setConstraint(IpSubnetDto.class);
        this.subnet = null;
        this.pool = pool;
        this.id = id;
        recordChange("IP Subnet", null, id);
    }

    public void addPort(IpIfDto port) {
        if (this.ports.contains(port)) {
            return;
        }
        this.ports.add(port);
        recordChange("member-port", null, NameUtil.getNodeIfName(port));
    }

    public void removePort(IpIfDto port) {
        if (!this.ports.contains(port)) {
            return;
        }
        this.ports.remove(port);
        recordChange("member-port", NameUtil.getNodeIfName(port), null);
    }

    public void resetMemberPorts() {
        this.ports.clear();
        recordChange("member-port", "Reset all", null);
    }

    public void setOperStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
    }

    public void setPurpose(String purpose) {
        setValue(MPLSNMS_ATTR.PURPOSE, purpose);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public void setSource(String source) {
        setValue(ATTR.SOURCE, source);
    }

    public void setLinkType(String type) {
        setValue(MPLSNMS_ATTR.LINK_TYPE, type);
    }

    public void setLinkId(String id) {
        if (Util.equals(id, this.originalId)) {
            return;
        }
        this.id = id;
        recordChange(MPLSNMS_ATTR.LINK_NAME, this.originalId, id);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (this.subnet != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        Set<IpIfDto> currentPorts = new MvoDtoSet<IpIfDto>();
        if (this.subnet == null) {
            InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_IPSUBNET,
                    ATTR.ATTR_IPSUBNET_ID, this.id,
                    ATTR.ATTR_IPSUBNET_POOL, this.pool.getAbsoluteName());
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        } else {
            InventoryBuilder.changeContext(cmd, this.subnet);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, this.subnet, this.attributes);
            for (PortDto member : this.subnet.getMemberIpifs()) {
                currentPorts.add((IpIfDto) member);
            }
        }
        List<IpIfDto> addedPorts = Util.getAddedList(currentPorts, this.ports);
        List<IpIfDto> removedPorts = Util.getRemovedList(currentPorts, this.ports);
        String subnet = getContext();
        for (PortDto added : addedPorts) {
            getCommandsOfAddingMemberToIpSubnet(this.cmd, this.id, subnet, added);
        }
        for (PortDto removed : removedPorts) {
            getCommandsOfRemovingMemberFromIpSubnet(this.cmd, this.id, subnet, removed);
        }
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    public static void getCommandsOfAddingMemberToIpSubnet(ShellCommands cmd, String subnetName,
                                                           String subnetAbsoluteName, PortDto added) {
        InventoryBuilder.changeContext(cmd, subnetAbsoluteName);
        InventoryBuilder.buildBindPortToNetworkCommands(cmd, added.getAbsoluteName());
        InventoryBuilder.changeContext(cmd, added);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, subnetName);
    }

    public static void getCommandsOfRemovingMemberFromIpSubnet(ShellCommands cmd, String subnetName,
                                                               String subnetAbsoluteName, PortDto removed) {
        InventoryBuilder.changeContext(cmd, subnetAbsoluteName);
        InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, removed.getAbsoluteName());
        InventoryBuilder.changeContext(cmd, removed);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, null);
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkBuilt();
        if (this.subnet == null) {
            throw new IllegalStateException("link is not exist.");
        }
        for (PortDto removed : this.subnet.getMemberPorts()) {
            InventoryBuilder.changeContext(cmd, subnet);
            InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, removed.getAbsoluteName());
            InventoryBuilder.changeContext(cmd, removed);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, null);
        }
        InventoryBuilder.changeContext(cmd, subnet);
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_IPSUBNET_ID, ATTR.ATTR_IPSUBNET_POOL);
        recordChange("Link", "delete", null);
        return BuildResult.SUCCESS;
    }

    public String getContext() {
        if (this.subnet == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.pool.getAbsoluteName())
                    .append(ATTR.NAME_DELIMITER_PRIMARY)
                    .append(ATTR.TYPE_ID)
                    .append(ATTR.NAME_DELIMITER_SECONDARY)
                    .append(this.id);
            return sb.toString();
        } else {
            return this.subnet.getAbsoluteName();
        }
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}