package voss.nms.inventory.builder;

import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
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
import voss.core.server.util.Util;

import java.io.IOException;
import java.util.Set;

@SuppressWarnings("serial")
public class SimpleIpSubnetAddressCommandBuilder extends AbstractCommandBuilder {
    private final IpSubnetAddressDto address;
    private final IpSubnetAddressDto originalParent;
    private final String originalStartAddress;
    private final String originalEndAddress;
    private final Integer originalMaskLength;
    private IpSubnetAddressDto parent = null;
    private String caption = null;
    private String startAddress = null;
    private String endAddress = null;
    private Integer maskLength = null;

    public SimpleIpSubnetAddressCommandBuilder(IpSubnetAddressDto target, String editorName) {
        super(IpSubnetAddressDto.class, target, editorName);
        this.address = target;
        if (this.address != null) {
            this.parent = target.getParent();
            this.caption = target.getName();
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
        this.originalParent = this.parent;
        this.originalStartAddress = this.startAddress;
        this.originalEndAddress = this.endAddress;
        this.originalMaskLength = this.maskLength;
    }

    public void setParent(IpSubnetAddressDto parent) {
        if (parent == null) {
            throw new IllegalArgumentException();
        }
        this.parent = parent;
    }

    public void setCaption(String caption) {
        if (caption == null) {
            throw new IllegalArgumentException();
        }
        this.caption = caption;
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
        if (this.address == null) {
            if (this.parent == null || this.caption == null) {
                throw new IllegalStateException("no myself/parent name.");
            }
            log().debug("create: " + this.caption);
            InventoryBuilder.changeContext(this.cmd, this.parent);
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.POOL_TYPE_IPSUBNET_ADDRESS, this.caption);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            this.cmd.addLastEditCommands();
            recordChange("Subnet Name", null, this.caption);
        } else {
            log().debug("update: " + this.address.getAbsoluteName());
            InventoryBuilder.changeContext(this.cmd, this.address);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            this.cmd.addLastEditCommands();
            if (!DtoUtil.mvoEquals(this.parent, this.originalParent)) {
                InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, this.parent.getAbsoluteName());
                recordChange("Parent Subnet", this.originalParent.getName(), this.parent.getName());
            }
            if (!this.caption.equals(this.address.getName())) {
                InventoryBuilder.buildRenameCommands(this.cmd, this.caption);
                recordChange("Subnet Name", this.address.getName(), this.caption);
            }
        }
        buildRangeCommand();
        return BuildResult.SUCCESS;
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
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (this.address == null) {
            return BuildResult.NO_CHANGES;
        }
        if (this.address.getChildren().size() > 0) {
            throw new InventoryException("one or more child subnets found: " + DtoUtil.toDebugString(address));
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
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.SUBNET.getCaption();
    }

}