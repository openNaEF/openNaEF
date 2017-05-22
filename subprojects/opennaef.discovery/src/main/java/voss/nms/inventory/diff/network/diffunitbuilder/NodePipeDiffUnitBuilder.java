package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.InterconnectionIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CommandUtil;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.model.*;
import voss.nms.inventory.builder.TextBasedNodePipeCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodePipeDiffUnitBuilder extends DiffUnitBuilderImpl<NodePipe<?>, InterconnectionIfDto> {
    private static final Logger log = LoggerFactory.getLogger(NodePipeDiffUnitBuilder.class);
    private Map<Port, String> portToAbsoluteNameMap;
    private DiffPolicy policy;
    private String userName;

    public NodePipeDiffUnitBuilder(DiffSet set, Map<Port, String> portToAbsoluteNameMap,
                                   DiffPolicy policy, String editorName) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.PIPE.getCaption(), DiffConstants.pipeDepth, editorName);
        this.portToAbsoluteNameMap = portToAbsoluteNameMap;
        this.policy = policy;
        this.userName = editorName;
    }

    @Override
    protected DiffUnit update(String inventoryID, NodePipe<?> pipe, InterconnectionIfDto dbPw) throws IOException,
            InventoryException, ExternalServiceException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        TextBasedNodePipeCommandBuilder builder = new TextBasedNodePipeCommandBuilder(dbPw, userName);
        builder.setVersionCheck(false);
        builder.setServiceID(pipe.getPipeName());
        String ac1Name = null;
        String ifNodeName1 = null;
        String ac2Name = null;
        String ifNodeName2 = null;
        Port ac = null;
        Port ac1 = pipe.getAttachmentCircuit1();
        if (ac1 != null) {
            ac1Name = getAbsoluteNameForAc(ac1);
            ifNodeName1 = getNodeIfName(ac1);
            ac = ac1;
        }
        Port ac2 = pipe.getAttachmentCircuit2();
        if (ac2 != null) {
            ac2Name = getAbsoluteNameForAc(ac2);
            ifNodeName2 = getNodeIfName(ac2);
            ac = ac2;
        }
        builder.updateAttachmentCircuitName(ac1Name, ifNodeName1, ac2Name, ifNodeName2);
        builder.setBandwidth(pipe.getBandwidth());
        if (ac != null) {
            if (ac instanceof EthernetPort || ac instanceof EthernetPortsAggregator
                    || ac instanceof DefaultLogicalEthernetPort || ac instanceof RouterVlanIf) {
                builder.setPseudoWireType(AlcatelPipeType.EPIPE.name());
            } else if (ac instanceof Channel) {
                builder.setPseudoWireType(AlcatelPipeType.CPIPE.name());
            } else {
            }
        }
        super.applyExtraAttributes(DiffOperationType.UPDATE, builder, pipe, dbPw);
        builders.add(builder);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbPw).toString(), inventoryID, DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        CommandUtil.logCommands(builder);
        return unit;
    }

    @Override
    protected DiffUnit create(String inventoryID, NodePipe<?> pipe) throws IOException, InventoryException,
            InventoryException, ExternalServiceException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        TextBasedNodePipeCommandBuilder builder = new TextBasedNodePipeCommandBuilder(pipe.getDevice().getDeviceName(), userName);
        builder.setSource(DiffCategory.DISCOVERY.name());
        builder.setVersionCheck(false);
        builder.setServiceID(pipe.getPipeName());
        builder.setPipeName(pipe.getPipeName());
        builder.setDescription(pipe.getIfDescr());
        String ac1Name = null;
        String ifNodeName1 = null;
        String ac2Name = null;
        String ifNodeName2 = null;
        Port ac = null;
        Port ac1 = pipe.getAttachmentCircuit1();
        if (ac1 != null) {
            ac1Name = getAbsoluteNameForAc(ac1);
            ifNodeName1 = getNodeIfName(ac1);
            ac = ac1;
        }
        Port ac2 = pipe.getAttachmentCircuit2();
        if (ac2 != null) {
            ac2Name = getAbsoluteNameForAc(ac2);
            ifNodeName2 = getNodeIfName(ac2);
            ac = ac2;
        }
        builder.updateAttachmentCircuitName(ac1Name, ifNodeName1, ac2Name, ifNodeName2);
        builder.setBandwidth(pipe.getBandwidth());
        if (ac != null) {
            if (ac instanceof EthernetPort
                    || ac instanceof EthernetPortsAggregator
                    || ac instanceof EthernetProtectionPort
                    || ac instanceof DefaultLogicalEthernetPort
                    || ac instanceof RouterVlanIf) {
                builder.setPseudoWireType(AlcatelPipeType.EPIPE.name());
            } else if (ac instanceof Channel) {
                builder.setPseudoWireType(AlcatelPipeType.CPIPE.name());
            } else {
            }
        }
        builder.setFacilityStatus(this.policy.getDefaultFacilityStatus());
        super.applyExtraAttributes(DiffOperationType.ADD, builder, pipe, null);
        builders.add(builder);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        CommandUtil.logCommands(builder);
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        unit.addBuilder(builder);
        return unit;
    }

    private String getAbsoluteNameForAc(Port ac) {
        if (ac == null) {
            return null;
        } else if (ac instanceof DefaultLogicalEthernetPort) {
            ac = ((DefaultLogicalEthernetPort) ac).getPhysicalPort();
        }
        String absoluteName = this.portToAbsoluteNameMap.get(ac);
        if (absoluteName == null) {
            throw new IllegalStateException("no absolute-name: " + ac.getFullyQualifiedName());
        }
        log.debug("* ac: " + ac.getFullyQualifiedName() + "->" + absoluteName);
        return absoluteName;
    }

    private String getNodeIfName(Port ac) {
        if (ac == null) {
            throw new IllegalStateException("no attachment circuit.");
        }
        if (ac instanceof DefaultLogicalEthernetPort) {
            ac = ((DefaultLogicalEthernetPort) ac).getPhysicalPort();
        }
        return ac.getDevice().getDeviceName() + ":" + ac.getIfName();
    }

    protected DiffUnit delete(String inventoryID, InterconnectionIfDto dbPw) throws IOException,
            InventoryException, ExternalServiceException {
        TextBasedNodePipeCommandBuilder builder = new TextBasedNodePipeCommandBuilder(dbPw, userName);
        super.applyExtraAttributes(DiffOperationType.REMOVE, builder, null, dbPw);
        builder.buildDeleteCommand();
        CommandUtil.logCommands(builder);
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbPw).toString(), inventoryID, DiffOperationType.REMOVE);
        unit.addBuilder(builder);
        return unit;
    }

}