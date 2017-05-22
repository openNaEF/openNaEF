package voss.nms.inventory.builder;

import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.IpSubnetAddressUtil;
import voss.core.server.util.Util;

import java.io.IOException;

public class RegularIpSubnetNamespaceCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private IpSubnetNamespaceDto parent = null;
    private final IpSubnetNamespaceDto subnet;
    private final IpSubnetAddressDto address;
    private String vpnPrefix = null;
    private String startAddress = null;
    private Integer maskLength = null;

    public RegularIpSubnetNamespaceCommandBuilder(IpSubnetNamespaceDto target, String editorName) {
        super(IpSubnetNamespaceDto.class, target, editorName);
        this.subnet = target;
        if (this.subnet == null) {
            throw new IllegalArgumentException("target ip-subnet-namespace is null.");
        }
        this.parent = target.getParent();
        this.vpnPrefix = DtoUtil.getStringOrNull(target, ATTR.VPN_PREFIX);
        this.address = this.subnet.getIpSubnetAddress();
        this.startAddress = this.address.getAddress().toString();
        this.maskLength = this.address.getSubnetMask();
    }

    public RegularIpSubnetNamespaceCommandBuilder(String editorName) {
        super(IpSubnetNamespaceDto.class, null, editorName);
        this.subnet = null;
        this.address = null;
        this.vpnPrefix = null;
        this.startAddress = null;
        this.maskLength = null;
    }

    public void setParent(IpSubnetNamespaceDto parent) {
        if (DtoUtil.mvoEquals(this.parent, parent)) {
            return;
        }
        if (this.parent == null) {
            recordChange("Parent ip-subnet-namespace", null, parent.getName());
        } else {
            recordChange("Parent ip-subnet-namespace", this.parent.getName(), parent.getName());
        }
        this.parent = parent;
        this.startAddress = null;
        this.maskLength = null;
        this.vpnPrefix = DtoUtil.getStringOrNull(parent, ATTR.VPN_PREFIX);
        setValue(ATTR.VPN_PREFIX, this.vpnPrefix);
    }

    public void setVpnPrefix(String prefix) {
        if (this.subnet != null) {
            throw new IllegalStateException("Cannot change current-address.");
        }
        this.vpnPrefix = prefix;
        setValue(ATTR.VPN_PREFIX, prefix);
    }

    public void setStartAddress(String address) {
        if (this.parent == null) {
            throw new IllegalArgumentException("Parent is not set.");
        }
        if (address == null) {
            throw new IllegalArgumentException();
        }
        try {
            IpAddress ip = IpAddress.gain(address);
            if (!this.parent.getIpSubnetAddress().isInRange(ip)) {
                throw new IllegalArgumentException("Not delegatable IP Address: " + address);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal IP Address: " + address);
        }
        if (this.startAddress != null) {
            throw new IllegalStateException("Cannot change current-address.");
        }
        this.startAddress = address;
    }

    public void setMaskLength(Integer length) {
        if (length == null) {
            throw new IllegalArgumentException();
        }
        if (this.parent.getIpSubnetAddress() != null &&
                this.parent.getIpSubnetAddress().getSubnetMask().intValue() >= length.intValue()) {
            throw new IllegalArgumentException("Not delegatable mask-length: " + length);
        }
        if (this.maskLength != null) {
            throw new IllegalStateException("Cannot change current-address.");
        }
        this.maskLength = length;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (Util.isNull(this.startAddress, this.maskLength)) {
            throw new IllegalArgumentException("start-address or mask-length is missing: "
                    + this.startAddress + ", " + this.maskLength);
        }
        StringBuilder sb = new StringBuilder();
        if (this.vpnPrefix != null) {
            sb.append(this.vpnPrefix).append("/");
        }
        sb.append(startAddress).append("/").append(this.maskLength);
        String subnetName = sb.toString();
        InventoryBuilder.changeContext(this.cmd, this.parent);
        if (this.subnet == null) {
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_IPSUBNET, subnetName);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes, false);
            String range = IpSubnetAddressUtil.toIpSubnetNamespaceRangeString(this.vpnPrefix, this.startAddress, this.maskLength);
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, range);
            cmd.addLastEditCommands();
            recordChange(DiffObjectType.IP_DELEGATION.name(), null, subnetName);
        } else {
            InventoryBuilder.changeContext(cmd, this.subnet);
            if (!subnetName.equals(this.subnet.getName())) {
                InventoryBuilder.buildRenameCommands(cmd, subnetName);
                cmd.addLastEditCommands();
                recordChange(DiffObjectType.IP_DELEGATION.name(), this.subnet.getName(), subnetName);
            }
        }
        sb.insert(0, "IP:");
        String addressName = sb.toString();
        if (this.address == null && this.parent.getIpSubnetAddress() != null) {
            String range = this.startAddress + "/" + this.maskLength;
            InventoryBuilder.changeContext(cmd, this.parent.getIpSubnetAddress());
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_IPSUBNET_ADDRESS, addressName);
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    ATTR.ATTR_IPSUBNETADDRESS_SUBNETNAMESPACE,
                    InventoryBuilder.getRelativeName(ATTR.POOL_TYPE_IPSUBNET, subnetName));
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes, false);
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, range);
            cmd.addLastEditCommands();
            recordChange(DiffObjectType.IP_DELEGATION.name(), null, addressName);
        } else {
            InventoryBuilder.changeContext(cmd, this.address);
            if (!addressName.equals(this.address.getName())) {
                InventoryBuilder.buildRenameCommands(cmd, addressName);
                cmd.addLastEditCommands();
                recordChange(DiffObjectType.IP_DELEGATION.name(), this.address.getName(), subnetName);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (this.subnet == null) {
            throw new IllegalStateException("subnet is null.");
        } else if (this.subnet.getChildren().size() > 0) {
            throw new IllegalStateException("subnet has children.");
        } else if (this.subnet.getUsers().size() > 0) {
            throw new IllegalStateException("subnet has users.");
        } else if (this.address != null && this.address.getChildren().size() > 0) {
            throw new IllegalStateException("address has children.");
        }
        String subnetName = getNameForDelete(this.subnet.getName());
        InventoryBuilder.changeContext(cmd, this.subnet);
        for (IdRange<String> range : this.subnet.getIdRanges()) {
            String _range = range.lowerBound + "-" + range.upperBound;
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, _range);
        }
        InventoryBuilder.buildRenameCommands(cmd, subnetName);
        InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.IP_SUBNET_NAMESPACE_TRASH);
        cmd.addLastEditCommands();
        if (this.address != null) {
            String addressName = getNameForDelete(this.address.getName());
            InventoryBuilder.changeContext(cmd, this.address);
            for (IdRange<IpAddress> range : this.address.getIdRanges()) {
                String _range = range.lowerBound.toString() + "-" + range.upperBound.toString();
                InventoryBuilder.translate(cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, _range);
            }
            InventoryBuilder.buildRenameCommands(cmd, addressName);
            InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.IP_SUBNET_ADDRESS_TRASH);
            cmd.addLastEditCommands();
        }
        recordChange(DiffObjectType.IP_DELEGATION.name(), subnet.getName(), null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.IP_DELEGATION.name();
    }

}