package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.*;

public class AtmAPSCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeDto owner;
    private final String nodeName;
    private final String apsName;
    private final AtmApsIfDto aps;
    private final Set<String> addedPorts = new HashSet<String>();
    private final Set<String> removedPorts = new HashSet<String>();

    public AtmAPSCommandBuilder(String nodeName, String apsName, String editorName) {
        super(PortDto.class, null, null, editorName);
        setConstraint(AtmApsIfDto.class);
        this.owner = null;
        this.aps = null;
        this.apsName = apsName;
        this.nodeName = BuilderUtil.getNodeName(nodeName);
    }

    public AtmAPSCommandBuilder(AtmApsIfDto aps, String editorName) {
        super(PortDto.class, aps.getNode(), aps, editorName);
        setConstraint(aps.getClass());
        this.owner = aps.getNode();
        this.aps = aps;
        this.apsName = aps.getName();
        this.nodeName = null;
    }

    public void setMemberPorts(Map<String, String> memberIfNamesAndAbsoluteNames) {
        if (aps != null) {
            Map<String, String> addMap = new HashMap<String, String>(memberIfNamesAndAbsoluteNames);
            for (AtmPortDto member : aps.getAtmPorts()) {
                String id = InventoryIdCalculator.getId(member);
                String value = memberIfNamesAndAbsoluteNames.get(id);
                if (value == null) {
                    this.removedPorts.add(member.getAbsoluteName());
                    recordChange("aps-member", value, null);
                } else {
                    addMap.remove(id);
                }
            }
            for (Map.Entry<String, String> entry : addMap.entrySet()) {
                this.addedPorts.add(entry.getValue());
                recordChange("aps-member", null, entry.getKey());
            }
        } else {
            for (Map.Entry<String, String> entry : memberIfNamesAndAbsoluteNames.entrySet()) {
                this.addedPorts.add(entry.getValue());
                recordChange("aps-member", null, entry.getKey());
            }
        }
    }

    public void setIgpCost(Integer cost) {
        setValue(MPLSNMS_ATTR.IGP_COST, cost);
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

    public String getPortContext() {
        if (aps != null) {
            return aps.getAbsoluteName();
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
        return base + ATTR.NAME_DELIMITER_PRIMARY + apsName;
    }

    public String getNodeContext() {
        if (owner != null) {
            return owner.getNode().getName();
        }
        return nodeName;
    }

    @Override
    public BuildResult buildPortCommands() throws InventoryException {
        if (aps != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        String currentApsName;
        String currentApsAbsName;
        if (aps == null) {
            currentApsName = this.apsName;
            currentApsAbsName = this.nodeName + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_ATM_APS_PORT
                    + ATTR.NAME_DELIMITER_SECONDARY + this.apsName;
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_ATM_APS_PORT, currentApsName);
            setValue(MPLSNMS_ATTR.PORT_TYPE, PortType.ATM_APS.getCaption());
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        } else {
            currentApsName = this.aps.getName();
            currentApsAbsName = this.aps.getAbsoluteName();
            InventoryBuilder.changeContext(cmd, aps);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, aps, attributes);
        }
        for (String addedPort : this.addedPorts) {
            InventoryBuilder.buildAddPortToBundleCommand(cmd, currentApsAbsName, addedPort);
        }
        for (String removedPort : this.removedPorts) {
            InventoryBuilder.buildRemovePortFromBundleCommand(cmd, currentApsAbsName, removedPort);
        }
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
        this.cmd.addLastEditCommands();
        assignTargetPortToShellContextVariable();
        if (aps != null) {
            if (!currentApsName.equals(this.apsName)) {
                InventoryBuilder.buildRenameCommands(cmd, this.apsName);
                recordChange("Name", currentApsName, this.apsName);
            }
        }
        this.result = BuildResult.SUCCESS;
        return result;
    }

    @Override
    protected void buildPortDeleteCommand() {
        InventoryBuilder.changeContext(cmd, aps);
        for (AtmPortDto eth : aps.getAtmPorts()) {
            InventoryBuilder.buildRemovePortFromBundleCommand(cmd, aps.getAbsoluteName(), eth.getAbsoluteName());
        }
        InventoryBuilder.changeContext(cmd, aps.getOwner());
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                CMD.ARG_TYPE, aps.getObjectTypeName(),
                CMD.ARG_NAME, aps.getName());
    }

    public AtmApsIfDto getPort() {
        return this.aps;
    }

    public String getObjectType() {
        return DiffObjectType.PORT.getCaption();
    }

}