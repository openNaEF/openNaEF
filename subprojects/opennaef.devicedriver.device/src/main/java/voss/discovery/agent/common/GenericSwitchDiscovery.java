package voss.discovery.agent.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.discovery.utils.ListUtil;
import voss.model.GenericEthernetSwitch;
import voss.model.MplsVlanDevice;
import voss.model.PhysicalPort;
import voss.model.Port;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public abstract class GenericSwitchDiscovery extends DeviceDiscoveryImpl {
    private static final Logger log = LoggerFactory.getLogger(GenericSwitchDiscovery.class);
    protected final GenericEthernetSwitch device;
    protected String textConfiguration;

    public GenericSwitchDiscovery(DeviceAccess access) {
        super(access);
        this.device = new GenericEthernetSwitch();
    }

    public GenericSwitchDiscovery(DeviceAccess access, MplsVlanDevice device) {
        super(access);
        this.device = device;
    }

    protected abstract ConsoleCommand getShowConfigCommand();

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        ConsoleCommand showConfig = getShowConfigCommand();
        if (showConfig == null) {
            return null;
        }
        try {
            console = this.getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(showConfig);
            this.textConfiguration = res1;
            List<String> lines = ListUtil.toLines(res1);
            String head = ListUtil.toContent(ListUtil.head(lines, 10));
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + head + "\r\n----");
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    protected void setPortAttributes() throws AbortedException, IOException {
        Map<String, Long> map = new HashMap<String, Long>();

        MibTable ifXTable = new MibTable(getSnmpAccess(), "ifXTable", InterfaceMib.ifXTable);
        ifXTable.addColumn(InterfaceMib.ifName_SUFFIX, "ifName");
        ifXTable.addColumn(InterfaceMib.ifHighSpeed_SUFFIX, "ifHighSpeed");
        ifXTable.addColumn(InterfaceMib.ifAlias_SUFFIX, "ifAlias");
        ifXTable.walk();

        Set<String> knownIfName = new HashSet<String>();
        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(ifXTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = ifXTable.getKeyAndRows().get(key);
            int ifIndex = key.intValue(0);

            StringSnmpEntry ifNameEntry = row.getColumnValue(
                    InterfaceMib.ifName_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            if (ifNameEntry == null) {
                log.warn("no ifName.");
                continue;
            }

            String ifName = ifNameEntry.getValue();
            if (knownIfName.contains(ifName)) {
                continue;
            }
            knownIfName.add(ifName);
            Port port = device.getPortByIfName(ifName);
            if (port == null) {
                log.warn("no port found: " + ifName);
                continue;
            }

            StringSnmpEntry ifAliasEntry = row.getColumnValue(
                    InterfaceMib.ifAlias_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            if (ifAliasEntry != null) {
                String ifAlias = ifAliasEntry.getValue();
                port.setIfDescr(ifAlias);
                port.setUserDescription(ifAlias);
            }

            if (!(port instanceof PhysicalPort)) {
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
            }

            int adminStatus = row.getColumnValue(InterfaceMib.ifAdminStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            int operStatus = row.getColumnValue(InterfaceMib.ifOperStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            port.setAdminStatus(InterfaceMibImpl.getAdminStatusString(adminStatus));
            port.setOperationalStatus(InterfaceMibImpl.getOperStatusString(operStatus));

            if (!(port instanceof PhysicalPort)) {
                continue;
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
        }
    }

    protected String getSwitchVlanIfName(int vlanID) {
        return String.format("vlan%04d", vlanID);
    }
}