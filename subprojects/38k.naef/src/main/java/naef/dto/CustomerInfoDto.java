package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;

import java.util.Set;

public class CustomerInfoDto extends NaefDto implements NamedModel {

    public static class ExtAttr {

        public static final SetRefAttr<NaefDto, CustomerInfoDto> REFERENCES
            = new SetRefAttr<NaefDto, CustomerInfoDto>( "naef.dto.customer-info.references");

        public static final SingleRefAttr<SystemUserDto, CustomerInfoDto> SYSTEM_USER
            = new SingleRefAttr<SystemUserDto, CustomerInfoDto>("naef.system-user");
    }

    public CustomerInfoDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public Set<NaefDto> getReferences() {
        return ExtAttr.REFERENCES.deref(this);
    }

    public SystemUserDto getSystemUser() {
        return ExtAttr.SYSTEM_USER.deref(this);
    }
}
