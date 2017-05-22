package voss.multilayernms.inventory.builder;

import naef.dto.PortDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class PortFacilityStatusBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private FacilityStatus status = null;

    public PortFacilityStatusBuilder(PortDto target, String editorName) {
        super(PortDto.class, target, editorName);
        setConstraint(PortDto.class);
    }

    public void setFacilityStatus(FacilityStatus status) {
        FacilityStatus current = FacilityStatusUtil.getStatus(target);
        if (!FacilityStatusUtil.isAllowableChange(current, status)) {
            throw new IllegalStateException("unexpected facility-status change: "
                    + current.getDisplayString() + "->" + status.getDisplayString());
        }
        String statusValue = (status == null ? null : status.getDisplayString());
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, statusValue);
        this.status = status;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (isChanged(MPLSNMS_ATTR.FACILITY_STATUS)) {
            InventoryBuilder.changeContext(cmd, target);
            String statusName = (this.status == null ? null : this.status.getDisplayString());
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FACILITY_STATUS, statusName);
            cmd.addLastEditCommands();
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        return BuildResult.NO_CHANGES;
    }

    public String getObjectType() {
        return DiffObjectType.PSEUDOWIRE.getCaption();
    }

}