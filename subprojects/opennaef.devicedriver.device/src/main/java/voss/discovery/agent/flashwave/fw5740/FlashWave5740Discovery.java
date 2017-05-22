package voss.discovery.agent.flashwave.fw5740;


import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.flashwave.FlashWaveExtInfoNames;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.agent.util.Utils;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlashWave5740Discovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(FlashWave5740Discovery.class);

    private final FlashWave5740CollectMethods method;
    private final GenericEthernetSwitch device;

    private ConsoleCommand show_running_config = new ConsoleCommand(new NullMode(), "show running-config");

    private final InterfaceMibImpl interfaceMib;
    private final Map<Integer, Integer> portTypeMap = new HashMap<Integer, Integer>();

    public FlashWave5740Discovery(DeviceAccess access) {
        super(access);
        this.method = new FlashWave5740CollectMethods(this.getDeviceAccess().getSnmpAccess());
        this.device = new GenericEthernetSwitch();

        this.interfaceMib = new InterfaceMibImpl(getSnmpAccess());
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getDeviceInformation() throws AbortedException, IOException {
        device.setDescription(Mib2Impl.getSysDescr(getSnmpAccess()));
        device.setIpAddress(method.getManagementIpAddress());
        device.setDeviceName(method.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setOsTypeName(OSType.FRASHWAVE.caption);
        device.setVendorName(Constants.VENDOR_FUJITSU);
        device.setModelTypeName("FLASHWAVE 5740");
        device.setOsVersion(method.getOsVersion());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getSnmpAccess()).longValue()));

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws AbortedException, IOException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        getSlotAndModule();
        getPortInfo();
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void getSlotAndModule() throws AbortedException, IOException {
        method.getSlotAndModules(getSnmpAccess(), device);
        method.getModuleSerialNumber(getSnmpAccess(), device);
    }

    private void getPortInfo() throws AbortedException, IOException {
        List<Integer> ifindices = method.getPorts();
        for (Integer ifindex : ifindices) {
            EthernetPort port = new EthernetPortImpl();
            device.addPort(port);

            port.initIfIndex(ifindex);
            port.initIfName(method.getConfigName(device, ifindex));
            port.initPortIndex(getPortIndex(port.getIfName()));
            port.setConfigName(method.getConfigName(device, ifindex));

            Slot slot = device.getSlotBySlotIndex(method.getSlotId(ifindex));
            Module module = slot.getModule();
            if (module != null) {
                module.addPort(port);
            }

            try {
                interfaceMib.setIfName(port);
            } catch (IOException e) {
                log.warn("no ifName on [" + port.getIfIndex() + ":" + port.getIfName() + "]", e);
                port.initIfName(getWorkaroundIfName(module, port));
            }
            try {
                interfaceMib.setIfOperStatus(port);
                interfaceMib.setIfAdminStatus(port);
                interfaceMib.setIfType(port);
                interfaceMib.setIfAlias(port);
                interfaceMib.setIfDescription(port);
            } catch (IOException e) {
                log.warn("no port attribute on [" + port.getIfIndex() + ":" + port.getIfName() + "]", e);
            }

            int lagIfIndex = method.getLagIfIndexOf(ifindex);
            if (lagIfIndex > 0) {
                int lagId = lagIfIndex - FlashWave5740CollectMethods.LAG_BASE_ID;
                EthernetPortsAggregator aggregator =
                        device.getEthernetPortsAggregatorByAggregationGroupId(lagId);
                if (aggregator == null) {
                    aggregator = new EthernetPortsAggregatorImpl();
                    aggregator.initDevice(device);
                    aggregator.initAggregationGroupId(lagId);
                    aggregator.initIfIndex(lagIfIndex);
                    String lagName = method.getLinkAggregationName(lagIfIndex);
                    aggregator.initIfName(lagName);
                    aggregator.setAggregationName(lagName);
                    interfaceMib.setIfOperStatus(aggregator);
                    interfaceMib.setIfAdminStatus(aggregator);
                    interfaceMib.setIfType(aggregator);
                    interfaceMib.setIfDescription(aggregator);
                }
                aggregator.addPhysicalPort(port);
            }

            int ffpIndex = -1;
            if (ffpIndex > 0) {
                int ffpId = ffpIndex - 40000000;
                EthernetProtectionPortImpl ffp = (EthernetProtectionPortImpl) device.getPortByIfIndex(ffpIndex);
                if (ffp == null) {
                    ffp = new EthernetProtectionPortImpl();
                    ffp.initDevice(device);
                    ffp.initIfIndex(ffpIndex);
                    ffp.initIfName("eps " + ffpId);

                    interfaceMib.setIfOperStatus(ffp);
                    interfaceMib.setIfAdminStatus(ffp);
                    interfaceMib.setIfType(ffp);
                    interfaceMib.setIfDescription(ffp);
                }

                ffp.addPhysicalPort(port);

                if (method.getFfpWorkingIfIndex(ffpIndex) == ifindex) {
                    ffp.setWorkingPort(port);
                }
            }
        }

        if (device.getPortByIfIndex(2) == null) {
            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfIndex(2);
            port.initIfName("eth0");
            port.initPortIndex(0);
        }

        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        Map<Integer, Integer> ifIndexToPortType = method.getPortTypes();
        for (EthernetPort port : device.getEthernetPorts()) {
            FwEtherPortType portType = null;
            int ifIndex = port.getIfIndex();
            LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
            Integer portTypeValue = ifIndexToPortType.get(ifIndex);
            if (portTypeValue == null) {
                log.debug("no port type.");
                portType = FwEtherPortType.notSet;
            } else {
                portType = FwEtherPortType.get(portTypeValue.intValue());
            }
            try {
                String value;
                switch (portType) {
                    case notSet:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_notSet;
                        break;
                    case portVLAN:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_portVLAN;
                        break;
                    case cportVLAN:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_cportVLAN;
                        break;
                    case vportVLAN:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_vportVLAN;
                        break;
                    case tagVLAN:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_tagVLAN;
                        break;
                    case itagVLAN:
                        value = FlashWaveExtInfoNames.FW5740_CONFIG_ETH_PORT_MODE_itagVLAN;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }

                String prevValue = (String) logical.gainConfigurationExtInfo().get(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE);
                if (prevValue != null && !prevValue.equals(value)) {
                    throw new IllegalStateException();
                }
                if (prevValue == null) {
                    logical.gainConfigurationExtInfo().put(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE, value);
                    log.debug(logical.getIfName() + " ETH_PORT_MODE=" + value);
                }
            } catch (IllegalArgumentException e) {
                log.debug(port.getIfName() + " does not have ETH_PORT_MODE");
            }
        }
    }

    private FwEtherPortType getFwEtherPortType(int ifIndex) throws IOException, AbortedException {
        if (this.portTypeMap.size() == 0) {
            this.portTypeMap.putAll(method.getPortTypes());
        }
        Integer value = this.portTypeMap.get(Integer.valueOf(ifIndex));
        if (value == null) {
            throw new IllegalStateException("no port-type: ifIndex=" + ifIndex);
        }
        return FwEtherPortType.get(value.intValue());
    }

    private int getPortIndex(String ifname) {
        String[] elem = ifname.split("/");
        if (elem.length > 1) {
            return Integer.parseInt(elem[elem.length - 1]);
        }
        Pattern pattern = Pattern.compile(".*([0-9]+).*");
        Matcher matcher = pattern.matcher(ifname);
        if (matcher.matches()) {
            String matched = matcher.group(1);
            return Integer.parseInt(matched);
        }
        return -1;
    }

    private String getWorkaroundIfName(Module module, Port port) {
        if (port == null) {
            return null;
        } else if (module == null) {
            return port.getConfigName();
        }
        String modulePrefix = getModulePrefix(module);
        String configName = port.getConfigName();
        if (modulePrefix == null) {
            return port.getConfigName();
        }
        if (configName == null) {
            return null;
        }
        if (configName.startsWith("fastethernet")) {
            return configName.replace("fastethernet", modulePrefix);
        } else if (configName.startsWith("gigabitethernet")) {
            return configName.replace("gigabitethernet", modulePrefix);
        }
        return configName;
    }

    private String getModulePrefix(Module module) {
        if (module == null) {
            return null;
        }
        String moduleName = module.getModelTypeName();
        if (moduleName.equals("FCF554FE3")) {
            return "FET-32";
        } else if (moduleName.equals("FCF554GE3")) {
            return "GEX-8";
        }
        return null;
    }

    @Override
    public void getLogicalConfiguration() throws AbortedException, IOException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        getVlans();
        getVports();

        if (device.getPortByIfIndex(1) == null) {
            LoopbackInterface lo = new LoopbackInterface();
            lo.initDevice(device);
            lo.initIfIndex(1);
            lo.initIfName("lo0");
        }

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    static final String fwDomainJoinPorts = FlashWave5740CollectMethods.fwDomainJoinPorts;

    private void getVlans() throws AbortedException, IOException {
        Map<Integer, String> vlanNames = method.getVlanIfs();
        for (Map.Entry<Integer, String> entry : vlanNames.entrySet()) {
            int vlanId = entry.getKey().intValue();
            VlanIf vlan = device.getVlanIfByVlanId(vlanId);
            if (vlan == null) {
                vlan = new VlanIfImpl();
                device.addPort(vlan);
                vlan.initVlanId(vlanId);
                vlan.initIfName("vlan" + String.format("%04d", vlanId));
                vlan.setVlanName(entry.getValue());
                log.debug("create vlan-if; vlan-id=" + vlanId + " ifName=" + vlan.getIfName());
            }

            byte[] joinPorts;
            try {
                joinPorts = SnmpUtil.getByte(getSnmpAccess(), fwDomainJoinPorts + "." + vlanId);
            } catch (SnmpResponseException e) {
                throw new IOException(e);
            } catch (NoSuchMibException e) {
                throw new IOException(e);
            }

            int[] portList = SnmpUtil.decodeBitList(joinPorts);
            log.debug("portNumbers are [" + Arrays.toString(portList) + "]");
            for (int portNumber : portList) {
                int ifIndex = 0;
                String ifName = "";
                LogicalEthernetPort port = null;
                int slotId = portNumber / 64 + 1;
                int portId = portNumber % 64;
                log.debug("fwDomainJoinPorts.portNumber=" + portNumber + " (slot/port=" + slotId + "/" + portId + ")");
                if (portNumber <= 512) {
                    ifIndex = method.getIfIndex(slotId, portId);
                    ifName = interfaceMib.getIfName(ifIndex);
                    port = device.getLogicalEthernetPort((EthernetPort) device.getPortByIfName(ifName));
                } else {
                    if (slotId == 13) {
                        ifIndex = 40000000 + portId;
                        ifName = "eps " + portId;
                        port = (LogicalEthernetPort) device.getPortByIfName(ifName);
                    } else if (slotId == 9) {
                        ifIndex = FlashWave5740CollectMethods.LAG_BASE_ID + portId;
                        ifName = method.getLinkAggregationName(ifIndex);
                        port = (LogicalEthernetPort) device.getPortByIfName(ifName);
                    }
                }

                int physicalIfIndex = port.getPhysicalPorts()[0].getIfIndex();
                FwEtherPortType portType = getFwEtherPortType(physicalIfIndex);
                log.debug("bind " + port.getIfName() + " to vlan " + vlan.getVlanId() + " by " + portType);
                switch (portType) {
                    case portVLAN:
                    case cportVLAN:
                        vlan.addUntaggedPort(port);
                        break;
                    case tagVLAN:
                    case itagVLAN:
                        vlan.addTaggedPort(port);
                        break;
                    case vportVLAN:
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }
    }

    private void getVports() throws AbortedException, IOException {
        Map<Integer, List<Integer>> vports = method.getVports();
        for (Map.Entry<Integer, List<Integer>> entry : vports.entrySet()) {
            int ifindex = entry.getKey().intValue();
            Port port = device.getPortByIfIndex(ifindex);
            LogicalEthernetPort logicalEth;
            if (port instanceof LogicalEthernetPort) {
                logicalEth = (LogicalEthernetPort) port;
            } else if (port instanceof EthernetPort) {
                logicalEth = ((VlanDevice) port.getDevice()).getLogicalEthernetPort((EthernetPort) port);
            } else {
                log.debug("vport parent [" + port.getFullyQualifiedName() + "] is not ethernet port.");
                continue;
            }
            List<Integer> vportsOnIf = entry.getValue();
            addVportsToLogicalEthernetPort(logicalEth, vportsOnIf);
        }
    }

    private void addVportsToLogicalEthernetPort(LogicalEthernetPort logicalEth,
                                                List<Integer> vports) throws AbortedException, IOException {
        int ifindex = Utils.getIfIndex(logicalEth);
        for (Integer vportId : vports) {
            createVport(logicalEth, ifindex, vportId);
        }
    }

    private void createVport(LogicalEthernetPort logicalEth, int ifindex, Integer vportId)
            throws AbortedException, IOException {
        String ifName;
        if (logicalEth instanceof EthernetPortsAggregatorImpl || logicalEth instanceof EthernetProtectionPortImpl) {
            ifName = "vport:" + logicalEth.getIfName() + ":" + vportId;
        } else {
            ifName = "vport:" + logicalEth.getPhysicalPorts()[0].getIfName() + ":" + vportId;
        }
        log.debug("create vport " + ifName + " on [" + logicalEth.getFullyQualifiedName() + "] vport-id=" + vportId + " ifIndex=" + ifindex);

        TagChanger tagChanger =
                new AbstractLogicalEthernetPort.TagChangerImpl();
        tagChanger.initDevice(logicalEth.getDevice());
        tagChanger.initLogicalEthernetPort(logicalEth);
        tagChanger.initIfName(ifName);
        tagChanger.setTagChangerId(vportId);
        tagChanger.setOuterVlanId(method.getOuterVlanId(ifindex, vportId));
        tagChanger.setInnerVlanId(method.getInnerVlanId(ifindex, vportId));
        tagChanger.setIfDescr(method.getVportDescription(ifindex, vportId));

        VlanIf innerVlanIf = device.getVlanIfByVlanId(tagChanger.getInnerVlanId());
        if (innerVlanIf != null) {
            innerVlanIf.addTaggedPort(tagChanger);
        } else {
            log.warn("no vlanIf: innerVlanId = [" + tagChanger.getInnerVlanId() + "]");
        }
        createSecondaryTagTranslationMap(ifindex, vportId.intValue(), tagChanger);
    }

    private void createSecondaryTagTranslationMap(int ifIndex, int vportId, TagChanger tc)
            throws IOException, AbortedException {
        Map<Integer, Integer> translationMap = this.method.getVportSecondaryTagTranslationMap(ifIndex, vportId, tc);
        for (Map.Entry<Integer, Integer> entry : translationMap.entrySet()) {
            Integer _domain = entry.getKey();
            Integer _secondary = entry.getValue();
            tc.addSecondaryMap(_domain, _secondary);
            VlanIf innerVlanIf = device.getVlanIfByVlanId(_domain);
            if (innerVlanIf != null) {
                innerVlanIf.addTaggedPort(tc);
                log.debug("bind [" + tc.getIfName() + "] to vlan(inner)=" + _domain
                        + ", vlan(outer)=" + tc.getOuterVlanId() + "." + _secondary + " by inner-vport.");
            } else {
                log.warn("no vlanIf: innerVlanId = [" + _domain + "]");
            }
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
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
            console.close();
        }

        entry.close();
    }

    public static enum FwEtherPortType {
        notSet(0),
        portVLAN(1),
        cportVLAN(2),
        vportVLAN(3),
        tagVLAN(4),
        itagVLAN(5),;
        final int value;

        FwEtherPortType(int value) {
            this.value = value;
        }

        static FwEtherPortType get(int value) {
            for (FwEtherPortType type : FwEtherPortType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException();
        }
    }

}