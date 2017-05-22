package voss.core.server.naming.naef;

public class IfNameFactory {

    public static String getVpnIpIfIfName(String vpnName, String ifName) {
        if (ifName == null) {
            throw new IllegalArgumentException("ifName is null");
        }
        if (vpnName == null) {
            return ifName;
        }
        return vpnName + "/" + ifName;
    }
}