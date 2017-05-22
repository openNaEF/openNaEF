package voss.nms.inventory.builder;

import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.pos.PosApsIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.AtmPvcCoreUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.MvoDtoSet;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.List;

public class AtmApsPvcPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final PortDto owner;
    private final AtmPvcIfDto port;
    private Integer vpi = null;
    private Integer vci = null;

    public AtmApsPvcPortCommandBuilder(PortDto owner, String editorName) {
        super(PortDto.class, owner.getNode(), null, editorName);
        if (!isSupportedParentPort(owner)) {
            throw new IllegalArgumentException("parent port is not aps port. parent=" + owner.getAbsoluteName());
        }
        setConstraint(AtmPvcIfDto.class);
        this.owner = owner;
        this.port = null;
    }

    public AtmApsPvcPortCommandBuilder(AtmPvcIfDto pvc, String editorName) {
        super(PortDto.class, pvc.getNode(), pvc, editorName);
        if (pvc.getOwner() == null || pvc.getOwner().getOwner() == null) {
            throw new IllegalArgumentException("unexpected node-element structure:" + pvc.getAbsoluteName());
        } else if (!(pvc.getOwner().getOwner() instanceof PortDto)) {
            throw new IllegalArgumentException("parent aps is not port. unexpected node-element structure:" + pvc.getAbsoluteName());
        } else if (isSupportedParentPort((PortDto) pvc.getOwner().getOwner())) {
            throw new IllegalArgumentException("parent port is not aps port. parent=" + pvc.getOwner().getOwner().getAbsoluteName());
        }
        setConstraint(AtmPvcIfDto.class);
        this.owner = null;
        this.port = pvc;
        this.vpi = pvc.getVpi();
        this.vci = pvc.getVci();
        this.cmd.addVersionCheckTarget(port);
    }

    public void setVpi(Integer vpi) {
        if (vpi == null) {
            return;
        }
        this.vpi = vpi;
    }

    public void setVci(Integer vci) {
        if (vci == null) {
            return;
        }
        setValue(ATTR.ATTR_ATM_PVC_VCI, vci.toString());
        this.vci = vci;
    }

    public void setBandwidth(Long bandwidth) {
        String bandwidthValue = (bandwidth == null ? null : bandwidth.toString());
        setValue(MPLSNMS_ATTR.BANDWIDTH, bandwidthValue);
    }

    public void setIgpCost(Integer cost) {
        String costValue = (cost == null ? null : cost.toString());
        setValue(MPLSNMS_ATTR.IGP_COST, costValue);
    }

    public void setBestEffortGuaranteedBandwidth(Long bestEffortValue) {
        String bEValue = (bestEffortValue == null ? null : bestEffortValue.toString());
        setValue(MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH, bEValue);
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
        if (port != null) {
            return port.getOwner().getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY
                    + vpi + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
        } else if (owner != null) {
            return owner.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY
                    + vpi + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
        } else {
            throw new IllegalStateException();
        }
    }

    public String getNodeContext() {
        if (port != null) {
            return port.getNode().getName();
        } else if (owner != null) {
            return owner.getNode().getName();
        }
        throw new IllegalStateException();
    }

    @Override
    public BuildResult buildPortCommands() {
        try {
            boolean changed = false;
            PortDto parentPort = (this.owner == null ? (PortDto) this.port.getOwner().getOwner() : this.owner);

            List<PortDto> members = NodeUtil.getMemberPorts(parentPort);
            MvoDtoSet<PortDto> memberSet = new MvoDtoSet<PortDto>();
            memberSet.addAll(members);
            if (this.port != null) {
                for (PortDto memberPvc : this.port.getParts()) {
                    if (!(memberPvc instanceof AtmPvcIfDto)) {
                        continue;
                    } else if (memberPvc.getOwner() == null || memberPvc.getOwner().getOwner() == null) {
                        continue;
                    }
                    NodeElementDto parent = memberPvc.getOwner().getOwner();
                    if (!(parent instanceof PortDto)) {
                        throw new IllegalStateException("unexpected data: " +
                                "sub-interface on non-port object[" + memberPvc.getAbsoluteName());
                    }
                    PortDto pvcParentPort = (PortDto) parent;
                    if (memberSet.contains(pvcParentPort)) {
                        continue;
                    }
                    InventoryBuilder.buildRemovePortFromBundleCommand(cmd, port, memberPvc);
                    AtmPvcPortCommandBuilder remover = new AtmPvcPortCommandBuilder((AtmPvcIfDto) memberPvc, getEditor());
                    remover.buildDeleteCommand();
                    ShellCommands pvcCmd = remover.getCommand();
                    this.cmd.addCommand("## remove member pvc");
                    this.cmd.addVersionCheckTarget(pvcCmd.getAssertions());
                    this.cmd.putValueCheckContents(pvcCmd.getValueCheckContents());
                    this.cmd.addCommands(pvcCmd.getCommands());
                    changed = true;
                }
            }

            AtmPvcPortCommandBuilder apsPvcBuilder;
            if (port == null) {
                apsPvcBuilder = new AtmPvcPortCommandBuilder(owner, getEditor());
            } else {
                apsPvcBuilder = new AtmPvcPortCommandBuilder(port, getEditor());
            }
            apsPvcBuilder.setValues(getAttributes());
            BuildResult apsResult = apsPvcBuilder.buildCommand();
            if (BuildResult.SUCCESS == apsResult) {
                ShellCommands apsPvcCmd = apsPvcBuilder.getCommand();
                this.cmd.addCommand("## create/update aps pvc");
                this.cmd.addVersionCheckTarget(apsPvcCmd.getAssertions());
                this.cmd.putValueCheckContents(apsPvcCmd.getValueCheckContents());
                this.cmd.addCommands(apsPvcCmd.getCommands());
                changed = true;
            }


            for (PortDto member : members) {
                AtmPvpIfDto vp = AtmPvcCoreUtil.getPvp(member, this.vpi);
                AtmPvcIfDto vc = (vp == null ? null : AtmPvcCoreUtil.getPvc(vp, this.vci));
                AtmPvcPortCommandBuilder pvcBuilder;
                if (vc == null) {
                    pvcBuilder = new AtmPvcPortCommandBuilder(member, getEditor());
                } else {
                    pvcBuilder = new AtmPvcPortCommandBuilder(vc, getEditor());
                }
                pvcBuilder.setValues(getAttributes());
                BuildResult memberResult = pvcBuilder.buildCommand();
                if (BuildResult.SUCCESS == memberResult) {
                    ShellCommands pvcCmd = apsPvcBuilder.getCommand();
                    this.cmd.addCommand("## create/update aps member pvc");
                    this.cmd.addVersionCheckTarget(pvcCmd.getAssertions());
                    this.cmd.putValueCheckContents(pvcCmd.getValueCheckContents());
                    this.cmd.addCommands(pvcCmd.getCommands());
                    changed = true;
                }
            }
            if (changed) {
                return setResult(BuildResult.SUCCESS);
            } else {
                return setResult(BuildResult.NO_CHANGES);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public PortDto getPort() {
        return this.port;
    }

    private boolean isSupportedParentPort(PortDto port) {
        if (port == null) {
            throw new IllegalArgumentException("parent-port must be not null.");
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        }
        return false;
    }

    public String getObjectType() {
        return DiffObjectType.ATM_PVC.getCaption();
    }
}