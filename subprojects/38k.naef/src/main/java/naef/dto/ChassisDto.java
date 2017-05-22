package naef.dto;

import java.util.Set;

public class ChassisDto extends HardwareDto {

    public static class ExtAttr {

        public static final SetRefAttr<SlotDto, ChassisDto> SLOTS
            = new SetRefAttr<SlotDto, ChassisDto>("slots");
        public static final SetRefAttr<JackDto, ChassisDto> JACKS
            = new SetRefAttr<JackDto, ChassisDto>("jacks");
    }

    public ChassisDto() {
    }

    public Set<SlotDto> getSlots() {
        return ExtAttr.SLOTS.deref(this);
    }

    public Set<JackDto> getJacks() {
        return ExtAttr.JACKS.deref(this);
    }
}
