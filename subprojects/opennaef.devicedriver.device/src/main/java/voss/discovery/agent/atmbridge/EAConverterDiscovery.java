package voss.discovery.agent.atmbridge;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.AgentConfiguration;
import voss.discovery.agent.common.DeviceDiscoveryImpl;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.agent.common.DiscoveryStatus;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.*;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue.Admin;
import voss.model.value.PortSpeedValue.Oper;

import java.io.IOException;
import java.util.Date;

public abstract class EAConverterDiscovery extends DeviceDiscoveryImpl {
    private final static Logger log = LoggerFactory.getLogger(EAConverterDiscovery.class);

    protected final SnmpAccess snmp;
    protected final ConsoleAccess telnet;
    protected final EAConverterTelnetUtil telnetUtil;
    protected final AgentConfiguration config = AgentConfiguration
            .getInstance();
    protected EAConverter device;

    public EAConverterDiscovery(DeviceAccess access,
                                EAConverterTelnetUtil telnetUtil) {
        super(access);
        if (access.getConsoleAccess() == null) {
            throw new IllegalArgumentException("no console access: "
                    + access.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress());
        }
        this.snmp = access.getSnmpAccess();
        this.telnet = access.getConsoleAccess();
        this.telnetUtil = telnetUtil;
        this.device = new EAConverter();
        this.device.setCommunityRO(snmp.getCommunityString());
        this.device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
    }

