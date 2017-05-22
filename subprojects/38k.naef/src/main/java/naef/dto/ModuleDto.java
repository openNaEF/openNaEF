package naef.dto;

import java.util.Set;

public class ModuleDto extends HardwareDto {

    public static class ExtAttr {

        public static final SetRefAttr<JackDto, ModuleDto> JACKS
            = new SetRefAttr<JackDto, ModuleDto>( "jacks" );
    }

    public ModuleDto() {
    }

    public Set<JackDto> getJacks() {
        return ExtAttr.JACKS.deref( this );
    }
}
