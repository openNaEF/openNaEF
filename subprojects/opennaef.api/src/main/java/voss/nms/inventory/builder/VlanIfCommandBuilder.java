package voss.nms.inventory.builder;

import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.common.diff.DiffEntry;
import voss.core.common.diff.DiffUtil;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.*;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.util.*;

public class VlanIfCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;

    public static final String TAGGED_PORTS = "Tagged Ports";
    public static final String UNTAGGED_PORTS = "Untagged Ports";

    private final String ownerName;
    private final String nodeName;
    private VlanDto vlan = null;
    private VlanDto originalVlan = null;
    private VlanIdPoolDto vlanPool = null;
    private String vlanIdName = null;
    private final VlanIfDto vlanIf;
    private String vlanName = null;
    private boolean keepBinding = false;
    private boolean removeVlanLink = true;
    private final List<String> taggedPorts = new ArrayList<String>();
    private final List<String> untaggedPorts = new ArrayList<String>();
    private final List<String> originalTaggedPorts = new ArrayList<String>();
    private final List<String> originalUntaggedPorts = new ArrayList<String>();
    private final List<String> originalTaggedPortIfNames = new ArrayList<String>();
    private final List<String> originalUntaggedPortIfNames = new ArrayList<String>();

    public VlanIfCommandBuilder(String ownerName, String editorName) {
        super(VlanIfDto.class, null, null, editorName);
        try {
            this.ownerName = ownerName;
            this.nodeName = BuilderUtil.getNodeName(ownerName);
            this.vlanIf = null;
            setConstraint(VlanIfDto.class);
            initialize(null);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VlanIfCommandBuilder(NodeDto node, String editorName) {
        super(VlanIfDto.class, node, null, editorName);
        this.ownerName = node.getAbsoluteName();
        this.nodeName = node.getName();
        this.vlanIf = null;
        initialize(null);
        setConstraint(VlanIfDto.class);
    }

    public VlanIfCommandBuilder(PortDto owner, String editorName) {
        super(VlanIfDto.class, owner.getNode(), null, editorName);
        if (!(owner instanceof EthPortDto || owner instanceof EthLagIfDto)) {
            throw new IllegalArgumentException();
        }
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = owner.getNode().getName();
        this.vlanIf = null;
        initialize(null);
        setConstraint(VlanIfDto.class);
    }

    public VlanIfCommandBuilder(PortDto owner, VlanIfDto port, String editorName) {
        super(VlanIfDto.class, owner.getNode(), port, editorName);
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = owner.getNode().getName();
        this.vlanIf = port;
        initialize(port);
        setConstraint(VlanIfDto.class);
    }

    public VlanIfCommandBuilder(NodeDto owner, VlanIfDto port, String editorName) {
        super(VlanIfDto.class, owner, port, editorName);
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = owner.getName();
        this.vlanIf = port;
        initialize(port);
        setConstraint(VlanIfDto.class);
    }

    private void initialize(VlanIfDto port) {
        if (port == null) {
            return;
        }
        this.vlanName = port.getName();
        this.vlan = port.getTrafficDomain();
        this.originalVlan = this.vlan;
        this.vlanPool = (this.vlan == null ? null : vlan.getIdPool());
        for (PortDto tagged : port.getTaggedVlans()) {
            this.originalTaggedPorts.add(tagged.getAbsoluteName());
            this.originalTaggedPortIfNames.add(DtoUtil.getIfName(tagged));
        }
        for (PortDto tagged : port.getLowerLayers()) {
            this.originalTaggedPorts.add(tagged.getAbsoluteName());
            this.originalTaggedPortIfNames.add(DtoUtil.getIfName(tagged));
        }
        for (PortDto untagged : port.getUntaggedVlans()) {
            if (VrfIfDto.class.isInstance(untagged)) {
                continue;
            }
            this.originalUntaggedPorts.add(untagged.getAbsoluteName());
            this.originalUntaggedPortIfNames.add(DtoUtil.getIfName(untagged));
        }
        this.cmd.addVersionCheckTarget(port);
    }

    public void setVlan(VlanDto vlan) {
        if (this.vlan != null) {
            if (DtoUtil.mvoEquals(this.vlan, vlan)) {
                return;
            } else {
                throw new IllegalArgumentException("It is not possible to change the vlan to which the vlan-if exist. [" +
                        NameUtil.getCaption(this.vlan) + "]->[" + NameUtil.getCaption(vlan) + "]");
            }
        }
        this.vlan = vlan;
        this.vlanPool = this.vlan.getIdPool();
        recordChange("VLAN Pool/ID", null, NameUtil.getCaption(vlan));
    }

    public void setVlan(String vlanPoolName, String vlanIdName) {
        if (Util.isAllNull(vlanPoolName, vlanIdName)) {
            return;
        }
        if (Util.isNull(vlanPoolName, vlanIdName)) {
            throw new IllegalArgumentException("One of vlanPoolName or vlanIdName is null. "
                    + vlanPoolName + ":" + vlanIdName);
        }
        VlanIdPoolDto pool;
        try {
            pool = VlanUtil.getPool(vlanPoolName);
        } catch (Exception e) {
            throw new IllegalArgumentException("There is a problem with the specification contents of vlan-pool: " + vlanPoolName, e);
        }
        if (pool == null) {
            throw new IllegalArgumentException("vlan-pool does not exist: " + vlanPoolName);
        }
        this.vlanPool = pool;
        Integer id = null;
        try {
            id = Integer.valueOf(vlanIdName);
        } catch (Exception e) {
            throw new IllegalArgumentException("There is a problem with the specification contents of vlan-id: " + vlanIdName, e);
        }
        VlanDto vlan = VlanUtil.getVlan(pool, id);
        if (vlan != null) {
            setVlan(vlan);
        } else {
            this.vlanIdName = vlanIdName;
            recordChange("VLAN Pool/ID", null, vlanPoolName + ":" + vlanIdName);
        }
    }

    public void setVlanName(String vlanName) {
        if (vlanName == null) {
            throw new IllegalArgumentException("vlanName is null.");
        } else if (this.vlanName != null && this.vlanName.equals(vlanName)) {
            return;
        }
        recordChange("VLAN Name", this.vlanName, vlanName);
        this.vlanName = vlanName;
    }

    public String getVlanName() {
        if (this.vlanName != null) {
            return this.vlanName;
        } else {
            return getIfName();
        }
    }

    public void setVlanId(Integer vlanID) {
        if (vlanID == null) {
            throw new IllegalArgumentException("vlan-id is null.");
        }
        setValue(ATTR.ATTR_VLAN_IF_ID, vlanID.toString());
        if (getIfName() == null) {
            setIfName(AbsoluteNameFactory.getSwitchVlanIfName(vlanID));
        }
    }

    public void setSuffix(String suffix) {
        setValue(MPLSNMS_ATTR.SUFFIX, suffix);
    }

    public void setTaggedPorts(List<String> portAbsoluteNames, List<String> portIfNames) {
        if (DiffUtil.isAllSameString(this.originalTaggedPorts, portAbsoluteNames)) {
            return;
        }
        this.taggedPorts.addAll(portAbsoluteNames);
        recordChange(TAGGED_PORTS, this.originalTaggedPortIfNames, portIfNames);
    }

    public void setUntaggedPorts(List<String> portAbsoluteNames, List<String> portIfNames) {
        if (DiffUtil.isAllSameString(this.originalUntaggedPorts, portAbsoluteNames)) {
            return;
        }
        this.untaggedPorts.addAll(portAbsoluteNames);
        recordChange(UNTAGGED_PORTS, this.originalUntaggedPortIfNames, portIfNames);
    }

    public void setKeepBinding(boolean value) {
        this.keepBinding = value;
    }

    public void setRemoveVlanLink(boolean value) {
        this.removeVlanLink = value;
    }

    public void setSviEnable(boolean enable) {
        if (enable) {
            setValue(MPLSNMS_ATTR.SVI_ENABLED, Boolean.TRUE.toString());
        } else {
            setValue(MPLSNMS_ATTR.SVI_ENABLED, (String) null);
        }
    }

    public void setBandwidth(Long bandwidth) {
        String bandwidthValue = null;
        if (bandwidth != null) {
            bandwidthValue = bandwidth.toString();
        }
        setValue(MPLSNMS_ATTR.BANDWIDTH, bandwidthValue);
    }

    public void setExternalInventoryDBStatus(String status) {
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_STATUS, status);
    }

    public void setOspfAreaID(String area) {
        setValue(MPLSNMS_ATTR.OSPF_AREA_ID, area);
    }

    public void setIgpCost(Integer cost) {
        String costValue = null;
        if (cost != null) {
            costValue = cost.toString();
        }
        setValue(MPLSNMS_ATTR.IGP_COST, costValue);
    }

    public void setZone(String zone) {
        setValue(MPLSNMS_ATTR.ZONE, zone);
    }

    public void setBestEffortGuaranteedBandwidth(Long value) {
        String bestEffortValue = null;
        if (value != null) {
            bestEffortValue = value.toString();
        }
        setValue(MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH, bestEffortValue);
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    @Override
    public String getPortContext() {
        return this.ownerName + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
    }

    @Override
    public String getNodeContext() {
        return this.nodeName;
    }

    @Override
    public BuildResult buildPortCommands() {
        String ifName = getIfName();
        if (this.vlanIf != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (vlanIf == null) {
            if (ifName == null) {
                throw new IllegalArgumentException();
            }
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, ownerName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.FEATURE_VLAN, ATTR.FEATURE_VLAN_DOT1Q);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_VLAN_IF, getVlanName());
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            InventoryBuilder.changeContext(cmd, vlanIf.getOwner());
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.FEATURE_VLAN, ATTR.FEATURE_VLAN_DOT1Q);
            InventoryBuilder.changeContext(cmd, vlanIf);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, vlanIf, attributes);
        }
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
        this.cmd.addLastEditCommands();
        assignTargetPortToShellContextVariable();
        String vlanIfVariableName = "VLANIF_" + Integer.toHexString(System.identityHashCode(this));
        InventoryBuilder.assignVar(cmd, vlanIfVariableName);
        maintainTrafficDomain(vlanIfVariableName);
        if (!keepBinding) {
            updateTaggedPorts(vlanIfVariableName);
            updateUntaggedPorts(vlanIfVariableName);
        }
        if (vlanIf != null) {
            String currentPortName = vlanIf.getName();
            if (!currentPortName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        this.result = BuildResult.SUCCESS;
        return result;
    }

    private void maintainTrafficDomain(String vlanIfVariableName) {
        if (this.originalVlan != null) {
            return;
        } else if (this.vlanPool == null) {
            return;
        }
        if (this.vlan != null) {
            InventoryBuilder.changeContext(cmd, this.vlan);
        } else if (this.vlanIdName != null) {
            InventoryBuilder.changeContext(cmd, this.vlanPool, ATTR.NETWORK_TYPE_ID, this.vlanIdName);
        } else {
            throw new IllegalArgumentException();
        }
        cmd.addLastEditCommands();
        InventoryBuilder.translate(cmd, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, "$;" + vlanIfVariableName);
    }

    private void updateTaggedPorts(String vlanIfVariableName) {
        if (!isChanged(TAGGED_PORTS)) {
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
        removeVlanDomain();
        if (this.removeVlanLink) {
            removeVlanLinks();
        }
        InventoryBuilder.changeContext(cmd, this.vlanIf);
        if (!keepBinding) {
            for (PortDto tagged : this.vlanIf.getTaggedVlans()) {
                cmd.addCommand(InventoryBuilder.translate(CMD.PORT_UNSTACK,
                        CMD.ARG_LOWER, tagged.getAbsoluteName(),
                        CMD.ARG_UPPER, this.vlanIf.getAbsoluteName()));
            }
            for (PortDto untagged : this.vlanIf.getUntaggedVlans()) {
                if (VrfIfDto.class.isInstance(untagged)) {
                    continue;
                }
                cmd.addCommand(InventoryBuilder.translate(
                        CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                        "_INSTANCE_", this.vlanIf.getAbsoluteName(),
                        "_PORT_", untagged.getAbsoluteName()));
            }
        }
        super.buildPortDeleteCommand();
    }

    @Override
    protected void checkNetworkUsage() {
        if (!this.preCheckEnable) {
            return;
        }
        Set<NetworkDto> networks = new HashSet<NetworkDto>();
        MvoDtoSet<NetworkDto> vlanUpperNetwork = new MvoDtoSet<NetworkDto>();
        if (this.vlanIf.getTrafficDomain() != null) {
            vlanUpperNetwork.addAll(this.vlanIf.getTrafficDomain().getUpperLayers());
        }
        for (NetworkDto network : vlanIf.getNetworks()) {
            if (DtoUtil.mvoEquals(network, this.vlan)) {
                continue;
            } else if (vlanUpperNetwork.contains(network)) {
                continue;
            } else if (VlanSegmentDto.class.isInstance(network)) {
                if (this.removeVlanLink) {
                    continue;
                }
            }
            networks.add(network);
        }
        if (networks.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (NetworkDto network : networks) {
                sb.append(DtoUtil.toDebugString(network)).append(", ");
            }
            throw new IllegalStateException("vlan-if is used by network: " + sb.toString());
        }
    }

    private void removeVlanDomain() {
        if (this.vlan == null) {
            return;
        }
        VlanIfDto target = null;
        for (VlanIfDto vif : this.vlan.getMemberVlanifs()) {
            if (DtoUtil.mvoEquals(vif, this.vlanIf)) {
                target = vif;
                break;
            }
        }
        if (target == null) {
            return;
        }
        InventoryBuilder.changeContext(this.cmd, vlan);
        this.cmd.addVersionCheckTarget(vlan);
        InventoryBuilder.translate(this.cmd, CMD.UNBIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, vlanIf.getAbsoluteName());
        this.cmd.addLastEditCommands();

    }

    private void removeVlanLinks() {
        if (this.vlan == null) {
            return;
        }
        removeVlanLinksFromVlan();
        for (VlanSegmentDto vlanLink : this.vlanIf.getVlanLinks()) {
            removeVlanLink(vlanLink);
        }
    }

    private void removeVlanLinksFromVlan() {
        for (NetworkDto lower : this.vlan.getLowerLayerLinks()) {
            if (!VlanSegmentDto.class.isInstance(lower)) {
                continue;
            }
            VlanSegmentDto vlanLink = (VlanSegmentDto) lower;
            if (vlanLink.getMemberPorts().size() != 2) {
                throw new IllegalStateException("The member port number of vlan-link is not 2: " + DtoUtil.toDebugString(vlanLink));
            }
            for (PortDto member : vlanLink.getMemberPorts()) {
                if (DtoUtil.mvoEquals(this.vlanIf, member)) {
                    removeVlanLinkFromVlan(vlanLink);
                    break;
                }
            }
        }
    }

    private void removeVlanLinkFromVlan(VlanSegmentDto vlanLink) {
        InventoryBuilder.changeContext(this.cmd, this.vlan);
        this.cmd.log(vlanLink);
        InventoryBuilder.translate(this.cmd, CMD.EXCLUDE_ELEMENT_FROM_NETWORK,
                CMD.ARG_FQN, InventoryBuilder.getMvoContext(vlanLink));
        this.cmd.addLastEditCommands();
        this.cmd.addVersionCheckTarget(this.vlan);
    }

    private void removeVlanLink(VlanSegmentDto vlanLink) {
        InventoryBuilder.changeContext(this.cmd, vlanLink);
        for (NetworkDto network : vlanLink.getLowerLayerLinks()) {
            if (!LinkDto.class.isInstance(network)) {
                throw new IllegalStateException("- unexpected lower-layer-link found: " + network.getAbsoluteName());
            }
            removeLowerLink((LinkDto) network);
        }
        this.cmd.addCommand(CMD.CONTEXT_RESET);
        this.cmd.log(vlanLink);
        InventoryBuilder.translate(this.cmd, CMD.LINK_DISCONNECT_BY_MVOID,
                CMD.ARG_MVOID, DtoUtil.getMvoId(vlanLink).toString());
    }

    private void removeLowerLink(LinkDto l2Link) {
        if (l2Link.getMemberPorts().size() != 2) {
            throw new IllegalStateException("not p2p link: " + DtoUtil.toDebugString(l2Link));
        }
        this.cmd.log(l2Link);
        InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                CMD.ARG_LOWER, InventoryBuilder.getMvoContext(l2Link));
    }

    public PortDto getPort() {
        return this.vlanIf;
    }

    public String getObjectType() {
        return DiffObjectType.VLAN_SUBIF.getCaption();
    }

}