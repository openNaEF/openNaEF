package voss.core.server.util;

import naef.dto.IdRange;
import naef.dto.ip.IpSubnetAddressDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.naef.AbsoluteNameFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class IpSubnetAddressUtil {

    public static IpSubnetAddressDto getIpSubnetAddress(String name) throws InventoryException, ExternalServiceException {
        try {
            List<IpSubnetAddressDto> subnets = CoreConnector.getInstance().getRootIpSubnetAddresses();
            return findIpSubnetAddress(subnets, name);
        } catch (IOException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static IpSubnetAddressDto findIpSubnetAddress(IpSubnetAddressDto subnet, String name) {
        if (subnet.getName().equals(name)) {
            return subnet;
        } else if (subnet.getChildren() == null) {
            return null;
        }
        return findIpSubnetAddress(subnet.getChildren(), name);
    }

    public static IpSubnetAddressDto findIpSubnetAddress(Collection<IpSubnetAddressDto> subnets, String name) {
        for (IpSubnetAddressDto subnet : subnets) {
            if (subnet.getName().equals(name)) {
                return subnet;
            }
            Set<IpSubnetAddressDto> children = subnet.getChildren();
            if (children != null && children.size() > 0) {
                IpSubnetAddressDto child = findIpSubnetAddress(children, name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public static IpSubnetAddressDto findMostAppropriateSubnet(IpSubnetAddressDto base, String ipAddress) {
        if (base == null) {
            throw new IllegalArgumentException("subnet is null.");
        } else if (ipAddress == null) {
            throw new IllegalArgumentException("ip-address is null.");
        }
        if (!isContained(base, ipAddress)) {
            return null;
        }
        Set<IpSubnetAddressDto> children = base.getChildren();
        if (children == null || children.size() == 0) {
            return base;
        }
        for (IpSubnetAddressDto child : children) {
            IpSubnetAddressDto candidate = findMostAppropriateSubnet(child, ipAddress);
            if (candidate != null) {
                return candidate;
            }
        }
        return base;
    }

    public static boolean isContained(IpSubnetAddressDto subnet, String ipAddress) {
        if (subnet == null) {
            throw new IllegalArgumentException("subnet is null.");
        } else if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress is null.");
        }
        IpAddress ip = IpAddress.gain(ipAddress);
        Set<IdRange<IpAddress>> ranges = subnet.getIdRanges();
        for (IdRange<IpAddress> range : ranges) {
            IpAddress lower = range.lowerBound;
            IpAddress upper = range.upperBound;
            if (0 <= lower.compareTo(ip) && upper.compareTo(ip) <= 0) {
                return true;
            }
        }
        return false;
    }

    public static IpSubnetAddressDto findMostAppropriateSubnet(IpSubnetAddressDto base, String ipAddress, int maskLength) {
        if (base == null) {
            throw new IllegalArgumentException("subnet is null.");
        } else if (ipAddress == null) {
            throw new IllegalArgumentException("ip-address is null.");
        }
        if (!isContained(base, ipAddress, maskLength)) {
            return null;
        }
        Set<IpSubnetAddressDto> children = base.getChildren();
        if (children == null || children.size() == 0) {
            return base;
        }
        for (IpSubnetAddressDto child : children) {
            IpSubnetAddressDto candidate = findMostAppropriateSubnet(child, ipAddress, maskLength);
            if (candidate != null) {
                return candidate;
            }
        }
        return base;
    }

    public static boolean isContained(IpSubnetAddressDto subnet, String ipAddress, int maskLength) {
        if (subnet == null) {
            throw new IllegalArgumentException("subnet is null.");
        } else if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress is null.");
        } else if (maskLength < 0) {
            throw new IllegalArgumentException("maskLength is negative.");
        }
        Integer mask = subnet.getSubnetMask();
        if (mask == null) {
            return false;
        } else if (mask.intValue() > maskLength) {
            return false;
        }
        IpAddress ip = IpAddress.gain(ipAddress);
        Set<IdRange<IpAddress>> ranges = subnet.getIdRanges();
        for (IdRange<IpAddress> range : ranges) {
            IpAddress lower = range.lowerBound;
            IpAddress upper = range.upperBound;
            if (lower.compareTo(ip) < 0 || 0 < upper.compareTo(ip)) {
                continue;
            }
        }
        return false;
    }

    public static String toIpSubnetNamespaceRangeString(String vpnPrefix, String baseAddrStr, int maskLength) {
        IpAddress baseAddr = IpAddress.gain(baseAddrStr);
        IpAddress endAddr = IpAddress.SubnetAddressUtils.endAddress(baseAddr, maskLength);
        StringBuilder sb = new StringBuilder();
        if (vpnPrefix != null) {
            sb.append(vpnPrefix).append("/");
        }
        sb.append(AbsoluteNameFactory.toHexFormat(baseAddr));
        sb.append("-");
        if (vpnPrefix != null) {
            sb.append(vpnPrefix).append("/");
        }
        sb.append(AbsoluteNameFactory.toHexFormat(endAddr));
        return sb.toString();
    }

    public static String toSubnetMask(Object o) {
        if (o == null) {
            return null;
        } else if (Integer.class.isInstance(o)) {
            return toSubnetMask((Integer) o);
        } else if (String.class.isInstance(o)) {
            return toSubnetMask((String) o);
        }
        return null;
    }

    public static String toSubnetMask(Integer length) {
        if (length == null) {
            return null;
        } else if (length.intValue() < 0) {
            throw new IllegalArgumentException("too small: " + length);
        } else if (length.intValue() > 32) {
            throw new IllegalArgumentException("too big: " + length);
        } else if (length.intValue() == 0) {
            return "0.0.0.0";
        }
        int base = 0xFFFFFFFF;
        int mask = base >> (32 - length);
        System.out.println(Integer.toHexString(mask));
        mask = mask << (32 - length);
        System.out.println(Integer.toHexString(mask));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            int val = mask;
            val = val >> ((3 - i) * 8);
            val = val & 0xFF;
            sb.append(val);
        }
        return sb.toString();
    }

    public static String toSubnetMask(String length) {
        return toSubnetMask(Integer.valueOf(length));
    }
}