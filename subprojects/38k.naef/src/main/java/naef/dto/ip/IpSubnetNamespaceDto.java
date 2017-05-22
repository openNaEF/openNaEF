package naef.dto.ip;

import naef.dto.IdPoolDto;

public class IpSubnetNamespaceDto
    extends IdPoolDto.StringType<IpSubnetNamespaceDto, IpSubnetDto>
{
    public static class ExtAttr {

        public static final SingleRefAttr<IpSubnetAddressDto, IpSubnetNamespaceDto> IP_SUBNET_ADDRESS
            = new SingleRefAttr<IpSubnetAddressDto, IpSubnetNamespaceDto>("naef.dto.ip-subnet-address");
    }

    public IpSubnetNamespaceDto() {
    }

    @Override public String getId(IpSubnetDto dto) {
        return dto.getSubnetName();
    }

    public IpSubnetAddressDto getIpSubnetAddress() {
        return ExtAttr.IP_SUBNET_ADDRESS.deref(this);
    }
}
