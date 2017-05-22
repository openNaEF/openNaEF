package voss.nms.inventory.builder;

import naef.dto.IdRange;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
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
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IpSubnetCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;

    private IpSubnetNamespaceDto namespace;
    private String vpnPrefix;
    private IpSubnetDto subnet;
    private IpSubnetAddressDto subnetAddress;
    private String startAddress = null;
    private Integer maskLength = null;
    private boolean cascadeDelete = false;
    private final List<String> lowerLayers = new ArrayList<String>();
    private final List<String> originalLowerLayers = new ArrayList<String>();

    public IpSubnetCommandBuilder(IpSubnetNamespaceDto namespace, String editorName) {
        super(IpSubnetDto.class, null, editorName);
        setConstraint(IpSubnetDto.class);
        this.namespace = namespace;
        this.vpnPrefix = DtoUtil.getStringOrNull(this.namespace, ATTR.VPN_PREFIX);
        this.subnet = null;
        this.subnetAddress = null;
        setValue(ATTR.VPN_PREFIX, this.vpnPrefix);
    }

    public IpSubnetCommandBuilder(IpSubnetDto target, String editorName) {
        super(IpSubnetDto.class, target, editorName);
        setConstraint(IpSubnetDto.class);
        if (target == null) {
            throw new IllegalArgumentException("target is null.");
        }
        this.subnet = target;
        this.namespace = this.subnet.getNamespace();
        this.vpnPrefix = DtoUtil.getStringOrNull(this.namespace, ATTR.VPN_PREFIX);
        this.subnetAddress = this.subnet.getSubnetAddress();
        if (this.subnetAddress != null) {
            this.startAddress = this.subnetAddress.getAddress().toString();
            this.maskLength = this.subnetAddress.getSubnetMask();
        }
        for (NetworkDto lower : this.subnet.getLowerLayerLinks()) {
            this.lowerLayers.add(lower.getAbsoluteName());
            this.originalLowerLayers.add(lower.getAbsoluteName());
        }
    }

    public void setStartAddress(String address) {
        if (Util.equals(address, this.startAddress)) {
            return;
        }
        if (address == null) {
            throw new IllegalArgumentException("address must not be null.");
        }
        try {
            IpAddress.gain(address);
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal network-address: " + address);
        }
        recordChange("Network Address", this.startAddress, address);
        this.startAddress = address;
    }

    public void setMaskLength(Integer length) {
        if (Util.equals(length, this.maskLength)) {
            return;
        }
        if (length == null) {
            throw new IllegalArgumentException("mask-length must not be null.");
        }
        IpSubnetAddressDto source = this.namespace.getIpSubnetAddress();
        if (source != null) {
            Integer _length = source.getSubnetMask();
            if (_length != null && _length.intValue() >= length.intValue()) {
                throw new IllegalArgumentException("Illegal mask-length: " + length);
            }
        }
        recordChange("Subnet Mask Length", this.maskLength, length);
        this.maskLength = length;
    }

    public void addLowerLayerNetwork(NetworkDto lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        }
        addLowerLayerNetwork(lower.getAbsoluteName());
    }

    public void addLowerLayerNetwork(String lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        } else if (this.lowerLayers.contains(lower)) {
            return;
        }
        this.lowerLayers.add(lower);
        recordChange("Lower-layer", null, lower);
    }

    public void removeLowerLayerNetwork(NetworkDto lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        }
        removeLowerLayerNetwork(lower.getAbsoluteName());
    }

    public void removeLowerLayerNetwork(String lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        } else if (!this.lowerLayers.contains(lower)) {
            return;
        }
        this.lowerLayers.remove(lower);
        recordChange("Lower-layer", lower, null);
    }

    public void setCascadeDelete(boolean value) {
        this.cascadeDelete = value;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        checkBuilt();
        if (this.subnet != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        checkBuilt();
        if (this.subnet == null) {
            buildIpSubnet();
        } else {
            InventoryBuilder.changeContext(this.cmd, this.subnet);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            cmd.addLastEditCommands();
        }
        if (this.subnetAddress == null && this.namespace.getIpSubnetAddress() != null) {
            buildIpSubnetAddress();
        }
        buildStack();
        return BuildResult.SUCCESS;
    }

    private void buildIpSubnet() {
        String subnetName = getIpSubnetName();
        InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_IPSUBNET,
                ATTR.ATTR_IPSUBNET_ID, subnetName,
                ATTR.ATTR_IPSUBNET_POOL, this.namespace.getName());
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, MPLSNMS_ATTR.NETWORK_ADDRESS, this.startAddress);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, MPLSNMS_ATTR.MASK_LENGTH, this.maskLength.toString());
        cmd.addLastEditCommands();
        recordChange(DiffObjectType.IP_SUBNET.name(), null, subnetName);
    }

    private void buildIpSubnetAddress() {
        String subnetName = getIpSubnetName();
        String addressName = getIpSubnetAddressName();
        IpSubnetAddressDto parent = this.namespace.getIpSubnetAddress();
        String range = this.startAddress + "/" + this.maskLength;
        InventoryBuilder.changeContext(cmd, parent);
        InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_IPSUBNET_ADDRESS, addressName);
        InventoryBuilder.buildAttributeSetOrReset(cmd,
                ATTR.ATTR_IPSUBNETADDRESS_SUBNET,
                InventoryBuilder.appendContext(this.namespace, ATTR.NETWORK_TYPE_ID, subnetName));
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes, false);
        InventoryBuilder.translate(cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, range);
        cmd.addLastEditCommands();
        recordChange(DiffObjectType.IP_SUBNET.name(), null, addressName);
    }

    private void buildStack() {
        InventoryBuilder.changeContext(this.cmd, getContext());
        List<String> removed = Util.getRemovedList(this.originalLowerLayers, this.lowerLayers);
        for (String remove : removed) {
            InventoryBuilder.buildNetworkUnstackCommand(this.cmd, remove);
        }
        List<String> added = Util.getAddedList(this.originalLowerLayers, this.lowerLayers);
        for (String add : added) {
            InventoryBuilder.buildNetworkStackCommand(this.cmd, add);
        }
        if (removed.size() == 0 && added.size() == 0) {
            return;
        }
        this.cmd.addLastEditCommands();
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (this.subnet == null) {
            throw new IllegalStateException("no subnet.");
        } else if (!this.cascadeDelete && this.subnet.getMemberPorts().size() > 0) {
            throw new IllegalStateException("Remaining member ports ("
                    + this.subnet.getMemberPorts().size() + ") found.");
        }
        if (this.cascadeDelete) {
            removeMemberPort();
        }
        InventoryBuilder.changeContext(cmd, this.subnet);
        cmd.addLastEditCommands();
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_IPSUBNET_ID, ATTR.ATTR_IPSUBNET_POOL);
        if (this.subnetAddress != null) {
            String addressName = getNameForDelete(this.subnetAddress.getName());
            InventoryBuilder.changeContext(cmd, this.subnetAddress);
            for (IdRange<IpAddress> range : this.subnetAddress.getIdRanges()) {
                String _range = range.lowerBound.toString() + "-" + range.upperBound.toString();
                InventoryBuilder.translate(cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, _range);
            }
            InventoryBuilder.buildRenameCommands(cmd, addressName);
            InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.IP_SUBNET_ADDRESS_TRASH);
            cmd.addLastEditCommands();
        }
        recordChange(DiffObjectType.IP_SUBNET.name(), subnet.getSubnetName(), null);
        return BuildResult.SUCCESS;
    }

    private void removeMemberPort() {
        if (!this.cascadeDelete) {
            return;
        }
        if (this.subnetAddress != null) {
            for (IpIfDto member : this.subnetAddress.getUsers()) {
                String ip = DtoUtil.getStringOrNull(member, ATTR.ATTR_IPIF_IP_ADDRESS);
                InventoryBuilder.changeContext(this.cmd, member);
                InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_IP_ADDRESS, null);
                InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, null);
                InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_IP_ADDRESS, ip);
            }
        }
        InventoryBuilder.changeContext(this.cmd, this.subnet);
        for (PortDto member : this.subnet.getMemberPorts()) {
            InventoryBuilder.buildUnbindPortFromNetworkCommands(this.cmd, member);
        }
        InventoryBuilder.changeContext(this.cmd, this.subnet);
        for (String lower : this.originalLowerLayers) {
            InventoryBuilder.buildNetworkStackCommand(this.cmd, lower);
        }
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.IP_SUBNET.getCaption();
    }

    public String getIpSubnetName() {
        return AbsoluteNameFactory.toIpSubnetName(this.vpnPrefix, this.startAddress, this.maskLength);
    }

    public String getIpSubnetAddressName() {
        return AbsoluteNameFactory.toIpSubnetAddressName(this.vpnPrefix, this.startAddress, this.maskLength);
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setFoundOnNetwork(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK, value.toString());
    }

    public void setMaxPorts(Integer ports) {
        setValue(ATTR.LINK_MAX_PORTS, String.valueOf(ports));
    }

    public void setPurpose(String value) {
        setValue(MPLSNMS_ATTR.PURPOSE, value);
    }

    public void setUser(String value) {
        setValue(MPLSNMS_ATTR.END_USER, value);
    }

    public String getContext() {
        if (this.subnet != null) {
            return subnet.getAbsoluteName();
        } else {
            return InventoryBuilder.appendContext(namespace, ATTR.NETWORK_TYPE_ID, getIpSubnetName());
        }
    }
}