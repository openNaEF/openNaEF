package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanSegmentGatewayIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class VlanSegmentGatewayIfCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final PortDto owner;
    private final String ownerName;
    private final String nodeName;
    private final VlanSegmentGatewayIfDto tagChangerIf;
    private String innerVlanIfAbsoluteName = null;
    private String originalInnerVlanIfAbsoluteName = null;

    public VlanSegmentGatewayIfCommandBuilder(String ownerName, String editorName) {
        super(VlanSegmentGatewayIfDto.class, null, null, editorName);
        try {
            this.owner = null;
            this.ownerName = ownerName;
            this.nodeName = BuilderUtil.getNodeName(ownerName);
            this.tagChangerIf = null;
            setConstraint(VlanSegmentGatewayIfDto.class);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public VlanSegmentGatewayIfCommandBuilder(PortDto owner, String editorName) {
        super(VlanSegmentGatewayIfDto.class, owner.getNode(), null, editorName);
        if (!(owner instanceof EthPortDto || owner instanceof EthLagIfDto)) {
            throw new IllegalArgumentException();
        }
        this.owner = owner;
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = owner.getNode().getName();
        this.tagChangerIf = null;
        setConstraint(VlanSegmentGatewayIfDto.class);
    }

    public VlanSegmentGatewayIfCommandBuilder(VlanSegmentGatewayIfDto port, String editorName) {
        super(VlanSegmentGatewayIfDto.class, port.getNode(), port, editorName);
        this.owner = (PortDto) port.getOwner();
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = port.getNode().getName();
        this.tagChangerIf = port;
        initialize(port);
        setConstraint(VlanSegmentGatewayIfDto.class);
    }

    private void initialize(VlanSegmentGatewayIfDto port) {
        if (port == null) {
            throw new IllegalStateException();
        }
        this.innerVlanIfAbsoluteName = port.getVlanIf().getAbsoluteName();
        this.originalInnerVlanIfAbsoluteName = this.innerVlanIfAbsoluteName;
        this.cmd.addVersionCheckTarget(port);
    }

    public void setInnerVlanId(Integer innerVlanID) {
        if (innerVlanID == null) {
            throw new IllegalArgumentException("vlan-id must not be null.");
        }
        String newInnerVlanIfName = AbsoluteNameFactory.getSwitchVlanIfName(innerVlanID);
        String newInnerVlanIfAbsoluteName = ATTR.TYPE_NODE + ATTR.NAME_DELIMITER_SECONDARY + this.nodeName
                + ATTR.NAME_DELIMITER_PRIMARY
                + ATTR.TYPE_VLAN_IF + ATTR.NAME_DELIMITER_SECONDARY + newInnerVlanIfName;
        if (newInnerVlanIfAbsoluteName.equals(this.innerVlanIfAbsoluteName)) {
            return;
        }
        recordChange(ATTR.TAGCHANGER_INNER_VLAN_ID, this.innerVlanIfAbsoluteName, newInnerVlanIfAbsoluteName);
        this.innerVlanIfAbsoluteName = newInnerVlanIfAbsoluteName;
    }

    public void setOuterVlanId(String outerVlanID) {
        if (outerVlanID == null) {
            throw new IllegalArgumentException("vlan-id is null.");
        }
        setValue(ATTR.TAGCHANGER_OUTER_VLAN_ID, outerVlanID);
    }

    public void setSuffix(String suffix) {
        setValue(MPLSNMS_ATTR.SUFFIX, suffix);
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

    public String getPortContext() {
        if (owner != null) {
            return owner.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
        }
        return this.ownerName + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
    }

    public String getNodeContext() {
        if (owner != null) {
            return owner.getNode().getName();
        }
        return this.nodeName;
    }

    @Override
    public BuildResult buildPortCommands() {
        String ifName = getIfName();
        if (this.tagChangerIf != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (tagChangerIf == null) {
            if (ifName == null) {
                throw new IllegalArgumentException();
            }
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, this.ownerName);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_VLAN_SEGMENT_GATEWAY_IF, ifName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TAGCHANGER_INNER_VLAN_ID, this.innerVlanIfAbsoluteName);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            InventoryBuilder.changeContext(cmd, tagChangerIf);
            if (this.originalInnerVlanIfAbsoluteName == null
                    || !this.originalInnerVlanIfAbsoluteName.equals(this.innerVlanIfAbsoluteName)) {
                InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TAGCHANGER_INNER_VLAN_ID, this.innerVlanIfAbsoluteName);
            }
            InventoryBuilder.buildAttributeUpdateCommand(cmd, tagChangerIf, attributes);
        }
        this.cmd.addLastEditCommands();
        assignTargetPortToShellContextVariable();
        if (tagChangerIf != null) {
            String currentPortName = tagChangerIf.getName();
            if (!currentPortName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        this.result = BuildResult.SUCCESS;
        return result;
    }

    public PortDto getPort() {
        return this.tagChangerIf;
    }

    public String getObjectType() {
        return DiffObjectType.TAG_CHANGER.getCaption();
    }

}