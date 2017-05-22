package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.mvo.vlan.SwitchPortMode;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.VlanUtil;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.*;

public class EthernetLAGCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeDto owner;
    private final String nodeName;
    private final String lagName;
    private final EthLagIfDto lag;
    private PortMode portMode = null;
    private SwitchPortMode switchPortMode = null;
    private final Set<String> addedPorts = new HashSet<String>();
    private final Set<String> removedPorts = new HashSet<String>();

    public EthernetLAGCommandBuilder(String nodeName, String lagName, String editorName) {
        super(PortDto.class, null, null, editorName);
        setConstraint(EthLagIfDto.class);
        this.owner = null;
        this.lag = null;
        this.lagName = lagName;
        this.nodeName = BuilderUtil.getNodeName(nodeName);
    }

    public EthernetLAGCommandBuilder(EthLagIfDto lag, String editorName) {
        super(PortDto.class, lag.getNode(), lag, editorName);
        setConstraint(lag.getClass());
        this.owner = lag.getNode();
        this.lag = lag;
        this.lagName = lag.getName();
        this.nodeName = null;
        if (lag != null) {
            String _portMode = VlanUtil.getPortMode(lag);
            if (_portMode != null) {
                this.portMode = PortMode.valueOf(_portMode);
            }
            String _switchPortMode = VlanUtil.getSwitchPortMode(lag);
            if (_switchPortMode != null) {
                this.switchPortMode = SwitchPortMode.valueOf(_switchPortMode);
            }
        }
    }

    public void setMemberPorts(Map<String, String> memberIfNamesAndAbsoluteNames) {
        if (lag != null) {
            Map<String, String> addMap = new HashMap<String, String>(memberIfNamesAndAbsoluteNames);
            for (EthPortDto member : lag.getBundlePorts()) {
                String id = InventoryIdCalculator.getId(member);
                String value = memberIfNamesAndAbsoluteNames.get(id);
                if (value == null) {
                    this.removedPorts.add(member.getAbsoluteName());
                    recordChange("lag-member", id, null);
                } else {
                    addMap.remove(id);
                }
            }
            for (Map.Entry<String, String> entry : addMap.entrySet()) {
                this.addedPorts.add(entry.getValue());
                recordChange("lag-member", null, entry.getKey());
            }
        } else {
            for (Map.Entry<String, String> entry : memberIfNamesAndAbsoluteNames.entrySet()) {
                this.addedPorts.add(entry.getValue());
                recordChange("lag-member", null, entry.getKey());
            }
        }
    }

    public void setPortMode(String portModeValue) {
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
    }

    public void setIgpCost(Integer cost) {
        setValue(MPLSNMS_ATTR.IGP_COST, cost);
    }

    public void setPortType(String ifType) {
        setValue(MPLSNMS_ATTR.PORT_TYPE, ifType);
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

    public String getPortContext() {
        if (lag != null) {
            return lag.getAbsoluteName();
        }
        if (owner == null && nodeName == null) {
            throw new IllegalStateException("no owner info.");
        }
        String base = null;
        if (owner != null) {
            base = owner.getName();
        } else {
            base = nodeName;
        }
        return base + ATTR.NAME_DELIMITER_PRIMARY + lagName;
    }

    public String getNodeContext() {
        if (owner != null) {
            return owner.getNode().getName();
        }
        return nodeName;
    }

    @Override
    public BuildResult buildPortCommands() throws InventoryException {
        if (lag != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        String currentLagName;
        String currentLagAbsName;
        if (lag == null) {
            currentLagName = this.lagName;
            currentLagAbsName = this.nodeName + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_LAG_PORT
                    + ATTR.NAME_DELIMITER_SECONDARY + this.lagName;
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_LAG_PORT, currentLagName);
            setValue(MPLSNMS_ATTR.PORT_TYPE, PortType.LAG.getCaption());
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            currentLagName = this.lag.getName();
            currentLagAbsName = this.lag.getAbsoluteName();
            InventoryBuilder.changeContext(cmd, lag);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, lag, attributes);
        }
        for (String addedPort : this.addedPorts) {
            InventoryBuilder.buildAddPortToBundleCommand(cmd, currentLagAbsName, addedPort);
        }
        for (String removedPort : this.removedPorts) {
            InventoryBuilder.buildRemovePortFromBundleCommand(cmd, currentLagAbsName, removedPort);
        }
        this.cmd.addLastEditCommands();
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
        assignTargetPortToShellContextVariable();
        if (lag != null) {
            if (!currentLagName.equals(this.lagName)) {
                InventoryBuilder.buildRenameCommands(cmd, this.lagName);
                recordChange("Name", currentLagName, this.lagName);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected void buildPortDeleteCommand() {
        InventoryBuilder.changeContext(cmd, lag);
        for (EthPortDto eth : lag.getBundlePorts()) {
            InventoryBuilder.buildRemovePortFromBundleCommand(cmd, lag.getAbsoluteName(), eth.getAbsoluteName());
        }
        InventoryBuilder.changeContext(cmd, lag.getOwner());
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                CMD.ARG_TYPE, lag.getObjectTypeName(),
                CMD.ARG_NAME, lag.getName());
    }

    public EthLagIfDto getPort() {
        return this.lag;
    }

    public String getObjectType() {
        return DiffObjectType.PORT.getCaption();
    }

}