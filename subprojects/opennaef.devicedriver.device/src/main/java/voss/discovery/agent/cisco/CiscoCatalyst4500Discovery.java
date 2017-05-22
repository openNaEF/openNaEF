package voss.discovery.agent.cisco;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.*;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.EntityMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.*;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class CiscoCatalyst4500Discovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(CiscoCatalyst4500Discovery.class);
    private final Map<String, String> productList;
    private final EntityMibAnalyzer entityMib;
    private final AgentConfiguration agentConfig;

    private final ConsoleCommand show_running_config;
    private final ConsoleCommand show_interface;
    private final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public CiscoCatalyst4500Discovery(DeviceAccess access) {
        super(access);
        this.snmp = access.getSnmpAccess();
        this.device = new GenericEthernetSwitch();
        this.agentConfig = AgentConfiguration.getInstance();
        this.productList = this.agentConfig.getDiscoveryParameter(DiscoveryParameterType.CISCO_PRODUCT_LIST);
        this.entityMib = new EntityMibAnalyzer(this.agentConfig.getDiscoveryParameter(DiscoveryParameterType.CISCO_ENTITY_MAP));

        this.show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
        this.show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    }

    protected final SnmpAccess snmp;
    protected final VlanDevice device;

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());
        device.setCommunityRO(snmp.getCommunityString());
        device.setDeviceName(Mib2Impl.getSysName(snmp));
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setDescription(Mib2Impl.getSysDescr(snmp));
        device.setContactInfo(Mib2Impl.getSysContact(snmp));
        device.setLocation(Mib2Impl.getSysLocation(snmp));
        device.setIpAddresses(Mib2Impl.getIpAddresses(snmp).toArray(new String[0]));
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());

        String sysObjectId = Mib2Impl.getSysObjectId(snmp);
        device.setModelTypeName(productList.get(sysObjectId));
        osType();
        device.setBasePhysicalAddress(Mib2Impl.getSystemMacAddress(snmp));
        Mib2Impl mib2 = new Mib2Impl(snmp);
        mib2.setDefaultGateway(device);
        mib2.setSnmpTrapReceiverAddress(device);
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(snmp).longValue()));

        try {
            String serial = SnmpUtil.getString(snmp, EntityMib.EntityMibEntPhysicalSerialNumEntry.OID + ".1");
            device.setSerialNumber(serial);
        } catch (IOException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        } catch (NoSuchMibException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        } catch (SnmpResponseException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        }
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    protected void osType() throws IOException, AbortedException {
        CiscoImageMib ciscoImageMib = new CiscoImageMib(snmp);
        String sysDescr = ciscoImageMib.getSysDescription();
        String osType = (sysDescr.indexOf("IOS") == -1 ? OSType.CATOS.caption : OSType.IOS.caption);
        device.setOsTypeName(osType);

        ciscoImageMib.setOsVersion(device);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        InterfaceMibImpl ifmib = new InterfaceMibImpl(snmp);
        ifmib.createPhysicalPorts(device);

        CiscoPagpMib pagpMib = new CiscoPagpMib(this.snmp);
        pagpMib.createAggregationGroup(device);

        ifmib.setAllIfNames(device);
        ifmib.setAllIfDescriptions(device);
        ifmib.setAllIfAliases(device);
        ifmib.setAllIfAdminStatus(device);
        ifmib.setAllIfOperStatus(device);
        ifmib.setAllIfSpeed(device);

        for (EthernetPortsAggregator aggregator : device.getEthernetPortsAggregators()) {
            try {
                int channel = Integer.parseInt(aggregator.getIfName().replace("Po", ""));
                aggregator.initAggregationGroupId(channel);
                aggregator.setAggregationName(aggregator.getIfName());
            } catch (NumberFormatException e) {
                throw new IOException("illegal ifname on aggregator: " + aggregator.getIfName());
            }
        }

        try {
            entityMib.addFilter(EntityType.CHASSIS);
            entityMib.addFilter(EntityType.CONTAINER);
            entityMib.addFilter(EntityType.MODULE);
            entityMib.addFilter(EntityType.PORT);
            entityMib.analyze(snmp);
            Map<Integer, PhysicalEntry> result = entityMib.getEntities();
            PhysicalEntry topmost = result.get(1);
            createPhysicalModel(topmost, null);

        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void createPhysicalModel(PhysicalEntry entry, VlanModel target) {
        VlanModel current = null;
        if (entry.type == EntityType.CHASSIS) {

            current = device;

        } else if (entry.type == EntityType.CONTAINER
                && entry.physicalName.startsWith("Slot")
                && target instanceof Device) {

            Slot slot = new SlotImpl();
            slot.initContainer(device);
            slot.initSlotIndex(entry.position);
            slot.initSlotId(entry.physicalName);
            device.addSlot(slot);
            log.debug("@ create slot '" + slot.getSlotIndex()
                    + "' on '" + device.getDeviceName() + "'");

            current = slot;

        } else if (entry.type == EntityType.MODULE && target instanceof Slot) {
            Module module = new ModuleImpl();
            module.setModelTypeName(entry.name);
            module.initSlot((Slot) target);
            log.debug("@ insert module '" + entry.name
                    + "' slot " + module.getSlot().getSlotIndex()
                    + " on '" + device.getDeviceName() + "'");

            current = module;

        } else if (entry.type == EntityType.PORT && target instanceof Module) {
            Module module = (Module) target;
            String ifname = CiscoIosCommandParseUtil.getShortIfName(entry.physicalName);
            PhysicalPort port = (PhysicalPort) device.getPortByIfName(ifname);
            module.addPort(port);
            current = port;
            log.debug("@ add port '" + ifname + "' ifindex=" + entry.ifindex
                    + " on device '" + device.getDeviceName() + "'");

        } else if (entry.type == EntityType.CONTAINER && target instanceof Module) {
            Module module = (Module) target;
            String portName = CiscoIosCommandParseUtil.guessIfName(entry.physicalName);
            PhysicalPort port = (PhysicalPort) device.getPortByIfName(portName);
            module.addPort(port);
            current = port;
            log.debug("@ add port '" + port.getIfIndex()
                    + " to slot '" + module.getSlot().getSlotIndex()
                    + " on device '" + device.getDeviceName() + "'");

        } else {
            log.debug("ignored: " + entry.toString());
        }
        log.debug(entry.toString());
        for (PhysicalEntry child : entry.getChildren()) {
            createPhysicalModel(child, current);
        }

    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }

        CiscoVtpMibImpl vtpMib = new CiscoVtpMibImpl(this.snmp, this.device);
        vtpMib.createVlanIf();
        vtpMib.createTaggedVlan();

        CiscoVlanMembershipMib vmMib = new CiscoVlanMembershipMib(this.snmp);
        vmMib.createUntaggedVlan(device);

        CiscoStpExtensionsMIB stpxMib = new CiscoStpExtensionsMIB(this.snmp);
        SpanningTreeType type = stpxMib.getSpanningTreeType(device);
        switch (type) {
            case MISTP:
            case MISTP_PVSTplus:
                stpxMib.createMistpInstanceAndVlanMapping(device);
                break;
            case MST:
            case RSTP_PVSTplus:
            case PVSTplus:
                break;
            default:
                throw new IllegalStateException("unknown stp type");
        }

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
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