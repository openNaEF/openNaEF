package naef.dto.vrf;

import java.util.Set;

import naef.dto.NetworkDto;
import tef.skelton.Attribute;

public class VrfDto extends NetworkDto {

    public static class ExtAttr {

        public static final SingleRefAttr<VrfIntegerIdPoolDto, VrfDto> VRF_INTEGER_IDPOOL
            = new SingleRefAttr<VrfIntegerIdPoolDto, VrfDto>("naef.dto.vrf.id-pool.integer-type");
        public static final Attribute.SingleInteger<VrfDto> VRF_INTEGER_ID
            = new Attribute.SingleInteger<VrfDto>("naef.dto.vrf.id.integer-type");
        public static final SingleRefAttr<VrfStringIdPoolDto, VrfDto> VRF_STRING_IDPOOL
            = new SingleRefAttr<VrfStringIdPoolDto, VrfDto>("naef.dto.vrf.id-pool.string-type");
        public static final Attribute.SingleString<VrfDto> VRF_STRING_ID
            = new Attribute.SingleString<VrfDto>("naef.dto.vrf.id.string-type");
        public static final SetRefAttr<VrfIfDto, VrfDto> MEMBER_VRF_IF
            = new SetRefAttr<VrfIfDto, VrfDto>("メンバー VRF IF");
    }

    public VrfDto() {
    }

    public VrfIntegerIdPoolDto getIntegerIdPool() {
        return ExtAttr.VRF_INTEGER_IDPOOL.deref(this);
    }

    public Integer getIntegerId() {
        return ExtAttr.VRF_INTEGER_ID.get(this);
    }

    public VrfStringIdPoolDto getStringIdPool() {
        return ExtAttr.VRF_STRING_IDPOOL.deref(this);
    }

    public String getStringId() {
        return ExtAttr.VRF_STRING_ID.get(this);
    }

    public Set<VrfIfDto> getMemberVrfifs() {
        return ExtAttr.MEMBER_VRF_IF.deref(this);
    }
}
