package voss.discovery.runner.simple;


import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DeviceDiscoveryImpl;
import voss.discovery.agent.common.DiscoveryFactory;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.model.Device;
import voss.model.NodeInfo;

import java.io.IOException;

public class CommandLineAgent {
    private final DeviceAccessFactory factory;
    private final boolean saveArchive;

    public CommandLineAgent(DeviceAccessFactory factory, boolean save) {
        if (factory == null) {
            throw new IllegalArgumentException("DeviceAccessFactory is null.");
        }
        this.factory = factory;
        this.saveArchive = save;
    }

    public Device getDevice(NodeInfo nodeinfo) throws IOException, AbortedException {
        if (this.factory == null) {
            throw new IllegalStateException("SNMPClientFactory not set.");
        }

        DeviceDiscovery discovery = null;
        Device device = null;
        try {
            DeviceAccess access = factory.getDeviceAccess(nodeinfo);
            discovery = DiscoveryFactory.getInstance().getDiscovery(access);
            if (discovery instanceof DeviceDiscoveryImpl) {
                ((DeviceDiscoveryImpl) discovery).setRecord(saveArchive);
            }
            System.err.println("using: " + discovery.getClass().getName() + "; " + access.getSnmpAccess());
            discovery.getDeviceInformation();
            discovery.getPhysicalConfiguration();
            discovery.getLogicalConfiguration();
            device = discovery.getDevice();
            Mib2Impl.setIpAddressWithIfIndex(access.getSnmpAccess(), device);
        } finally {
            if (discovery != null) {
                if (saveArchive && discovery instanceof DeviceDiscoveryImpl) {
                    ((DeviceDiscoveryImpl) discovery).saveCacheAsArchive();
                }
                discovery.close();
            }
        }
        return device;
    }
}