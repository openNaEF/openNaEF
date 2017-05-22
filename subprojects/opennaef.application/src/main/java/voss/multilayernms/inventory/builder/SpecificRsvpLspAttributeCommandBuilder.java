package voss.multilayernms.inventory.builder;

import naef.dto.mpls.RsvpLspDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class SpecificRsvpLspAttributeCommandBuilder extends RsvpLspAttributeCommandBuilder {
    private static final long serialVersionUID = 1L;
    private FacilityStatus status = null;
    private final String targetName;

    public SpecificRsvpLspAttributeCommandBuilder(RsvpLspDto target, String editorName) {
        super(target, editorName);
        this.targetName = null;
    }

    public SpecificRsvpLspAttributeCommandBuilder(String targetName, String editorName) {
        super((String) null, editorName);
        this.targetName = targetName;
    }

    public void setFacilityStatus(FacilityStatus status) {
        FacilityStatus current = FacilityStatusUtil.getStatus(target);
        if (!FacilityStatusUtil.isAllowableChange(current, status)) {
            throw new IllegalStateException("unexpected facility-status change: "
                    + current.getDisplayString() + "->" + status.getDisplayString());
        }
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status.getDisplayString());
        this.status = status;
    }

    public void setFacilityStatusWithoutCheck(FacilityStatus status) {
        String value = (status == null ? null : status.getDisplayString());
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, value);
        this.status = status;
    }

    public void setBestEffortGuaranteedBandwidth(Long bandwidth) {
        setValue(MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH, bandwidth);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        String statusName = (this.status == null ? null : this.status.getDisplayString());
        if (this.target != null) {
            InventoryBuilder.changeContext(cmd, target);
        } else {
            InventoryBuilder.changeContext(cmd, this.targetName);
        }
        cmd.addLastEditCommands();
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        if (isChanged(MPLSNMS_ATTR.FACILITY_STATUS)) {
            RsvpLspDto pair = RsvpLspExtUtil.getOppositLsp((RsvpLspDto) target);
            if (pair != null) {
                InventoryBuilder.changeContext(cmd, pair);
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FACILITY_STATUS, statusName);
                cmd.addLastEditCommands();
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        return BuildResult.NO_CHANGES;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP.getCaption();
    }

}