package voss.discovery.agent.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.model.MplsVlanDevice;
import voss.model.Port;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class OspfMibImpl {

    public static final String ospf = ".1.3.6.1.2.1.14";

    public static final String ospfIfTable = ospf + ".7";
    public static final String ospfIfEntry = ospfIfTable + ".1";
    public static final String ospfIfIpAddress = ospfIfEntry + ".1";
    public static final String ospfIfAreaId = ospfIfEntry + ".3";
    public static final String ospfIfMetricTable = ospf + ".8";
    public static final String ospfIfMetricEntry = ospfIfMetricTable + ".1";
    public static final String ospfIfMetricIpAddress = ospfIfMetricEntry + ".1";
    public static final String ospfIfMetricValue = ospfIfMetricEntry + ".4";


    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(OspfMibImpl.class);

    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public OspfMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public static void getOspfIfParameters(SnmpAccess snmp, MplsVlanDevice device) throws IOException, AbortedException {

        Map<OidKey, IpAddressSnmpEntry> ospfIfIpAddressMap =
                SnmpUtil.getWalkResult(snmp, ospfIfIpAddress, ipAddressEntryBuilder, oidKeyCreator);
        Map<OidKey, IpAddressSnmpEntry> ospfIfAreaIdMap =
                SnmpUtil.getWalkResult(snmp, ospfIfAreaId, ipAddressEntryBuilder, oidKeyCreator);

        for (OidKey key : ospfIfIpAddressMap.keySet()) {
            int ifIndex = getIfIndexByIpAddress(snmp, ospfIfIpAddressMap.get(key).getIpAddress());
            Port port = device.getPortByIfIndex(ifIndex);
            if (port != null) {
                String ifAreaId = ospfIfAreaIdMap.get(key).getIpAddress();
                port.setOspfAreaID(ifAreaId);
            }
        }

        Map<OidKey, IpAddressSnmpEntry> ospfIfMetricIpAddressMap =
                SnmpUtil.getWalkResult(snmp, ospfIfMetricIpAddress, ipAddressEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> ospfIfMetricValueMap =
                SnmpUtil.getWalkResult(snmp, ospfIfMetricValue, intEntryBuilder, oidKeyCreator);

        for (OidKey key : ospfIfMetricIpAddressMap.keySet()) {
            int ifMetricTos = key.getInt(5);
            if (ifMetricTos != 0) {
                continue;
            }
            int ifIndex = getIfIndexByIpAddress(snmp, ospfIfMetricIpAddressMap.get(key).getIpAddress());
            Port port = device.getPortByIfIndex(ifIndex);
            if (port != null) {
                int ifMetricValue = ospfIfMetricValueMap.get(key).intValue();
                port.setIgpCost(ifMetricValue);
            }
        }
    }

    public void getOspfIfParameters() throws IOException, AbortedException {
        getOspfIfParameters(snmp, device);
    }

    private static int getIfIndexByIpAddress(SnmpAccess snmp, String ipAddress) throws IOException, AbortedException {

        final String ipAdEntIfIndex = ".1.3.6.1.2.1.4.20.1.2";

        try {
            return SnmpUtil.getInteger(snmp, ipAdEntIfIndex + "." + ipAddress);
        } catch (SnmpResponseException e) {
            throw new IOException();
        } catch (NoSuchMibException e) {
            return 0;
        }
    }
}