package naef.dto.vlan;

import java.util.Set;

import naef.dto.NetworkDto;
import tef.skelton.Attribute;

public class VlanDto extends NetworkDto {

    public static class ExtAttr {

        public static final SingleRefAttr<VlanIdPoolDto, VlanDto> VLAN_ID_POOL
            = new SingleRefAttr<VlanIdPoolDto, VlanDto>("vlan id pool");
        public static final Attribute.SingleInteger<VlanDto> VLAN_ID
            = new Attribute.SingleInteger<VlanDto>("VLAN ID");
        public static final SetRefAttr<VlanIfDto, VlanDto> MEMBER_VLAN_IF
            = new SetRefAttr<VlanIfDto, VlanDto>("メンバー VLAN IF");
    }

    public VlanDto() {
    }

    public VlanIdPoolDto getIdPool() {
        return ExtAttr.VLAN_ID_POOL.deref(this);
    }

    public Integer getVlanId() {
        return ExtAttr.VLAN_ID.get(this);
    }

    public Set<VlanIfDto> getMemberVlanifs() {
        return ExtAttr.MEMBER_VLAN_IF.deref(this);
    }
}
