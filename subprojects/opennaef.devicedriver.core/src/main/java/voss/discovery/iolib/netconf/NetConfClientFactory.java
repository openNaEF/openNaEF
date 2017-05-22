package voss.discovery.iolib.netconf;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.SupportedDiscoveryType;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;

public interface NetConfClientFactory {
    NetConfClient getNetConfClient(NodeInfo nodeinfo, SnmpAccess snmp,
                                   InetAddress inetAddress, SupportedDiscoveryType type) throws IOException, AbortedException;
}