package voss.discovery.agent.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class InterfaceMibImpl implements InterfaceMib {
    private final static Logger log = LoggerFactory.getLogger(InterfaceMibImpl.class);
    private final SnmpAccess snmp;

    public InterfaceMibImpl(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public void createPhysicalPorts_(Device device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> ifTypes =
                SnmpUtil.getWalkResult(snmp, ifType, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> ifConnectorPresents =
                SnmpUtil.getWalkResult(snmp, ifConnectorPresent, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ifConnectorPresents.keySet()) {
            boolean connectorPresent = (ifConnectorPresents.get(key).intValue()) == 1;
            if (!connectorPresent) {
                continue;
            }

            IanaIfType ianaType = IanaIfType.valueOf(ifTypes.get(key).intValue());
            log.trace("createPhysicalPorts(): ifindex=" + key.getInt() + ", ianaType=" + ianaType);
            int ifindex = -1;
            try {
                ifindex = key.getInt();
                PhysicalPort port = getPhysicalPort(ianaType);
                port.initDevice(device);
                port.initIfIndex(ifindex);
                port.setPortTypeName(ianaType.toString());
                log.debug("@ add port " + ifindex + " type " + port.getClass().getSimpleName()
                        + " (" + ianaType.toString() + ")"
                        + " to device='" + device.getDeviceName() + "'");
            } catch (UnknownIfTypeException e) {
                log.warn("unknown if type: ifindex=" + ifindex + ";" + e.getMessage());
                continue;
            }
        }
    }

    public void createPhysicalPorts(Device device) throws IOException, AbortedException {
        MibTable mibIfTable = new MibTable(snmp, "ifTable", ifTable);
        mibIfTable.addColumn(ifType_SUFFIX, "ifType");
        mibIfTable.walk();

        MibTable mibIfXTable = new MibTable(snmp, "ifXTable", ifXTable);
        mibIfXTable.addColumn(ifConnectorPresent_SUFFIX, "ifConnectorPresent");
        mibIfXTable.walk();
        Map<KeyHolder, TableRow> ifXTableResult = mibIfXTable.getKeyAndRows();

        for (TableRow row : mibIfTable.getRows()) {
            int ifindex = row.getKey().intValue(0);
            IanaIfType ianaType = IanaIfType.valueOf(row.getColumnValue(ifType_SUFFIX, intEntryBuilder).intValue());
            Port p = device.getPortByIfIndex(ifindex);
            if (p != null) {
                log.warn("port exists. ifIndex=" + ifindex);
                continue;
            }

            TableRow row2 = ifXTableResult.get(row.key);
            if (row2 == null) {
                log.warn("- no ifXTable::ifConnectorPresent. ifIndex=" + ifindex);
                continue;
            }
            IntSnmpEntry connectorPresentValue = row2.getColumnValue(ifConnectorPresent_SUFFIX, intEntryBuilder);
            boolean connectorPresent = (connectorPresentValue == null ? false : connectorPresentValue.intValue() == 1);
            if (!connectorPresent) {
                continue;
            }

            log.debug("createPhysicalPorts(): ifindex=" + ifindex + ", ianaType=" + ianaType);
            PhysicalPort port;
            try {
                port = getPhysicalPort(ianaType);
                port.initDevice(device);
                port.initIfIndex(ifindex);
                port.setPortTypeName(ianaType.toString());
                log.debug("@ add port " + ifindex + " type " + port.getClass().getSimpleName()
                        + " (" + ianaType.toString() + ")"
                        + " to device='" + device.getDeviceName() + "'");
            } catch (UnknownIfTypeException e) {
                log.warn("unknown if type: ifindex=" + ifindex + ";" + e.getMessage());
                continue;
            }
        }
    }

    public void setAllIfNames(Device device) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> ifNames =
                SnmpUtil.getWalkResult(snmp, ifName, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ifNames.keySet()) {
            int ifindex = key.getInt();
            String ifname = ifNames.get(key).getValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port == null) {
                log.trace("ignored: " + device.getDeviceName() + " ifindex=" + ifindex + ", name=" + ifname);
                continue;
            }
            setIfName(port, ifname);
        }
    }

    public String getIfName(int ifIndex) throws IOException, AbortedException {
        try {
            String ifname = SnmpUtil.getString(snmp, ifName + "." + ifIndex);
            return ifname;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void setIfName(Port port) throws IOException, AbortedException {
        try {
            port.getIfIndex();
        } catch (NotInitializedException e) {
            return;
        }

        try {
            String ifname = SnmpUtil.getString(snmp, ifName + "." + port.getIfIndex());
            setIfName(port, ifname);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private final static Pattern portNumberPattern = Pattern.compile(".*[^0-9]([0-9]+)");

    private void setIfName(Port port, String ifname) {
        assert port != null;
        log.trace("getIfNames(): port=" + port + ", ifindex=" + port.getIfIndex());
        if (port != null) {
            log.debug("@ set port " + port.getIfIndex() + " ifName [" + ifname
                    + "] on device [" + port.getDevice().getDeviceName() + "]");
            port.initIfName(ifname);

            if (port instanceof PhysicalPort) {
                PhysicalPort pp = (PhysicalPort) port;
                try {
                    pp.getPortIndex();
                } catch (NotInitializedException e) {
                    Matcher matcher = portNumberPattern.matcher(ifname);
                    if (matcher.matches()) {
                        String numberPart = matcher.group(1);
                        int portIndex = Integer.parseInt(numberPart, 10);
                        pp.initPortIndex(portIndex);
                    } else {
                        pp.initPortIndex(pp.getIfIndex());
                    }
                }
            }

        }

    }

    public void setAllIfAliases(Device device) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> ifAliases =
                SnmpUtil.getWalkResult(snmp, ifAlias, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ifAliases.keySet()) {
            int ifindex = key.getInt();
            String ifname = ifAliases.get(key).getValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port == null) {
                log.trace("ignored: " + device.getDeviceName() + " ifindex=" + ifindex + ", alias=" + ifname);
                continue;
            }
            setIfAlias(port, ifname);
        }
    }

    public void setIfAlias(Port port) throws IOException, AbortedException {
        try {
            port.getIfIndex();
        } catch (NotInitializedException e) {
            return;
        }

        try {
            String ifname = SnmpUtil.getString(snmp, ifAlias + "." + port.getIfIndex());
            setIfAlias(port, ifname);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setIfAlias(Port port, String ifname) {
        if (port != null && port instanceof PhysicalPort) {
            log.debug("@ set port " + port.getIfIndex() + " ifalias [" + ifname
                    + "] on device [" + port.getDevice().getDeviceName() + "]");
            ((PhysicalPort) port).setPortName(ifname);
        }
    }

    public void setAllIfTypes(Device device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> ifTypes =
                SnmpUtil.getWalkResult(snmp, ifType, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ifTypes.keySet()) {
            int ifindex = key.getInt();
            String iftype = IanaIfType.valueOf(ifTypes.get(key).intValue()).toString();
            Port port = device.getPortByIfIndex(ifindex);
            if (port == null) {
                log.trace("ignored: " + device.getDeviceName() + " ifindex=" + ifindex + ", typeName=" + iftype);
                continue;
            }
            setIfType(port, iftype);
        }
    }

    public void setIfType(Port port) throws IOException, AbortedException {
        try {
            port.getIfIndex();
        } catch (NotInitializedException e) {
            return;
        }

        try {
            String iftype = IanaIfType.valueOf(SnmpUtil.getInteger(snmp, ifType + "." + port.getIfIndex())).toString();
            setIfType(port, iftype);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setIfType(Port port, String type) {
        if (port != null && port instanceof PhysicalPort) {
            log.debug("@ set port " + port.getIfIndex() + " typeName [" + type
                    + "] on device [" + port.getDevice().getDeviceName() + "]");
            ((PhysicalPort) port).setPortTypeName(type);
        }
    }

    public IanaIfType getIfType(int ifIndex) throws IOException, AbortedException {
        try {
            IanaIfType iftype = IanaIfType.valueOf(SnmpUtil.getInteger(snmp, ifType + "." + ifIndex));
            return iftype;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void setAllIfDescriptions(Device device) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> ifDescriptions =
                SnmpUtil.getWalkResult(snmp, ifDesc, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ifDescriptions.keySet()) {
            int ifindex = key.getInt();
            String ifdescription = ifDescriptions.get(key).getValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port != null) {
                setIfDescription(port, ifdescription);
            } else {
                log.trace("getIfDescriptions(): no port found: ifindex=" + ifindex);
            }
        }
    }

    public void setIfDescription(Port port) throws IOException, AbortedException {
        try {
            String ifdescr = SnmpUtil.getString(snmp, ifDesc + "." + port.getIfIndex());
            setIfDescription(port, ifdescr);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setIfDescription(Port port, String ifdescription) {
        log.debug("@ set port " + port.getIfIndex() + " ifdescr [" + ifdescription
                + "] on device [" + port.getDevice().getDeviceName() + "]");
        port.setIfDescr(ifdescription);
    }

    public void setAllIfAdminStatus(Device device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> values =
                SnmpUtil.getWalkResult(snmp, ifAdminStatus, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : values.keySet()) {
            int ifindex = key.getInt();
            int adminStatus = values.get(key).intValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port != null) {
                setIfAdminStatus(port, adminStatus);
            } else {
                log.trace("getIfAdminStatus(): no port found: ifindex=" + ifindex);
            }
        }
    }

    public void setIfAdminStatus(Port port) throws IOException, AbortedException {
        try {
            int adminStatus = SnmpUtil.getInteger(snmp, ifAdminStatus + "." + port.getIfIndex());
            setIfAdminStatus(port, adminStatus);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setIfAdminStatus(Port port, int adminStatus) {
        log.debug("@ set port " + port.getIfIndex() + " adminstatus [" + getAdminStatusString(adminStatus)
                + "] on device [" + port.getDevice().getDeviceName() + "]");
        port.setAdminStatus(getAdminStatusString(adminStatus));
    }

    public static String getAdminStatusString(int adminStatus) {
        return adminStatus == 1 ? "enable" :
                adminStatus == 2 ? "disable" :
                        adminStatus == 3 ? "testing" :
                                "UNKNOWN";
    }

    public void setAllIfOperStatus(Device device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> values =
                SnmpUtil.getWalkResult(snmp, ifOperStatus, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : values.keySet()) {
            int ifindex = key.getInt();
            int status = values.get(key).intValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port != null) {
                setIfOperStatus(port, status);
            } else {
                log.trace("getIfOperStatus(): no port found: ifindex=" + ifindex);
            }
        }
    }

    public void setIfOperStatus(Port port) throws IOException, AbortedException {
        try {
            int operStatus = SnmpUtil.getInteger(snmp, ifOperStatus + "." + port.getIfIndex());
            setIfOperStatus(port, operStatus);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setIfOperStatus(Port port, int operStatus) {
        log.debug("@ set port " + port.getIfIndex() + " [" + port.getIfName()
                + "] operstatus [" + getOperStatusString(operStatus)
                + "] on device [" + port.getDevice().getDeviceName() + "]");
        port.setOperationalStatus(getOperStatusString(operStatus));
    }

    public static String getOperStatusString(int value) {
        return value == 1 ? "up" : value == 2 ? "down" : value == 3 ? "testing"
                : value == 4 ? "unknown" : value == 5 ? "dormant"
                : value == 6 ? "notPresent"
                : value == 7 ? "lowerLayerDown" : "---";
    }

    public void setAllIfSpeed(Device device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> speeds =
                SnmpUtil.getWalkResult(snmp, ifSpeed, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> highSpeeds =
                SnmpUtil.getWalkResult(snmp, ifHighSpeed, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : speeds.keySet()) {
            int ifindex = key.getInt();
            Port port = device.getPortByIfIndex(ifindex);
            setIfSpeed(port, speeds.get(key).longValue(), highSpeeds.get(key).longValue());
        }

    }

    public void setIfSpeed(Port port) throws IOException, AbortedException {
        if (port == null || port.getBandwidth() != null) {
            return;
        }

        try {
            port.getIfIndex();
        } catch (NotInitializedException e) {
            return;
        }

        Long speed = null;
        Long highSpeed = null;
        try {
            try {
                speed = SnmpUtil.getLong(snmp, ifSpeed + "." + port.getIfIndex());
            } catch (NoSuchMibException e) {
                log.warn("no ifSpeed found: " + port.getFullyQualifiedName());
            }
            try {
                highSpeed = SnmpUtil.getLong(snmp, ifHighSpeed + "." + port.getIfIndex());
            } catch (NoSuchMibException e) {
                log.warn("no ifHighSpeed found: " + port.getFullyQualifiedName());
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
        setIfSpeed(port, speed, highSpeed);
    }

    private void setIfSpeed(final Port port, final Long speed, final Long highSpeed) {
        if (port == null) {
            return;
        }
        Long s = null;
        if (speed != null) {
            if (speed.longValue() > Constants.GIGA && highSpeed != null) {
                s = Long.valueOf(highSpeed * Constants.MEGA);
            } else {
                s = speed;
            }
        } else if (highSpeed != null) {
            s = highSpeed * Constants.MEGA;
        }

        if (s == null) {
            log.warn("no speed value found on mib: " + port.getFullyQualifiedName());
            return;
        }

        if (port instanceof EthernetPort) {
            log.debug("@ set port " + port.getIfIndex() + " speed [" + s
                    + "] on device [" + port.getDevice().getDeviceName() + "]");
            ((EthernetPort) port).setPortOperationalSpeed(new PortSpeedValue.Oper(s));
            port.setBandwidth(s);
        } else {
            log.debug("@ set port " + port.getIfIndex() + " speed [" + s
                    + "] on device [" + port.getDevice().getDeviceName() + "]");
            port.setBandwidth(s);
        }

    }

    public PhysicalPort getPhysicalPort(IanaIfType type) throws UnknownIfTypeException {
        if (type.isFamilyOf("ethernet")) {
            EthernetPort port = new EthernetPortImpl();
            port.setPortTypeName(type.toString());
            return port;
        } else if (type.isFamilyOf("ieee8023")) {
            EthernetPort port = new EthernetPortImpl();
            port.setPortTypeName(type.toString());
            return port;
        } else if (type.isFamilyOf("atm")) {
            AtmPortImpl port = new AtmPortImpl();
            port.setPortTypeName(type.toString());
            return port;
        } else if (type.isFamilyOf("sonet", "ds1", "pos", "ds3")) {
            POSImpl port = new POSImpl();
            port.setPortTypeName(type.toString());
            return port;
        } else if (type.isFamilyOf("serial", "framerelay")) {
            SerialPortImpl port = new SerialPortImpl();
            port.setPortTypeName(type.toString());
            return port;
        }
        throw new UnknownIfTypeException("unknown type: " + type);
    }

    public static class UnknownIfTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnknownIfTypeException(String msg) {
            super(msg);
        }
    }

    public Map<Integer, List<Integer>> getIfStackStatusMapGroupByLower(int filter) throws IOException, AbortedException {
        Map<TwoIntegerKey, IntSnmpEntry> ifStackStatuses =
                SnmpUtil.getWalkResult(snmp, ifStackStatus_OID, intEntryBuilder, twoIntegerKeyCreator);
        Map<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
        for (Map.Entry<TwoIntegerKey, IntSnmpEntry> entry : ifStackStatuses.entrySet()) {
            TwoIntegerKey key = entry.getKey();
            IntSnmpEntry val = entry.getValue();
            int higherIndex = key.intValue1();
            int lowerIndex = key.intValue2();
            int value = (val == null ? -1 : val.intValue());
            log.trace("ifStackStatus: [" + higherIndex + ", " + lowerIndex + "]=" + value);
            if (filter != -1) {
                if (value != filter) {
                    continue;
                }
            }
            List<Integer> list = result.get(lowerIndex);
            if (list == null) {
                list = new ArrayList<Integer>();
                result.put(lowerIndex, list);
            }
            list.add(higherIndex);
        }
        for (List<Integer> value : result.values()) {
            Collections.sort(value);
        }
        return result;
    }

    public Map<Integer, List<Integer>> getIfStackStatusMapGroupByHigher(int filter) throws IOException, AbortedException {
        Map<TwoIntegerKey, IntSnmpEntry> ifStackStatuses =
                SnmpUtil.getWalkResult(snmp, ifStackStatus_OID, intEntryBuilder, twoIntegerKeyCreator);
        Map<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
        for (Map.Entry<TwoIntegerKey, IntSnmpEntry> entry : ifStackStatuses.entrySet()) {
            TwoIntegerKey key = entry.getKey();
            IntSnmpEntry val = entry.getValue();
            int higherIndex = key.intValue1();
            int lowerIndex = key.intValue2();
            int value = (val == null ? -1 : val.intValue());
            log.trace("ifStackStatus: [" + higherIndex + ", " + lowerIndex + "]=" + value);
            if (filter != -1) {
                if (value != filter) {
                    continue;
                }
            }
            List<Integer> list = result.get(higherIndex);
            if (list == null) {
                list = new ArrayList<Integer>();
                result.put(higherIndex, list);
            }
            list.add(lowerIndex);
        }
        for (List<Integer> value : result.values()) {
            Collections.sort(value);
        }
        return result;
    }

    public int getIfStackStatusHigherValue(int lowerIfIndex) throws IOException, AbortedException {
        Map<TwoIntegerKey, IntSnmpEntry> ifStackStatuses =
                SnmpUtil.getWalkResult(snmp, ifStackStatus_OID, intEntryBuilder, twoIntegerKeyCreator);

        for (TwoIntegerKey key : ifStackStatuses.keySet()) {
            int higherIndex = key.intValue1();
            int lowerIndex = key.intValue2();
            log.trace("req=" + lowerIfIndex + ", higher=" + higherIndex + ", lowerIndex=" + lowerIndex);
            if (lowerIndex == lowerIfIndex) {
                return higherIndex;
            }
        }
        return -1;
    }

    public void supplementAggregationInterface(VlanDevice device, String lagSignificantKey) throws IOException, AbortedException {
        Map<TwoIntegerKey, IntSnmpEntry> ifStackStatuses =
                SnmpUtil.getWalkResult(snmp, ifStackStatus_OID, intEntryBuilder, twoIntegerKeyCreator);

        for (TwoIntegerKey key : ifStackStatuses.keySet()) {
            int aggregatorIfIndex = key.intValue1();
            int portIfIndex = key.intValue2();
            log.trace("supplementAggregationInterface(): agg=" + aggregatorIfIndex + "/port=" + portIfIndex);

            if (aggregatorIfIndex == 0 || portIfIndex == 0) {
                continue;
            }

            String ifDescr = Mib2Impl.getIfDescr(snmp, aggregatorIfIndex);
            if (ifDescr == null || !(ifDescr.startsWith(lagSignificantKey))) {
                continue;
            }

            Port port = device.getPortByIfIndex(portIfIndex);
            if (port == null) {
                log.warn("ifindex=" + portIfIndex + " seemed to non-port. really?");
            } else {
                if (port instanceof EthernetPort) {
                    LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) port);
                    if (logical == null) {
                        EthernetPortsAggregator aggregator;
                        Port p = device.getPortByIfIndex(aggregatorIfIndex);
                        if (p == null) {
                            log.debug("@ create lag " + aggregatorIfIndex
                                    + " on device [" + device.getDeviceName() + "]");
                            aggregator = new EthernetPortsAggregatorImpl();
                            aggregator.initDevice(device);
                            aggregator.initIfIndex(aggregatorIfIndex);
                            Pattern pattern = Pattern.compile(lagSignificantKey + "([0-9]+).*");
                            Matcher matcher = pattern.matcher(ifDescr);
                            if (matcher.matches()) {
                                int lagID = Integer.parseInt(matcher.group(1));
                                aggregator.initAggregationGroupId(lagID);
                            }

                        } else {
                            if (p instanceof EthernetPortsAggregator) {
                                aggregator = (EthernetPortsAggregator) p;
                            } else {
                                throw new IOException("duplicated ifindex: "
                                        + aggregatorIfIndex
                                        + ", alredy: " + p.getFullyQualifiedName());
                            }
                        }
                        log.debug("@ set lag " + aggregatorIfIndex
                                + " add-port " + port.getIfIndex()
                                + " on device [" + device.getDeviceName() + "]");
                        aggregator.addPhysicalPort((EthernetPort) port);
                    }
                }
            }

        }
    }

}