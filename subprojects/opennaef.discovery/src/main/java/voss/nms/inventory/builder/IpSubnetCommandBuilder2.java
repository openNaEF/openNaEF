package voss.nms.inventory.builder;

import naef.dto.IdRange;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.builder.*;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.MvoDtoSet;
import voss.core.server.util.Util;
import voss.nms.inventory.builder.complement.IpSubnetAddressComplementBuilder;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.IpAddressModel;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class IpSubnetCommandBuilder2 extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final IpSubnetNamespaceDto pool;
    private final IpSubnetDto subnet;
    private String id;
    private String originalId;
    private IpAddressModel ipModel = IpAddressModel.IP_IF;
    private final MvoDtoSet<IpIfDto> ports = new MvoDtoSet<IpIfDto>();
    private final IpSubnetAddressDto address;
    private final IpSubnetAddressDto originalParent;
    private final String originalVpnPrefix;
    private final String originalStartAddress;
    private final String originalEndAddress;
    private final Integer originalMaskLength;
    private IpSubnetAddressDto parent = null;
    private String ipSubnetAddressName = null;
    private String vpnPrefix = null;
    private String startAddress = null;
    private String endAddress = null;
    private Integer maskLength = null;

    public IpSubnetCommandBuilder2(IpSubnetDto target, String editorName) {
        super(IpSubnetDto.class, target, editorName);
        if (target == null) {
            throw new IllegalArgumentException("target subnet is null.");
        }
        setConstraint(IpSubnetDto.class);
        this.id = target.getSubnetName();
        this.subnet = target;
        for (PortDto member : subnet.getMemberIpifs()) {
            this.ports.add((IpIfDto) member);
        }
        this.cmd.addVersionCheckTarget(subnet);
        this.pool = this.subnet.getNamespace();
        try {
            this.ipModel = DiffConfiguration.getInstance().getDiffPolicy().getIpAddressModel();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        this.address = target.getSubnetAddress();
        if (this.address != null) {
            this.parent = this.address.getParent();
            this.vpnPrefix = DtoUtil.getStringOrNull(this.address, MPLSNMS_ATTR.VPN_PREFIX);
            this.ipSubnetAddressName = this.address.getName();
            Set<IdRange<IpAddress>> ranges = this.address.getIdRanges();
            if (ranges.size() > 1) {
                throw new IllegalStateException("more than 1 range found: " + DtoUtil.toDebugString(address));
            } else if (ranges.size() == 0) {
                this.startAddress = null;
                this.endAddress = null;
                this.maskLength = null;
            } else {
                IdRange<IpAddress> range = ranges.iterator().next();
                this.startAddress = range.lowerBound.toString();
                if (this.address.getSubnetMask() != null) {
                    this.endAddress = null;
                    this.maskLength = this.address.getSubnetMask();
                } else {
                    this.endAddress = range.upperBound.toString();
                    this.maskLength = null;
                }
            }
        }
        this.originalVpnPrefix = this.vpnPrefix;
        this.originalParent = this.parent;
        this.originalStartAddress = this.startAddress;
        this.originalEndAddress = this.endAddress;
        this.originalMaskLength = this.maskLength;
    }

    public IpSubnetCommandBuilder2(String id, IpSubnetNamespaceDto pool, String editorName) {
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
        try {
            this.ipModel = DiffConfiguration.getInstance().getDiffPolicy().getIpAddressModel();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        this.address = null;
        this.originalVpnPrefix = null;
        this.originalParent = null;
        this.originalStartAddress = null;
        this.originalEndAddress = null;
        this.originalMaskLength = null;
    }

    public IpAddressModel getIpAddressModel() {
        return this.ipModel;
    }

    public void setIpAddressModel(IpAddressModel model) {
        if (model == null) {
            throw new IllegalArgumentException("ip-model must not be null.");
        }
        this.ipModel = model;
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

    public void setVpnPrefix(String prefix) {
        if (this.address != null) {
            if (!this.originalVpnPrefix.equals(prefix)) {
                throw new IllegalArgumentException("Cannot change current vpn-prefix. "
                        + this.originalVpnPrefix + "->" + prefix);
            }
        } else if (prefix == null) {
            return;
        }
        this.vpnPrefix = prefix;
        recordChange(MPLSNMS_ATTR.VPN_PREFIX, null, this.vpnPrefix);
    }

    public String getVpnPrefix() {
        return this.vpnPrefix;
    }

    public void setParent(IpSubnetAddressDto parent) {
        if (parent == null) {
            throw new IllegalArgumentException();
        }
        this.parent = parent;
    }

    public void setIpSubnetAddressName(String ipSubnetAddressName) {
        if (ipSubnetAddressName == null) {
            throw new IllegalArgumentException();
        }
        this.ipSubnetAddressName = ipSubnetAddressName;
    }

    public String getIpSubnetAddressName() {
        if (this.ipSubnetAddressName != null) {
            return this.ipSubnetAddressName;
        } else {
            if (this.startAddress == null) {
                throw new IllegalStateException("no start-address.");
            }
            if (Util.isAllNull(this.maskLength, this.endAddress)) {
                throw new IllegalStateException("mask-length or end-address must be set.");
            }
            StringBuilder sb = new StringBuilder();
            if (this.vpnPrefix != null) {
                sb.append(this.vpnPrefix).append(ATTR.VPN_DELIMITER);
            }
            sb.append(this.startAddress);
            if (this.endAddress != null) {
                sb.append("-").append(this.endAddress);
            } else {
                sb.append("/").append(this.maskLength);
            }
            return sb.toString();
        }
    }

    public void setStartAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException();
        }
        this.startAddress = address;
    }

    public void setEndAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException();
        }
        this.endAddress = address;
        this.maskLength = null;
    }

    public void setMaskLength(Integer length) {
        if (length == null) {
            throw new IllegalArgumentException();
        }
        this.maskLength = length;
        this.endAddress = null;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        BuildResult result1 = buildIpSubnetCommands();
        switch (result1) {
            case SUCCESS:
            case NO_CHANGES:
                break;
            case FAIL:
                return BuildResult.FAIL;
        }
        BuildResult result2 = buildIpSubnetAddressCommands();
        built();
        switch (result2) {
            case SUCCESS:
                return BuildResult.SUCCESS;
            case NO_CHANGES:
                if (result1.equals(BuildResult.SUCCESS)) {
                    return BuildResult.SUCCESS;
                } else {
                    return BuildResult.NO_CHANGES;
                }
            case FAIL:
                return BuildResult.FAIL;
        }
        throw new IllegalStateException("Unexpected build-result: " + result1 + ", " + result2);
    }

    protected BuildResult buildIpSubnetCommands() throws IOException, InventoryException {
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

    protected BuildResult buildIpSubnetAddressCommands() throws IOException, InventoryException, ExternalServiceException {
        if (!this.ipModel.equals(IpAddressModel.SUBNET)) {
            return BuildResult.NO_CHANGES;
        }
        String name = getIpSubnetAddressName();
        if (this.address == null) {
            if (this.parent == null) {
                throw new IllegalStateException("no parent name.");
            }
            log().debug("create: " + name);
            InventoryBuilder.changeContext(this.cmd, this.parent);
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_IPSUBNET_ADDRESS, name);
            InventoryBuilder.buildAttributeSetOrReset(this.cmd, MPLSNMS_ATTR.VPN_PREFIX, this.vpnPrefix);
            this.cmd.addLastEditCommands();
            recordChange("Subnet Name", null, name);
        } else {
            log().debug("update: " + this.address.getAbsoluteName());
            InventoryBuilder.changeContext(this.cmd, this.address);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            this.cmd.addLastEditCommands();
            if (!DtoUtil.mvoEquals(this.parent, this.originalParent)) {
                InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, this.parent.getAbsoluteName());
                recordChange("Parent Subnet", this.originalParent.getName(), this.parent.getName());
            }
            if (!name.equals(this.address.getName())) {
                InventoryBuilder.buildRenameCommands(this.cmd, name);
                recordChange("Subnet Name", this.address.getName(), name);
            }
        }
        buildRangeCommand();
        built();
        return setResult(BuildResult.SUCCESS);
    }

    private void buildRangeCommand() {
        if (Util.isAllNull(this.maskLength, this.endAddress)) {
            throw new IllegalArgumentException("mask-length or end-address is needed.");
        }
        if (Util.equals(this.originalStartAddress, this.startAddress) &&
                Util.equals(this.originalMaskLength, this.maskLength) &&
                Util.equals(this.originalEndAddress, this.endAddress)) {
            log().debug("no changes on range.");
            return;
        }
        if (this.address != null) {
            for (IdRange<IpAddress> range : this.address.getIdRanges()) {
                String s = range.lowerBound.toString() + "-" + range.upperBound.toString();
                InventoryBuilder.translate(this.cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, s);
                recordChange("Subnet Range", s, null);
            }
        }
        String s;
        if (this.maskLength != null) {
            s = this.startAddress + "/" + this.maskLength;
            InventoryBuilder.translate(this.cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, s);
        } else {
            s = this.startAddress + "-" + this.endAddress;
            InventoryBuilder.translate(this.cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, s);
        }
        recordChange("Subnet Range", null, s);
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        deleteIpSubnetAddress();
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

    protected void deleteIpSubnetAddress() throws InventoryException {
        if (this.address == null) {
            return;
        }
        if (this.address.getChildren().size() > 0) {
            throw new InventoryException("one or more child subnets found: " + DtoUtil.toDebugString(address));
        } else if (this.address.getUsers().size() > 0) {
            throw new InventoryException("one or more member ip-if found: " + DtoUtil.toDebugString(address));
        }
        this.cmd.addVersionCheckTarget(this.address);
        String path = getNameForDelete(this.address.getName());
        InventoryBuilder.changeContext(this.cmd, this.address);
        for (IdRange<IpAddress> range : this.address.getIdRanges()) {
            String s = range.lowerBound.toString() + "-" + range.upperBound.toString();
            InventoryBuilder.translate(this.cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, s);
            recordChange("Subnet Range", s, null);
        }
        InventoryBuilder.buildRenameCommands(this.cmd, path);
        InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.IP_SUBNET_ADDRESS_TRASH);
        recordChange(this.address.getName(), "Delete", null);
        InventoryBuilder.changeContext(this.cmd, this.subnet);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPSUBNET_SUBNET_ADDRESS, null);
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

    public String getIpSubnetAddressContext() {
        if (this.ipModel != IpAddressModel.SUBNET) {
            return null;
        }
        if (this.address != null) {
            return this.address.getAbsoluteName();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.parent.getAbsoluteName())
                    .append(ATTR.NAME_DELIMITER_PRIMARY)
                    .append(ATTR.TYPE_ID)
                    .append(ATTR.NAME_DELIMITER_SECONDARY)
                    .append(this.id);
            return sb.toString();
        }
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}