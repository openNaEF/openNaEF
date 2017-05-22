package voss.multilayernms.inventory.renderer;

import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;

import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("serial")
public class IpSubnetAddressRenderer implements Serializable {
    private final IpSubnetNamespaceDto subnet;
    private final IpSubnetAddressDto address;

    public IpSubnetAddressRenderer(IpSubnetNamespaceDto subnet) {
        this.subnet = subnet;
        if (this.subnet != null) {
            this.address = subnet.getIpSubnetAddress();
        } else {
            this.address = null;
        }
    }

    public String getVpnPrefix() {
        return DtoUtil.getStringOrNull(this.subnet, ATTR.VPN_PREFIX);
    }

    public String getIpSubnetNamespaceName() {
        if (this.subnet == null) {
            return null;
        }
        return this.subnet.getName();
    }

    public String getIpSubnetAddressName() {
        if (this.address == null) {
            return "N/A";
        }
        return this.address.getName();
    }

    public String getHead() {
        return "IP Subnet Address: " + getIpSubnetNamespaceName();
    }

    public IdRange<IpAddress> getIdRange() {
        if (this.address == null) {
            return null;
        }
        Set<IdRange<IpAddress>> ranges = this.address.getIdRanges();
        if (ranges.size() > 1) {
            throw new IllegalStateException("There are multiple id-ranges.");
        } else if (ranges.size() == 0) {
            return null;
        }
        return ranges.iterator().next();
    }

    public String getStartAddress() {
        IdRange<IpAddress> range = getIdRange();
        if (range == null) {
            return null;
        }
        IpAddress addr = range.lowerBound;
        if (addr == null) {
            throw new IllegalArgumentException("id-range has no start ip-address: " + DtoUtil.toDebugString(this.address));
        }
        return addr.toString();
    }

    public String getEndAddress() {
        IdRange<IpAddress> range = getIdRange();
        if (range == null) {
            return null;
        }
        IpAddress addr = range.upperBound;
        if (addr == null) {
            throw new IllegalArgumentException("id-range has no end ip-address: " + DtoUtil.toDebugString(this.address));
        }
        return addr.toString();
    }

    public Integer getMaskLength() {
        if (this.address == null) {
            return null;
        }
        return this.address.getSubnetMask();
    }

    public String getSubnetAddress() {
        if (this.address == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        if (this.address.getSubnetMask() == null) {
            for (IdRange<IpAddress> range : this.address.getIdRanges()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(range.lowerBound.toString());
                sb.append("-");
                sb.append(range.upperBound.toString());
            }
        } else {
            IpAddress addr = this.address.getAddress();
            sb.append(addr.toString());
            sb.append("/");
            sb.append(this.address.getSubnetMask());
        }
        return sb.toString();
    }

    public String getLastEditor() {
        return GenericRenderer.getLastEditor(this.address);
    }

    public String getLastEditTime() {
        return GenericRenderer.getLastEditTime(this.address);
    }
}