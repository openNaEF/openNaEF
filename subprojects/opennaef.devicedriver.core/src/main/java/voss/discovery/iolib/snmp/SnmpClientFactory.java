package voss.discovery.iolib.snmp;

import net.snmp.SnmpClient;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;

public interface SnmpClientFactory {
    public SnmpClient createSnmpClient(InetAddress inetAddress, NodeInfo nodeinfo) throws IOException;

    public void setTimeout(int timeout);

    public void setRetry(int retryCount);
}