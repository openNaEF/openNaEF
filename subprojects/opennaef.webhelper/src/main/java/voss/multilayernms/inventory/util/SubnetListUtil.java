package voss.multilayernms.inventory.util;

import naef.dto.PortDto;
import voss.multilayernms.inventory.renderer.PortRenderer;

public class SubnetListUtil {
    public static String getNetworkAddress(PortDto port) {
        return int2IpAddressString(getIpRangeMin(port));
    }

    public static int getIpRangeMin(PortDto port) {
        return getIpRangeMin(PortRenderer.getIpAddress(port), PortRenderer.getSubnetMask(port));
    }

    public static int getIpRangeMin(String addressStr, String maskStr) {
        int ip = ipString2int(addressStr);
        int mask = maskLength2int(maskStr);

        int network = ip & mask;
        return network;
    }

    public static int getIpRangeMax(PortDto port) {
        int network = getIpRangeMin(port);
        int mask = Integer.parseInt(PortRenderer.getSubnetMask(port));

        for (int i = 31; i >= 0; i--) {
            if (mask-- > 0) {
                continue;
            }
            network += 1 << i;
        }
        return network;
    }

    private static int maskLength2int(String str) {
        Integer maskLength = Integer.parseInt(str);
        int subnet = 0;

        for (int i = 31; i >= 0; i--) {
            if (maskLength-- < 1) {
                break;
            }
            subnet += 1 << i;
        }
        return subnet;
    }

    public static int ipString2int(String str) {
        String[] ipStrings = str.split("\\.");
        int ipInt = 0;

        int j = 0;
        for (int i = 3; i >= 0; i--) {
            Integer num = Integer.parseInt(ipStrings[j++]);
            ipInt += num << (8 * i);
        }
        return ipInt;
    }

    public static String int2IpAddressString(long i) {
        long b0 = (i >> 24) & 0xff;
        long b1 = (i >> 16) & 0xff;
        long b2 = (i >> 8) & 0xff;
        long b3 = i & 0xff;

        StringBuilder sb = new StringBuilder();
        sb.append(b0).append(".");
        sb.append(b1).append(".");
        sb.append(b2).append(".");
        sb.append(b3);
        return sb.toString();
    }
}