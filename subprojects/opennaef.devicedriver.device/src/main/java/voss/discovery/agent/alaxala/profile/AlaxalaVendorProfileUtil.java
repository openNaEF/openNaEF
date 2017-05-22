package voss.discovery.agent.alaxala.profile;

import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;

public class AlaxalaVendorProfileUtil {
    public static String getIosLikeIfName(SnmpAccess snmp, AlaxalaPortEntry entry) throws IOException, AbortedException {
        String ifname = Mib2Impl.getIfName(snmp, entry.getIfIndex());

        if (ifname.startsWith("GigabitEther")) {
            return ifname.toLowerCase().replace("ether", "ethernet");
        } else if (ifname.startsWith("TenGigabitEther")) {
            return ifname.toLowerCase().replace("ether", "ethernet");
        } else if (ifname.startsWith("TenGigabiEther")) {
            return ifname.toLowerCase().replace("iether", "itethernet");
        } else if (ifname.startsWith("FastEther")) {
            return ifname.toLowerCase().replace("ether", "ethernet");
        } else {
            throw new IllegalStateException("unknown ifname: " + ifname);
        }
    }

    public static String getIosLikeAggregationName(int aggregationId) {
        return "port-channel " + aggregationId;
    }

    public static String getAlaxalaLikeAggregationName(int aggregationId) {
        return "la-id " + aggregationId;
    }

}