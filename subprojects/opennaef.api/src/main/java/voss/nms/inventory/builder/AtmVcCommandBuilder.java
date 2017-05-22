package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class AtmVcCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final String ownerName;
    private final String nodeName;
    private final AtmPvcIfDto pvc;
    private String vci = null;

    public AtmVcCommandBuilder(String ownerName, String editorName) {
        super(PortDto.class, null, null, editorName);
        this.ownerName = ownerName;
        this.nodeName = ownerName.substring(0, ownerName.indexOf(','));
        this.pvc = null;
        setConstraint(AtmPvcIfDto.class);
    }

    public AtmVcCommandBuilder(AtmPvcIfDto pvc, String editorName) {
        super(PortDto.class, pvc.getNode(), pvc, editorName);
        setConstraint(AtmPvcIfDto.class);
        this.ownerName = null;
        this.nodeName = pvc.getNode().getName();
        this.pvc = pvc;
        this.cmd.addVersionCheckTarget(pvc);
    }

    public void setVci(String vci) {
        if (vci == null) {
            return;
        }
        setValue(ATTR.ATTR_ATM_PVC_VCI, vci);
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
        if (pvc != null) {
            return pvc.getAbsoluteName();
        }
        return ownerName + ATTR.NAME_DELIMITER_PRIMARY + vci;
    }

    public String getNodeContext() {
        if (pvc != null) {
            return pvc.getNode().getName();
        }
        return nodeName;
    }

    @Override
    public BuildResult buildPortCommands() {
        try {
            if (this.pvc != null && !hasChange()) {
                return BuildResult.NO_CHANGES;
            }
            String ifName = getIfName();
            if (pvc == null) {
                LoggerFactory.getLogger(AtmVcCommandBuilder.class).debug("this.ownerName=" + this.ownerName);
                String registerDate = InventoryBuilder.getInventoryDateString(new Date());
                setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
                InventoryBuilder.changeContext(cmd, this.ownerName);
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_ATM_PVC_IF, String.valueOf(vci));
            } else {
                InventoryBuilder.changeContext(cmd, pvc);
            }
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
            this.cmd.addLastEditCommands();
            assignTargetPortToShellContextVariable();

            if (pvc != null) {
                String currentIfName = DtoUtil.getStringOrNull(pvc, MPLSNMS_ATTR.IFNAME);
                if (!currentIfName.equals(ifName)) {
                    InventoryBuilder.buildRenameCommands(cmd, ifName);
                }
            }
            return BuildResult.SUCCESS;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public AtmPvcIfDto getPort() {
        return this.pvc;
    }

    public String getObjectType() {
        return DiffObjectType.ATM_PVC.getCaption();
    }

}