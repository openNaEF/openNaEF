package voss.discovery.agent.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.VossIfType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.Device;
import voss.model.PhysicalPort;
import voss.model.Port;

import java.io.IOException;
import java.util.List;

public class MauMib {
    private final static Logger log = LoggerFactory.getLogger(MauMib.class);

    public static final String ifMauAutoNegAdminStatus_OID = ".1.3.6.1.2.1.26.5.1.1.1";
    public static final String ifMauAutoNegAdminStatus_SYMBOL = ".iso.org.dod.internet.mgmt.mib-2" +
            ".snmpDot3MauMgt.dot3IfMauAutoNegGroup.ifMauAutoNegTable.ifMauAutoNegEntry" +
            ".ifMauAutoNegAdminStatus";

    public static final String ifMauType_OID = ".1.3.6.1.2.1.26.2.1.1.3";
    public static final String ifMauType_SYMBOL =
            "snmpDot3MauMgt.dot3IfMauBasicGroup.ifMauTable.ifMauEntry.ifMauType";

    public MauMib(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    private final SnmpAccess snmp;

    public boolean isAutoNegoEnabled(Port port) throws IOException, AbortedException {
        try {
            int result = SnmpUtil.getInteger(snmp, ifMauAutoNegAdminStatus_OID + "." + port.getIfIndex() + ".1");
            if (result == 1) {
                return true;
            } else {
                return false;
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public void setMauTypes(Device device) throws IOException, AbortedException {
        try {
            List<SnmpEntry> entries = SnmpUtil.getSnmpEntries(snmp, ifMauType_OID);
            for (SnmpEntry entry : entries) {
                int ifindex = entry.oidSuffix[0].intValue();
                Port port = device.getPortByIfIndex(ifindex);
                if (port == null) {
                    continue;
                }
                if (port instanceof PhysicalPort) {
                    String oid = entry.getVarBind().getValueAsString();
                    setMauType((PhysicalPort) port, oid);
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void setMauType(PhysicalPort port) throws IOException, AbortedException {
        try {
            String oid = SnmpUtil.getString(snmp, ifMauType_OID + "." + port.getIfIndex() + ".1");
            setMauType(port, oid);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    private void setMauType(PhysicalPort port, String oid) {
        String mauType = getMauTypeOidString(oid).getValue();
        port.setPortTypeName(mauType);
        log.debug("@ set port " + port.getIfIndex() + " portType " + mauType);
    }

    public String getMauType(Port port) throws IOException, AbortedException {
        try {
            String oid = SnmpUtil.getOID(snmp, ifMauType_OID);
            String mauType = getMauTypeOidString(oid).getValue();
            return mauType;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private static VossIfType getMauTypeOidString(String oid) {
        log.debug("getMauTypeOidString():oid=" + oid);
        if (oid == null) {
            return VossIfType.UNDEFINED;
        }
        return oid.equals(".1.3.6.1.2.1.26.4.1")
                ? VossIfType.E10AUI
                : oid.equals(".1.3.6.1.2.1.26.4.2")
                ? VossIfType.E10BASE5
                : oid.equals(".1.3.6.1.2.1.26.4.3")
                ? VossIfType.E10FOIRL
                : oid.equals(".1.3.6.1.2.1.26.4.4")
                ? VossIfType.E10BASE2
                : oid.equals(".1.3.6.1.2.1.26.4.5")
                ? VossIfType.E10BASET
                : oid.equals(".1.3.6.1.2.1.26.4.6")
                ? VossIfType.E10BASEFP
                : oid.equals(".1.3.6.1.2.1.26.4.7")
                ? VossIfType.E10BASEFB
                : oid.equals(".1.3.6.1.2.1.26.4.8")
                ? VossIfType.E10BASEFL
                : oid.equals(".1.3.6.1.2.1.26.4.9")
                ? VossIfType.E10BROAD36
                : oid.equals(".1.3.6.1.2.1.26.4.10")
                ? VossIfType.E10BASET
                : oid.equals(".1.3.6.1.2.1.26.4.11")
                ? VossIfType.E10BASET
                : oid.equals(".1.3.6.1.2.1.26.4.12")
                ? VossIfType.E10BASEFL
                : oid.equals(".1.3.6.1.2.1.26.4.13")
                ? VossIfType.E10BASEFL
                : oid.equals(".1.3.6.1.2.1.26.4.14")
                ? VossIfType.E100BASET4
                : oid.equals(".1.3.6.1.2.1.26.4.15")
                ? VossIfType.E100BASETX
                : oid.equals(".1.3.6.1.2.1.26.4.16")
                ? VossIfType.E100BASETX
                : oid.equals(".1.3.6.1.2.1.26.4.17")
                ? VossIfType.E100BASEFX
                : oid.equals(".1.3.6.1.2.1.26.4.18")
                ? VossIfType.E100BASEFX
                : oid.equals(".1.3.6.1.2.1.26.4.19")
                ? VossIfType.E100BASET2
                : oid.equals(".1.3.6.1.2.1.26.4.20")
                ? VossIfType.E100BASET2
                : oid.equals(".1.3.6.1.2.1.26.4.21")
                ? VossIfType.E1000BASEX
                : oid.equals(".1.3.6.1.2.1.26.4.22")
                ? VossIfType.E1000BASEX
                : oid.equals(".1.3.6.1.2.1.26.4.23")
                ? VossIfType.E1000BASELX
                : oid.equals(".1.3.6.1.2.1.26.4.24")
                ? VossIfType.E1000BASELX
                : oid.equals(".1.3.6.1.2.1.26.4.25")
                ? VossIfType.E1000BASESX
                : oid.equals(".1.3.6.1.2.1.26.4.26")
                ? VossIfType.E1000BASESX
                : oid.equals(".1.3.6.1.2.1.26.4.27")
                ? VossIfType.E1000BASECX
                : oid.equals(".1.3.6.1.2.1.26.4.28")
                ? VossIfType.E1000BASECX
                : oid.equals(".1.3.6.1.2.1.26.4.29")
                ? VossIfType.E1000BASET
                : oid.equals(".1.3.6.1.2.1.26.4.30")
                ? VossIfType.E1000BASET
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.1")
                ? VossIfType.E1000BASESX
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.2")
                ? VossIfType.E1000BASELX
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.3")
                ? VossIfType.E1000BASECX
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.4")
                ? VossIfType.E1000BASESX
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.5")
                ? VossIfType.E1000BASELX
                : oid.equals(".1.3.6.1.4.1.1916.3.1.1.6")
                ? VossIfType.E1000BASECX
                : oid.equals(".0")
                ? VossIfType.NOCONNECTOR
                : VossIfType.OTHER;
    }
}