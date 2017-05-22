package naef.dto.ip;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.ip.IpAddress;
import naef.mvo.ip.IpIf;

import java.util.Set;

public class IpIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<IpSubnetDto, IpIfDto> IP_SUBNET
            = new SingleRefAttr<IpSubnetDto, IpIfDto>("naef.dto.ip-subnet");

        public static final SingleRefAttr<IpSubnetAddressDto, IpIfDto> IP_SUBNET_ADDRESS
            = new SingleRefAttr<IpSubnetAddressDto, IpIfDto>("naef.dto.ip-subnet-address");

        /**
         * @deprecated {@link #BOUND_PORTS} に置き換えられました.
         */
        public static final SetRefAttr<PortDto, IpIfDto> ASSOCIATED_PORTS
            = new SetRefAttr<PortDto, IpIfDto>("naef.dto.associated-ports");

        /**
         * {@link naef.mvo.ip.IpIf.Attr#BOUND_PORTS} のDTO転写属性です.
         */
        public static final MapKeyRefAttr<PortDto, Integer, IpIfDto> BOUND_PORTS
            = new MapKeyRefAttr<PortDto, Integer, IpIfDto>(IpIf.Attr.BOUND_PORTS.getName());
    }

    public IpIfDto() {
    }

    public IpSubnetDto getSubnet() {
        return ExtAttr.IP_SUBNET.deref(this);
    }

    public IpSubnetAddressDto getSubnetAddress() {
        return ExtAttr.IP_SUBNET_ADDRESS.deref(this);
    }

    public IpAddress getIpAddress() {
        return get(IpIf.Attr.IP_ADDRESS);
    }

    public Integer getSubnetMaskLength() {
        return get(IpIf.Attr.SUBNET_MASK_LENGTH);
    }

    public Set<PortDto> getAssociatedPorts() {
        return ExtAttr.ASSOCIATED_PORTS.deref(this);
    }
}
