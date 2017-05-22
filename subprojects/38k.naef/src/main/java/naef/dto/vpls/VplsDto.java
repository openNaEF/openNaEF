package naef.dto.vpls;

import java.util.Set;

import naef.dto.NetworkDto;
import tef.skelton.Attribute;

public class VplsDto extends NetworkDto {

    public static class ExtAttr {

        public static final SingleRefAttr<VplsIntegerIdPoolDto, VplsDto> VPLS_INTEGER_IDPOOL
            = new SingleRefAttr<VplsIntegerIdPoolDto, VplsDto>("naef.dto.vpls.id-pool.integer-type");
        public static final Attribute.SingleInteger<VplsDto> VPLS_INTEGER_ID
            = new Attribute.SingleInteger<VplsDto>("naef.dto.vpls.id.integer-type");
        public static final SingleRefAttr<VplsStringIdPoolDto, VplsDto> VPLS_STRING_IDPOOL
            = new SingleRefAttr<VplsStringIdPoolDto, VplsDto>("naef.dto.vpls.id-pool.string-type");
        public static final Attribute.SingleString<VplsDto> VPLS_STRING_ID
            = new Attribute.SingleString<VplsDto>("naef.dto.vpls.id.string-type");
        public static final SetRefAttr<VplsIfDto, VplsDto> MEMBER_VPLS_IF
            = new SetRefAttr<VplsIfDto, VplsDto>("メンバー VPLS IF");
    }

    public VplsDto() {
    }

    public VplsIntegerIdPoolDto getIntegerIdPool() {
        return ExtAttr.VPLS_INTEGER_IDPOOL.deref(this);
    }

    public Integer getIntegerId() {
        return ExtAttr.VPLS_INTEGER_ID.get(this);
    }

    public VplsStringIdPoolDto getStringIdPool() {
        return ExtAttr.VPLS_STRING_IDPOOL.deref(this);
    }

    public String getStringId() {
        return ExtAttr.VPLS_STRING_ID.get(this);
    }

    public Set<VplsIfDto> getMemberVplsifs() {
        return ExtAttr.MEMBER_VPLS_IF.deref(this);
    }
}
