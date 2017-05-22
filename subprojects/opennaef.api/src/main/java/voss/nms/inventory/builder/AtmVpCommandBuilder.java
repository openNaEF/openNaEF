package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.atm.AtmPvpIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class AtmVpCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final String ownerName;
    private final String nodeName;
    private final AtmPvpIfDto vp;
    private Integer vpi = null;

    public AtmVpCommandBuilder(String ownerName, String editorName) {
        super(PortDto.class, null, null, editorName);
        this.ownerName = ownerName;
        this.nodeName = ownerName.substring(0, ownerName.indexOf(','));
        this.vp = null;
        setConstraint(AtmPvpIfDto.class);
        setIpNeeds(false);
    }

    public AtmVpCommandBuilder(AtmPvpIfDto vp, String editorName) {
        super(PortDto.class, vp.getNode(), vp, editorName);
        setConstraint(AtmPvpIfDto.class);
        this.ownerName = vp.getOwner().getAbsoluteName();
        this.nodeName = vp.getNode().getName();
        this.vp = vp;
        this.cmd.addVersionCheckTarget(vp);
        setIpNeeds(false);
    }

    public void setVpi(String vpi) {
        setValue(ATTR.ATTR_ATM_PVP_VPI, vpi);
        this.vpi = Integer.valueOf(vpi);
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public String getPortContext() {
        if (this.vp != null) {
            return vp.getAbsoluteName();
        }
        return ownerName + ATTR.NAME_DELIMITER_PRIMARY + vpi;
    }

    public String getNodeContext() {
        if (this.vp != null) {
            return this.vp.getNode().getName();
        }
        return this.nodeName;
    }

    @Override
    public BuildResult buildPortCommands() {
        try {
            if (this.vp != null && !hasChange()) {
                return BuildResult.NO_CHANGES;
            }
            if (this.vp == null) {
                String registerDate = InventoryBuilder.getInventoryDateString(new Date());
                setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
                InventoryBuilder.changeContext(cmd, this.ownerName);
                SimpleNodeBuilder.buildAtmPvcFeatureEnableCommand(cmd);
                SimplePvcBuilder.buildAtmPvpCreationCommand(this.cmd, this.ownerName, null, this.vpi, false);
            } else {
                InventoryBuilder.changeContext(this.cmd, this.vp);
            }
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
            InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
            this.cmd.addLastEditCommands();
            assignTargetPortToShellContextVariable();
            return BuildResult.SUCCESS;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public AtmPvpIfDto getVp() {
        return this.vp;
    }

    public String getObjectType() {
        return DiffObjectType.ATM_VP.getCaption();
    }

}