package voss.discovery.iolib;

import net.snmp.OidTLV;
import net.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AgentConfiguration;
import voss.discovery.constant.AccessMode;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleClientFactory;
import voss.discovery.iolib.console.RealConsoleClientFactory;
import voss.discovery.iolib.netconf.NetConfAccess;
import voss.discovery.iolib.netconf.NetConfClient;
import voss.discovery.iolib.netconf.NetConfClientFactory;
import voss.discovery.iolib.netconf.RealNetConfClientFactory;
import voss.discovery.iolib.remote.RemoteSnmpAccessClient;
import voss.discovery.iolib.remote.RemoteSnmpAccessService;
import voss.discovery.iolib.remote.RemoteSnmpAccessSession;
import voss.discovery.iolib.snmp.RealSnmpClientFactory;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpAccessImpl;
import voss.discovery.iolib.snmp.SnmpClientFactory;
import voss.model.NodeInfo;
import voss.model.Protocol;
import voss.model.ProtocolPort;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class RealDeviceAccessFactory implements DeviceAccessFactory {
    private static final Logger log = LoggerFactory.getLogger(RealDeviceAccessFactory.class);
    public static final AccessMode MODE = AccessMode.REAL;
    private final ConsoleClientFactory consoleClientFactory;
    private final SnmpClientFactory snmpClientFactory;
    private final NetConfClientFactory netconfClientFactory;
    private final AgentConfiguration config = AgentConfiguration.getInstance();
    private final int PING_TIMEOUT = config.getPingTimeoutSeconds() * 1000;

    private final DiscoveryTypeFactory typeFactory;
    private String remoteSnmpAccessServer = null;
    private int remoteSnmpAccessServerPort = RemoteSnmpAccessService.DEFAULT_PORT;

    public RealDeviceAccessFactory() {
        this.typeFactory = new DiscoveryTypeFactory();
        this.snmpClientFactory = new RealSnmpClientFactory();
        this.consoleClientFactory = new RealConsoleClientFactory();
        this.netconfClientFactory = new RealNetConfClientFactory();
    }

    public void setRemoteSnmpAccessServer(String addr, int port) {
        this.remoteSnmpAccessServer = addr;
        this.remoteSnmpAccessServerPort = port;
    }

    public String getRemoteSnmpAccessServer() {
        return this.remoteSnmpAccessServer;
    }

    public int getRemoteSnmpAccessServerPort() {
        return this.remoteSnmpAccessServerPort;
    }

    @Override
    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo) throws IOException, AbortedException {
        return getDeviceAccess(nodeinfo, nodeinfo.listIpAddress());
    }

    @Override
    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo, List<InetAddress> specified)
            throws IOException, AbortedException {
        setupNodeInfo(nodeinfo);
        ProgressMonitor monitor = new ProgressMonitor();
        ReachableTarget reachableTarget = selectReachableInetAddress(nodeinfo, specified);
        if (reachableTarget == null) {
            return null;
        }
        log.info("reachableInetAddress=" + reachableTarget.inetAddress.getHostAddress());

        SnmpAccess snmpAccess = null;
        if (this.remoteSnmpAccessServer == null) {
            snmpAccess = new SnmpAccessImpl(reachableTarget.client);
        } else {
            RemoteSnmpAccessService remote = getRemoteSnmpAccessService();
            RemoteSnmpAccessSession session = remote.getSession(nodeinfo);
            snmpAccess = new RemoteSnmpAccessClient(session);
            reachableTarget.client.close();
        }
        snmpAccess.setMonitor(monitor);

        SupportedDiscoveryType type = null;
        try {
            type = this.typeFactory.getType(snmpAccess);
        } catch (UnknownTargetException e) {
            try {
                snmpAccess.close();
            } catch (Exception ex) {
            }
            throw new IOException("unknown type: " + nodeinfo.getFirstIpAddress() + "->" + e.getSysObjectID());
        }

        ConsoleClient console = getConsoleClient(nodeinfo, snmpAccess, reachableTarget.inetAddress, type);
        ConsoleAccess consoleAccess = null;
        if (console != null) {
            consoleAccess = new ConsoleAccess(console);
        }
        NetConfClient netconf = getNetConfClient(nodeinfo, snmpAccess, reachableTarget.inetAddress, type);
        NetConfAccess netconfAccess = null;
        if (netconf != null) {
            netconfAccess = new NetConfAccess(netconf);
        }
        DeviceAccess access = new DeviceAccess(nodeinfo, getMode(), monitor, type);
        access.setConsoleAccess(consoleAccess);
        access.setSnmpAccess(snmpAccess);
        access.setNetConfAccess(netconfAccess);
        access.setTargetAddress(reachableTarget.inetAddress);
        return access;
    }

    private void setupNodeInfo(NodeInfo info) {
        if (info.getSnmpRetryTimes() == -1) {
            info.setSnmpRetryTimes(this.config.getSnmpRetryTimes());
        }
        if (info.getSnmpTimeoutSec() == -1) {
            info.setSnmpTimeoutSec(this.config.getSnmpTimeoutSeconds());
        }
        if (info.getWalkIntervalMilliSec() == -1) {
            info.setWalkIntervalMilliSec(this.config.getWalkIntervalMilliSeconds());
        }
        if (info.getConsoleTimeoutSec() == -1) {
            info.setConsoleTimeoutSec(this.config.getTelnetTimeoutSeconds());
        }
        if (info.getConsoleIntervalMilliSec() == -1) {
            info.setConsoleIntervalMilliSec(this.config.getTelnetCommandIntervalMilliSeconds());
        }
        if (info.getMoreIntervalMilliSec() == -1) {
            info.setMoreIntervalMilliSec(this.config.getMoreIntervalMilliSeconds());
        }
        if (info.getMoreIntervalMilliSec() == -1) {
            info.setMoreIntervalMilliSec(this.config.getPingTimeoutSeconds());
        }
    }

    private ReachableTarget selectReachableInetAddress(NodeInfo nodeinfo,
                                                       List<InetAddress> list) throws IOException {
        for (InetAddress inetAddress : list) {
            ReachableTarget result = getReachableTarget(nodeinfo, inetAddress);
            if (result != null) {
                return result;
            }
        }
        throw new IOException("no response from " + nodeinfo.getNodeIdentifier());
    }

    private ReachableTarget getReachableTarget(NodeInfo nodeinfo, InetAddress inetAddress) throws IOException {
        log.debug("selectReachableInetAddress():" + inetAddress.getHostAddress());
        SnmpClient client = snmpClientFactory.createSnmpClient(inetAddress, nodeinfo);
        client.setSocketTimeout(PING_TIMEOUT);

        boolean fallback = false;
        if (nodeinfo.isSupported(Protocol.SNMP_V2C_GETBULK)) {
            client.setWalkMode(SnmpClient.WALK_MODE_BULK);
            log.debug("- using snmpv2 bulk walk.");
            if (testSnmpVersion(client)) {
                nodeinfo.setEffectiveSnmpProtocol(ProtocolPort.SNMP_V2C_GETBULK);
                nodeinfo.setEffectiveAddress(inetAddress);
                return new ReachableTarget(inetAddress, client);
            }
            fallback = true;
        }
        if (fallback || nodeinfo.isSupported(Protocol.SNMP_V2C_GETNEXT)) {
            client.setWalkMode(SnmpClient.WALK_MODE_REGULAR);
            log.debug("- using snmpv2 regular walk.");
            if (testSnmpVersion(client)) {
                nodeinfo.setEffectiveSnmpProtocol(ProtocolPort.SNMP_V2C_GETNEXT);
                nodeinfo.setEffectiveAddress(inetAddress);
                return new ReachableTarget(inetAddress, client);
            }
            fallback = true;
        }
        if (fallback || nodeinfo.isSupported(Protocol.SNMP_V1)) {
            client.setSnmpVersion1();
            log.debug("- using snmpv1.");
            if (testSnmpVersion(client)) {
                nodeinfo.setEffectiveSnmpProtocol(ProtocolPort.SNMP_V1);
                nodeinfo.setEffectiveAddress(inetAddress);
                return new ReachableTarget(inetAddress, client);
            }
        }
        client.close();
        return null;
    }

    private boolean testSnmpVersion(SnmpClient client) {
        try {
            client.snmpGet(OidTLV.getInstance(".1.3.6.1.2.1.1.1.0"));
            return true;
        } catch (Exception e) {
            log.trace("testSnmpVersion(): test failed.", e);
            return false;
        }
    }

    private ConsoleClient getConsoleClient(NodeInfo nodeinfo, SnmpAccess snmp, InetAddress inetAddress,
                                           SupportedDiscoveryType type) throws IOException, AbortedException {
        if (type == null) {
            return null;
        }
        return consoleClientFactory.getConsoleClient(nodeinfo, snmp, inetAddress, type);
    }

    private NetConfClient getNetConfClient(NodeInfo nodeinfo, SnmpAccess snmp, InetAddress inetAddress,
                                           SupportedDiscoveryType type) throws IOException, AbortedException {
        if (type == null) {
            return null;
        }
        return netconfClientFactory.getNetConfClient(nodeinfo, snmp, inetAddress, type);
    }

    @Override
    public AccessMode getMode() {
        return MODE;
    }

    private static class ReachableTarget {
        public final InetAddress inetAddress;
        public final SnmpClient client;

        public ReachableTarget(InetAddress inetAddress, SnmpClient client) {
            this.inetAddress = inetAddress;
            this.client = client;
        }
    }

    private RemoteSnmpAccessService getRemoteSnmpAccessService()
            throws IOException, RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(remoteSnmpAccessServer, remoteSnmpAccessServerPort);
            return (RemoteSnmpAccessService) registry.lookup(RemoteSnmpAccessService.SERVICE_NAME);
        } catch (NotBoundException e) {
            throw new IOException("remote service not found on " + remoteSnmpAccessServer, e);
        }
    }

}