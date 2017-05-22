package voss.multilayernms.inventory.renderer;

import naef.dto.ModuleDto;
import naef.dto.SlotDto;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class HardwareRenderer extends GenericRenderer {
    public static String getSlotType(SlotDto slot) {
        return DtoUtil.getStringOrNull(slot, MPLSNMS_ATTR.SLOT_TYPE);
    }

    public static String getModuleType(ModuleDto module) {
        return DtoUtil.getStringOrNull(module, MPLSNMS_ATTR.MODULE_TYPE);
    }

}