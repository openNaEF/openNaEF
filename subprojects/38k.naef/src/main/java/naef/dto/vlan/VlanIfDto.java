package naef.dto.vlan;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import tef.skelton.Attribute;

import java.util.Set;

public class VlanIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final Attribute.SingleInteger<VlanIfDto> VLAN_ID
            = new Attribute.SingleInteger<VlanIfDto>("VLAN ID");
        public static final SetRefAttr<VlanSegmentGatewayIfDto, VlanIfDto> VLAN_SEGMENT_GATEWAY_IFS
            = new SetRefAttr<VlanSegmentGatewayIfDto, VlanIfDto>("naef.dto.vlan-if.vlan-segment-gateway-ifs");
        public static final SetRefAttr<PortDto, VlanIfDto> TAGGED_PORTS
            = new SetRefAttr<PortDto, VlanIfDto>("naef.dto.vlan-if.tagged-ports");
        public static final SetRefAttr<PortDto, VlanIfDto> UNTAGGED_PORTS
            = new SetRefAttr<PortDto, VlanIfDto>("naef.dto.vlan-if.untagged-ports");
        public static final SingleRefAttr<VlanDto, VlanIfDto> VLAN
            = new SingleRefAttr<VlanDto, VlanIfDto>("naef.dto.vlan-if.vlan");
        public static final SetRefAttr<VlanSegmentDto, VlanIfDto> VLAN_LINKS
            = new SetRefAttr<VlanSegmentDto, VlanIfDto>("naef.dto.vlan-if.vlan-links");
    }

    public VlanIfDto() {
    }

    public Integer getVlanId() {
        return ExtAttr.VLAN_ID.get(this);
    }

    public Set<VlanSegmentGatewayIfDto> getVlanSegmentGatewayIfs() {
        return ExtAttr.VLAN_SEGMENT_GATEWAY_IFS.deref(this);
    }

    public Set<PortDto> getTaggedVlans() {
        return ExtAttr.TAGGED_PORTS.deref(this);
    }

    public Set<PortDto> getUntaggedVlans() {
        return ExtAttr.UNTAGGED_PORTS.deref(this);
    }

    public VlanDto getTrafficDomain() {
        return ExtAttr.VLAN.deref(this);
    }

    public Set<VlanSegmentDto> getVlanLinks() {
        return ExtAttr.VLAN_LINKS.deref(this);
    }
}
