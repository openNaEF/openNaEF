package voss.discovery.agent.alcatel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.mib.*;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.mib.OspfMibImpl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.Device;
import voss.model.LogicalPort;
import voss.model.MplsVlanDevice;
import voss.model.NotInitializedException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Alcatel7710SRDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(Alcatel7710SRDiscovery.class);
    private final Map<String, String> productList = new HashMap<String, String>();

    private final MplsVlanDevice device;
    private final MplsModelBuilder builder;

    private final Mib2Impl mib2;
    private final TimetraChassisMibImpl timetraChassisMib;
    private final TimetraSystemMibImpl timetraSystemMib;
    private final TimetraMplsMibImpl timetraMplsMib;
    private final TimetraServMibImpl timetraServMib;
    private final TimetraSdpMibImpl timetraSdpMib;
    private final TimetraPortMibImpl timetraPortMib;
    private final TimetraVrtrMibImpl timetraVrtrMib;
    private final ApsMibImpl apsMib;
    private final OspfMibImpl ospfMib;

    private String textConfiguration;
    private final AgentConfiguration agentConfig;

    private ConsoleCommand show_configuration = new ConsoleCommand(new NullMode(), "admin display-config");
    private ConsoleCommand show_interface = new ConsoleCommand(new NullMode(), "show port");

    public Alcatel7710SRDiscovery(DeviceAccess access) throws IOException, AbortedException {
        super(access);

        SnmpAccess snmp = access.getSnmpAccess();
        this.agentConfig = AgentConfiguration.getInstance();
        this.device = new MplsVlanDevice();
        this.device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        this.builder = new MplsModelBuilder(device);
        this.productList.putAll(agentConfig.getDiscoveryParameter(DiscoveryParameterType.ALCATEL_PRODUCT_LIST));
        this.mib2 = new Mib2Impl(snmp);

        this.timetraPortMib = new TimetraPortMibImpl(this);
        this.timetraVrtrMib = new TimetraVrtrMibImpl(this);
        this.timetraSystemMib = new TimetraSystemMibImpl(this);
        this.timetraMplsMib = new TimetraMplsMibImpl(this);
        this.timetraSdpMib = new TimetraSdpMibImpl(this);
        this.timetraServMib = new TimetraServMibImpl(this);
        this.timetraChassisMib = new TimetraChassisMibImpl(this);
        this.apsMib = new ApsMibImpl(this);
        this.ospfMib = new OspfMibImpl(snmp, device);
    }

    public MplsModelBuilder getMplsModelBuilder() {
        return builder;
    }

    public TimetraMplsMibImpl getTimetraMplsMib() {
        return timetraMplsMib;
    }

    public TimetraPortMibImpl getTimetraPortMib() {
        return timetraPortMib;
    }

    public TimetraSdpMibImpl getTimetraSdpMib() {
        return timetraSdpMib;
    }
    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = this.getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            this.textConfiguration = console.getResponse(show_configuration);
            log.debug("got configuration: \r\n" + this.textConfiguration);
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        SnmpAccess snmp = this.getSnmpAccess();

        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setContactInfo(mib2.getSysContact());
        device.setVendorName(Constants.VENDOR_ALCATEL);
        device.setModelTypeName(timetraChassisMib.getChassisTypeName());
        device.setSerialNumber(timetraChassisMib.getSerialNumber());
        device.setDescription(mib2.getSysDescr());
        device.setCommunityRO(snmp.getCommunityString());
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());
        device.setSysUpTime(new Date(mib2.getSysUpTimeInMilliseconds().longValue()));
        device.setOsTypeName(OSType.ALCATEL_OS.caption);
        device.setOsVersion(timetraSystemMib.getSwVersion());
        mib2.setDefaultGateway(device);
        mib2.setSnmpTrapReceiverAddress(device);
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        apsMib.createAPS();
        timetraVrtrMib.createLogicalPorts();
        timetraPortMib.createLogicalPorts();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);

        ospfMib.getOspfIfParameters();

        for (LogicalPort port : device.getLogicalPorts()) {
            try {
                timetraPortMib.setIfOperStatus(port);
                timetraPortMib.setIfAdminStatus(port);
                timetraPortMib.setIfSpeed(port);
                timetraPortMib.setIfType(port);
                timetraPortMib.setIfAlias(port);
                timetraPortMib.setIfDescription(port);
            } catch (NotInitializedException e) {
                log.debug("skip uninitialized port '" + port.getIfName() + "'");
            }
        }

        getLsp();
        timetraSdpMib.setSdpIdToLsp();
        getPseudoWire();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void getLsp() throws IOException, AbortedException {
        timetraMplsMib.getMplsConfiguration();
    }

    public void getPseudoWire() throws IOException, AbortedException {
        timetraServMib.getService();
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        timetraChassisMib.createSlotAndModule();

        timetraPortMib.createPhysicalPorts();
        timetraPortMib.setAllIfNames(device);
        timetraPortMib.setAllIfOperStatus(device);
        timetraPortMib.setAllIfAdminStatus(device);
        timetraPortMib.setAllIfSpeed(device);
        timetraPortMib.setAllIfTypes(device);
        timetraPortMib.setAllIfAliases(device);
        timetraPortMib.setAllIfDescriptions(device);

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_configuration);
            entry.addConsoleResult(show_configuration, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            console.close();
        }
        entry.close();
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
        apsMib.createAPS();
        timetraVrtrMib.createLogicalPorts();
        timetraPortMib.createLogicalPorts();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        for (LogicalPort port : device.getLogicalPorts()) {
            try {
                timetraPortMib.setIfOperStatus(port);
            } catch (NotInitializedException e) {
                log.debug("skip uninitialized port '" + port.getIfName() + "'");
            }
        }
        timetraMplsMib.getMplsStatus();
        timetraServMib.getServiceStatus();
    }
}