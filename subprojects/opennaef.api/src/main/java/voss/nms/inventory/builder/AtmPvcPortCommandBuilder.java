package voss.nms.inventory.builder;

import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.AtmPvcCoreUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AtmPvcPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final PortDto owner;
    private final AtmPvcIfDto port;
    private Integer vpi = null;

    public AtmPvcPortCommandBuilder(PortDto owner, String editorName) {
        super(PortDto.class, owner.getNode(), null, editorName);
        this.owner = owner;
        this.port = null;
        setConstraint(AtmPvcIfDto.class);
    }

    public AtmPvcPortCommandBuilder(AtmPvcIfDto pvc, String editorName) {
        super(PortDto.class, pvc.getNode(), pvc, editorName);
        setConstraint(AtmPvcIfDto.class);
        this.owner = null;
        this.port = pvc;
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
            if (this.port != null && !hasChange()) {
                return BuildResult.NO_CHANGES;
            }
            String ifName = getIfName();
            if (ifName == null) {
                throw new IllegalStateException();
            }
            if (port == null) {
                if (!isVpSupportedPortType(this.owner)) {
                    throw new IllegalStateException("illegal atm-vp owner: " + this.owner.getAbsoluteName());
                }
                if (!AtmPvcCoreUtil.isAtmPvcEnabled(owner)) {
                    InventoryBuilder.changeContext(cmd, this.owner);
                    SimpleNodeBuilder.buildAtmPvcFeatureEnableCommand(cmd);
                }
                cmd.addVersionCheckTarget(owner);
                AtmPvpIfDto pvp = AtmPvcCoreUtil.getPvp(owner, vpi);
                if (pvp == null) {
                    Map<String, String> vpAttributes = new HashMap<String, String>();
                    vpAttributes.put(ATTR.IMPLICIT, Boolean.TRUE.toString());
                    SimplePvcBuilder.buildAtmPvpCreationCommand(cmd, owner, null, vpi, false);
                    InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, vpAttributes);
                } else {
                    if (!NodeUtil.isImplicit(pvp)) {
                        throw new IllegalStateException("The specified VP is already in use as a PVP. VPI=" + vpi);
                    }
                    InventoryBuilder.changeContext(cmd, pvp);
                }
                String registerDate = InventoryBuilder.getInventoryDateString(new Date());
                setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_ATM_PVC_IF, ifName);
                InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            } else {
                InventoryBuilder.changeContext(cmd, port);
                InventoryBuilder.buildAttributeUpdateCommand(cmd, port, attributes);
            }
            InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
            this.cmd.addLastEditCommands();
            assignTargetPortToShellContextVariable();

            if (port != null) {
                String currentIfName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IFNAME);
                if (!currentIfName.equals(ifName)) {
                    InventoryBuilder.buildRenameCommands(cmd, ifName);
                }
            }
            this.result = BuildResult.SUCCESS;
            return this.result;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private boolean isVpSupportedPortType(NodeElementDto port) {
        if (port instanceof AtmPortDto) {
            return true;
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof SerialPortDto) {
            return true;
        } else if (port instanceof PosPortDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        }
        return false;
    }

    public PortDto getPort() {
        return this.port;
    }

    public String getObjectType() {
        return DiffObjectType.ATM_PVC.getCaption();
    }
}