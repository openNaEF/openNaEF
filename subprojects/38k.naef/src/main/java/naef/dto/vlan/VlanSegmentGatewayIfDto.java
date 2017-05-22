package naef.dto.vlan;

import naef.dto.SoftPortDto;
import naef.mvo.vlan.VlanSegmentGatewayIf;

public class VlanSegmentGatewayIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<VlanLinkDto, VlanSegmentGatewayIfDto> VLAN_LINK
            = new SingleRefAttr<VlanLinkDto, VlanSegmentGatewayIfDto>("naef.dto.vlan-segment-gateway-if.vlan-link");
        public static final SingleRefAttr<VlanIfDto, VlanSegmentGatewayIfDto> VLAN_IF
            = new SingleRefAttr<VlanIfDto, VlanSegmentGatewayIfDto>("naef.dto.vlan-segment-gateway-if.vlan-if");
    }

    public VlanSegmentGatewayIfDto() {
    }

    public Integer getId() {
        return get(VlanSegmentGatewayIf.Attr.ID);
    }

    public VlanLinkDto getVlanLink() {
        return ExtAttr.VLAN_LINK.deref(this);
    }

    public VlanIfDto getVlanIf() {
        return ExtAttr.VLAN_IF.deref(this);
    }
}
