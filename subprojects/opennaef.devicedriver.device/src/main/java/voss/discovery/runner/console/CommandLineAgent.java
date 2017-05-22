package voss.discovery.runner.console;

import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DiscoveryFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.model.NodeInfo;

import java.io.IOException;

public class CommandLineAgent {
    private final DeviceAccessFactory factory;

    public CommandLineAgent(DeviceAccessFactory factory, boolean save) {
        if (factory == null) {
            throw new IllegalArgumentException("DeviceAccessFactory is null.");
        }
        this.factory = factory;
    }

    public DeviceDiscovery getDeviceDiscovery(NodeInfo nodeinfo) throws IOException, AbortedException {
        if (this.factory == null) {
            throw new IllegalStateException("SNMPClientFactory not set.");
        }
        DeviceAccess access = factory.getDeviceAccess(nodeinfo);
        DeviceDiscovery discovery = DiscoveryFactory.getInstance().getDiscovery(access);
        System.err.println("using: " + discovery.getClass().getName() + "; " + access.getSnmpAccess());
        discovery.getDeviceInformation();
        return discovery;
    }
}