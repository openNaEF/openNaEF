package naef.dto.ip;

import naef.dto.IdPoolDto;
import naef.dto.IdRange;
import naef.mvo.ip.IpAddress;
import naef.mvo.ip.IpIf;
import tef.skelton.Range;

import java.util.Set;

public class IpSubnetAddressDto extends IdPoolDto<IpSubnetAddressDto, IpAddress, IpIfDto> {

    public static class ExtAttr {

        public static final SingleRefAttr<IpSubnetNamespaceDto, IpSubnetAddressDto> IP_SUBNET_NAMESPACE
            = new SingleRefAttr<IpSubnetNamespaceDto, IpSubnetAddressDto>("naef.dto.ip-subnet-namespace");
        public static final SingleRefAttr<IpSubnetDto, IpSubnetAddressDto> IP_SUBNET
            = new SingleRefAttr<IpSubnetDto, IpSubnetAddressDto>("naef.dto.ip-subnet");
    }

    public IpSubnetAddressDto() {
    }

    @Override public IpAddress getId(IpIfDto dto) {
        return dto.get(IpIf.Attr.IP_ADDRESS);
    }

    @Override public IdRange<IpAddress> newIdRange(Range<?> range) {
        return new IdRange<IpAddress>((IpAddress) range.getLowerBound(), (IpAddress) range.getUpperBound()) {

            @Override public long getNumberOfIds() {
                return java.lang.Long.MAX_VALUE;
            }
        };
    }

    public IpSubnetNamespaceDto getIpSubnetNamespace() {
        return ExtAttr.IP_SUBNET_NAMESPACE.deref(this);
    }

    public IpSubnetDto getIpSubnet() {
        return ExtAttr.IP_SUBNET.deref(this);
    }

    public IpAddress getAddress() {
        IdRange<IpAddress> addressRange = getAddressRange();
        return addressRange == null
            ? null
            : addressRange.lowerBound;
    }

    public Integer getSubnetMask() {
        IdRange<IpAddress> addressRange = getAddressRange();
        return addressRange == null
            ? null
            : IpAddress.SubnetAddressUtils.maskLength(addressRange.lowerBound, addressRange.upperBound);
    }

    private IdRange<IpAddress> getAddressRange() {
        Set<IdRange<IpAddress>> addressRanges = getIdRanges();
        return addressRanges == null || addressRanges.size() == 0
            ? null
            : addressRanges.iterator().next();
    }
}
