package naef.dto.vxlan;

import naef.dto.NetworkDto;
import tef.skelton.Attribute;

import java.util.Set;

public class VxlanDto extends NetworkDto {

    public static final SingleRefAttr<VxlanIdPoolDto, VxlanDto> VXLAN_ID_POOL
        = new SingleRefAttr<VxlanIdPoolDto, VxlanDto>("naef.dto.vxlan.id-pool");
    public static final Attribute.SingleLong<VxlanDto> VXLAN_ID
        = new Attribute.SingleLong<VxlanDto>("naef.dto.vxlan.id");
    public static final SetRefAttr<VtepIfDto, VxlanDto> VTEP_IFS
        = new SetRefAttr<VtepIfDto, VxlanDto>("naef.dto.vxlan.vtep-ifs");

    public VxlanDto() {
    }

    public VxlanIdPoolDto getIdPool() {
        return VXLAN_ID_POOL.deref(this);
    }

    public Long getId() {
        return VXLAN_ID.get(this);
    }

    public Set<VtepIfDto> getMemberVtefIfs() {
        return VTEP_IFS.deref(this);
    }
}
