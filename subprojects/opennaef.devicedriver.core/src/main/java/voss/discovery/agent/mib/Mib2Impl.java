package voss.discovery.agent.mib;

import net.snmp.OidTLV;
import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Mib2Impl implements Mib2 {
    private static final Logger log = LoggerFactory.getLogger(Mib2Impl.class);
    private final SnmpAccess snmp;

    public Mib2Impl(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public static synchronized String getIfDescr(SnmpAccess snmp, int ifIndex)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, InterfaceMib.ifDesc + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public static synchronized String getIfName(SnmpAccess snmp, int ifIndex)
            throws AbortedException, IOException {
        try {
            return SnmpUtil
                    .getString(snmp, InterfaceMib.ifName + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public static synchronized Map<Integer, String> getIfNameMap(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, InterfaceMib.ifName);
            Map<Integer, String> result = new HashMap<Integer, String>();
            for (StringSnmpEntry entry : entries) {
                String ifName = entry.getValue();
                int ifIndex = entry.getLastOIDIndex().intValue();
                if (ifName == null) {
                    continue;
                }
                result.put(Integer.valueOf(ifIndex), ifName);
            }

            return result;
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public static synchronized String getIfAlias(SnmpAccess snmp, int ifIndex)
            throws AbortedException, IOException {
        return getIfAlias(snmp, ifIndex, true);
    }

    public static synchronized String getIfAlias(SnmpAccess snmp, int ifIndex, boolean ignoreException)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, InterfaceMib.ifAlias + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public static synchronized String getSysName(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, sysName + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public synchronized String getSysName() throws IOException, AbortedException {
        return getSysName(this.snmp);
    }

    public static synchronized String getSysDescr(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, sysDescr + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public synchronized String getSysDescr() throws IOException, AbortedException {
        return getSysDescr(this.snmp);
    }

    public static synchronized String getSysContact(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, sysContact + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public synchronized String getSysContact() throws IOException, AbortedException {
        return getSysContact(this.snmp);
    }

    public static synchronized String getSysLocation(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, sysContact + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public synchronized String getSysLocation() throws IOException, AbortedException {
        return getSysLocation(this.snmp);
    }

    public static synchronized Long getSysUpTime(SnmpAccess snmp)
            throws AbortedException, IOException {
        try {
            return SnmpUtil.getLong(snmp, sysUptime + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public static synchronized Long getSysUpTimeInMilliseconds(SnmpAccess snmp)
            throws AbortedException, IOException {
        long sysUptime = getSysUpTime(snmp);
        return sysUptime * 10L;
    }

    public synchronized Long getSysUpTimeInMilliseconds() throws IOException, AbortedException {
        return getSysUpTimeInMilliseconds(this.snmp);
    }

    public synchronized static String getSysObjectId(SnmpAccess snmpAccess)
            throws AbortedException, IOException {
        try {
            VarBind objectIDVarbind = snmpAccess.get(sysObjectID + ".0");
            OidTLV objectIdOid = (OidTLV) objectIDVarbind.getValue();
            return objectIdOid.getOidString();
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public synchronized String getSysObjectId() throws IOException, AbortedException {
        return getSysObjectId(this.snmp);
    }

    public synchronized static String getSystemMacAddress(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            byte[] value = SnmpUtil.getByte(snmp, dot1dBaseBridgeAddress + ".0");
            return SnmpUtil.getPhysicalAddressString(value);
        } catch (NoSuchMibException e) {
            return null;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public synchronized String getSystemMacAddress() throws IOException, AbortedException {
        return getSystemMacAddress(this.snmp);
    }

    public synchronized static void setIfIndex(SnmpAccess snmp, Port port, String ifName) throws IOException, AbortedException {
        try {
            port.getIfIndex();
            return;
        } catch (NotInitializedException e) {
        }
        try {
            List<StringSnmpEntry> ifNameEntries = SnmpUtil.getStringSnmpEntries(snmp, InterfaceMib.ifName);
            for (StringSnmpEntry ifNameEntry : ifNameEntries) {
                String _ifName = ifNameEntry.getValue();
                if (_ifName == null) {
                    continue;
                }
                if (_ifName.equals(ifName)) {
                    port.initIfIndex(ifNameEntry.getLastOIDIndex().intValue());
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized static List<String> getIfPhysAddress(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            List<String> result = new ArrayList<String>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, ifPhysAddress);
            for (StringSnmpEntry entry : entries) {
                String mac = entry.getValue();
                result.add(mac);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized List<String> getIfPhysAddress() throws IOException, AbortedException {
        return getIfPhysAddress(this.snmp);
    }

    public synchronized static List<String> getIpAddresses(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            List<String> result = new ArrayList<String>();
            List<IpAddressSnmpEntry> entries = SnmpUtil.getIpAddressSnmpEntries(snmp, ipAdEntAddr);
            for (IpAddressSnmpEntry entry : entries) {
                String ip = entry.getIpAddress();
                result.add(ip);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized List<String> getIpAddresses() throws IOException, AbortedException {
        return getIpAddresses(this.snmp);
    }

    public synchronized void setIpAddresses(Device device) throws IOException, AbortedException {
        try {
            List<String> result = new ArrayList<String>();
            List<IpAddressSnmpEntry> entries = SnmpUtil.getIpAddressSnmpEntries(snmp, ipAdEntAddr);
            for (IpAddressSnmpEntry entry : entries) {
                String ip = entry.getIpAddress();
                result.add(ip);
            }
            String[] ipAddresses = result.toArray(new String[0]);
            device.setIpAddresses(ipAddresses);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized void setIpAddressWithIfIndex(Device device) throws IOException, AbortedException {
        setIpAddressWithIfIndex(snmp, device);
    }

    public static synchronized void setIpAddressWithIfIndex(SnmpAccess snmp,
                                                            Device device) throws IOException, AbortedException {
        InterfaceMibImpl ifmib = new InterfaceMibImpl(snmp);

        try {
            List<StringSnmpEntry> ifNames = SnmpUtil.getStringSnmpEntries(snmp, InterfaceMib.ifName);
            Map<Integer, String> ifIndexToIfNameMap = new HashMap<Integer, String>();
            for (StringSnmpEntry entry : ifNames) {
                ifIndexToIfNameMap.put(entry.getLastOIDIndex().intValue(), entry.getValue());
            }
            MibTable table = new MibTable(snmp, "ipAddressTable", Mib2.ipAddressTable);
            table.addColumn("1", "ipAdEntAddr");
            table.addColumn("2", "ipAdEntIfIndex");
            table.addColumn("3", "ipAdEntNetMask");
            table.walk();
            for (TableRow row : table.getRows()) {
                String ip = row.getColumnValue("1", SnmpHelper.ipAddressEntryBuilder).getIpAddress();
                int ifIndex = row.getColumnValue("2", SnmpHelper.intEntryBuilder).intValue();
                int maskLen = row.getColumnValue("3", SnmpHelper.ipAddressEntryBuilder).getMaskLength();
                String ifName = ifIndexToIfNameMap.get(Integer.valueOf(ifIndex));
                log.debug("@ create ip " + ip + "/" + maskLen + " for port[" + ifIndex + "=" + ifName + "]");
                InetAddress addr = InetAddress.getByName(ip);
                CidrAddress addressMask = new CidrAddress(addr, maskLen);

                Port port = getPortByIfName(device, ifName);
                if (port == null) {
                    port = getPortByIfIndex(device, ifIndex);
                }
                VrfInstance vrf = null;
                if (MplsVlanDevice.class.isInstance(device) && port != null) {
                    vrf = ((MplsVlanDevice) device).getPortRelatedVrf(port);
                }
                if (vrf != null) {
                    CidrAddress addr_ = vrf.getVpnIpAddress(port);
                    if (addr_ != null) {
                        log.debug("* assigned.");
                        continue;
                    }
                } else if (port != null) {
                    Device parentDevice = port.getDevice();
                    Set<CidrAddress> addresses = parentDevice.getIpAddresses(port);
                    if (addresses.contains(addressMask)) {
                        log.debug("* assigned.");
                        continue;
                    }
                }

                if (port == null) {
                    port = createDummyPort(device, ifIndex, ifmib);
                }
                if (port.getIfName() != null && !port.getIfName().equals(ifName)) {
                    log.warn("ifName mismatch: port.ifName=" + port.getIfName() + ", ip.ifName=" + ifName);
                }
                if (vrf != null) {
                    vrf.addVpnIpAddress(port, addressMask);
                    log.debug("@ add vrf ip " + addressMask.toString()
                            + " to vrf[:" + vrf.getIfName() + ":" + port.getRawIfIndex() + ":" + port.getIfName() + "]");
                } else {
                    port.getDevice().addIpAddressToPort(addressMask, port);
                    log.debug("@ add ip " + addressMask.toString()
                            + " to port[" + port.getRawIfIndex() + " :" + port.getFullyQualifiedName() + "]");
                }
            }
        } catch (UnknownHostException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private static Port getPortByIfName(Device device, String ifName) {
        Port port = device.getPortByIfName(ifName);
        if (port == null && device.getVirtualDevices().size() > 0) {
            for (Device virtualDevice : device.getVirtualDevices()) {
                port = virtualDevice.getPortByIfName(ifName);
                if (port != null) {
                    break;
                }
            }
        }
        return port;
    }

    private static Port getPortByIfIndex(Device device, int ifIndex) {
        Port port = device.getPortByIfIndex(ifIndex);
        if (port == null && device.getVirtualDevices().size() > 0) {
            for (Device virtualDevice : device.getVirtualDevices()) {
                port = virtualDevice.getPortByIfIndex(ifIndex);
                if (port != null) {
                    break;
                }
            }
        }
        return port;
    }

    private static Port createDummyPort(Device device, int ifIndex, InterfaceMibImpl ifmib)
            throws IOException, AbortedException {
        Port port = new GenericLogicalPort();
        port.initDevice(device);
        port.initIfIndex(ifIndex);

        boolean set = false;
        try {
            ifmib.setIfName(port);
            set = true;
        } catch (IOException e) {
            log.warn("ifXTable is not supported.", e);
        }
        ifmib.setIfDescription(port);
        if (!set) {
            port.initIfName(port.getIfDescr());
        }
        ifmib.setIfType(port);
        ifmib.setIfOperStatus(port);
        ifmib.setIfAdminStatus(port);
        ifmib.setIfAdminStatus(port);
        log.debug("@ complemented port " + port.getIfIndex() + " ifName:" + port.getIfName());
        return port;
    }

    public synchronized void setDefaultGateway(Device device) throws IOException, AbortedException {
        try {
            List<IpAddressSnmpEntry> entries = SnmpUtil.getIpAddressSnmpEntries(snmp, netDefaultGateway);
            for (IpAddressSnmpEntry entry : entries) {
                String defaultGateway = entry.getIpAddress();
                device.setGatewayAddress(defaultGateway);
                break;
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized void setSnmpTrapReceiverAddress(Device device) throws IOException, AbortedException {
        try {
            List<ByteSnmpEntry> entries =
                    SnmpUtil.getByteSnmpEntries(snmp, snmpTargetAddrTAddress);
            Set<String> trapReceivers = new HashSet<String>();
            for (ByteSnmpEntry entry : entries) {
                byte[] value = entry.value;
                if (value.length > 4 && value.length < 16) {
                    byte[] newbytes = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        newbytes[i] = value[i];
                    }
                    value = newbytes;
                }

                String trapTarget = SnmpUtil.getIpAddressByHexaBytes(value);
                trapReceivers.add(trapTarget);
            }
            device.setTrapReceiverAddresses(trapReceivers.toArray(new String[0]));
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized String[] getTrapReceiverAddresses() throws IOException, AbortedException {
        try {
            List<String> traphosts = SnmpUtil.getStringByWalk(snmp, Mib2.trapDestAddress);
            List<String> result = new ArrayList<String>();
            for (String traphost : traphosts) {
                String[] elem = traphost.split("\\.");
                assert elem.length >= 4;
                String ipaddress = elem[0] + "." + elem[1] + "." + elem[2] + "." + elem[3];
                result.add(ipaddress);
            }
            if (result.size() == 0) {
                return null;
            }

            return result.toArray(new String[result.size() - 1]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private static synchronized String packIpAddressFromOidSuffix(BigInteger[] suffix) throws IOException {
        if (suffix.length != 4 && suffix.length != 16) {
            throw new IllegalArgumentException("unknown suffix length: " + suffix.length);
        }

        byte[] result = new byte[suffix.length];
        for (int i = 0; i < suffix.length; i++) {
            int octet = suffix[i].intValue();
            result[i] = (byte) (octet & 0xff);
        }
        InetAddress addr = InetAddress.getByAddress(result);
        return addr.getHostAddress();
    }

    private static synchronized String getValueWhenIpAddressInOidMatch(List<IpAddressSnmpEntry> entries,
                                                                       String targetIpAddress) throws IOException {

        byte[] addr = VossMiscUtility.getByteFormIpAddress(targetIpAddress);
        InetAddress inetAddress = InetAddress.getByAddress(addr);
        String canonicalized = inetAddress.getHostAddress();

        for (IpAddressSnmpEntry entry : entries) {
            String ip = packIpAddressFromOidSuffix(entry.oidSuffix);
            if (ip.equals(canonicalized)) {
                return entry.getIpAddress();
            }
        }
        return null;
    }

    public static synchronized String getSubnetMaskByIpAddress(SnmpAccess snmp, String ipAddress)
            throws IOException, AbortedException {
        try {
            String oid = Mib2.ipAdEntNetMask;
            List<IpAddressSnmpEntry> entries
                    = SnmpUtil.getIpAddressSnmpEntries(snmp, oid);
            String netmask = getValueWhenIpAddressInOidMatch(entries, ipAddress);
            return netmask;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized String getSubnetMaskByIpAddress(String ipAddress) throws IOException, AbortedException {
        try {
            String oid = Mib2.ipAdEntNetMask;
            List<IpAddressSnmpEntry> entries
                    = SnmpUtil.getIpAddressSnmpEntries(snmp, oid);
            String netmask = getValueWhenIpAddressInOidMatch(entries, ipAddress);
            return netmask;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized String getGatewayAddress() throws IOException, AbortedException {
        try {
            List<IpAddressSnmpEntry> entries
                    = SnmpUtil.getIpAddressSnmpEntries(snmp, Mib2.ipRouteDest);
            String gatewayAddress = getValueWhenIpAddressInOidMatch(entries, "0.0.0.0");
            return gatewayAddress;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public synchronized void setGatewayAddress(Device device, String route) throws IOException, AbortedException {
        try {
            List<IpAddressSnmpEntry> entries
                    = SnmpUtil.getIpAddressSnmpEntries(snmp, Mib2.ipRouteDest);
            String gatewayAddress = getValueWhenIpAddressInOidMatch(entries, route);
            device.setGatewayAddress(gatewayAddress);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

}