package naef.dto;

import tef.skelton.SkeltonTefService;
import tef.skelton.dto.EntityDto;

import java.util.Set;

public abstract class NaefDto extends EntityDto {

    protected NaefDto() {
    }

    public String getAbsoluteName() {
        return NaefDtoAttrs.ABSOLUTE_NAME.get(this);
    }

    public String getShellClassId() {
        return NaefDtoAttrs.SHELL_CLASS_ID.get(this);
    }

    public final String getObjectTypeName() {
        return NaefDtoAttrs.OBJECT_TYPE_NAME.get(this);
    }

    public static final String getFqnPrimaryDelimiter() {
        return SkeltonTefService.instance().getFqnPrimaryDelimiter();
    }

    public static final String getFqnSecondaryDelimiter() {
        return SkeltonTefService.instance().getFqnSecondaryDelimiter();
    }

    public static final String getFqnTertiaryDelimiter() {
        return SkeltonTefService.instance().getFqnTertiaryDelimiter();
    }

    public static final String getFqnLeftBracket() {
        return SkeltonTefService.instance().getFqnLeftBracket();
    }

    public static final String getFqnRightBracket() {
        return SkeltonTefService.instance().getFqnRightBracket();
    }

    public Set<CustomerInfoDto> getCustomerInfos() {
        return NaefDtoAttrs.CUSTOMER_INFOS.deref(this);
    }
}