    @Override
    public Device getDeviceInner() {
        try {
            telnetUtil.supplementInterfaceAttributes(snmp, device);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            disconnect();
            log.info("disconnected from " + getDeviceAccess().getTargetAddress().getHostAddress());
        } catch (Exception e) {
            log.warn("exception occurred on disconnect.", e);
        }
        return device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        try {
            connect();

            device.setDeviceName(Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess()));
            device.setModelTypeName(telnetUtil.getModelName(telnet));
            device.setOsTypeName(telnetUtil.getOSTypeName(telnet));
            device.setOsVersion(telnetUtil.getOSVersion(telnet));
            device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
            device.setBasePhysicalAddress(telnetUtil
                    .getBasePhysicalAddress(telnet));
            device.setGatewayAddress(telnetUtil.getGatewayAddress(telnet));
            device.setTrapReceiverAddresses(telnetUtil
                    .getTrapReceiverAddresses(telnet));
            device.setSyslogServerAddresses(telnetUtil
                    .getSyslogServerAddresses(telnet));
            device.setDescription(Mib2Impl.getSysDescr(getSnmpAccess()));
            device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(snmp)));

            setVendorSpecificAttributes(device);
            setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        try {
            if (device == null) {
                throw new IllegalArgumentException();
            }
            if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
                getDeviceInformation();
            }
            slots();
            ethernetPorts();
            atmPhysicalPorts();
            setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        atmLogicalPorts();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    abstract protected void setVendorSpecificAttributes(Device device) throws IOException, ConsoleException,
            AbortedException;

    abstract protected void slots() throws IOException, ConsoleException,
            AbortedException;

    private void ethernetPorts() throws IOException, AbortedException {
        chassisEthernetPorts();
        for (int i = 0; i < device.getSlots().length; i++) {
            if (!device.getSlots()[i].hasModule()) {
                continue;
            }
            moduleEthernetPorts(device.getSlots()[i].getSlotId(), device
                    .getSlots()[i].getModule());
        }
    }

    private void chassisEthernetPorts() throws IOException, AbortedException {
        try {
            int[] portIndex = telnetUtil.getChassisEthernetPortIndexs(telnet);
            for (int i = 0; i < portIndex.length; i++) {
                EthernetPort portInfo = new EthernetPortImpl();
                portInfo.initDevice(device);
                portInfo.initPortIndex(portIndex[i]);
                portInfo.initIfName(Integer.toString(portIndex[i]));
                addEthernetPortInfo(portInfo);
                device.addPort(portInfo);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void moduleEthernetPorts(final String slotName, final Module module)
            throws IOException, AbortedException {
        try {
            int[] portIndex = telnetUtil.getModuleEthernetPortIndexs(telnet,
                    slotName);
            for (int i = 0; i < portIndex.length; i++) {
                EthernetPort portInfo = new EthernetPortImpl();
                portInfo.initDevice(device);
                portInfo.initModule(module);
                portInfo.initPortIndex(portIndex[i]);
                portInfo.initIfName(slotName + "/"
                        + Integer.toString(portIndex[i]));

                addEthernetPortInfo(portInfo);
                module.addPort(portInfo);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void addEthernetPortInfo(EthernetPort ethernetPortInfo)
            throws IOException, AbortedException {
        try {
            if (ethernetPortInfo.getIfName() == null) {
                throw new RuntimeException("port.ifName == null");
            }
            ethernetPortInfo.setPortTypeName(telnetUtil.getPhysicalPortType(
                    telnet, ethernetPortInfo.getIfName()));
            Duplex duplex = telnetUtil.getEthernetPortDuplex(telnet,
                    ethernetPortInfo.getIfName());
            if (duplex != null) {
                ethernetPortInfo.setDuplex(duplex);
            }
            String portName = telnetUtil.getPhysicalPortName(telnet,
                    ethernetPortInfo.getIfName());
            if (portName != null) {
                ethernetPortInfo.setPortName(portName);
            }

            Admin adminSpeed = telnetUtil.getPortAdminSpeed(telnet,
                    ethernetPortInfo.getIfName());
            if (adminSpeed != null) {
                ethernetPortInfo.setPortAdministrativeSpeed(adminSpeed);
            }
            String adminStatus = telnetUtil.getPortAdminStatus(telnet,
                    ethernetPortInfo.getIfName());
            if (adminStatus != null) {
                ethernetPortInfo.setAdminStatus(adminStatus);
            }
            String descr = telnetUtil.getPortIfDescr(telnet, ethernetPortInfo
                    .getIfName());
            if (descr != null) {
                ethernetPortInfo.setIfDescr(descr);
            }
            String operStatus = telnetUtil.getPortOperationalStatus(telnet,
                    ethernetPortInfo.getIfName());
            if (operStatus != null) {
                ethernetPortInfo.setOperationalStatus(operStatus);
            }
            Oper operSpeed = telnetUtil.getPortSpeed(telnet, ethernetPortInfo
                    .getIfName());
            if (operSpeed != null) {
                ethernetPortInfo.setPortOperationalSpeed(operSpeed);
            }
            String status = telnetUtil.getPortStatus(telnet, ethernetPortInfo
                    .getIfName());
            if (status != null) {
                ethernetPortInfo.setStatus(status);
            }
            addLogicalEthernet(ethernetPortInfo);
            vlanIf(ethernetPortInfo);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void addLogicalEthernet(EthernetPort ethernetPort)
            throws IOException, AbortedException {
        try {
            if (!telnetUtil.hasAggregationNameAndID(telnet, ethernetPort.getIfName())) {
                DefaultLogicalEthernetPort logicalEthernet = new DefaultLogicalEthernetPortImpl();
                device.addPort(logicalEthernet);
                logicalEthernet.initPhysicalPort(ethernetPort);
                logicalEthernet.initIfName("[logical]" + ethernetPort.getIfName());
                return;
            }
            int lagID = telnetUtil.getAggregationID(telnet, ethernetPort.getIfName());
            EthernetPortsAggregator lag = DeviceInfoUtil
                    .getOrCreateLogicalEthernetPortByAggregationId(device, lagID);
            String lagName = telnetUtil.getAggregationName(telnet, ethernetPort.getIfName());
            lag.setAggregationName(lagName);
            lag.initIfName(lagName);
            lag.addPhysicalPort(ethernetPort);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void atmPhysicalPorts() throws IOException, AbortedException {
        for (int i = 0; i < device.getSlots().length; i++) {
            if (device.getSlots()[i].getModule() == null) {
                continue;
            }
            moduleAtmPhysicalPorts(device.getSlots()[i].getSlotId(), device
                    .getSlots()[i].getModule());
        }
    }

    private void moduleAtmPhysicalPorts(final String slotName,
                                        final Module module) throws IOException, AbortedException {
        try {
            int[] portIndexes = telnetUtil.getAtmPhysicalPortIndexs(telnet,
                    slotName);
            for (int i = 0; i < portIndexes.length; i++) {
                int portIndex = i + 1;
                AtmPhysicalPort port = new AtmPhysicalPort(device, slotName
                        + "/" + portIndex, "ATM");
                port.initPortIndex(portIndex);
                addAtmPhysicalInfo(port);
                module.addPort(port);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void addAtmPhysicalInfo(AtmPhysicalPort atmPhysicalPortInfo)
            throws IOException, AbortedException {
        try {
            if (atmPhysicalPortInfo.getIfName() == null) {
                throw new RuntimeException("port.ifName == null");
            }
            atmPhysicalPortInfo.setPortTypeName(telnetUtil
                    .getAtmPhysicalPortType(telnet, atmPhysicalPortInfo
                            .getIfName()));
            atmPhysicalPortInfo.setPortName(telnetUtil.getAtmPhysicalPortName(
                    telnet, atmPhysicalPortInfo.getIfName()));
            atmPhysicalPortInfo.setAdminStatus(telnetUtil
                    .getAtmPhysicalPortAdminStatus(telnet, atmPhysicalPortInfo
                            .getIfName()));
            atmPhysicalPortInfo.setIfDescr(telnetUtil
                    .getAtmPhysicalPortIfDescr(telnet, atmPhysicalPortInfo
                            .getIfName()));
            atmPhysicalPortInfo.setOperationalStatus(telnetUtil
                    .getAtmPhysicalPortOperationalStatus(telnet,
                            atmPhysicalPortInfo.getIfName()));
            atmPhysicalPortInfo.setStatus(telnetUtil.getAtmPhysicalPortStatus(
                    telnet, atmPhysicalPortInfo.getIfName()));
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void atmLogicalPorts() throws IOException, AbortedException {
        try {
            for (int i = 0; i < device.getAtmPhysicalPorts().length; i++) {
                String[] pvc = telnetUtil.getAtmPVCs(telnet, device.getAtmPhysicalPorts()[i].getIfName());
                atmLogicalPorts(pvc, device.getAtmPhysicalPorts()[i]);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private AtmVp getOrCreateAtmVp(AtmPhysicalPort physical, int vpi) {
        for (AtmVp vp : device.getVps()) {
            if (vp.getPhysicalPort() == physical && vp.getVpi() == vpi) {
                return vp;
            }
        }
        AtmVp vp = new AtmVp(physical, physical.getIfName() + "/" + vpi, vpi);
        vp.initDevice(device);
        return vp;
    }

    private void atmLogicalPorts(String[] pvc, AtmPhysicalPort physical)
            throws IOException, AbortedException {
        try {
            for (int i = 0; i < pvc.length; i++) {
                int vpi = telnetUtil.getVpi(pvc[i]);
                AtmVp vp = getOrCreateAtmVp(physical, vpi);
                int vci = telnetUtil.getVci(pvc[i]);
                AtmPvc logical = new AtmPvc(vp, physical.getIfName() + "/" + pvc[i], vci);
                logical.initDevice(device);
                logical.setAtmQos(telnetUtil.getQos(telnet, pvc[i], physical.getIfName()));
                logical.setPcr(telnetUtil.getPcr(telnet, pvc[i], physical.getIfName()));
                logical.setMcr(telnetUtil.getMcr(telnet, pvc[i], physical.getIfName()));
                logical.setOperationalStatus(telnetUtil.getAtmPVCOperStatus(telnet, pvc[i], physical.getIfName()));
                logical.setAdminStatus(telnetUtil.getAtmPVCAdminStatus(telnet, pvc[i], physical.getIfName()));

                atmVlanBridge(pvc[i], logical);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void atmVlanBridge(String pvc, AtmPvc logical) throws IOException,
            AbortedException {
        try {
            int bridgePortNumber = telnetUtil.getBridgePortNumber(telnet, pvc,
                    logical.getVp().getPhysicalPort().getIfName());
            if (bridgePortNumber < 1) {
                return;
            }
            String bridgeName = telnetUtil.getBridgePortName(telnet,
                    bridgePortNumber);

            AtmVlanBridge bridge = new AtmVlanBridge(logical, bridgeName);
            bridge.initDevice(device);
            bridge.setBridgePortNumber(bridgePortNumber);
            bridge.setOperationalStatus(telnetUtil.getBridgeOperStatus(telnet, bridgePortNumber));
            bridge.setAdminStatus(telnetUtil.getBridgeAdminStatus(telnet, bridgePortNumber));
            vlanIf(bridge);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void vlanIf(AtmVlanBridge bridgeInfo) throws IOException,
            AbortedException {
        try {
            VlanIf[] vlanIfs = supplementVlanIfInfo(telnetUtil.getVlanIfName(
                    telnet, bridgeInfo.getBridgePortNumber()));
            if (vlanIfs == null) {
                return;
            }
            for (int i = 0; i < vlanIfs.length; i++) {
                vlanIf(bridgeInfo, vlanIfs[i]);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void vlanIf(EthernetPort ethernetPort) throws IOException,
            AbortedException {
        try {
            if (ethernetPort.getIfName() == null) {
                return;
            }
            int bridgePortNumber = telnetUtil.getBridgePortNumber(telnet,
                    ethernetPort.getIfName());
            VlanIf[] vlanIfs = supplementVlanIfInfo(telnetUtil.getVlanIfName(
                    telnet, bridgePortNumber));
            if (vlanIfs == null) {
                return;
            }
            for (int i = 0; i < vlanIfs.length; i++) {
                vlanIf(ethernetPort, bridgePortNumber, vlanIfs[i]);
            }
            LogicalEthernetPort logicalEthernetPort = device
                    .getLogicalEthernetPort(ethernetPort);
            if (logicalEthernetPort != null
                    && (logicalEthernetPort instanceof EthernetPortsAggregator)) {
                EthernetPortsAggregator lag = (EthernetPortsAggregator) logicalEthernetPort;
                vlanIfOfLag(ethernetPort, lag.getAggregationGroupId());
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void vlanIfOfLag(EthernetPort ethernetPort,
                             Integer bridgePortNumberOfLag) throws IOException, AbortedException {
        try {
            if (bridgePortNumberOfLag == null) {
                return;
            }
            VlanIf[] vlanIfs = supplementVlanIfInfo(telnetUtil.getVlanIfName(
                    telnet, bridgePortNumberOfLag.intValue()));
            if (vlanIfs == null) {
                return;
            }
            for (int i = 0; i < vlanIfs.length; i++) {
                vlanIf(ethernetPort, bridgePortNumberOfLag.intValue(),
                        vlanIfs[i]);
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void vlanIf(AtmVlanBridge bridgeInfo, VlanIf vlanIf)
            throws IOException, AbortedException {
        try {
            if (vlanIf == null) {
                return;
            }
            bridgeInfo.setUntaggedVlanIf(vlanIf);
            String[] ifNames = telnetUtil.getEthernetIfNames(telnet, vlanIf
                    .getIfName());
            for (int i = 0; i < ifNames.length; i++) {
                EthernetPort ethernetPort = DeviceInfoUtil
                        .getEthernetPortByIfName(device, ifNames[i]);
                if (telnetUtil.isTaggedVlan(telnet, vlanIf.getIfName(),
                        bridgeInfo.getBridgePortNumber())) {
                    DeviceInfoUtil.addTaggedPort(vlanIf, ethernetPort);
                } else {
                    DeviceInfoUtil.addUntaggedPort(vlanIf, ethernetPort);
                }
            }
            device.addPort(vlanIf);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private void vlanIf(EthernetPort ethernetPort, int bridgePortNumber,
                        VlanIf vlanIf) throws IOException, AbortedException {
        try {
            if (vlanIf == null || ethernetPort == null) {
                return;
            }
            if (telnetUtil.isTaggedVlan(telnet, vlanIf.getIfName(),
                    bridgePortNumber)) {
                DeviceInfoUtil.addTaggedPort(vlanIf, ethernetPort);
            } else {
                DeviceInfoUtil.addUntaggedPort(vlanIf, ethernetPort);
            }
            device.addPort(vlanIf);
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private VlanIf[] supplementVlanIfInfo(String[] ifNames) throws IOException,
            AbortedException {
        if (ifNames == null) {
            return new VlanIf[0];
        }
        VlanIf[] result = new VlanIf[ifNames.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = supplementVlanIfInfo(ifNames[i]);
        }
        return result;
    }

    private VlanIf supplementVlanIfInfo(String ifName) throws IOException,
            AbortedException {
        try {
            if (ifName == null) {
                return null;
            }
            if (DeviceInfoUtil.getVlanIfByIfName(device, ifName) != null) {
                return DeviceInfoUtil.getVlanIfByIfName(device, ifName);
            } else {
                VlanIf result = new VlanIfImpl();
                result.initDevice(device);
                result.initIfName(ifName);
                result.initVlanId(telnetUtil.getVlanEncapsIfTag(telnet, ifName));
                result.setVlanName(telnetUtil.getVlanIfDescr(telnet, ifName));
                return result;
            }
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }
}