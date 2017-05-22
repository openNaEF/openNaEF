package voss.nms.inventory.builder;

import naef.dto.HardPortDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.mvo.vlan.SwitchPortMode;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.AtmPvcCoreUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.core.server.util.VlanUtil;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;
import java.util.List;

public class PhysicalPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeElementDto owner;
    private final String ownerName;
    private final String nodeName;
    private final HardPortDto port;

    private String portName = null;
    private PortType portType = null;
    private PortMode portMode = null;
    private SwitchPortMode switchPortMode = null;

    private String vlanEnabled = null;
    private String nodeVlanEnabled = null;
    private Boolean atmPvcEnabled = null;
    private Boolean frPvcEnabled = null;

    private String originalVlanEnabled = null;
    private String originalNodeVlanEnabled = null;
    private Boolean originalAtmPvcEnabled = null;
    private Boolean originalFrPvcEnabled = null;

    public PhysicalPortCommandBuilder(NodeElementDto owner, String editorName) {
        super(PortDto.class, owner.getNode(), null, editorName);
        this.owner = owner;
        this.port = null;
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = owner.getNode().getName();
    }

    public PhysicalPortCommandBuilder(String ownerName, String editorName) {
        super(PortDto.class, null, null, editorName);
        this.owner = null;
        this.port = null;
        this.ownerName = ownerName;
        this.nodeName = BuilderUtil.getNodeName(ownerName);
    }

    public PhysicalPortCommandBuilder(NodeElementDto owner, HardPortDto port, String editorName) {
        super(PortDto.class, owner.getNode(), port, editorName);
        if (port == null) {
            throw new IllegalArgumentException("port is null.");
        }
        setConstraint(port.getClass());
        this.owner = owner;
        this.port = port;
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = BuilderUtil.getNodeName(ownerName);
        if (port != null) {
            initialize(port);
        }
    }

    private void initialize(HardPortDto port) {
        this.portName = port.getOwner().getName();
        this.atmPvcEnabled = Boolean.valueOf(DtoUtil.getString(port, ATTR.FEATURE_ATM_PVC));
        this.originalAtmPvcEnabled = this.atmPvcEnabled;
        this.frPvcEnabled = Boolean.valueOf(DtoUtil.getString(port, ATTR.ATTR_FR_ENCAPSULATION));
        this.originalFrPvcEnabled = this.frPvcEnabled;
        this.vlanEnabled = DtoUtil.getStringOrNull(port, ATTR.FEATURE_VLAN);
        this.originalVlanEnabled = this.vlanEnabled;
        this.nodeVlanEnabled = DtoUtil.getStringOrNull(port.getNode(), ATTR.FEATURE_VLAN);
        this.originalNodeVlanEnabled = this.nodeVlanEnabled;
        this.portType = PortType.getByType(port.getObjectTypeName());
        String _portMode = VlanUtil.getPortMode(port);
        if (_portMode != null) {
            this.portMode = PortMode.valueOf(_portMode);
        }
        String _switchPortMode = VlanUtil.getSwitchPortMode(port);
        if (_switchPortMode != null) {
            this.switchPortMode = SwitchPortMode.valueOf(_switchPortMode);
        }
        this.cmd.addVersionCheckTarget(port);
    }

    public void setPortName(String name) {
        this.portName = name;
    }

    public void setPortType(PortType type) {
        if (Util.equals(type, DtoUtil.getStringOrNull(target, MPLSNMS_ATTR.PORT_TYPE))) {
            return;
        }
        setValue(MPLSNMS_ATTR.PORT_TYPE, type.getCaption());
        this.portType = type;
    }

    public void setPortMode(String portModeValue) {
        if (!CoreAppBuilderUtil.isEthernetType(this.portType)) {
            return;
        }
        if (portModeValue != null) {
            this.portMode = PortMode.valueOf(portModeValue);
            setValue(MPLSNMS_ATTR.ATTR_PORT_MODE, this.portMode.name());
        } else {
            if (this.switchPortMode != null) {
                throw new IllegalArgumentException("switch-port-mode is not null.");
            }
            this.portMode = null;
            setValue(MPLSNMS_ATTR.ATTR_PORT_MODE, (String) null);
        }
    }

    public void setSwitchPortMode(String switchPortModeValue) {
        if (!CoreAppBuilderUtil.isEthernetType(this.portType)) {
            return;
        }
        if (switchPortModeValue != null) {
            if (this.portMode == null) {
                throw new IllegalArgumentException("port-mode is null.");
            }
            this.switchPortMode = SwitchPortMode.valueOf(switchPortModeValue);
            setValue(MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE, this.switchPortMode.name());
        } else {
            this.switchPortMode = null;
            setValue(MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE, (String) null);
        }
        if (SwitchPortMode.TRUNK == this.switchPortMode) {
            switch (this.portMode) {
                case IP:
                    setVlanEnabled(true);
                    break;
                case VLAN:
                    setNodeVlanEnabled(true);
                    break;
            }
        }
    }

    public void setBandwidth(Long bandwidth) {
        setValue(MPLSNMS_ATTR.BANDWIDTH, bandwidth);
    }

    public void setAdminBandwidth(String bandwidth) {
        setValue(MPLSNMS_ATTR.PORTSPEED_ADMIN, bandwidth);
    }

    public void setAdminDuplex(String duplex) {
        setValue(MPLSNMS_ATTR.DUPLEX_ADMIN, duplex);
    }

    public void setOperDuplex(String duplex) {
        setValue(MPLSNMS_ATTR.DUPLEX_OPER, duplex);
    }

    public void setIgpCost(Integer cost) {
        setValue(MPLSNMS_ATTR.IGP_COST, cost);
    }

    public void setZone(String zone) {
        setValue(MPLSNMS_ATTR.ZONE, zone);
    }

    public void setBestEffortGuaranteedBandwidth(Long bestEffortValue) {
        setValue(MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH, bestEffortValue.toString());
    }

    public void setOspfAreaID(String area) {
        setValue(MPLSNMS_ATTR.OSPF_AREA_ID, area);
    }

    public void setFixedRTT(String rtt) {
        setValue(MPLSNMS_ATTR.FIXED_RTT, rtt);
    }

    public void setVariableRTT(String rtt) {
        setValue(MPLSNMS_ATTR.VARIABLE_RTT, rtt);
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public void setStormControlActions(List<String> actions) {
        replaceValues(MPLSNMS_ATTR.STORMCONTROL_ACTION, actions, true);
    }

    public void addStormControlAction(String action) {
        addValue(MPLSNMS_ATTR.STORMCONTROL_ACTION, action);
    }

    public void removeStormControlAction(String action) {
        removeValue(MPLSNMS_ATTR.STORMCONTROL_ACTION, action);
    }

    public void setStormControlBroadcastLevel(String level) {
        setValue(MPLSNMS_ATTR.STORMCONTROL_BROADCAST_LEVEL, level);
    }

    public void setVlanEnabled(boolean vlanEnabled) {
        if (this.vlanEnabled != null && vlanEnabled) {
            return;
        } else if (this.vlanEnabled == null && !vlanEnabled) {
            return;
        }
        if (vlanEnabled) {
            this.vlanEnabled = ATTR.FEATURE_VLAN_DOT1Q;
        } else {
            this.vlanEnabled = null;
        }
        recordChange("Port VLAN Feature", this.originalVlanEnabled, this.vlanEnabled);
    }

    public void setNodeVlanEnabled(boolean vlanEnabled) {
        if (this.nodeVlanEnabled != null && vlanEnabled) {
            return;
        } else if (this.nodeVlanEnabled == null && !vlanEnabled) {
            return;
        }
        if (vlanEnabled) {
            this.nodeVlanEnabled = ATTR.FEATURE_VLAN_DOT1Q;
        } else {
            this.nodeVlanEnabled = null;
        }
        recordChange("Switch Feature", this.originalNodeVlanEnabled, this.nodeVlanEnabled);
    }

    public void setAtmPvcEnabled(boolean atmPvcEnabled) {
        if (this.atmPvcEnabled.booleanValue() == atmPvcEnabled) {
            return;
        }
        this.atmPvcEnabled = Boolean.valueOf(atmPvcEnabled);
        recordChange("ATM Feature", this.originalAtmPvcEnabled, this.atmPvcEnabled);
        checkFeature();
    }

    public void setFrPvcEnabled(boolean frPvcEnabled) {
        if (this.frPvcEnabled.booleanValue() == frPvcEnabled) {
            return;
        }
        this.frPvcEnabled = Boolean.valueOf(frPvcEnabled);
        recordChange("FrameRelay Feature", this.originalFrPvcEnabled, this.frPvcEnabled);
        checkFeature();
    }

    public void checkFeature() {
        if (atmPvcEnabled != null && atmPvcEnabled.booleanValue()
                && frPvcEnabled != null && frPvcEnabled.booleanValue()) {
            throw new IllegalStateException("Cannot instanciate ATM Feature and Serial Feature simultaneously.");
        }
    }

    public String getPortContext() {
        if (owner == null && ownerName == null) {
            throw new IllegalStateException("no owner info.");
        }
        String base = null;
        if (owner != null) {
            base = owner.getAbsoluteName();
        } else {
            base = ownerName;
        }
        return base + ATTR.NAME_DELIMITER_PRIMARY + portName + ATTR.NAME_DELIMITER_PRIMARY;
    }

    public String getNodeContext() {
        if (owner != null) {
            return owner.getNode().getName();
        }
        return nodeName;
    }

    @Override
    public BuildResult buildPortCommands() throws InventoryException {
        if (portType == null) {
            throw new IllegalStateException("no port-type selected.");
        }
        if (isVlanFeatureToBeEnable()) {
            recordChange(ATTR.FEATURE_VLAN, null, ATTR.FEATURE_VLAN_DOT1Q);
        } else if (isVlanFeatureToBeDisable()) {
            recordChange(ATTR.FEATURE_VLAN, ATTR.FEATURE_VLAN_DOT1Q, null);
        } else if (this.port != null && isNodeVlanFeatureToBeEnable()) {
            recordChange(this.port.getNode(), ATTR.FEATURE_VLAN, null, ATTR.FEATURE_VLAN_DOT1Q);
        }
        if (isAtmFeatureEnabled()) {
            recordChange(ATTR.FEATURE_ATM_PVC, null, Boolean.TRUE.toString());
        } else if (isAtmFeatureDisabled()) {
            recordChange(ATTR.FEATURE_ATM_PVC, Boolean.TRUE.toString(), null);
        }
        if (port != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        checkFeature();
        if (port == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, ownerName);
            SimpleNodeBuilder.buildHardwareCreationCommands(cmd, ATTR.TYPE_JACK, portName);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, portType.getType(), "");
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes, false);
        } else {
            InventoryBuilder.changeContext(cmd, port);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, port, attributes);
        }
        this.cmd.addLastEditCommands();
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
        assignTargetPortToShellContextVariable();
        maintainFeature();
        if (port != null) {
            String currentPortName = port.getOwner().getName();
            if (!currentPortName.equals(portName)) {
                cmd.addCommand(CMD.CONTEXT_DOWN);
                InventoryBuilder.buildRenameCommands(cmd, portName);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected void buildPortDeleteCommand() {
        InventoryBuilder.changeContext(cmd, port.getOwner());
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                CMD.ARG_TYPE, port.getObjectTypeName(),
                CMD.ARG_NAME, port.getName());
        cmd.addCommand(CMD.CONTEXT_DOWN);
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                CMD.ARG_TYPE, ATTR.TYPE_JACK,
                CMD.ARG_NAME, port.getOwner().getName());
        recordChange("Port", DtoUtil.getIfName(port), null);
    }

    private void maintainFeature() {
        if (isVlanFeatureToBeEnable()) {
            SimpleNodeBuilder.buildVlanFeatureEnableCommands(cmd);
        } else if (isVlanFeatureToBeDisable()) {
            SimpleNodeBuilder.buildVlanFeatureDisableCommands(cmd);
        }
        if (isAtmFeatureEnabled()) {
            SimpleNodeBuilder.buildAtmPvcFeatureEnableCommand(cmd);
        } else if (isAtmFeatureDisabled()) {
            SimpleNodeBuilder.buildAtmPvcFeatureDisableCommand(cmd);
        }
        if (isNodeVlanFeatureToBeEnable()) {
            InventoryBuilder.changeContext(this.cmd, getNodeContext());
            SimpleNodeBuilder.buildVlanFeatureEnableCommands(cmd);
            InventoryBuilder.changeContext(this.cmd, getPortContext());
        }
    }

    private boolean isVlanFeatureToBeEnable() {
        if (this.vlanEnabled == null) {
            return false;
        }
        if (this.originalVlanEnabled == null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isVlanFeatureToBeDisable() {
        if (this.vlanEnabled != null) {
            return false;
        }
        if (this.originalVlanEnabled == null) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isNodeVlanFeatureToBeEnable() {
        if (this.nodeVlanEnabled == null) {
            return false;
        }
        if (this.originalNodeVlanEnabled == null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAtmFeatureEnabled() {
        if (AtmPvcCoreUtil.isAtmCapablePort(portType.getType())) {
            return false;
        }
        return atmPvcEnabled != null && atmPvcEnabled && !AtmPvcCoreUtil.isAtmPvcEnabled(port);
    }

    private boolean isAtmFeatureDisabled() {
        return atmPvcEnabled != null && !atmPvcEnabled && AtmPvcCoreUtil.isAtmPvcEnabled(port);
    }

    public PortDto getPort() {
        return this.port;
    }

    public String getObjectType() {
        return DiffObjectType.PORT.getCaption();
    }

}