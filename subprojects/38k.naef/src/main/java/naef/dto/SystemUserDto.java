package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;

import java.util.Set;

public class SystemUserDto extends NaefDto implements NamedModel {

    public static class ExtAttr {

        public static final SetRefAttr<CustomerInfoDto, SystemUserDto> CUSTOMER_INFOS
            = new SetRefAttr<CustomerInfoDto, SystemUserDto>("naef.customer-infos");
    }

    public SystemUserDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public Set<CustomerInfoDto> getCustomerInfos() {
        return ExtAttr.CUSTOMER_INFOS.deref(this);
    }
}
