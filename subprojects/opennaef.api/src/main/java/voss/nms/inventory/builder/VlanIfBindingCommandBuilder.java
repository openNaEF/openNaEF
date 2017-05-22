package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.common.diff.DiffEntry;
import voss.core.common.diff.DiffUtil;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.NameUtil;

import java.util.ArrayList;
import java.util.List;

public class VlanIfBindingCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;

    public static final String TAGGED_PORTS = "Tagged Ports";
    public static final String UNTAGGED_PORTS = "Untagged Ports";

    private final VlanIfDto vlanIf;
    private final String ownerName;
    private final String vlanName;
    private final String nodeName;
    private final List<String> taggedPorts = new ArrayList<String>();
    private final List<String> untaggedPorts = new ArrayList<String>();
    private final List<String> originalTaggedPorts = new ArrayList<String>();
    private final List<String> originalUntaggedPorts = new ArrayList<String>();
    private final List<String> originalTaggedPortIfNames = new ArrayList<String>();
    private final List<String> originalUntaggedPortIfNames = new ArrayList<String>();

    public VlanIfBindingCommandBuilder(String ownerName, String vlanName, String editorName) {
        super(VlanIfDto.class, null, null, editorName);
        try {
            this.ownerName = ownerName;
            this.vlanName = vlanName;
            this.vlanIf = null;
            this.nodeName = BuilderUtil.getNodeName(ownerName);
            setConstraint(VlanIfDto.class);
            initialize(null);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VlanIfBindingCommandBuilder(NodeElementDto owner, String vlanName, String editorName) {
        super(VlanIfDto.class, owner.getNode(), null, editorName);
        if (!(owner instanceof NodeDto || owner instanceof EthPortDto
                || owner instanceof EthLagIfDto || owner instanceof VlanIfDto)) {
            throw new IllegalArgumentException();
        }
        this.ownerName = owner.getAbsoluteName();
        this.vlanName = vlanName;
        this.vlanIf = null;
        this.nodeName = owner.getNode().getName();
        initialize(null);
        setConstraint(VlanIfDto.class);
    }

    public VlanIfBindingCommandBuilder(VlanIfDto vlanIf, String editorName) {
        super(VlanIfDto.class, vlanIf.getNode(), vlanIf, editorName);
        this.ownerName = null;
        this.vlanName = null;
        this.vlanIf = vlanIf;
        this.nodeName = vlanIf.getNode().getName();
        initialize(vlanIf);
        setConstraint(VlanIfDto.class);
    }

    private void initialize(VlanIfDto port) {
        if (port == null) {
            return;
        }
        for (PortDto tagged : port.getTaggedVlans()) {
            this.taggedPorts.add(tagged.getAbsoluteName());
            this.originalTaggedPorts.add(tagged.getAbsoluteName());
            this.originalTaggedPortIfNames.add(DtoUtil.getIfName(tagged));
        }
        for (PortDto untagged : port.getUntaggedVlans()) {
            if (VrfIfDto.class.isInstance(untagged)) {
                continue;
            } else if (VplsIfDto.class.isInstance(untagged)) {
                continue;
            }
            this.untaggedPorts.add(untagged.getAbsoluteName());
            this.originalUntaggedPorts.add(untagged.getAbsoluteName());
            this.originalUntaggedPortIfNames.add(DtoUtil.getIfName(untagged));
        }
        this.cmd.addVersionCheckTarget(port);
    }

    public void setTaggedPorts(List<String> portAbsoluteNames, List<String> portIfNames) {
        if (DiffUtil.isAllSameString(this.originalTaggedPorts, portAbsoluteNames)) {
            return;
        }
        this.taggedPorts.clear();
        this.taggedPorts.addAll(portAbsoluteNames);
        recordChange(TAGGED_PORTS, this.originalTaggedPortIfNames, portIfNames);
    }

    public void setUntaggedPorts(List<String> portAbsoluteNames, List<String> portIfNames) {
        if (DiffUtil.isAllSameString(this.originalUntaggedPorts, portAbsoluteNames)) {
            return;
        }
        this.untaggedPorts.clear();
        this.untaggedPorts.addAll(portAbsoluteNames);
        recordChange(UNTAGGED_PORTS, this.originalUntaggedPortIfNames, portIfNames);
    }

    public void addTaggedPort(PortDto port) {
        if (port == null) {
            return;
        }
        String ifName = DtoUtil.getIfName(port);
        if (ifName == null) {
            return;
        }
        if (this.taggedPorts.contains(port.getAbsoluteName())) {
            return;
        }
        this.taggedPorts.add(port.getAbsoluteName());
        recordChange(TAGGED_PORTS, null, NameUtil.getNodeIfName(port));
    }

    public void removeTaggedPort(PortDto port) {
        if (port == null) {
            return;
        }
        String ifName = DtoUtil.getIfName(port);
        if (ifName == null) {
            return;
        }
        if (!this.taggedPorts.contains(port.getAbsoluteName())) {
            return;
        }
        this.taggedPorts.remove(port.getAbsoluteName());
        recordChange(TAGGED_PORTS, NameUtil.getNodeIfName(port), null);
    }

    public void addUntaggedPort(PortDto port) {
        if (port == null) {
            return;
        }
        String ifName = DtoUtil.getIfName(port);
        if (ifName == null) {
            return;
        }
        if (this.untaggedPorts.contains(port.getAbsoluteName())) {
            return;
        }
        this.untaggedPorts.add(port.getAbsoluteName());
        recordChange(UNTAGGED_PORTS, null, NameUtil.getNodeIfName(port));
    }

    public void removeUntaggedPort(PortDto port) {
        if (port == null) {
            return;
        }
        String ifName = DtoUtil.getIfName(port);
        if (ifName == null) {
            return;
        }
        if (!this.untaggedPorts.contains(port.getAbsoluteName())) {
            return;
        }
        this.untaggedPorts.remove(port.getAbsoluteName());
        recordChange(UNTAGGED_PORTS, NameUtil.getNodeIfName(port), null);
    }

    @Override
    public String getNodeContext() {
        return this.nodeName;
    }

    @Override
    public String getPortContext() {
        if (this.vlanIf != null) {
            return this.vlanIf.getAbsoluteName();
        } else {
            return this.ownerName
                    + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_VLAN_IF
                    + ATTR.NAME_DELIMITER_SECONDARY + this.vlanName;
        }
    }

    @Override
    public BuildResult buildPortCommands() {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.vlanIf == null) {
            InventoryBuilder.changeContext(cmd, getPortContext());
        } else {
            InventoryBuilder.changeContext(cmd, this.vlanIf);
        }
        String vlanIfVariableName = "VLANIF_" + Integer.toHexString(System.identityHashCode(this));
        InventoryBuilder.assignVar(cmd, vlanIfVariableName);
        updateTaggedPorts(vlanIfVariableName);
        updateUntaggedPorts(vlanIfVariableName);
        this.cmd.addLastEditCommands();
        assignTargetPortToShellContextVariable();
        this.result = BuildResult.SUCCESS;
        return result;
    }

    private void updateTaggedPorts(String vlanIfVariableName) {
        if (!isChanged(TAGGED_PORTS)) {
            log().debug("no changes: " + TAGGED_PORTS);
            return;
        }
        List<DiffEntry<String>> taggedDiffs = DiffUtil.getStringDiff(this.originalTaggedPorts, this.taggedPorts);
        for (DiffEntry<String> taggedDiff : taggedDiffs) {
            if (taggedDiff.isCreated()) {
                cmd.addCommand(InventoryBuilder.translate(
                        CMD.PORT_STACK,
                        CMD.ARG_LOWER, taggedDiff.getTarget(),
                        CMD.ARG_UPPER, "$;" + vlanIfVariableName));
            } else if (taggedDiff.isDeleted()) {
                cmd.addCommand(InventoryBuilder.translate(
                        CMD.PORT_UNSTACK,
                        CMD.ARG_LOWER, taggedDiff.getBase(),
                        CMD.ARG_UPPER, "$;" + vlanIfVariableName));
            }
        }
    }

    private void updateUntaggedPorts(String vlanIfVariableName) {
        if (!isChanged(UNTAGGED_PORTS)) {
            log().debug("no changs: " + UNTAGGED_PORTS);
            return;
        }
        List<DiffEntry<String>> untaggedDiffs = DiffUtil.getStringDiff(this.originalUntaggedPorts, this.untaggedPorts);
        for (DiffEntry<String> untaggedDiff : untaggedDiffs) {
            if (untaggedDiff.isCreated()) {
                cmd.addCommand(InventoryBuilder.translate(
                        CMD.CONNECT_NETWORK_INSTANCE_PORT,
                        "_INSTANCE_", "$;" + vlanIfVariableName,
                        "_PORT_", untaggedDiff.getTarget()));
            } else if (untaggedDiff.isDeleted()) {
                cmd.addCommand(InventoryBuilder.translate(
                        CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                        "_INSTANCE_", "$;" + vlanIfVariableName,
                        "_PORT_", untaggedDiff.getBase()));
            }
        }
    }

    @Override
    public void buildPortDeleteCommand() {
        InventoryBuilder.changeContext(cmd, this.vlanIf);
        for (PortDto tagged : this.vlanIf.getTaggedVlans()) {
            cmd.addCommand(InventoryBuilder.translate(CMD.PORT_UNSTACK,
                    CMD.ARG_LOWER, tagged.getAbsoluteName(),
                    CMD.ARG_UPPER, this.vlanIf.getAbsoluteName()));
        }
        for (PortDto untagged : this.vlanIf.getUntaggedVlans()) {
            cmd.addCommand(InventoryBuilder.translate(
                    CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, this.vlanIf.getAbsoluteName(),
                    CMD.ARG_PORT, untagged.getAbsoluteName()));
        }
    }

    public PortDto getPort() {
        return this.vlanIf;
    }

    public String getObjectType() {
        return DiffObjectType.VLAN_SUBIF.getCaption();
    }

}