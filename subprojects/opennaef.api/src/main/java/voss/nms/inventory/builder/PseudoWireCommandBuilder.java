package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireLongIdPoolDto;
import naef.dto.mpls.RsvpLspDto;
import tef.MVO.MvoId;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.PseudoWireUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class PseudoWireCommandBuilder extends AbstractCommandBuilder {
    public static final String LSP1 = "lsp1";
    public static final String LSP2 = "lsp2";
    private static final long serialVersionUID = 1L;
    private final PseudowireDto pw;
    private PseudowireLongIdPoolDto pool;
    private Long pseudoWireID;
    private PortDto ac1;
    private PortDto ac2;
    private String ac1Name;
    private String ac2Name;
    private RsvpLspDto lsp1;
    private RsvpLspDto lsp2;
    private String lsp1Name;
    private String lsp2Name;
    private String originalLsp1Name;
    private String originalLsp2Name;

    public PseudoWireCommandBuilder(String poolName, String editorName) {
        super(PseudowireDto.class, null, editorName);
        setConstraint(PseudowireDto.class);
        this.pw = null;
        try {
            this.pool = PseudoWireUtil.getPool(poolName);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public PseudoWireCommandBuilder(PseudowireLongIdPoolDto pool, String editorName) {
        super(PseudowireDto.class, null, editorName);
        setConstraint(PseudowireDto.class);
        this.pw = null;
        try {
            this.pool = pool;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public PseudoWireCommandBuilder(PseudowireDto target, String editorName) {
        super(PseudowireDto.class, target, editorName);
        setConstraint(PseudowireDto.class);
        this.pw = target;
        if (target != null) {
            this.pseudoWireID = target.getLongId();
            this.pool = target.getLongIdPool();
            this.ac1 = target.getAc1();
            this.ac2 = target.getAc2();
            if (target.getRsvpLsps() != null) {
                Iterator<RsvpLspDto> lsps = target.getRsvpLsps().iterator();
                if (lsps.hasNext()) {
                    this.lsp1 = lsps.next();
                    this.lsp1Name = this.lsp1.getAbsoluteName();
                    this.originalLsp1Name = this.lsp1Name;
                }
                if (lsps.hasNext()) {
                    this.lsp2 = lsps.next();
                    this.lsp2Name = this.lsp2.getAbsoluteName();
                    this.originalLsp2Name = this.lsp2Name;
                }
            }
        }
    }

    public void setPseudoWireID(Long id) {
        this.pseudoWireID = id;
    }

    public void setServiceID(String id) {
        setValue(MPLSNMS_ATTR.SERVICE_ID, id);
    }

    public void setSdpID(String id) {
        setValue(MPLSNMS_ATTR.LSP_SDP_ID, id);
    }

    public void setPseudoWireName(String name) {
        setValue(MPLSNMS_ATTR.PSEUDOWIRE_NAME, name);
    }

    public void setPool(PseudowireLongIdPoolDto pool) {
        this.pool = pool;
    }

    public void setAttachmentCircuit1(PortDto ac) {
        String prevAc = null;
        if (this.pw != null) {
            if (DtoUtil.isSameMvoEntity(pw.getAc1(), ac)) {
                return;
            }
            prevAc = DtoUtil.getAbsoluteName(this.pw.getAc1());
        }
        recordChange(ATTR.PW_AC1, prevAc, DtoUtil.getAbsoluteName(ac));
        this.ac1 = ac;
    }

    public void setAttachmentCircuit2(PortDto ac) {
        String prevAc = null;
        if (this.pw != null) {
            if (DtoUtil.isSameMvoEntity(pw.getAc2(), ac)) {
                return;
            }
            prevAc = DtoUtil.getAbsoluteName(this.pw.getAc2());
        }
        recordChange(ATTR.PW_AC2, prevAc, DtoUtil.getAbsoluteName(ac));
        this.ac2 = ac;
    }

    public void setAttachmentCircuit1Name(String acName, String ifName) {
        String oldAcName = null;
        if (this.pw != null) {
            if (pw.getAc1() != null && DtoUtil.getString(pw.getAc1(), MPLSNMS_ATTR.IFNAME).equals(ifName)) {
                return;
            } else if (pw.getAc1() == null && acName == null) {
                return;
            }
            oldAcName = pw.getAc1().getAbsoluteName();
        }
        recordChange(ATTR.PW_AC1, oldAcName, acName);
        this.ac1Name = acName;
    }

    public void setAttachmentCircuit2Name(String acName, String ifName) {
        String oldAcName = null;
        String oldIfName = null;
        if (this.pw != null) {
            oldIfName = DtoUtil.getString(pw.getAc2(), MPLSNMS_ATTR.IFNAME);
            if (pw.getAc2() != null && oldIfName.equals(ifName)) {
                return;
            } else if (pw.getAc2() == null && acName == null) {
                return;
            }
            oldAcName = pw.getAc2().getAbsoluteName();
        }
        recordChange(ATTR.PW_AC2, oldAcName, acName);
        this.ac2Name = acName;
    }

    public void setRsvpLsp1(RsvpLspDto lsp) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (DtoUtil.isSameMvoEntity(lsp, lsp_)) {
                    return;
                }
            }
        }
        String lspName = (lsp == null ? "" : lsp.getAbsoluteName());
        recordChange(LSP1, lsp1Name, lspName);
        this.lsp1 = lsp;
    }

    public void setRsvpLsp1Name(String lspName) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (lspName.equals(lsp_.getAbsoluteName())) {
                    return;
                }
            }
        }
        recordChange(LSP1, lsp1Name, lspName);
        this.lsp1Name = lspName;
    }

    public void setRsvpLsp2(RsvpLspDto lsp) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (DtoUtil.isSameMvoEntity(lsp, lsp_)) {
                    return;
                }
            }
        }
        String lspName = (lsp == null ? "" : lsp.getAbsoluteName());
        recordChange(LSP2, lsp2Name, lspName);
        this.lsp2 = lsp;
    }

    public void setRsvpLsp2Name(String lspName) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (lspName.equals(lsp_.getAbsoluteName())) {
                    return;
                }
            }
        }
        recordChange(LSP2, lsp2Name, lspName);
        this.lsp2Name = lspName;
    }

    public void setPseudoWireType(String type) {
        setValue(MPLSNMS_ATTR.PSEUDOWIRE_TYPE, type);
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setServiceType(String type) {
        setValue(MPLSNMS_ATTR.SERVICE_TYPE, type);
    }

    public void setActualServiceType(String type) {
        setValue(MPLSNMS_ATTR.ACCOMMODATION_SERVICE_TYPE, type);
    }

    public void setBandwidth(Long bandwidth) {
        String value;
        if (bandwidth == null) {
            value = null;
        } else {
            value = bandwidth.toString();
        }
        setValue(MPLSNMS_ATTR.CONTRACT_BANDWIDTH, value);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.pw != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.pseudoWireID == null) {
            throw new IllegalStateException("no pseudo-wire id selected.");
        }
        PseudowireDto pseudoWire = PseudoWireUtil.getPseudoWire(pool, this.pseudoWireID.toString());
        if (pseudoWire != null && !DtoUtil.isSameMvoEntity(target, pseudoWire)) {
            throw new IllegalStateException("another pseudo-wire has same pseudowire-id.");
        }

        if (this.pw == null) {
            InventoryBuilder.changeContext(cmd, pool);
            InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_PSEUDOWIRE,
                    ATTR.ATTR_PW_ID_LONG, this.pseudoWireID.toString(),
                    ATTR.ATTR_PW_POOL_LONG, pool.getName());
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(cmd, this.pw);
        }
        cmd.addLastEditCommands();
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        updateAc1();
        updateAc2();
        updateLsps();
        return BuildResult.SUCCESS;
    }

    private void updateAc1() {
        if (!isChanged(ATTR.PW_AC1)) {
            return;
        }
        PortDto originalAc = (this.pw == null ? null : this.pw.getAc1());
        updateAc(originalAc, this.ac1, this.ac1Name, ATTR.PW_AC1);
    }

    private void updateAc2() {
        if (!isChanged(ATTR.PW_AC2)) {
            return;
        }
        PortDto originalAc = (this.pw == null ? null : this.pw.getAc2());
        updateAc(originalAc, this.ac2, this.ac2Name, ATTR.PW_AC2);
    }

    private void updateAc(PortDto originalAc, PortDto ac, String acName, String acTarget) {
        if (ac != null && acName == null) {
            acName = ac.getAbsoluteName();
        }
        if (originalAc == null && acName == null) {
            return;
        } else if (originalAc != null && acName == null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, acTarget);
        } else if (originalAc == null && acName != null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, acTarget,
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, acName);
        } else {
            if (ac != null) {
                if (DtoUtil.isSameMvoEntity(originalAc, ac)) {
                    return;
                }
            } else if (acName != null) {
                String originalAcName = getSimpleAbsoluteName(ac);
                if (Util.equals(originalAcName, acName)) {
                    return;
                }
            } else {
                throw new IllegalStateException("no ac");
            }

            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, acTarget);
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, acTarget,
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, ac.getAbsoluteName());
        }
    }

    private void updateLsps() {
        if (this.pw != null) {
            updateLspsByDto();
        } else {
            bindLsps();
        }
    }

    private void bindLsps() {
        if (lsp1Name != null) {
            if (this.originalLsp1Name != null) {
                InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                        CMD.ARG_LOWER, this.originalLsp1Name);
            }
            InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, lsp1Name);
        }
        if (lsp2Name != null) {
            if (this.originalLsp2Name != null) {
                InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                        CMD.ARG_LOWER, this.originalLsp2Name);
            }
            InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, lsp2Name);
        }
    }

    private void updateLspsByDto() {
        HashMap<MvoId, RsvpLspDto> deleteSet = new HashMap<MvoId, RsvpLspDto>();
        if (this.pw != null) {
            for (RsvpLspDto lsp : this.pw.getRsvpLsps()) {
                deleteSet.put(DtoUtil.getMvoId(lsp), lsp);
            }
        }
        RsvpLspDto lsp1_ = null;
        if (lsp1 != null) {
            lsp1_ = deleteSet.get(DtoUtil.getMvoId(lsp1));
        }
        if (lsp1_ != null) {
            deleteSet.remove(DtoUtil.getMvoId(lsp1));
        }
        RsvpLspDto lsp2_ = null;
        if (lsp2 != null) {
            lsp2_ = deleteSet.get(DtoUtil.getMvoId(lsp2));
        }
        if (lsp2_ != null) {
            deleteSet.remove(DtoUtil.getMvoId(lsp2));
        }
        for (RsvpLspDto deleteLsp : deleteSet.values()) {
            InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, deleteLsp.getAbsoluteName());
        }
        if (lsp1_ == null && lsp1 != null) {
            InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, lsp1.getAbsoluteName());
        }
        if (lsp2_ == null && lsp2 != null) {
            InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, lsp2.getAbsoluteName());
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (this.pw == null) {
            return BuildResult.FAIL;
        }
        InventoryBuilder.changeContext(cmd, this.pw);
        if (pw.getAc1() != null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, ATTR.PW_AC1);
        }
        if (pw.getAc2() != null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, ATTR.PW_AC2);
        }
        for (RsvpLspDto lsp : pw.getRsvpLsps()) {
            InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, lsp.getAbsoluteName());
        }
        cmd.addLastEditCommands();
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_PW_ID_LONG, ATTR.ATTR_PW_POOL_LONG);
        recordChange("PseudoWire", pw.getLongId(), null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.PSEUDOWIRE.getCaption();
    }

}