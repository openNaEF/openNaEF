package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.dto.EntityDto;

public class NaefDtoAttrs {

    public static final Attribute.SingleString<NaefDto> ABSOLUTE_NAME
        = new Attribute.SingleString<NaefDto>( "naef.dto.absolute-name" );
    public static final Attribute.SingleString<NaefDto> SHELL_CLASS_ID
        = new Attribute.SingleString<NaefDto>( "naef.dto.shell-class-id" );
    public static final Attribute.SingleString<NaefDto> OBJECT_TYPE_NAME
        = new Attribute.SingleString<NaefDto>( "naef.dto.object-type-name" );

    public static final EntityDto.SetRefAttr<CustomerInfoDto, NaefDto> CUSTOMER_INFOS
        = new EntityDto.SetRefAttr<CustomerInfoDto, NaefDto>( "naef.dto.customer-infos" );
}
