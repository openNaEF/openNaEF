package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import naef.dto.mpls.RsvpLspDto;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class TextBasedPseudoWireStringTypeCommandBuilder extends AbstractCommandBuilder {
    public static final String LSP1 = "lsp1";
    public static final String LSP2 = "lsp2";
    private static final long serialVersionUID = 1L;
    private final PseudowireDto pw;
    private PseudowireStringIdPoolDto pool;
    private String pseudoWireID;
    private String ac1Name;
    private String ac1NodeIfName;
    private String ac2Name;
    private String ac2NodeIfName;
    private String lsp1Name;
    private String lsp2Name;
    private String originalLsp1Name;
    private String originalLsp2Name;

    public TextBasedPseudoWireStringTypeCommandBuilder(PseudowireStringIdPoolDto pool, String editorName) {
        super(PseudowireDto.class, null, editorName);
        setConstraint(PseudowireDto.class);
        this.pw = null;
        try {
            this.pool = pool;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public TextBasedPseudoWireStringTypeCommandBuilder(PseudowireDto target, String editorName) {
        super(PseudowireDto.class, target, editorName);
        setConstraint(PseudowireDto.class);
        this.pw = target;
        if (target != null) {
            this.pseudoWireID = target.getStringId();
            this.pool = target.getStringIdPool();
            this.ac1Name = getAbsoluteName(target.getAc1());
            this.ac1NodeIfName = getNodeIfName(target.getAc1());
            this.ac2Name = getAbsoluteName(target.getAc2());
            this.ac2NodeIfName = getNodeIfName(target.getAc2());
            if (target.getRsvpLsps() != null) {
                Iterator<RsvpLspDto> lsps = target.getRsvpLsps().iterator();
                if (lsps.hasNext()) {
                    RsvpLspDto lsp1 = lsps.next();
                    this.lsp1Name = lsp1.getAbsoluteName();
                    this.originalLsp1Name = this.lsp1Name;
                }
                if (lsps.hasNext()) {
                    RsvpLspDto lsp2 = lsps.next();
                    this.lsp2Name = lsp2.getAbsoluteName();
                    this.originalLsp2Name = this.lsp2Name;
                }
            }
        }
    }

    private String getAbsoluteName(PortDto p) {
        if (p == null) {
            return null;
        }
        return p.getAbsoluteName();
    }

    private String getNodeIfName(PortDto p) {
        if (p == null) {
            return null;
        }
        return p.getNode().getName() + ":" + DtoUtil.getIfName(p);
    }

    public void setPseudoWireID(String id) {
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

    public void setAttachmentCircuit1Name(String acName, String ifName) {
        String oldAcName = null;
        String oldIfName = null;
        if (this.pw != null) {
            oldIfName = DtoUtil.getString(pw.getAc1(), MPLSNMS_ATTR.IFNAME);
            if (pw.getAc1() != null && DtoUtil.getString(pw.getAc1(), MPLSNMS_ATTR.IFNAME).equals(ifName)) {
                return;
            } else if (pw.getAc1() == null && acName == null) {
                return;
            }
            oldAcName = pw.getAc1().getAbsoluteName();
        }
        recordChange("Attachment Circuit 1", oldIfName, ifName);
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
        recordChange("Attachment Circuit 2", oldIfName, ifName);
        recordChange(ATTR.PW_AC2, oldAcName, acName);
        this.ac2Name = acName;
    }

    public void updateAttachmentCircuitName(String acAName, String aNodeIfName, String acBName, String bNodeIfName) {
        boolean ac1Used = false;
        boolean ac2Used = false;
        List<String> newNames = new ArrayList<String>();
        String nodeIfName = null;
        String aName = getMatchedName(aNodeIfName, ac1NodeIfName, ac2NodeIfName);
        if (aName != null) {
            if (aName.equals(ac1NodeIfName)) {
                ac1Used = true;
            } else {
                ac2Used = true;
            }
        } else {
            newNames.add(aName);
            nodeIfName = aNodeIfName;
        }

        String bName = getMatchedName(bNodeIfName, ac1NodeIfName, ac2NodeIfName);
        if (bName != null) {
            if (bName.equals(ac1NodeIfName)) {
                ac1Used = true;
            } else {
                ac2Used = true;
            }
        } else {
            newNames.add(bName);
            nodeIfName = bNodeIfName;
        }
        if (ac1Used && ac2Used) {
            return;
        }

        if (newNames.size() == 2) {
            if (ac1Used || ac2Used) {
                throw new IllegalStateException();
            }
            setAttachmentCircuit1Name(acAName, getIfNamePart(aNodeIfName));
            setAttachmentCircuit2Name(acBName, getIfNamePart(bNodeIfName));
        } else if (newNames.size() == 1) {
            String acName = newNames.iterator().next();
            if (ac1Used && ac2Used) {
                throw new IllegalStateException("no vacant ac.");
            } else if (!ac1Used && !ac2Used) {
                throw new IllegalStateException("too many vacant ac.");
            } else if (!ac1Used) {
                setAttachmentCircuit1Name(acName, getIfNamePart(nodeIfName));
            } else if (!ac2Used) {
                setAttachmentCircuit2Name(acName, getIfNamePart(nodeIfName));
            }
        } else {
            throw new IllegalStateException("no new names.");
        }
    }

    private String getIfNamePart(String nodeIfName) {
        if (nodeIfName == null) {
            throw new IllegalArgumentException();
        } else if (nodeIfName.indexOf(':') == -1) {
            throw new IllegalArgumentException();
        }
        return nodeIfName.split(":")[1];
    }

    private String getMatchedName(String key, String value1, String value2) {
        if (key == null) {
            return null;
        } else if (key.equals(value1)) {
            return value1;
        } else if (key.equals(value2)) {
            return value2;
        }
        return null;
    }

    public void setRsvpLsp1Name(String lspName) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (lspName.equals(lsp_.getAbsoluteName())) {
                    return;
                }
            }
        }
        recordChange(LSP1, this.lsp1Name, lspName);
        this.lsp1Name = lspName;
    }

    public void setRsvpLsp2Name(String lspName) {
        if (this.pw != null) {
            for (RsvpLspDto lsp_ : pw.getRsvpLsps()) {
                if (lspName.equals(lsp_.getAbsoluteName())) {
                    return;
                }
            }
        }
        recordChange(LSP2, this.lsp2Name, lspName);
        this.lsp2Name = lspName;
    }

    public void updateRsvpLspNames(String newLsp1Name, String newLsp2Name) {
        boolean lsp1Used = false;
        boolean lsp2Used = false;
        String _1 = getMatchedName(newLsp1Name, this.lsp1Name, this.lsp2Name);
        if (_1 != null) {
            if (_1.equals(this.lsp1Name)) {
                lsp1Used = true;
            } else {
                lsp2Used = true;
            }
        }
        String _2 = getMatchedName(newLsp2Name, this.lsp1Name, this.lsp2Name);
        if (_2 != null) {
            if (_2.equals(this.lsp1Name)) {
                lsp1Used = true;
            } else {
                lsp2Used = true;
            }
        }
        if (lsp1Used && lsp2Used) {
            return;
        }
        if (!lsp1Used && !lsp2Used) {
            if (!Util.isAllNull(this.lsp1Name, newLsp1Name)) {
                recordChange(LSP1, this.lsp1Name, newLsp1Name);
                this.lsp1Name = newLsp1Name;
            }
            if (!Util.isAllNull(this.lsp2Name, newLsp2Name)) {
                recordChange(LSP2, this.lsp2Name, newLsp2Name);
                this.lsp2Name = newLsp2Name;
            }
        } else if (!lsp1Used || !lsp2Used) {
            String newLspName = (_1 == null ? newLsp1Name : newLsp2Name);
            if (lsp1Used && !Util.isAllNull(this.lsp2Name, newLsp2Name)) {
                recordChange(LSP2, this.lsp2Name, newLsp2Name);
                this.lsp2Name = newLspName;

            } else if (lsp2Used && !Util.isAllNull(this.lsp1Name, newLsp1Name)) {
                recordChange(LSP1, this.lsp1Name, newLsp1Name);
                this.lsp1Name = newLspName;
            }
        } else {
            throw new IllegalStateException();
        }
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
        PseudowireDto pseudoWire = PseudoWireUtil.getPseudoWire(pool, this.pseudoWireID);
        if (pseudoWire != null && !DtoUtil.isSameMvoEntity(target, pseudoWire)) {
            throw new IllegalStateException("another pseudo-wire has same pseudowire-id.");
        }

        if (this.pw == null) {
            InventoryBuilder.changeContext(cmd, pool);
            InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_PSEUDOWIRE,
                    ATTR.ATTR_PW_ID_STRING, this.pseudoWireID,
                    ATTR.ATTR_PW_POOL_STRING, pool.getName());
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
        updateAc(originalAc, this.ac1Name, ATTR.PW_AC1);
    }

    private void updateAc2() {
        if (!isChanged(ATTR.PW_AC2)) {
            return;
        }
        PortDto originalAc = (this.pw == null ? null : this.pw.getAc2());
        updateAc(originalAc, this.ac2Name, ATTR.PW_AC2);
    }

    private void updateAc(PortDto originalAc, String acName, String acTarget) {
        if (originalAc == null && acName == null) {
            return;
        } else if (originalAc != null && acName == null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, acTarget);
        } else if (originalAc == null && acName != null) {
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, acTarget,
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, acName);
        } else {
            if (acName != null) {
                String originalAcName = getSimpleAbsoluteName(originalAc);
                if (Util.equals(originalAcName, acName)) {
                    return;
                }
            } else {
                throw new IllegalStateException("no ac");
            }

            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_REMOVE_AC, CMD.PSEUDOWIRE_REMOVE_AC_ARG, acTarget);
            InventoryBuilder.translate(cmd, CMD.PSEUDOWIRE_ADD_AC,
                    CMD.PSEUDOWIRE_ADD_AC_ARG1, acTarget,
                    CMD.PSEUDOWIRE_ADD_AC_ARG2, acName);
        }
    }

    private void updateLsps() {
        updateLsp(this.lsp1Name, this.originalLsp1Name);
        updateLsp(this.lsp2Name, this.originalLsp2Name);
    }

    private void updateLsp(String newName, String oldName) {
        if (newName != null) {
            if (oldName != null) {
                if (newName.equals(oldName)) {
                    return;
                }
                InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                        CMD.ARG_LOWER, oldName);
            }
            InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, newName);
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