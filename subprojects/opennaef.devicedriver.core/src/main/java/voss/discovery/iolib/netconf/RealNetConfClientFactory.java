package voss.discovery.iolib.netconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.SupportedDiscoveryType;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;

public class RealNetConfClientFactory implements NetConfClientFactory {
    public static final Logger log = LoggerFactory.getLogger(RealNetConfClientFactory.class);

    @Override
    public NetConfClient getNetConfClient(NodeInfo nodeinfo, SnmpAccess snmp,
                                          InetAddress inetAddress, SupportedDiscoveryType type)
            throws IOException, AbortedException {
        switch (type) {
            case CiscoNexusDiscovery:
                return new CiscoNexusNetConfClient(inetAddress.getHostAddress(), nodeinfo);
        }
        return null;
    }
}