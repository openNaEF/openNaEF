package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.RsvpLspUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.*;

public class RsvpLspCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final RsvpLspIdPoolDto pool;
    private final RsvpLspDto lsp;
    private String lspName;
    private String ingressNodeName;
    private String activePathTarget = null;
    private final String originalActivePathTarget;
    private String primaryHopName = null;
    private String secondaryHopName = null;
    private final String originalPrimaryHopName;
    private final String originalSecondaryHopName;

    public static final String ACTIVE_PATH_IS_PRIMARY = "primary";
    public static final String ACTIVE_PATH_IS_SECONDARY = "secondary";

    public RsvpLspCommandBuilder(RsvpLspIdPoolDto pool, String lspName, NodeDto ingress, String editorName) {
        super(RsvpLspDto.class, null, editorName);
        setConstraint(RsvpLspDto.class);
        this.lsp = null;
        this.pool = pool;
        this.lspName = lspName;
        this.ingressNodeName = ingress.getName();
        this.originalPrimaryHopName = null;
        this.originalSecondaryHopName = null;
        this.originalActivePathTarget = null;
        setValue(MPLSNMS_ATTR.LSP_NAME, lspName);
    }

    public RsvpLspCommandBuilder(String poolName, String lspName, String ingressName, String editorName) {
        super(RsvpLspDto.class, null, editorName);
        setConstraint(RsvpLspDto.class);
        if (lspName == null) {
            throw new IllegalArgumentException("no lspName.");
        }
        this.lsp = null;
        this.pool = RsvpLspUtil.getIdPool(poolName);
        this.lspName = lspName;
        this.ingressNodeName = ingressName;
        this.originalPrimaryHopName = null;
        this.originalSecondaryHopName = null;
        this.originalActivePathTarget = null;
        setValue(MPLSNMS_ATTR.LSP_NAME, lspName);
    }

    public RsvpLspCommandBuilder(RsvpLspDto target, String editorName) {
        super(RsvpLspDto.class, target, editorName);
        setConstraint(RsvpLspDto.class);
        if (target == null) {
            throw new IllegalArgumentException("target-lsp is not set.");
        }
        this.lsp = target;
        this.pool = target.getIdPool();
        RsvpLspHopSeriesDto primary = target.getHopSeries1();
        if (primary != null) {
            this.primaryHopName = primary.getAbsoluteName();
            this.originalPrimaryHopName = primaryHopName;
        } else {
            this.originalPrimaryHopName = null;
        }
        RsvpLspHopSeriesDto backup = target.getHopSeries2();
        if (backup != null) {
            this.secondaryHopName = backup.getAbsoluteName();
            this.originalSecondaryHopName = secondaryHopName;
        } else {
            this.originalSecondaryHopName = null;
        }
        RsvpLspHopSeriesDto active = target.getActiveHopSeries();
        if (active != null) {
            if (primary != null && DtoUtil.isSameMvoEntity(primary, active)) {
                this.activePathTarget = ACTIVE_PATH_IS_PRIMARY;
                this.originalActivePathTarget = ACTIVE_PATH_IS_PRIMARY;
            } else if (backup != null && DtoUtil.isSameMvoEntity(backup, active)) {
                this.activePathTarget = ACTIVE_PATH_IS_SECONDARY;
                this.originalActivePathTarget = ACTIVE_PATH_IS_SECONDARY;
            } else {
                this.originalActivePathTarget = null;
            }
        } else {
            this.originalActivePathTarget = null;
        }
    }
    public String getLspID() {
        if (this.lspName == null) {
            throw new IllegalArgumentException("lsp-name is not set.");
        } else if (this.ingressNodeName == null) {
            throw new IllegalArgumentException("ingress-node-name is not set.");
        }
        return this.ingressNodeName + ":" + this.lspName;
    }

    public void setPrimaryPathHopName(String pathName) {
        if (!Util.hasDiff(this.originalPrimaryHopName, pathName)) {
            return;
        }
        if (pathName == null) {
            setValue(MPLSNMS_ATTR.LSP_PRIMARY_PATH_OPER_STATUS, (String) null);
        }
        recordChange(MPLSNMS_ATTR.LSP_PRIMARY_PATH, this.originalPrimaryHopName, pathName);
        this.primaryHopName = pathName;
    }

    public void setSecondaryPathHopName(String pathName) {
        if (!Util.hasDiff(this.originalSecondaryHopName, pathName)) {
            return;
        }
        if (pathName == null) {
            setValue(MPLSNMS_ATTR.LSP_SECONDARY_PATH_OPER_STATUS, (String) null);
        }
        recordChange(MPLSNMS_ATTR.LSP_BACKUP_PATH, this.originalSecondaryHopName, pathName);
        this.secondaryHopName = pathName;
    }

    public void setActivePath(String target) {
        if (target != null && !target.equals(ACTIVE_PATH_IS_PRIMARY) && !target.equals(ACTIVE_PATH_IS_SECONDARY)) {
            throw new IllegalArgumentException("invalid target: " + target);
        } else if (!Util.hasDiff(this.activePathTarget, target)) {
            return;
        }
        this.activePathTarget = target;
    }

    public void setPrimaryPathOperStatus(String status) {
        setValue(MPLSNMS_ATTR.LSP_PRIMARY_PATH_OPER_STATUS, status);
    }

    public void setSecondaryPathOperStatus(String status) {
        setValue(MPLSNMS_ATTR.LSP_SECONDARY_PATH_OPER_STATUS, status);
    }

    public void setSdpId(String sdpId) {
        setValue(MPLSNMS_ATTR.LSP_SDP_ID, sdpId);
    }

    public void setTerm(Collection<String> terms) {
        List<String> temp = new ArrayList<String>();
        temp.addAll(terms);
        Collections.sort(temp);
        StringBuilder sb = new StringBuilder();
        for (String term : temp) {
            sb.append(":").append(term).append(":");
        }
        setValue(MPLSNMS_ATTR.LSP_TERM, sb.toString());
    }

    public void setServiceId(String sdpId) {
        setValue(MPLSNMS_ATTR.SERVICE_ID, sdpId);
    }

    public void setTunnelId(String sdpId) {
        setValue(MPLSNMS_ATTR.LSP_TUNNEL_ID, sdpId);
    }

    public void setServiceType(String serviceType) {
        setValue(MPLSNMS_ATTR.SERVICE_TYPE, serviceType);
    }

    public void setOperationStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public String getAbsoluteName() {
        return this.pool.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY + getLspID();
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (!Util.isNull(this.primaryHopName, this.secondaryHopName)
                && this.primaryHopName.equals(this.secondaryHopName)) {
            throw new IllegalStateException("primary and secondary path is pointing same path."
                    + this.lspName + " -> " + this.primaryHopName + ", " + this.secondaryHopName);
        }
        if (this.lsp == null) {
            String lspID = getLspID();
            InventoryBuilder.changeContext(cmd, this.pool);
            InventoryBuilder.buildNetworkIDCreationCommand(cmd,
                    ATTR.NETWORK_TYPE_RSVPLSP,
                    ATTR.ATTR_RSVPLSP_ID, lspID,
                    ATTR.ATTR_RSVPLSP_POOL, this.pool.getAbsoluteName());
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    naef.mvo.mpls.RsvpLsp.Attr.INGRESS_NODE.getName(),
                    this.ingressNodeName);
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(cmd, lsp);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        updatePathChanges();
        this.cmd.addLastEditCommands();
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        return BuildResult.SUCCESS;
    }

    private void updatePathChanges() {
        boolean activePathChanged = !Util.equals(this.originalActivePathTarget, this.activePathTarget);
        boolean primaryPathChanged = isChanged(MPLSNMS_ATTR.LSP_PRIMARY_PATH) &&
                !Util.equals(this.originalPrimaryHopName, this.primaryHopName);
        boolean secondaryPathChanged = isChanged(MPLSNMS_ATTR.LSP_BACKUP_PATH) &&
                !Util.equals(this.originalSecondaryHopName, this.secondaryHopName);
        if (!activePathChanged && !primaryPathChanged && !secondaryPathChanged) {
            return;
        }
        String originalActivePathName = null;
        if (this.lsp != null) {
            RsvpLspHopSeriesDto activePath = this.lsp.getActiveHopSeries();
            if (activePath != null) {
                originalActivePathName = activePath.getAbsoluteName();
            }
        }
        String activePathName = null;
        if (this.activePathTarget != null) {
            if (this.activePathTarget.equals(ACTIVE_PATH_IS_PRIMARY)) {
                activePathName = this.primaryHopName;
            } else if (this.activePathTarget.equals(ACTIVE_PATH_IS_SECONDARY)) {
                activePathName = this.secondaryHopName;
            }
        }
        if (activePathChanged) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_ACTIVE_PATH, null);
            recordChange(MPLSNMS_ATTR.LSP_ACTIVE_PATH, originalActivePathName, activePathName);
        } else if (activePathName == null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_ACTIVE_PATH, null);
            recordChange(MPLSNMS_ATTR.LSP_ACTIVE_PATH, originalActivePathName, null);
        }
        if (primaryPathChanged) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PRIMARY_PATH, this.primaryHopName);
            recordChange(MPLSNMS_ATTR.LSP_PRIMARY_PATH, this.originalPrimaryHopName, this.primaryHopName);
        }
        if (secondaryPathChanged) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_BACKUP_PATH, this.secondaryHopName);
            recordChange(MPLSNMS_ATTR.LSP_BACKUP_PATH, this.originalSecondaryHopName, this.secondaryHopName);
        }
        if (activePathName != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_ACTIVE_PATH, activePathName);
            recordChange(MPLSNMS_ATTR.LSP_ACTIVE_PATH, originalActivePathName, activePathName);
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (this.lsp == null) {
            throw new IllegalArgumentException("no lsp found.");
        } else if (this.lsp.getPseudowires().size() > 0) {
            throw new IllegalArgumentException("lsp is used by pseudowires.");
        }
        InventoryBuilder.changeContext(cmd, this.lsp);
        if (this.lsp.getActiveHopSeries() != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    MPLSNMS_ATTR.LSP_ACTIVE_PATH, null);
        }
        RsvpLspHopSeriesDto primaryPath = this.lsp.getHopSeries1();
        if (primaryPath != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PRIMARY_PATH, null);
        }
        RsvpLspHopSeriesDto secondaryPath = this.lsp.getHopSeries2();
        if (secondaryPath != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_BACKUP_PATH, null);
        }
        if (DtoUtil.getStringOrNull(lsp, MPLSNMS_ATTR.LSP_PAIR) != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, null);
        }
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_RSVPLSP_ID, ATTR.ATTR_RSVPLSP_POOL);
        recordChange("RSVP-LSP", this.lspName, null);
        return BuildResult.SUCCESS;
    }

    public static List<String> getActivePathCondidates() {
        List<String> result = new ArrayList<String>();
        result.add(RsvpLspCommandBuilder.ACTIVE_PATH_IS_PRIMARY);
        result.add(RsvpLspCommandBuilder.ACTIVE_PATH_IS_SECONDARY);
        return result;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP.getCaption();
    }
}