package voss.multilayernms.inventory.builder;

import naef.dto.mpls.PseudowireDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class PseudoWireFacilityStatusBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private FacilityStatus status = null;

    public PseudoWireFacilityStatusBuilder(PseudowireDto target, String editorName) {
        super(PseudowireDto.class, target, editorName);
        setConstraint(PseudowireDto.class);
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
    public BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        return BuildResult.NO_CHANGES;
    }

    public String getObjectType() {
        return DiffObjectType.PSEUDOWIRE.getCaption();
    }

}