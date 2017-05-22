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
import voss.model.ProtocolPort;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DynamicStatusAgent {
    private final DeviceAccessFactory factory;
    private final boolean saveArchive;

    public DynamicStatusAgent(DeviceAccessFactory factory, boolean save) {
        if (factory == null) {
            throw new IllegalArgumentException("DeviceAccessFactory is null.");
        }
        this.factory = factory;
        this.saveArchive = save;
    }

    public static NodeInfo createNodeInfo(String[] parameters) {
        if (parameters.length < 2) {
            throw new IllegalArgumentException();
        } else if (parameters[0].startsWith("#")) {
            return null;
        }

        try {
            NodeInfo nodeinfo = new NodeInfo();
            InetAddress inetAddress = InetAddress.getByAddress(VossMiscUtility.getByteFormIpAddress(parameters[0]));
            nodeinfo.addIpAddress(inetAddress);
            nodeinfo.setCommunityStringRO(parameters[1]);

            if (parameters.length >= 3) {
                nodeinfo.setUserAccount(parameters[2]);
                if (parameters.length >= 4) {
                    nodeinfo.setUserPassword(parameters[3]);
                    if (parameters.length >= 5) {
                        nodeinfo.setAdminAccount(parameters[4]);
                        if (parameters.length >= 6) {
                            nodeinfo.setAdminPassword(parameters[5]);
                            if (parameters.length >= 7 && parameters[6].toLowerCase().equals("v1")) {
                                nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V1);
                            } else if (parameters.length >= 7 && parameters[6].toLowerCase().equals("v2next")) {
                                nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETNEXT);
                            } else if (parameters.length >= 7 && parameters[6].toLowerCase().equals("v2walk")) {
                                nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETNEXT);
                            } else {
                                nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETBULK);
                            }
                        }
                    }
                }
            }
            nodeinfo.addSupportedProtocol(ProtocolPort.TELNET);
            return nodeinfo;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
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
            discovery.getDynamicStatus();
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