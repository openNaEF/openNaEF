package naef.dto;

public class SlotDto extends HardwareDto {

    public static class ExtAttr {

        public static final SingleRefAttr<ModuleDto, SlotDto> MODULE = new SingleRefAttr<ModuleDto, SlotDto>("module");
    }

    public SlotDto() {
    }

    public ModuleDto getModule() {
        return ExtAttr.MODULE.deref(this);
    }
}
