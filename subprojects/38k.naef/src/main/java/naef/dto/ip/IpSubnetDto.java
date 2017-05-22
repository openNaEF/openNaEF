package naef.dto.ip;

import naef.dto.NetworkDto;
import naef.dto.PortDto;
import tef.skelton.Attribute;

import java.util.Set;

public class IpSubnetDto extends NetworkDto {

    public static class ExtAttr {

        public static final Attribute.SingleString<IpSubnetDto> SUBNET_NAME
            = new Attribute.SingleString<IpSubnetDto>("subnet name");

        public static final SingleRefAttr<IpSubnetNamespaceDto, IpSubnetDto> NAMESPACE
            = new SingleRefAttr<IpSubnetNamespaceDto, IpSubnetDto>("namespace");

        public static final SingleRefAttr<IpSubnetAddressDto, IpSubnetDto> SUBNET_ADDRESS
            = new SingleRefAttr<IpSubnetAddressDto, IpSubnetDto>("naef.dto.subnet-address");

        public static final SetRefAttr<PortDto, IpSubnetDto> MEMBER_IP_IF
            = new SetRefAttr<PortDto, IpSubnetDto>("member ip if");
    }

    public IpSubnetDto() {
    }

    public String getSubnetName() {
        return ExtAttr.SUBNET_NAME.get(this);
    }

    public IpSubnetNamespaceDto getNamespace() {
        return ExtAttr.NAMESPACE.deref(this);
    }

    public IpSubnetAddressDto getSubnetAddress() {
        return ExtAttr.SUBNET_ADDRESS.deref(this);
    }

    public Set<PortDto> getMemberIpifs() {
        return ExtAttr.MEMBER_IP_IF.deref(this);
    }
}
