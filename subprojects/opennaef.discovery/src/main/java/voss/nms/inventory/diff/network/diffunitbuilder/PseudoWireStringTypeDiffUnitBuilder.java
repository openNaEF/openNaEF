package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.mpls.PseudowireDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CommandUtil;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.discovery.agent.alcatel.AlcatelExtInfoNames;
import voss.model.DefaultLogicalEthernetPort;
import voss.model.Port;
import voss.model.PseudoWirePort;
import voss.nms.inventory.builder.PseudoWireStringTypeCommandBuilder;
import voss.nms.inventory.builder.TextBasedPseudoWireStringTypeCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.diff.network.NetworkPseudoWire;

import java.io.IOException;
import java.util.*;

public class PseudoWireStringTypeDiffUnitBuilder extends DiffUnitBuilderImpl<NetworkPseudoWire, PseudowireDto> {
    private static final Logger log = LoggerFactory.getLogger(PseudoWireStringTypeDiffUnitBuilder.class);
    private Map<Port, String> portToAbsoluteNameMap;
    private DiffPolicy policy;
    private String userName;

    public PseudoWireStringTypeDiffUnitBuilder(DiffSet set, Map<Port, String> portToAbsoluteNameMap,
                                               DiffPolicy policy, String editorName) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.PSEUDOWIRE.getCaption(), DiffConstants.pwDepth, editorName);
        this.portToAbsoluteNameMap = portToAbsoluteNameMap;
        this.policy = policy;
        this.userName = editorName;
    }

    @Override
    protected DiffUnit update(String inventoryID, NetworkPseudoWire networkPw, PseudowireDto dbPw) throws IOException,
            InventoryException, ExternalServiceException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        TextBasedPseudoWireStringTypeCommandBuilder builder = new TextBasedPseudoWireStringTypeCommandBuilder(dbPw, userName);
        builder.setPseudoWireName(networkPw.getAc1().getPwName());
        String stauts = DtoUtil.getStringOrNull(dbPw, MPLSNMS_ATTR.FACILITY_STATUS);
        String newStatus = this.policy.getFacilityStatus(stauts);
        builder.setFacilityStatus(newStatus);

        PseudoWirePort pp1 = networkPw.getAc1();
        String ac1Name = null;
        String lsp1Name = null;
        String ifNodeName1 = null;
        String ac2Name = null;
        String lsp2Name = null;
        String ifNodeName2 = null;
        if (pp1 != null) {
            ac1Name = getAbsoluteNameForAc(pp1);
            ifNodeName1 = getNodeIfName(pp1);
            lsp1Name = getLspAbsoluteName(pp1);
        }
        PseudoWirePort pp2 = networkPw.getAc2();
        if (pp2 != null) {
            ac2Name = getAbsoluteNameForAc(pp2);
            ifNodeName2 = getNodeIfName(pp2);
            lsp2Name = getLspAbsoluteName(pp2);
        }
        builder.updateAttachmentCircuitName(ac1Name, ifNodeName1, ac2Name, ifNodeName2);
        builder.updateRsvpLspNames(lsp1Name, lsp2Name);

        builder.setBandwidth(pp1.getBandwidth());
        Object sdpID = pp1.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_PW_SDP_ID);
        if (sdpID != null) {
            builder.setSdpID(sdpID.toString());
        }
        Object serviceID = pp1.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_PW_SERVICE_ID);
        if (serviceID != null) {
            builder.setServiceID(serviceID.toString());
        }
        if (pp1.getAlcatelPipeType() != null) {
            builder.setPseudoWireType(pp1.getAlcatelPipeType().name());
        }
        super.applyExtraAttributes(DiffOperationType.UPDATE, builder, networkPw, dbPw);
        builders.add(builder);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        NetworkDiffUtil.setVersionCheckValues(builder);
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbPw).toString(), inventoryID, DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        CommandUtil.logCommands(builder);
        return unit;
    }

    @Override
    protected DiffUnit create(String inventoryID, NetworkPseudoWire networkPw) throws IOException, InventoryException,
            ExternalServiceException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        PseudoWireStringTypeCommandBuilder builder = new PseudoWireStringTypeCommandBuilder(policy.getDefaultPseudoWirePoolName(), userName);
        builder.setSource(DiffCategory.DISCOVERY.name());
        builder.setPseudoWireName(networkPw.getAc1().getPwName());
        String pwID = networkPw.getID();
        builder.setPseudoWireID(pwID);
        PseudoWirePort pp1 = networkPw.getAc1();
        buildAc1Command(builder, pp1);
        PseudoWirePort pp2 = networkPw.getAc2();
        buildAc2Command(builder, pp2);

        builder.setBandwidth(pp1.getBandwidth());
        Object sdpID = pp1.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_PW_SDP_ID);
        if (sdpID != null) {
            builder.setSdpID(sdpID.toString());
        }
        Object serviceID = pp1.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_PW_SERVICE_ID);
        if (serviceID != null) {
            builder.setServiceID(serviceID.toString());
        }
        if (pp1.getAlcatelPipeType() != null) {
            builder.setPseudoWireType(pp1.getAlcatelPipeType().name());
        }
        builder.setBandwidth(pp1.getBandwidth());
        builder.setFacilityStatus(this.policy.getDefaultFacilityStatus());
        super.applyExtraAttributes(DiffOperationType.ADD, builder, networkPw, null);
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

    private void buildAc1Command(PseudoWireStringTypeCommandBuilder builder, PseudoWirePort pwp) {
        String acName = getAbsoluteNameForAc(pwp);
        if (acName == null) {
            log.warn("attachment circuit not found: " + pwp.getDevice().getDeviceName() + ":" + pwp.getPseudoWireID());
        } else {
            builder.setAttachmentCircuit1Name(acName, pwp.getAttachedCircuitPort().getIfName());
        }
        String lspName = getLspAbsoluteName(pwp);
        if (lspName != null) {
            builder.setRsvpLsp1Name(lspName);
        }
        if (pwp.getAlcatelPipeType() != null) {
            builder.setPseudoWireType(pwp.getAlcatelPipeType().name());
        }
    }

    private void buildAc2Command(PseudoWireStringTypeCommandBuilder builder, PseudoWirePort pwp) {
        String acName = getAbsoluteNameForAc(pwp);
        if (acName == null) {
            log.warn("attachment circuit not found: " + pwp.getDevice().getDeviceName() + ":" + pwp.getPseudoWireID());
        } else {
            builder.setAttachmentCircuit2Name(acName, pwp.getAttachedCircuitPort().getIfName());
        }
        String lspName = getLspAbsoluteName(pwp);
        if (lspName != null) {
            builder.setRsvpLsp2Name(lspName);
        }
        if (pwp.getAlcatelPipeType() != null) {
            builder.setPseudoWireType(pwp.getAlcatelPipeType().name());
        }
    }

    private String getLspAbsoluteName(PseudoWirePort pwp) {
        String acLspName = (pwp.getTransmitLsp() == null ? null : pwp.getTransmitLsp().getIfName());
        if (acLspName == null) {
            return null;
        }
        String lspName = ATTR.POOL_TYPE_RSVP_LSP + ATTR.NAME_DELIMITER_SECONDARY
                + this.policy.getDefaultRsvpLspPoolName() + ATTR.NAME_DELIMITER_PRIMARY
                + ATTR.TYPE_ID + ATTR.NAME_DELIMITER_SECONDARY
                + pwp.getDevice().getDeviceName() + ":" + acLspName;
        return lspName;
    }

    private String getAbsoluteNameForAc(PseudoWirePort pwp) {
        Port ac = pwp.getAttachedCircuitPort();
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

    private String getNodeIfName(PseudoWirePort pp) {
        Port ac = pp.getAttachedCircuitPort();
        if (ac == null) {
            throw new IllegalStateException("no attachment circuit.");
        }
        if (ac instanceof DefaultLogicalEthernetPort) {
            ac = ((DefaultLogicalEthernetPort) ac).getPhysicalPort();
        }
        return ac.getDevice().getDeviceName() + ":" + ac.getIfName();
    }

    protected DiffUnit delete(String inventoryID, PseudowireDto dbPw) throws IOException,
            InventoryException, ExternalServiceException {
        PseudoWireStringTypeCommandBuilder builder = new PseudoWireStringTypeCommandBuilder(dbPw, userName);
        super.applyExtraAttributes(DiffOperationType.REMOVE, builder, null, dbPw);
        builder.buildDeleteCommand();
        CommandUtil.logCommands(builder);
        DiffUnit unit = new DiffUnit(DtoUtil.getMvoId(dbPw).toString(), inventoryID, DiffOperationType.REMOVE);
        unit.addBuilder(builder);
        return unit;
    }

    public static Map<String, NetworkPseudoWire> getNetworkPseudoWires(Collection<PseudoWirePort> pseudoWires) {
        Map<String, NetworkPseudoWire> networkPseudoWires = new HashMap<String, NetworkPseudoWire>();
        Map<String, NetworkPseudoWire> temp = new HashMap<String, NetworkPseudoWire>();
        for (PseudoWirePort pseudoWire : pseudoWires) {
            long vcID = pseudoWire.getPseudoWireID();
            String pwID;
            if (vcID == 0) {
                pwID = pseudoWire.getPwName();
            } else {
                pwID = String.valueOf(vcID);
            }
            NetworkPseudoWire pw = temp.get(pwID);
            if (pw == null) {
                pw = new NetworkPseudoWire(pwID);
                pw.setVcID(pseudoWire.getPseudoWireID());
                temp.put(pwID, pw);
            }
            pw.setAc(pseudoWire);
            String id = InventoryIdCalculator.getId(pseudoWire);
            networkPseudoWires.put(id, pw);
        }
        return networkPseudoWires;
    }
}