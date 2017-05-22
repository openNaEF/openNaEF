package naef.dto.vlan;

import java.util.Set;

import naef.dto.NetworkDto;

public class VlanSegmentDto extends NetworkDto {

    public static class ExtAttr {

        public static final SetRefAttr<VlanSegmentGatewayIfDto, VlanSegmentDto> VLAN_SEGMENT_GATEWAY_IFS
            = new SetRefAttr<VlanSegmentGatewayIfDto, VlanSegmentDto>("naef.dto.vlan-segment.vlan-segment-gateway-ifs");
    }

    public VlanSegmentDto() {
    }

    public Set<VlanSegmentGatewayIfDto> getVlanSegmentGatewayIfs() {
        return ExtAttr.VLAN_SEGMENT_GATEWAY_IFS.deref(this);
    }
}
