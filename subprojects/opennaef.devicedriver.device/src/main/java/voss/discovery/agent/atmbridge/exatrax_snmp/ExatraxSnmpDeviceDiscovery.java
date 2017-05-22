package voss.discovery.agent.atmbridge.exatrax_snmp;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.model.EthernetPort.AutoNego;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class ExatraxSnmpDeviceDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(ExatraxSnmpDeviceDiscovery.class);
    private final InterfaceMibImpl ifMib;
    private final Map<String, AtmPvc> pvcMap = new HashMap<String, AtmPvc>();

    public ExatraxSnmpDeviceDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
    }

    private EAConverter device;

    public Device getDeviceInner() {
        return this.device;
    }

    public void update(Device device) {

    }

    public void getDeviceInformation() throws IOException, AbortedException {
        device = new EAConverter();
        String hostName = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        device.setModelTypeName("NS-6100");
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_SII);
        device.setOsTypeName(OSType.EXAOS.caption);
        device.setDeviceName(hostName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        try {
            String serial = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                    ExatraxMib.nsSysSerialNumber);
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

    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        createSlot(1);
        createSlot(2);
        createPhysicalPorts();
        setPhysicalPortAttribute();
        createLAG();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void createPhysicalPorts() throws AbortedException, IOException {
        createEthernetPorts();
        createAtmPorts();
    }

    private void createSlot(int slotID) {
        Slot slot1 = new SlotImpl();
        slot1.initContainer(device);
        slot1.initSlotId(String.valueOf(slotID));
        slot1.initSlotIndex(slotID);
    }

    private void createEthernetPorts() throws AbortedException, IOException {
        MibTable nsEtherPhyTable = new MibTable(getSnmpAccess(), "", ExatraxMib.nsEtherPhyEntry);
        nsEtherPhyTable.addColumn(ExatraxMib.nsEtherStatsDuplexMode_suffix, "duplex");
        nsEtherPhyTable.addColumn(ExatraxMib.nsEtherConfigAutoNego_suffix, "autoNego");
        nsEtherPhyTable.walk();
        List<KeyHolder> keys = new ArrayList<KeyHolder>(nsEtherPhyTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            int ifIndex = key.intValue(0);
            EthernetPort eth = new EthernetPortImpl();
            eth.initDevice(device);
            eth.initIfIndex(ifIndex);
            eth.initIfName(String.valueOf(ifIndex));
            log.debug("new eth-port ifIndex=" + ifIndex + ", ifName=" + ifIndex);

            TableRow row = nsEtherPhyTable.getKeyAndRows().get(key);
            IntSnmpEntry duplexModeEntry = row.getColumnValue(
                    ExatraxMib.nsEtherStatsDuplexMode_suffix,
                    SnmpHelper.intEntryBuilder);
            int duplexMode = duplexModeEntry.getValueAsBigInteger().intValue();
            switch (duplexMode) {
                case 2:
                    eth.setDuplex(Duplex.HALF);
                    break;
                case 3:
                    eth.setDuplex(Duplex.FULL);
                    break;
                default:
                    break;
            }

            IntSnmpEntry autoNegoEntry = row.getColumnValue(
                    ExatraxMib.nsEtherConfigAutoNego_suffix,
                    SnmpHelper.intEntryBuilder);
            int autoNego = autoNegoEntry.getValueAsBigInteger().intValue();
            switch (autoNego) {
                case 1:
                    eth.setAutoNego(AutoNego.ON);
                    break;
                default:
                    break;
            }
        }
    }

    private void createAtmPorts() throws AbortedException, IOException {
        try {
            List<IntSnmpEntry> atmInterfaces = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), ExatraxMib.nsAtmIfInMissInsert);
            for (IntSnmpEntry entry : atmInterfaces) {
                int ifIndex = entry.getLastOIDIndex().intValue();
                Slot slot = null;
                String ifName;
                switch (ifIndex) {
                    case 201:
                        ifName = "1/1";
                        slot = device.getSlotBySlotIndex(1);
                        break;
                    case 202:
                        ifName = "1/2";
                        slot = device.getSlotBySlotIndex(1);
                        break;
                    case 203:
                        ifName = "2/1";
                        slot = device.getSlotBySlotIndex(2);
                        break;
                    case 204:
                        ifName = "2/2";
                        slot = device.getSlotBySlotIndex(2);
                        break;
                    default:
                        ifName = "Unknown:" + ifIndex;
                }
                AtmPhysicalPort atm = new AtmPhysicalPort(device, ifName, "ATM");
                atm.initIfIndex(ifIndex);
                if (slot != null) {
                    Module module = slot.getModule();
                    if (module == null) {
                        module = new ModuleImpl();
                        module.setModelTypeName("ATM Module");
                        slot.setModule(module);
                    }
                    module.addPort(atm);
                }
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setPhysicalPortAttribute() throws AbortedException, IOException {
        MibTable nsIfXTable = new MibTable(getSnmpAccess(), "", ExatraxMib.nsIfXTable);
        nsIfXTable.addColumn(ExatraxMib.nsIfName_suffix, "ifName");
        nsIfXTable.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(nsIfXTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = nsIfXTable.getKeyAndRows().get(key);
            StringSnmpEntry ifNameEntry = row.getColumnValue(
                    ExatraxMib.nsIfName_suffix,
                    SnmpHelper.stringEntryBuilder);
            int ifIndex = key.intValue(0);
            String description = ifNameEntry.getValue();
            Port port = device.getPortByIfIndex(ifIndex);
            if (port == null) {
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            PhysicalPort phy = (PhysicalPort) port;
            phy.setIfDescr(description);
            phy.setUserDescription(description);
        }

        Map<String, Long> map = new HashMap<String, Long>();
        MibTable ifTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifTable);
        ifTable.addColumn(InterfaceMib.ifAdminStatus_SUFFIX, "ifAdminStatus");
        ifTable.addColumn(InterfaceMib.ifOperStatus_SUFFIX, "ifOperStatus");
        ifTable.addColumn(InterfaceMib.ifSpeed_SUFFIX, "ifSpeed");
        ifTable.walk();
        for (TableRow row : ifTable.getRows()) {
            KeyHolder key = row.getKey();
            int ifIndex = key.intValue(0);

            Port port = device.getPortByIfIndex(ifIndex);
            if (port == null) {
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            PhysicalPort phy = (PhysicalPort) port;

            int speed = row.getColumnValue(
                    InterfaceMib.ifSpeed_SUFFIX,
                    SnmpHelper.intEntryBuilder).intValue();
            Long highSpeed_ = map.get(phy.getIfName());
            long highSpeed = (highSpeed_ == null ? 0L : highSpeed_.longValue());
            PortSpeedValue.Oper oper;
            if (highSpeed > speed) {
                oper = new PortSpeedValue.Oper(highSpeed);
            } else {
                oper = new PortSpeedValue.Oper(speed);
            }
            phy.setPortOperationalSpeed(oper);

            int adminStatus = row.getColumnValue(InterfaceMib.ifAdminStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            int operStatus = row.getColumnValue(InterfaceMib.ifOperStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            phy.setAdminStatus(InterfaceMibImpl.getAdminStatusString(adminStatus));
            phy.setOperationalStatus(InterfaceMibImpl.getOperStatusString(operStatus));
        }
    }

    private void createLAG() throws AbortedException, IOException {
        MibTable nsDot3adAggEntry = new MibTable(getSnmpAccess(), "", ExatraxMib.nsDot3adAggEntry);
        nsDot3adAggEntry.addColumn(ExatraxMib.nsDot3adAggGroupName_suffix, "lagName");
        nsDot3adAggEntry.addColumn(ExatraxMib.nsDot3AdAggPortListPorts_suffix, "memberPort");
        nsDot3adAggEntry.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(nsDot3adAggEntry.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = nsDot3adAggEntry.getKeyAndRows().get(key);
            StringSnmpEntry lagNameEntry = row.getColumnValue(
                    ExatraxMib.nsDot3adAggGroupName_suffix,
                    SnmpHelper.stringEntryBuilder);
            ByteSnmpEntry memberPortEntry = row.getColumnValue(
                    ExatraxMib.nsDot3AdAggPortListPorts_suffix,
                    SnmpHelper.byteEntryBuilder);
            int ifIndex = key.intValue(0);
            String lagName = lagNameEntry.getValue();
            log.debug("found LAG: " + lagName + "(" + ifIndex + ")");
            int[] memberPorts = SnmpUtil.decodeBitList(memberPortEntry.getValue());
            if (memberPorts.length == 0) {
                log.debug("no member port on LAG: " + lagName);
                continue;
            }
            EthernetPortsAggregator lag = new EthernetPortsAggregatorImpl();
            lag.initDevice(this.device);
            lag.initIfName(lagName);
            lag.setAggregationName(lagName);
            lag.initIfIndex(ifIndex);
            for (int memberPort : memberPorts) {
                Port p = this.device.getPortByIfIndex(memberPort);
                if (p == null || !EthernetPort.class.isInstance(p)) {
                    continue;
                }
                lag.addPhysicalPort((EthernetPort) p);
                log.debug("add member port LAG: " + lagName + ", Member: " + p.getIfName());
            }
        }
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        createPvc();
        setPvcAttribute();
        createAtmVlanBridge();
        createVlan();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void createPvc() throws AbortedException, IOException {
        MibTable nsAtmAal5Table = new MibTable(getSnmpAccess(), "", ExatraxMib.nsAtmAal5Table);
        nsAtmAal5Table.addColumn(ExatraxMib.nsAtmAal5AtmIfIndex_suffix, "atm");
        nsAtmAal5Table.addColumn(ExatraxMib.nsAtmAal5Vpi_suffix, "vpi");
        nsAtmAal5Table.addColumn(ExatraxMib.nsAtmAal5Vci_suffix, "vci");
        nsAtmAal5Table.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(nsAtmAal5Table.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = nsAtmAal5Table.getKeyAndRows().get(key);
            IntSnmpEntry atmPortEntry = row.getColumnValue(
                    ExatraxMib.nsAtmAal5AtmIfIndex_suffix,
                    SnmpHelper.intEntryBuilder);
            IntSnmpEntry vpiEntry = row.getColumnValue(
                    ExatraxMib.nsAtmAal5Vpi_suffix,
                    SnmpHelper.intEntryBuilder);
            IntSnmpEntry vciEntry = row.getColumnValue(
                    ExatraxMib.nsAtmAal5Vci_suffix,
                    SnmpHelper.intEntryBuilder);
            int ifIndex = key.intValue(0);
            int atmIfIndex = atmPortEntry.intValue();
            int vpi = vpiEntry.intValue();
            int vci = vciEntry.intValue();
            Port pvc_ = device.getPortByIfIndex(ifIndex);
            if (pvc_ != null) {
                if (AtmPvc.class.isInstance(pvc_)) {
                    continue;
                } else {
                    log.warn("unexpected port ifIndex=" + ifIndex + ", ifName=" + pvc_.getIfName());
                }
            }
            Port port = device.getPortByIfIndex(atmIfIndex);
            if (port == null) {
                continue;
            } else if (!(port instanceof AtmPhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            AtmPhysicalPort phy = (AtmPhysicalPort) port;
            AtmVp vp = phy.getVp(vpi);
            if (vp == null) {
                vp = new AtmVp(phy, vpi);
                vp.initIfName(phy.getIfName() + "/" + vpi);
                log.debug("new atm-pvp-if: " + vp.getIfName());
            }
            AtmPvc pvc = vp.getPvc(vci);
            if (pvc == null) {
                pvc = new AtmPvc(vp, vci);
                pvc.initIfIndex(ifIndex);
                pvc.initIfName(vp.getIfName() + "/" + vci);
                log.debug("new atm-pvc-if: " + pvc.getIfName() + "(" + pvc.getIfIndex() + ")");
            }
            String pvcMapKey = atmIfIndex + "." + vpi + "." + vci;
            this.pvcMap.put(pvcMapKey, pvc);
        }
    }

    private void setPvcAttribute() throws IOException, AbortedException {
        MibTable nsAtmPvcTable = new MibTable(getSnmpAccess(), "", ExatraxMib.nsAtmPvcTable);
        nsAtmPvcTable.addColumn(ExatraxMib.nsAtmPvcAdminStatus_suffix, "adminStatus");
        nsAtmPvcTable.addColumn(ExatraxMib.nsAtmPvcPcr_suffix, "PCR");
        nsAtmPvcTable.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(nsAtmPvcTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = nsAtmPvcTable.getKeyAndRows().get(key);
            IntSnmpEntry adminStatusEntry = row.getColumnValue(
                    ExatraxMib.nsAtmPvcAdminStatus_suffix,
                    SnmpHelper.intEntryBuilder);
            IntSnmpEntry pcrEntry = row.getColumnValue(
                    ExatraxMib.nsAtmPvcPcr_suffix,
                    SnmpHelper.intEntryBuilder);
            if (key.key.length != 3) {
                log.warn("illegal format in nsAtmPvcTable: key=" + key.toString());
                continue;
            }
            String pvcKey = key.intValue(0) + "." + key.intValue(1) + "." + key.intValue(2);
            int adminStatus = adminStatusEntry.intValue();
            int pcr = pcrEntry.intValue();
            AtmPvc pvc = this.pvcMap.get(pvcKey);
            if (pvc == null) {
                log.warn("unexpected pvc entry: key=" + key.toString());
                continue;
            }
            pvc.setPcr(Long.valueOf(pcr));
            switch (adminStatus) {
                case 1:
                    pvc.setAdminStatus("UP");
                    break;
                case 2:
                    pvc.setAdminStatus("down");
                    break;
            }
        }
    }

    private void createAtmVlanBridge() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), ExatraxMib.nsVlanBridgePortIfIndex);
            for (IntSnmpEntry entry : entries) {
                int bridgePortNumber = entry.getOIDSuffixLast();
                int ifIndex = entry.intValue();
                Port p = this.device.getPortByIfIndex(ifIndex);
                if (!AtmPvc.class.isInstance(p)) {
                    continue;
                }
                AtmPvc pvc = (AtmPvc) p;
                AtmVlanBridge bridge = new AtmVlanBridge(pvc, "bridge-" + bridgePortNumber);
                bridge.setBridgePortNumber(bridgePortNumber);
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void createVlan() throws IOException, AbortedException {
        MibTable nsVlanConfigTable = new MibTable(getSnmpAccess(), "", ExatraxMib.nsVlanConfigTable);
        nsVlanConfigTable.addColumn(ExatraxMib.nsVlanConfigName_suffix, "name");
        nsVlanConfigTable.addColumn(ExatraxMib.nsVlanConfigEgressPorts_suffix, "tagged");
        nsVlanConfigTable.addColumn(ExatraxMib.nsVlanConfigEgressUntaggedPorts_suffix, "untagged");
        nsVlanConfigTable.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(nsVlanConfigTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = nsVlanConfigTable.getKeyAndRows().get(key);
            StringSnmpEntry vlanNameEntry = row.getColumnValue(
                    ExatraxMib.nsVlanConfigName_suffix,
                    SnmpHelper.stringEntryBuilder);
            ByteSnmpEntry taggedEntry = row.getColumnValue(
                    ExatraxMib.nsVlanConfigEgressPorts_suffix,
                    SnmpHelper.byteEntryBuilder);
            ByteSnmpEntry untaggedEntry = row.getColumnValue(
                    ExatraxMib.nsVlanConfigEgressUntaggedPorts_suffix,
                    SnmpHelper.byteEntryBuilder);
            int vlanID = key.intValue(0);
            String vlanName = vlanNameEntry.getValue();
            VlanIf vlanIf = new VlanIfImpl();
            vlanIf.initDevice(this.device);
            vlanIf.initVlanId(vlanID);
            vlanIf.initIfName(vlanName);
            vlanIf.setVlanName(vlanName);
            List<Integer> untaggedBridgePorts = new ArrayList<Integer>();
            int[] taggedPortIndices = SnmpUtil.decodeBitList(taggedEntry.value);
            int[] untaggedPortIndices = SnmpUtil.decodeBitList(untaggedEntry.value);
            for (int untaggedPortIndex : untaggedPortIndices) {
                AtmVlanBridge bridge = this.device.getAtmVlanBridgeByBridgePortNumber(untaggedPortIndex);
                if (bridge == null) {
                    log.warn("bridge not found: " + untaggedPortIndex);
                    continue;
                }
                bridge.setUntaggedVlanIf(vlanIf);
                untaggedBridgePorts.add(untaggedPortIndex);
            }
            for (int taggedPortIndex : taggedPortIndices) {
                if (untaggedBridgePorts.contains(taggedPortIndex)) {
                    continue;
                }
                AtmVlanBridge bridge = this.device.getAtmVlanBridgeByBridgePortNumber(taggedPortIndex);
                if (bridge == null) {
                    log.warn("bridge not found: " + taggedPortIndex);
                    continue;
                }
                bridge.addTaggedVlanIf(vlanIf);
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
        ifMib.setAllIfOperStatus(device);

    }

    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    public void getConfiguration() throws IOException, AbortedException {
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    public void getStatisticalInformation() throws IOException, AbortedException {
    }

    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());
        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");
        entry.close();
    }
}