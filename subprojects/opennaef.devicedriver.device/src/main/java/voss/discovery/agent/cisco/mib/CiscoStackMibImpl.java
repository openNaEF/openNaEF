package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.CollectUtil;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class CiscoStackMibImpl implements CiscoStackMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoStackMibImpl.class);

    private final SnmpAccess snmp;
    private final Map<CiscoMibUtil.CiscoModulePortKey, Integer> cachedPortCrossIndex;
    private final Map<EthernetPort, CiscoMibUtil.CiscoModulePortKey> portToSlotPortIndex;
    private final Map<CiscoMibUtil.CiscoModulePortKey, EthernetPort> slotPortIndexToPort;
    boolean crossIndexInitialized = false;

    public CiscoStackMibImpl(SnmpAccess snmp, VlanDevice device) throws IOException, AbortedException {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.cachedPortCrossIndex = new HashMap<CiscoMibUtil.CiscoModulePortKey, Integer>();
        this.portToSlotPortIndex = new HashMap<EthernetPort, CiscoMibUtil.CiscoModulePortKey>();
        this.slotPortIndexToPort = new HashMap<CiscoMibUtil.CiscoModulePortKey, EthernetPort>();
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> portCrossIndices =
                SnmpUtil.getWalkResult(snmp, portCrossIndex, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        for (CiscoMibUtil.CiscoModulePortKey key : portCrossIndices.keySet()) {
            int crossIndex = portCrossIndices.get(key).intValue();
            this.cachedPortCrossIndex.put(key, crossIndex);
        }
    }

    public EthernetPort getEthernetPortByCrossIndex(int index) {
        if (!crossIndexInitialized) {
            throw new IllegalStateException();
        }
        for (CiscoMibUtil.CiscoModulePortKey key : cachedPortCrossIndex.keySet()) {
            int crossIndex = cachedPortCrossIndex.get(key);
            if (crossIndex == index) {
                return slotPortIndexToPort.get(key);
            }
        }
        return null;
    }

    public void setModelTypeName(VlanDevice device) throws IOException, AbortedException {
        try {
            String modelTypeName = SnmpUtil.getString(snmp, chassisModel);
            device.setModelTypeName(modelTypeName);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public void setChassisSerial(VlanDevice device) throws IOException, AbortedException {
        try {
            String serial = SnmpUtil.getString(snmp, chassisSerialNumberString);
            device.setSerialNumber(serial);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public void setGatewayAddress(VlanDevice device) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> mdgGatewayTypes = SnmpUtil.getIntSnmpEntries(snmp, mdgGatewayType);
            for (IntSnmpEntry entry : mdgGatewayTypes) {
                int value = entry.intValue();
                if (value == 2) {
                    BigInteger[] gatewayAddr = entry.oidSuffix;
                    if (gatewayAddr.length != 4) {
                        throw new IllegalArgumentException();
                    }
                    String gateway = SnmpUtil.getIpAddressByBigInteger(gatewayAddr);
                    device.setGatewayAddress(gateway);
                    break;
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void setSnmpTrapReceiverAddress(VlanDevice device) throws IOException, AbortedException {
        try {
            List<IpAddressSnmpEntry> sysTrapReceiverAddesses = SnmpUtil.getIpAddressSnmpEntries(snmp, sysTrapReceiverAddr);
            Set<String> trapReceiverAddresses = new HashSet<String>();
            for (IpAddressSnmpEntry entry : sysTrapReceiverAddesses) {
                String ip = entry.getIpAddress();
                trapReceiverAddresses.add(ip);
            }
            device.setTrapReceiverAddresses(trapReceiverAddresses.toArray(new String[0]));
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void setSyslogServerAddress(VlanDevice device) throws IOException, AbortedException {
        try {
            List<IpAddressSnmpEntry> entries = SnmpUtil.getIpAddressSnmpEntries(snmp, syslogServerAddr);
            Set<String> syslogServerAddresses = new HashSet<String>();
            for (IpAddressSnmpEntry entry : entries) {
                String ip = entry.getIpAddress();
                syslogServerAddresses.add(ip);
            }
            device.setSyslogServerAddresses(syslogServerAddresses.toArray(new String[0]));
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void createSlotAndModule(VlanDevice device) throws IOException, AbortedException {
        try {
            int numberOfSlot = SnmpUtil.getInteger(snmp, chassisNumSlots);
            for (int slotIndex = 1; slotIndex <= numberOfSlot; slotIndex++) {
                String slotId = Integer.toString(slotIndex);
                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotIndex(slotIndex);
                slot.initSlotId(slotId);
                device.addSlot(slot);
                log.debug("@ add slot " + slotIndex
                        + " to device '" + device.getDeviceName() + "'");
            }
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        Map<IntegerKey, StringSnmpEntry> moduleModels =
                SnmpUtil.getWalkResult(snmp, moduleModel, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : moduleModels.keySet()) {
            int moduleIndex = key.getInt();
            String moduleModel = moduleModels.get(key).getValue();
            log.debug("moduleIndex=" + moduleIndex + ", moduleModel=" + moduleModel);
            Module module = new ModuleImpl();
            module.setModelTypeName(moduleModel);
            Slot slot = device.getSlotBySlotIndex(moduleIndex);
            if (slot != null) {
                module.initSlot(slot);
                log.debug("@ add module '" + module.getModelTypeName()
                        + "' to slot " + moduleIndex
                        + " on device '" + device.getDeviceName() + "'");
            }
        }

    }

    public void createPhysicalPorts(VlanDevice device) throws IOException, AbortedException {
        createPhysicalPorts(device, false);
    }

    public void createPhysicalPorts(VlanDevice device, boolean ignoreSpeedDuplex) throws IOException, AbortedException {
        Map<CiscoMibUtil.CiscoModulePortKey, StringSnmpEntry> portNames =
                SnmpUtil.getWalkResult(snmp, portName, stringEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> portTypes =
                SnmpUtil.getWalkResult(snmp, portType, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> portIfIndices =
                SnmpUtil.getWalkResult(snmp, portIfIndex, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoMibUtil.CiscoModulePortKey, ByteSnmpEntry> portAdditionalOperStatuses =
                SnmpUtil.getWalkResult(snmp, portAdditionalOperStatus, byteEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> portDuplexes =
                SnmpUtil.getWalkResult(snmp, portDuplex, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoMibUtil.CiscoModulePortKey, ByteSnmpEntry> portAdminSpeeds =
                SnmpUtil.getWalkResult(snmp, portAdminSpeed, byteEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);

        for (CiscoMibUtil.CiscoModulePortKey key : portIfIndices.keySet()) {
            int slotIndex = key.slotIndex;
            int portIndex = key.portIndex;

            int ifindex = portIfIndices.get(key).intValue();
            String portName = portNames.get(key).getValue();

            if (ifindex == 0) {
                StackDedicatedPort stackDeticatedPort = new StackDedicatedPort(slotIndex, portIndex);
                stackDedicatedPortIndexSet.add(stackDeticatedPort);
                continue;
            }

            EthernetPort.Duplex duplex = getDuplex(portDuplexes.get(key).intValue());
            PortSpeedValue.Admin adminSpeed = getAsSpeed(portAdminSpeeds.get(key).getValueAsBigInteger());
            CiscoStackMibPortType portType = CiscoStackMibPortType.valueOf(portTypes.get(key).intValue());

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfIndex(ifindex);
            log.debug("@ create port " + ifindex + " on device [" + device.getDeviceName() + "]");

            port.initPortIndex(portIndex);
            port.setPortName(portName);
            port.setPortTypeName(portType.getValue());
            if (!ignoreSpeedDuplex) {
                port.setDuplex(duplex);
                port.setPortAdministrativeSpeed(adminSpeed);
            }

            if (portAdditionalOperStatuses.get(key) != null) {
                String portStatus = getPortAdditionalOperStatus(portAdditionalOperStatuses.get(key).getValue());
                port.setStatus(portStatus);
            }

            portToSlotPortIndex.put(port, key);
            slotPortIndexToPort.put(key, port);

            if (slotIndex > 0 && device.getSlotBySlotIndex(slotIndex) != null) {
                Module module = device.getSlotBySlotIndex(slotIndex).getModule();
                if (module == null) {
                    throw new IllegalStateException(
                            "port found, but no module found: device= "
                                    + device.getDeviceName() + " ifindex=" + ifindex);
                }
                module.addPort(port);
            } else {
                device.addPort(port);
            }
        }
        crossIndexInitialized = true;
    }

    protected final Set<StackDedicatedPort> stackDedicatedPortIndexSet
            = new HashSet<StackDedicatedPort>();

    class StackDedicatedPort {
        private int moduleIndex;
        private int portIndex;

        StackDedicatedPort(int _module, int _portIndex) {
            this.moduleIndex = _module;
            this.portIndex = _portIndex;
        }

        boolean isMemberOfModule(int _moduleIndex) {
            return this.moduleIndex == _moduleIndex;
        }

        boolean isStackDedicatedPort(int _moduleIndex, int _portIndex) {
            return (this.moduleIndex == _moduleIndex) && (this.portIndex == _portIndex);
        }
    }

    protected boolean isPortStackDedicatedPort(int _moduleIndex, int _portIndex) {
        for (StackDedicatedPort stackPort : stackDedicatedPortIndexSet) {
            if (stackPort.isStackDedicatedPort(_moduleIndex, _portIndex)) {
                return true;
            }
        }
        return false;
    }

    public Integer getCrossIndex(EthernetPort port) {
        CiscoMibUtil.CiscoModulePortKey key = portToSlotPortIndex.get(port);
        return cachedPortCrossIndex.get(key);
    }

    public String getPortAdditionalOperStatus(byte[] bytes) {
        int[] flags =
                SnmpUtil.decodeBitList(bytes);
        List<String> status = new ArrayList<String>();
        for (int flag : flags) {
            switch (flag) {
                case 0:
                    status.add("other");
                    break;
                case 1:
                    status.add("connected");
                    break;
                case 2:
                    status.add("standby");
                    break;
                case 3:
                    status.add("faulty");
                    break;
                case 4:
                    status.add("notConnected");
                    break;
                case 5:
                    status.add("inactive");
                    break;
                case 6:
                    status.add("shutdown");
                    break;
                case 7:
                    status.add("dripDis");
                    break;
                case 8:
                    status.add("disabled");
                    break;
                case 9:
                    status.add("monitor");
                    break;
                case 10:
                    status.add("errdisable");
                    break;
                case 11:
                    status.add("linkFaulty");
                    break;
                case 12:
                    status.add("onHook");
                    break;
                case 13:
                    status.add("offHook");
                    break;
            }
        }
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (String state : status) {
            if (first) {
                first = false;
            } else {
                result.append(", ");
            }
            result.append(state);
        }
        return result.toString();
    }

    public EthernetPort.Duplex getDuplex(int value) {
        switch (value) {
            case 1:
                return EthernetPort.Duplex.HALF;
            case 2:
                return EthernetPort.Duplex.FULL;
            case 3:
                return new EthernetPort.Duplex("disagree");
            case 4:
                return EthernetPort.Duplex.AUTO;
            default:
                return new EthernetPort.Duplex("Unknown(" + value + ")");
        }
    }

    public PortSpeedValue.Admin getAsSpeed(BigInteger speed) {
        return speed.equals(BigInteger.ONE)
                ? PortSpeedValue.Admin.AUTO
                : CollectUtil.getAdminSpeed(speed);
    }

    public void createVlanIf(VlanDevice device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> vlanIfIndices =
                SnmpUtil.getWalkResult(snmp, vlanIfIndex, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : vlanIfIndices.keySet()) {
            int vlanId = key.getInt();
            int ifindex = vlanIfIndices.get(key).intValue();

            VlanIf vlanIf = new VlanIfImpl();
            vlanIf.initDevice(device);
            vlanIf.initVlanId(vlanId);
            log.debug("@ create vlan id " + vlanId + " on " + device.getDeviceName());

            vlanIf.initIfIndex(ifindex);
            log.debug("@ set vlan id " + vlanId + " ifindex " + ifindex + " on " + device.getDeviceName());

            device.addPort(vlanIf);
        }

    }

    public void createUntaggedVlan(VlanDevice device) throws IOException, AbortedException {
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> vlanPortVlans =
                SnmpUtil.getWalkResult(snmp, vlanPortVlan, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);

        for (CiscoMibUtil.CiscoModulePortKey key : vlanPortVlans.keySet()) {
            EthernetPort port = slotPortIndexToPort.get(key);
            if (port == null) {
                throw new IllegalStateException();
            }
            int vlanId = vlanPortVlans.get(key).intValue();
            VlanIf vlanIf = device.getVlanIfByVlanId(vlanId);
            if (vlanIf == null) {
                log.warn("orphaned vlan id is set: vlan-id=" + vlanId + ", port=" + port.getIfName());
                continue;
            }
            LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
            vlanIf.addUntaggedPort(logical);
        }
    }

    public void setVlanPortUsage(VlanDevice device) throws IOException, AbortedException {
        Map<CiscoMibUtil.CiscoModulePortKey, IntSnmpEntry> vlanPortIslAdminStatuses =
                SnmpUtil.getWalkResult(snmp, vlanPortIslAdminStatus, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);

        for (CiscoMibUtil.CiscoModulePortKey key : vlanPortIslAdminStatuses.keySet()) {
            EthernetPort port = slotPortIndexToPort.get(key);
            if (port == null) {
                throw new IllegalStateException();
            }
            LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
            if (logical == null) {
                throw new IllegalStateException("no logical-eth: eth=" + port.getFullyQualifiedName());
            }
            int value = vlanPortIslAdminStatuses.get(key).intValue();
            VlanPortUsage usage = null;
            switch (value) {
                case 1:
                case 3:
                case 4:
                case 5:
                    usage = VlanPortUsage.TRUNK;
                    break;
                case 2:
                    usage = VlanPortUsage.ACCESS;
                    break;
            }
            if (usage != null) {
                logical.setVlanPortUsage(usage);
            }
        }
    }
}