package voss.discovery.agent.flashwave.fw5540;


import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.flashwave.FlashWaveExtInfoNames;
import voss.discovery.agent.flashwave.fw5540.Flashwave5500FwetherportMibImpl.FwEtherPortType;
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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlashWave5540Discovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(FlashWave5540Discovery.class);

    private final FlashWave5540CollectMethods method;
    private final GenericEthernetSwitch device;

    private ConsoleCommand show_running_config = new ConsoleCommand(new NullMode(), "show running-config");

    private final InterfaceMibImpl interfaceMib;

    public FlashWave5540Discovery(DeviceAccess access) {
        super(access);
        this.method = new FlashWave5540CollectMethods(this.getDeviceAccess().getSnmpAccess());
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
        device.setModelTypeName("FlashWave 5540");
        device.setOsVersion(method.getOsVersion());
        device.setSerialNumber(Flashwave5500FwequipmentMibImpl.getChassisSerialNumber(getSnmpAccess()));
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getSnmpAccess()).longValue()));

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws AbortedException, IOException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        Flashwave5500FwequipmentMibImpl.getSlotAndModule(getSnmpAccess(), device);
        getPortInfo();
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
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

            int lagIfIndex = Flashwave5500FwetherportMibImpl.getLagIndex(getSnmpAccess(), ifindex);
            if (lagIfIndex != 0) {
                int lagId = lagIfIndex - 10000000;
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

            int ffpIndex = Flashwave5500FwetherportMibImpl.getFfpIndex(getSnmpAccess(), ifindex);
            if (ffpIndex != 0) {
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

        Flashwave5500FwetherportMibImpl.setAllPhyPortInfo(getSnmpAccess(), device);

        if (device.getPortByIfIndex(2) == null) {
            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfIndex(2);
            port.initIfName("eth0");
            port.initPortIndex(0);
        }

        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        for (EthernetPort port : device.getEthernetPorts()) {
            int ifIndex = port.getIfIndex();
            LogicalEthernetPort logical = device.getLogicalEthernetPort(port);

            try {
                FwEtherPortType portType = Flashwave5500FwetherportMibImpl.getFwEtherPortType(getSnmpAccess(), ifIndex);

                String value;
                switch (portType) {
                    case notSet:
                        value = FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_notSet;
                        break;
                    case portVWAN:
                        value = FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_portVWAN;
                        break;
                    case tagVWAN:
                        value = FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_tagVWAN;
                        break;
                    case vportVWAN:
                        value = FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_vportVWAN;
                        break;
                    case tagVWANExt:
                        value = FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE_tagVWANExt;
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

    static final String fwVwanJoinPorts = ".1.3.6.1.4.1.211.1.24.7.1.1.2.5.2.2.1.3";

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
                joinPorts = SnmpUtil.getByte(getSnmpAccess(), fwVwanJoinPorts + "." + vlanId);
            } catch (SnmpResponseException e) {
                throw new IOException(e);
            } catch (NoSuchMibException e) {
                throw new IOException(e);
            }

            int[] portList = SnmpUtil.decodeBitList(joinPorts);
            for (int portNumber : portList) {
                int ifIndex = 0;
                String ifName = "";
                LogicalEthernetPort port = null;

                if (portNumber / 192 < 8) {
                    int slotId = portNumber / 192 + 1;
                    int portId = portNumber % 192;

                    ifIndex = method.getIfIndex(slotId, portId);
                    ifName = interfaceMib.getIfName(ifIndex);
                    port = device.getLogicalEthernetPort((EthernetPort) device.getPortByIfName(ifName));
                } else {
                    int slotId = portNumber / 128 + 1;
                    int portId = portNumber % 128;
                    if (slotId == 13) {
                        ifIndex = 40000000 + portId;
                        ifName = "eps " + portId;
                        port = (LogicalEthernetPort) device.getPortByIfName(ifName);
                    } else if (slotId == 14) {
                        ifIndex = 10000000 + portId;
                        ifName = method.getLinkAggregationName(ifIndex);
                        port = (LogicalEthernetPort) device.getPortByIfName(ifName);
                    }
                }

                int physicalIfIndex = port.getPhysicalPorts()[0].getIfIndex();
                FwEtherPortType portType = Flashwave5500FwetherportMibImpl.getFwEtherPortType(getSnmpAccess(), physicalIfIndex);
                log.debug("add " + portType + " " + port.getIfName() + " to vlan " + vlan.getVlanId());
                switch (portType) {
                    case portVWAN:
                        vlan.addUntaggedPort(port);
                        break;
                    case tagVWAN:
                    case tagVWANExt:
                        vlan.addTaggedPort(port);
                        break;
                    case vportVWAN:
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

        LogicalEthernetPort.TagChanger tagChanger =
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
}