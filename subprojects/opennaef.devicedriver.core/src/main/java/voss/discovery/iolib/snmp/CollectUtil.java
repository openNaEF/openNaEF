package voss.discovery.iolib.snmp;


import net.snmp.OidTLV;
import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class CollectUtil {

    public static final String SYS_OBJECT_ID = ".1.3.6.1.2.1.1.2";

    public static String[] getPhysicalAddresses(SnmpAccess snmpAccess)
            throws SocketTimeoutException, SocketException, AbortedException, IOException,
            RepeatedOidException, SnmpResponseException {
        final List<String> physicalAddresses = new ArrayList<String>();
        List<ByteSnmpEntry> mibs = SnmpUtil.getByteSnmpEntries(snmpAccess, InterfaceMib.ifPhysAddress);
        for (ByteSnmpEntry mib : mibs) {
            byte[] rawPhysicalAddress = mib.getValue();
            String physicalAddress =
                    SnmpUtil.getPhysicalAddressString(rawPhysicalAddress);
            if (physicalAddress.equals("")
                    || physicalAddress.equals("00:00:00:00:00:00")) {
                continue;
            }
            physicalAddresses.add(physicalAddress);
        }
        return physicalAddresses.toArray(new String[0]);
    }

    public static String getObjectId(SnmpAccess snmpAccess)
            throws SocketTimeoutException, SocketException, AbortedException, IOException,
            SnmpResponseException {
        VarBind objectIDVarbind = snmpAccess.getNextChild(SYS_OBJECT_ID);
        OidTLV objectIdOid = (OidTLV) objectIDVarbind.getValue();
        return objectIdOid.getOidString();
    }

    public static PortSpeedValue.Admin getAdminSpeed(BigInteger speed) {
        return (PortSpeedValue.Admin)
                getSpeed(speed, CollectUtil.PORT_SPEED_TYPE_ADMIN);
    }

    public static PortSpeedValue.Oper getOperSpeed(BigInteger speed) {
        return (PortSpeedValue.Oper)
                getSpeed(speed, CollectUtil.PORT_SPEED_TYPE_OPER);
    }


    private static PortSpeedValue getSpeed(BigInteger speed, boolean portSpeedType) {
        String value = speed.toString();
        return value.equals("4000000")
                ? CollectUtil.createPortSpeed(4 * 1000 * 1000, "4M", portSpeedType)
                : value.equals("10000000")
                ? CollectUtil.createPortSpeed(10 * 1000 * 1000, "10M", portSpeedType)
                : value.equals("16000000")
                ? CollectUtil.createPortSpeed(16 * 1000 * 1000, "16M", portSpeedType)
                : value.equals("45000000")
                ? CollectUtil.createPortSpeed(45 * 1000 * 1000, "45M", portSpeedType)
                : value.equals("64000000")
                ? CollectUtil.createPortSpeed(64 * 1000 * 1000, "64M", portSpeedType)
                : value.equals("100000000")
                ? CollectUtil.createPortSpeed(100 * 1000 * 1000, "100M", portSpeedType)
                : value.equals("155000000")
                ? CollectUtil.createPortSpeed(155 * 1000 * 1000, "155M", portSpeedType)
                : value.equals("400000000")
                ? CollectUtil.createPortSpeed(400 * 1000 * 1000, "400M", portSpeedType)
                : value.equals("622000000")
                ? CollectUtil.createPortSpeed(622 * 1000 * 1000, "622M", portSpeedType)
                : value.equals("1000000000")
                ? CollectUtil.createPortSpeed(1000 * 1000 * 1000, "1000M", portSpeedType)
                : value.equals("4294967295")
                ? CollectUtil.createPortSpeed(10L * 1000L * 1000L * 1000L, "10G", portSpeedType)
                : value.equals("1544000")
                ? CollectUtil.createPortSpeed((long) (1.544 * 1000 * 1000), "1.544M", portSpeedType)
                : value.equals("2000000")
                ? CollectUtil.createPortSpeed(2 * 1000 * 1000, "2M", portSpeedType)
                : value.equals("2048000")
                ? CollectUtil.createPortSpeed((long) (2.048 * 1000 * 1000), "2.048M", portSpeedType)
                : value.equals("64000")
                ? CollectUtil.createPortSpeed(64 * 1000, "64k", portSpeedType)
                : CollectUtil.createPortSpeed(speed.longValue(), value, portSpeedType);
    }

    public static final boolean PORT_SPEED_TYPE_ADMIN = true;
    public static final boolean PORT_SPEED_TYPE_OPER = false;

    public static PortSpeedValue createPortSpeed(long speed, String remarks, boolean type) {
        if (CollectUtil.PORT_SPEED_TYPE_ADMIN == type) {
            return new PortSpeedValue.Admin(speed, remarks);
        }
        if (CollectUtil.PORT_SPEED_TYPE_OPER == type) {
            return new PortSpeedValue.Oper(speed, remarks);
        }
        throw new RuntimeException();
    }

}