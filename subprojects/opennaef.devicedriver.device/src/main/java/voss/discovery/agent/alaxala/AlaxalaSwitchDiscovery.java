package voss.discovery.agent.alaxala;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alaxala.mib.AlaxalaMibImpl;
import voss.discovery.agent.alaxala.mib.AlaxalaVlanMibImpl;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.agent.common.*;
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
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.*;

import java.io.IOException;
import java.util.Date;

public abstract class AlaxalaSwitchDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(AlaxalaSwitchDiscovery.class);
    protected String textConfiguration;
    protected final AlaxalaMibImpl method;
    protected final Mib2Impl mib2;
    protected final InterfaceMibImpl ifmib;

    protected final AlaxalaVendorProfile profile;

    protected ConsoleCommand show_running_config = new ConsoleCommand(
            new GlobalMode(), "show configuration");
    protected ConsoleCommand show_interface = new ConsoleCommand(
            new GlobalMode(), "show interface");

    public AlaxalaSwitchDiscovery(DeviceAccess access,
                                  AlaxalaVendorProfile profile, AlaxalaMibImpl method) {
        super(access);
        this.profile = profile;
        this.method = method;
        this.mib2 = new Mib2Impl(access.getSnmpAccess());
        this.ifmib = new InterfaceMibImpl(access.getSnmpAccess());
    }

    protected AlaxalaVlanSwitch device;

    public Device getDeviceInner() {
        if (this.device != null) {
            return this.device;
        }
        throw new IllegalStateException("not discovered.");
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        SnmpAccess snmp = getDeviceAccess().getSnmpAccess();

        device = new AlaxalaVlanSwitch();
        String hostname = Mib2Impl.getSysName(snmp);
        device.setDeviceName(hostname);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());

        device.setVendorName(Constants.VENDOR_ALAXALA);
        device.setModelTypeName(profile.getModelName(snmp));
        device.setOsTypeName(profile.getOsType());
        device.setOsVersion(method.getOsVersion());

        String sysDescr = Mib2Impl.getSysDescr(snmp);
        device.setDescription(sysDescr);
        device.setContactInfo(Mib2Impl.getSysContact(snmp));
        device.setLocation(Mib2Impl.getSysLocation(snmp));
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(snmp).longValue()));
        device.setCommunityRO(snmp.getCommunityString());
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());

        device.setGatewayAddress(mib2.getGatewayAddress());
        device.setTrapReceiverAddresses(mib2.getTrapReceiverAddresses());
        device.setSyslogServerAddresses(null);

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        collectSlotsAndModules();
        createPhysicalPorts();
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        collectEthernetAggregators();

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    protected abstract void collectSlotsAndModules() throws IOException,
            AbortedException;

    protected void createPhysicalPorts() throws IOException, AbortedException {
        AlaxalaPortEntry[] entries = method.getPhysicalPorts();
        method.prepareAggregatorIfIndex();

        for (int i = 0; i < entries.length; i++) {
            int ifIndex = entries[i].getIfIndex();

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initPortIndex(entries[i].getPortId());
            port.initIfIndex(ifIndex);
            port.setPortTypeName(method.getPhysicalLineConnectorType(entries[i]
                    .getSlotId(), entries[i].getPortId()));

            ifmib.setIfName(port);
            ifmib.setIfAlias(port);
            ifmib.setIfAdminStatus(port);
            ifmib.setIfOperStatus(port);
            ifmib.setIfSpeed(port);

            if (method.getAdminSpeed(ifIndex) != null) {
                port.setPortAdministrativeSpeed(method.getAdminSpeed(ifIndex));
            }
            if (method.getDuplex(ifIndex) != null) {
                port.setDuplex(method.getDuplex(ifIndex));
            }

            Integer aggregatorIfIndex = method.getAggregatorIfIndex(ifIndex);
            if (aggregatorIfIndex != null) {
                int aggregationId = method.getAggregationId(aggregatorIfIndex
                        .intValue());
                EthernetPortsAggregator lag = DeviceInfoUtil
                        .getOrCreateLogicalEthernetPortByAggregationId(device,
                                aggregationId);
                lag.initIfIndex(aggregatorIfIndex.intValue());
                lag
                        .setAggregationName(method
                                .getAggregationName(aggregationId));
                lag.addPhysicalPort(port);
            }
            if (device.getSlots().length == 0) {
                continue;
            }

            Slot slot = device.getSlots()[entries[i].getSlotId()];
            assert slot != null : "no slot found.";
            Module module = slot.getModule();
            assert module != null : "no module found";
            module.addPort(port);
        }
    }

    protected void collectEthernetAggregators() throws IOException,
            AbortedException {
        EthernetPortsAggregator[] aggregators = device
                .getEthernetPortsAggregators();
        for (EthernetPortsAggregator aggregator : aggregators) {
            ifmib.setIfAdminStatus(aggregator);
            ifmib.setIfOperStatus(aggregator);
            ifmib.setIfDescription(aggregator);
            ifmib.setIfName(aggregator);
        }
    }

    protected LogicalEthernetPort getPortByPortId(String portid) {
        if (portid == null) {
            throw new IllegalArgumentException();
        }
        if (portid.startsWith("la")) {
            EthernetPortsAggregator[] lags = device
                    .getEthernetPortsAggregators();
            for (EthernetPortsAggregator lag : lags) {
                if (isMatchPortId(portid, lag.getAggregationName())) {
                    return lag;
                }
            }
        } else if (portid.startsWith("port")) {
            EthernetPort[] ports = device.getEthernetPorts();
            for (EthernetPort port : ports) {
                if (isMatchPortId(portid, port.getIfName())) {
                    return device.getLogicalEthernetPort(port);
                }
            }
        }
        return null;
    }

    private boolean isMatchPortId(String required, String port) {
        String[] base = required.split(" ");
        if (base.length != 2) {
            throw new IllegalStateException("illegal portid: " + required);
        }
        String[] target = port.split(" ");
        if (target.length != 2) {
            throw new IllegalStateException("illegal portid: " + port);
        }
        return base[1].equals(target[1]);
    }

    private boolean isPort(int ifIndex) {
        return profile.isIfIndexPortIfIndex(ifIndex);
    }

    private boolean isVlanIf(int ifIndex) {
        return profile.isIfIndexVlanIfIndex(ifIndex);
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }

        AlaxalaVlanMibImpl vlanMib = new AlaxalaVlanMibImpl(this.getDeviceAccess().getSnmpAccess(), this.profile);
        vlanMib.prepare();
        vlanMib.createVlanIf(device);

        collectQos();

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    protected void collectQos() throws IOException, AbortedException {
        AlaxalaQosFlowListEntry[] entries = method.getQosFlowEntries();
        assert entries != null;

        for (AlaxalaQosFlowListEntry entry : entries) {
            int ifindex = entry.getQosFlowListTargetIfIndex();
            Port port = device.getPortByIfIndex(ifindex);
            assert port != null : "port is null. ifIndex = " + ifindex;
            AlaxalaQosFlowProfile profile = null;

            if (isPort(ifindex)) {
                profile = device.getQosFlowProfileByName(entry);
                profile.addPort(port);

            } else if (isVlanIf(ifindex)) {
                assert port instanceof VlanIf : "not vlanif. type = "
                        + port.getClass().getName() + ", ifIndex = " + ifindex;
                VlanIf vlan = (VlanIf) port;
                profile = device.getQosFlowProfileByName(entry);
                profile.addVlanIf(vlan);
            } else {
                throw new IllegalStateException("unknown ifIndex type: "
                        + ifindex);
            }
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public abstract void getDynamicStatus() throws IOException,
            AbortedException;

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = this.getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(show_running_config);
            this.textConfiguration = res1;
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            return res1;
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            this.getTextConfiguration();
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        log.info("record(): start " + this.getDeviceAccess().getTargetAddress().getHostAddress());
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress()
                .getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            console.close();
        }

        entry.close();
        log.info("record(): end " + this.getDeviceAccess().getTargetAddress().getHostAddress());
    }
}