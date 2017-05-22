package voss.discovery.agent.util;

import voss.discovery.agent.common.WarningMessenger;
import voss.model.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DiscoveryUtils {

    private DiscoveryUtils() {
    }

    public static CidrAddress toCidrAddress(String address) throws UnknownHostException {
        String[] s = address.split("/");
        InetAddress addr = InetAddress.getByName(s[0]);
        int masklen = Integer.parseInt(s[1]);
        CidrAddress cidr = new CidrAddress(addr, masklen);
        return cidr;
    }

    public static WarningMessenger getWarningMessenger() {
        ThreadLocal<WarningMessenger> threadLocal = new ThreadLocal<WarningMessenger>();
        WarningMessenger warnings = threadLocal.get();
        if (warnings == null) {
            warnings = new WarningMessenger();
            threadLocal.set(warnings);
        }
        return warnings;
    }

    public static LogicalEthernetPort getLogicalEthernetPort(Port port) {
        if (port == null) {
            return null;
        } else if (port instanceof LogicalEthernetPort) {
            return (LogicalEthernetPort) port;
        } else if (port instanceof EthernetPort) {
            EthernetPort eth = (EthernetPort) port;
            if (eth.getDevice() instanceof VlanDevice) {
                VlanDevice vd = (VlanDevice) eth.getDevice();
                return vd.getLogicalEthernetPort(eth);
            }
        }
        return null;
    }
}