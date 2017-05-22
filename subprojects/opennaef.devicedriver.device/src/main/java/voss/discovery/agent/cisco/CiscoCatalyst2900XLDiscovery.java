package voss.discovery.agent.cisco;


import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.CiscoCatalyst2900XLMibImpl;
import voss.discovery.agent.cisco.mib.CiscoImageMib;
import voss.discovery.agent.cisco.mib.CiscoVlanMembershipMib;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.DeviceDiscoveryImpl;
import voss.discovery.agent.common.DiscoveryStatus;
import voss.discovery.agent.mib.EntityMib.EntityMibEntPhysicalModelNameEntry;
import voss.discovery.agent.mib.EntityMib.EntityMibEntPhysicalSerialNumEntry;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.Device;
import voss.model.EthernetPort;
import voss.model.GenericEthernetSwitch;
import voss.model.VlanDevice;

import java.io.IOException;
import java.util.Date;

public class CiscoCatalyst2900XLDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(CiscoCatalyst2900XLDiscovery.class);
    private VlanDevice device;
    private final Mib2Impl mib2;
    private final ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private final ConsoleCommand show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    private final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public CiscoCatalyst2900XLDiscovery(DeviceAccess access) {
        super(access);
        this.mib2 = new Mib2Impl(this.getDeviceAccess().getSnmpAccess());
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public Device getDeviceInner() {
        return device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        this.device = new GenericEthernetSwitch();
        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setVendorName(Constants.VENDOR_CISCO);

        try {
            String modelTypeName = SnmpUtil.getString(this.getDeviceAccess().getSnmpAccess(),
                    EntityMibEntPhysicalModelNameEntry.OID + ".1");
            device.setModelTypeName(modelTypeName);
        } catch (NoSuchMibException e) {
            String objectId = Mib2Impl.getSysObjectId(this.getDeviceAccess().getSnmpAccess());
            device.setModelTypeName(objectId);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        CiscoImageMib mib = new CiscoImageMib(this.getDeviceAccess().getSnmpAccess());
        mib.setDeviceDescription(device);
        device.setOsTypeName("IOS");
        mib.setOsVersion(device);

        device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setLocation(mib2.getSysLocation());
        device.setContactInfo(mib2.getSysContact());
        device.setDescription(mib2.getSysDescr());
        try {
            device.setBasePhysicalAddress(mib2.getSystemMacAddress());
        } catch (IOException e) {
            log.warn("no mib found: dot1dBridge.dot1dBase.dot1dBaseBridgeAddress.", e);
        }
        device.setGatewayAddress(mib2.getGatewayAddress());
        device.setTrapReceiverAddresses(mib2.getTrapReceiverAddresses());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        try {
            String serial = SnmpUtil.getString(this.getDeviceAccess().getSnmpAccess(),
                    EntityMibEntPhysicalSerialNumEntry.OID + ".1");
            device.setSerialNumber(serial);
        } catch (NoSuchMibException e) {
            log.warn("cannot get serial number: " + device.getIpAddress()
                    + ":" + EntityMibEntPhysicalSerialNumEntry.OID + ".1");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        CiscoCatalyst2900XLMibImpl catalyst2900XLMib = new CiscoCatalyst2900XLMibImpl(this.getDeviceAccess().getSnmpAccess());
        catalyst2900XLMib.createSlotsAndModules(this.device);
        catalyst2900XLMib.createC2900Interfaces(this.device);

        InterfaceMibImpl ifmib = new InterfaceMibImpl(this.getDeviceAccess().getSnmpAccess());
        for (EthernetPort port : device.getEthernetPorts()) {
            ifmib.setIfAlias(port);
            ifmib.setIfAdminStatus(port);
            ifmib.setIfOperStatus(port);
            ifmib.setIfDescription(port);
            ifmib.setIfSpeed(port);
            ifmib.setIfType(port);
        }

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }

        CiscoVlanMembershipMib mib = new CiscoVlanMembershipMib(this.getDeviceAccess().getSnmpAccess());
        mib.createVlanConfiguration(device);
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(show_running_config);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + res1 + "\r\n----");
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            return res1;
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            String res3 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res3);

            console.close();
        }

        entry.close();
    }
}