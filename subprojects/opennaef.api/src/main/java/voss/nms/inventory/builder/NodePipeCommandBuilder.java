package voss.nms.inventory.builder;

import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import voss.core.server.builder.*;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;

public class NodePipeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final InterconnectionIfDto target;
    private final String nodeName;
    private final NodeDto node;
    private String ifName = null;

    public NodePipeCommandBuilder(NodeDto node, String editorName) {
        super(InterconnectionIfDto.class, null, editorName);
        setConstraint(InterconnectionIfDto.class);
        this.node = node;
        this.nodeName = null;
        this.target = null;
    }

    public NodePipeCommandBuilder(String nodeName, String editorName) {
        super(InterconnectionIfDto.class, null, editorName);
        setConstraint(InterconnectionIfDto.class);
        this.nodeName = BuilderUtil.getNodeName(nodeName);
        this.node = null;
        this.target = null;
    }

    public NodePipeCommandBuilder(InterconnectionIfDto target, String editorName) {
        super(InterconnectionIfDto.class, target, editorName);
        setConstraint(InterconnectionIfDto.class);
        this.nodeName = null;
        this.node = target.getNode();
        this.target = target;
        if (target != null) {
            initialize(target);
        }
    }

    private void initialize(InterconnectionIfDto target) {
        this.ifName = DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.IFNAME);
        this.cmd.addVersionCheckTarget(target);
    }

    public void setIfName(String ifName) {
        if (ifName == null) {
            throw new IllegalStateException("ifName is mandatory.");
        }
        if (Util.stringToNull(ifName) != null && this.node != null) {
            PortDto portByIfName = NodeUtil.getPortByIfName(this.node, ifName);
            if (portByIfName != null && (!DtoUtil.isSameMvoEntity(portByIfName, target) || target == null)) {
                throw new IllegalStateException("There already is a port with the same name ifName.");
            }
            this.cmd.addVersionCheckTarget(this.node);
        }
        setValue(MPLSNMS_ATTR.IFNAME, ifName);
        setValue(MPLSNMS_ATTR.PORT_TYPE, PortType.NODEPIPE.getCaption());
        this.ifName = ifName;
    }

    public void setIgpCost(Integer cost) {
        if (cost == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.IGP_COST, cost.toString());
    }

    public void setOspfAreaID(String area) {
        setValue(MPLSNMS_ATTR.OSPF_AREA_ID, area);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    @Override
    protected BuildResult buildCommandInner() {
        if (this.target != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (target == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            if (this.node != null) {
                InventoryBuilder.changeContext(cmd, node);
            } else {
                InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
            }
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_INTERCONNECTION_IF, ifName);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            this.cmd.addLastEditCommands();
        } else {
            InventoryBuilder.changeContext(cmd, target);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, target, attributes);
            this.cmd.addLastEditCommands();
            String currentIfName = DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.IFNAME);
            if (!currentIfName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        return BuildResult.SUCCESS;
    }

    public PortDto getPort() {
        return this.target;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkBuilt();
        if (this.target == null) {
            return BuildResult.FAIL;
        }
        InventoryBuilder.changeContext(cmd, this.target);
        for (PortDto ac : this.target.getAttachedPorts()) {
            InventoryBuilder.translate(cmd, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, this.target.getAbsoluteName(),
                    CMD.ARG_PORT, ac.getAbsoluteName());
        }
        cmd.addLastEditCommands();
        cmd.addCommand(CMD.CONTEXT_DOWN);
        InventoryBuilder.buildNodeElementDeletionCommands(cmd, target);
        recordChange("target", DtoUtil.getIfName(target), null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.PSEUDOWIRE.getCaption();
    }

}