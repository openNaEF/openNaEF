package voss.nms.inventory.builder;

import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.vlan.MultipointVlanSegmentDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanLinkDto;
import naef.dto.vlan.VlanSegmentDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.LinkUtil;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.Iterator;

public class LinkCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final LinkDto link;
    private final String objectType;
    private PortDto port1;
    private PortDto port2;


    public LinkCommandBuilder(PortDto port1, PortDto port2, String editorName) {
        super(LinkDto.class, null, editorName);
        setConstraint(LinkDto.class);
        if (Util.isNull(port1, port2)) {
            throw new IllegalArgumentException("port1 or port2 is null: " +
                    "port1=" + (port1 == null ? "null" : port1.getAbsoluteName()) +
                    ", port2=" + (port2 == null ? "null" : port2.getAbsoluteName()));
        }
        this.link = null;
        this.port1 = port1;
        this.port2 = port2;
        this.objectType = getObjectType(port1, port2);
        recordChange("port1", null, NameUtil.getNodeIfName(port1));
        recordChange("port2", null, NameUtil.getNodeIfName(port2));
    }

    public LinkCommandBuilder(LinkDto link, String editorName) {
        super(LinkDto.class, link, editorName);
        if (link == null) {
            throw new IllegalArgumentException("link is null.");
        }
        setConstraint(LinkDto.class);
        this.link = link;
        if (this.link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("this link doesn't have 2 port: " + this.link.getAbsoluteName());
        }
        Iterator<PortDto> iterator = this.link.getMemberPorts().iterator();
        this.port1 = iterator.next();
        this.port2 = iterator.next();
        this.objectType = getObjectType(port1, port2);
        this.cmd.addVersionCheckTarget(link);
    }

    private String getObjectType(PortDto port1, PortDto port2) {
        if (Util.isNull(port1, port2)) {
            throw new IllegalArgumentException();
        }
        String objectType1 = getL2LinkObjectType(port1);
        String objectType2 = getL2LinkObjectType(port2);
        if (!Util.equals(objectType1, objectType2)) {
            throw new IllegalArgumentException("object type mismatch: port1="
                    + objectType1 + " (" + port1.getAbsoluteName()
                    + "), port2=" + objectType2 + " (" + port2.getAbsoluteName() + ")");
        }
        return objectType1;
    }

    private String getL2LinkObjectType(PortDto port) {
        if (port == null) {
            throw new IllegalArgumentException("port is null.");
        }
        String objectType = LinkUtil.getL2LinkTypeName(port);
        if (objectType == null) {
            throw new IllegalStateException("cannot determine link object type: " + port.getAbsoluteName());
        }
        return objectType;
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setSource(String source) {
        setValue(ATTR.SOURCE, source);
    }

    public void setLinkID(String id) {
        setValue(MPLSNMS_ATTR.LINK_NAME, id);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (this.link != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.link == null) {
            InventoryBuilder.translate(this.cmd, CMD.LINK_CONNECT,
                    "_TYPE_", this.objectType,
                    "_FQN1_", this.port1.getAbsoluteName(),
                    "_FQN2_", this.port2.getAbsoluteName());
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            InventoryBuilder.changeContext(this.cmd, this.link);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, link, attributes);
        }
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    public BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        buildUnwireCommands();
        recordChange("Link", "delete", null);
        return BuildResult.SUCCESS;
    }

    private void buildUnwireCommands() {
        for (NetworkDto upper : link.getUpperLayers()) {
            checkVlanLink(upper);
            VlanLinkDto upperVlanLink = (VlanLinkDto) upper;
            removeVlanLink(upperVlanLink);
        }
        this.cmd.log(DtoUtil.toDebugString(link));
        this.cmd.addCommand(InventoryBuilder.translate(CMD.LINK_DISCONNECT_BY_MVOID,
                CMD.ARG_MVOID, DtoUtil.getMvoId(link).toString()));
    }

    private void removeVlanLink(VlanSegmentDto link) {
        VlanDto vlan = (VlanDto) link.getOwner();
        if (vlan != null) {
            InventoryBuilder.changeContext(cmd, vlan);
            InventoryBuilder.translate(cmd, CMD.EXCLUDE_ELEMENT_FROM_NETWORK,
                    CMD.ARG_FQN, DtoUtil.getMvoId(link).toString());
            cmd.addLastEditCommands();
            cmd.addVersionCheckTarget(vlan);
        }
        InventoryBuilder.changeContext(cmd, link);
        for (NetworkDto lower : this.link.getLowerLayerLinks()) {
            cmd.log(lower);
            InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, DtoUtil.getMvoId(lower).toString());
        }
        cmd.addCommand(CMD.CONTEXT_RESET);
        InventoryBuilder.translate(cmd, CMD.LINK_DISCONNECT_BY_MVOID,
                CMD.ARG_MVOID, DtoUtil.getMvoId(link).toString());
    }

    private static void checkVlanLink(NetworkDto upper) {
        if (!(upper instanceof VlanSegmentDto)) {
            throw new IllegalStateException("unsupported upper-layer type: " + upper.getAbsoluteName());
        } else if (MultipointVlanSegmentDto.class.isInstance(upper)) {
            throw new IllegalStateException("multi-point-vlan-segment is not unsupported: " + upper.getAbsoluteName());
        } else if (upper.getMemberPorts().size() != 2) {
            throw new IllegalStateException("member port size is not 2: " + upper.getMemberPorts().size()
                    + " (" + upper.getAbsoluteName() + ")");
        }
    }

    public String getObjectType() {
        return DiffObjectType.L2_LINK.getCaption();
    }

}