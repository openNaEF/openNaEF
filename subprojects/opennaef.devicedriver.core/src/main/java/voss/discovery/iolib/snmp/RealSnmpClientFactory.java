package voss.discovery.iolib.snmp;

import net.snmp.SnmpClient;
import voss.model.NodeInfo;
import voss.model.Protocol;
import voss.model.ProtocolPort;

import java.io.IOException;
import java.net.InetAddress;

public class RealSnmpClientFactory implements SnmpClientFactory {

    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_RETRY = 2;

    private int timeout;
    private int retry;

    public RealSnmpClientFactory() {
        this(DEFAULT_TIMEOUT, DEFAULT_RETRY);
    }

    public RealSnmpClientFactory(int timeout, int retry) {
        this.timeout = timeout;
        this.retry = retry;
    }

    public SnmpClient createSnmpClient(InetAddress inetAddress, NodeInfo nodeinfo) throws IOException {
        Protocol snmpProtocol = nodeinfo.getPreferredSnmpMethod();
        if (snmpProtocol == null) {
            throw new IOException("no snmp parameter: " + nodeinfo.getNodeIdentifier());
        }
        ProtocolPort pp = nodeinfo.getProtocolPort(snmpProtocol);
        if (pp == null) {
            throw new IOException("no snmp parameter: " + nodeinfo.getNodeIdentifier());
        }
        return createSnmpClient(inetAddress,
                pp.getPort(),
                nodeinfo.getCommunityStringRO().getBytes());
    }

    public SnmpClient createSnmpClient(InetAddress nodeAddress, int snmpPort, byte[] communityString)
            throws IOException {
        SnmpClient snmpClient
                = new SnmpClient(nodeAddress, snmpPort, communityString, new SnmpClientLogger());
        snmpClient.setRetry(retry);
        snmpClient.setSocketTimeout(timeout);
        String walkIntervalStr = System.getProperty("snmp-walk-interval");
        try {
            int walkInterval = Integer.parseInt(walkIntervalStr);
            snmpClient.setWalkInterval(walkInterval);
        } catch (NumberFormatException nfe) {
        }
        return snmpClient;
    }

    public void setTimeout(int sec) {
        this.timeout = sec * 1000;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
}