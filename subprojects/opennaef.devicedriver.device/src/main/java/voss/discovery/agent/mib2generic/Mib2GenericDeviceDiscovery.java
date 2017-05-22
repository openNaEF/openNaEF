package voss.discovery.agent.mib2generic;


import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.EntityMib;
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
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.Device;
import voss.model.MplsVlanDevice;
import voss.model.PhysicalPort;
import voss.model.Port;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class Mib2GenericDeviceDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(Mib2GenericDeviceDiscovery.class);
    private final InterfaceMibImpl ifMib;

    protected final AgentConfiguration agentConfig;

    public Mib2GenericDeviceDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
    }

    private MplsVlanDevice device;

    public Device getDeviceInner() {
        return this.device;
    }

    public void update(Device device) {

    }

    public void getDeviceInformation() throws IOException, AbortedException {
        device = new MplsVlanDevice();
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        String sysObjectId = Mib2Impl.getSysObjectId(getDeviceAccess().getSnmpAccess());
        device.setModelTypeName(sysObjectId);
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        try {
            String serial = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                    EntityMib.EntityMibEntPhysicalSerialNumEntry.OID + ".1");
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
        ifMib.createPhysicalPorts(device);
        ifMib.setAllIfNames(device);
        setPortAttributes();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void setPortAttributes() throws AbortedException, IOException {
        Map<String, Long> map = new HashMap<String, Long>();

        MibTable ifXTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifXTable);
        ifXTable.addColumn(InterfaceMib.ifName_SUFFIX, "ifName");
        ifXTable.addColumn(InterfaceMib.ifConnectorPresent_SUFFIX, "ifConnectorPresent");
        ifXTable.addColumn(InterfaceMib.ifHighSpeed_SUFFIX, "ifHighSpeed");
        ifXTable.addColumn(InterfaceMib.ifAlias_SUFFIX, "ifAlias");
        ifXTable.walk();

        Set<String> knownIfName = new HashSet<String>();
        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(ifXTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = ifXTable.getKeyAndRows().get(key);
            StringSnmpEntry ifNameEntry = row.getColumnValue(
                    InterfaceMib.ifName_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            String ifName = ifNameEntry.getValue();
            if (knownIfName.contains(ifName)) {
                continue;
            }
            knownIfName.add(ifName);

            int ifIndex = key.intValue(0);
            Port port = device.getPortByIfName(ifName);
            if (port == null) {
                log.warn("no port found: " + ifName);
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            PhysicalPort phy = (PhysicalPort) port;
            try {
                log.debug("@set port ifName='" + port.getIfName() + "' ifindex='" + ifIndex + "';");
                phy.initIfIndex(ifIndex);
            } catch (IllegalStateException e) {
                log.debug("already set ifindex: port ifName='" + port.getIfName() + "' ifindex='" + ifIndex + "';");
            }

            IntSnmpEntry ifHighSpeedEntry = row.getColumnValue(
                    InterfaceMib.ifHighSpeed_SUFFIX,
                    SnmpHelper.intEntryBuilder);
            if (ifHighSpeedEntry != null) {
                int highSpeed = ifHighSpeedEntry.intValue();
                long speed = ((long) highSpeed) * 1000L * 1000L;
                map.put(ifName, speed);
            }

            StringSnmpEntry ifAliasEntry = row.getColumnValue(
                    InterfaceMib.ifAlias_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            String ifAlias = ifAliasEntry.getValue();
            phy.setIfDescr(ifAlias);
            phy.setUserDescription(ifAlias);
        }

        MibTable ifTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifTable);
        ifTable.addColumn(InterfaceMib.ifDesc_SUFFIX, "ifDesc");
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
            String ifDesc = row.getColumnValue(
                    InterfaceMib.ifDesc_SUFFIX,
                    SnmpHelper.stringEntryBuilder).getValue();

            phy.setPortName(ifDesc);
            phy.setSystemDescription(ifDesc);

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

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
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