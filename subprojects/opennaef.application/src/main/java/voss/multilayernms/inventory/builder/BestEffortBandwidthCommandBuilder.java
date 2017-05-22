package voss.multilayernms.inventory.builder;

import naef.dto.NaefDto;
import naef.dto.PortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BestEffortBandwidthCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BestEffortBandwidthCommandBuilder.class);
    private final List<BestEffortUnit> units = new ArrayList<BestEffortUnit>();

    public BestEffortBandwidthCommandBuilder(String editorName) {
        super(PortDto.class, null, editorName);
        setConstraint(PortDto.class);
    }

    public void addBestEffortValue(String inventoryID, long bestEffortValue) {
        if (inventoryID == null) {
            throw new IllegalArgumentException();
        }
        try {
            NaefDto dto = InventoryIdDecoder.getDto(inventoryID);
            if (dto == null) {
                log.error("cannot resolve: " + inventoryID);
            } else {
                BestEffortUnit unit = new BestEffortUnit(dto, bestEffortValue);
                this.units.add(unit);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        SimpleDateFormat df = GenericRenderer.getMvoDateFormat();
        for (BestEffortUnit unit : units) {
            InventoryBuilder.changeContext(cmd, unit.getTarget());
            Long prevValue_ = DtoUtil.getLong(unit.getTarget(), MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH);
            String prevValue = (prevValue_ == null ? null : prevValue_.toString());
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_BANDWIDTH, unit.getBestEffortValue());
            String prevDate = DtoUtil.getDateTime(unit.getTarget(), MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_CAPTURE_DATE);
            String nowDate = df.format(new Date());
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.BEST_EFFORT_GUARANTEED_CAPTURE_DATE, nowDate);
            NaefDto target = unit.getTarget();
            String caption = (target instanceof PortDto ?
                    DtoUtil.getIfName((PortDto) target) :
                    target.getAbsoluteName());
            recordChange("BestEffortValue:" + caption, prevValue, unit.getBestEffortValue());
            recordChange("BestEffortDate:" + caption, prevDate, nowDate);
        }
        if (units.size() == 0) {
            return BuildResult.NO_CHANGES;
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        return BuildResult.NO_CHANGES;
    }

    public static class BestEffortUnit implements Serializable {
        private static final long serialVersionUID = 1L;
        private final NaefDto target;
        private final long bestEffortValue;

        public BestEffortUnit(NaefDto target, long value) {
            this.target = target;
            this.bestEffortValue = value;
        }

        public NaefDto getTarget() {
            return this.target;
        }

        public String getBestEffortValue() {
            return String.valueOf(this.bestEffortValue);
        }
    }

    public String getObjectType() {
        return DiffObjectType.OTHER.getCaption();
    }
}