package voss.discovery.agent.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LldpMib {

    public static final String lldpXdot3LocLinkAggPortId_SYMBOL =
            ".iso.std.iso8802.ieee802dot1.ieee802dot1mibs.lldpMIB.lldpObjects" +
                    ".lldpExtensions.lldpXdot3MIB.lldpXdot3Objects.lldpXdot3LocalData" +
                    ".lldpXdot3LocLinkAggTable.lldpXdot3LocLinkAggEntry" +
                    ".lldpXdot3LocLinkAggPortId";

    public static final String lldpXdot3LocLinkAggPortId_OID =
            ".1.0.8802.1.1.2.1.5.4623.1.2.3.1.2";

    public static final String lldpLocPortId_OID = ".1.0.8802.1.1.2.1.3.7.1.3";

    public static final String lldpLocPortId_SYMBOL =
            ".iso.std.iso8802.ieee802dot1.ieee802dot1mibs.lldpMIB.lldpObjects"
                    + ".lldpLocalSystemData.lldpLocPortTable.lldpLocPortEntry"
                    + ".lldpLocPortId";

    public static final String lldpLocPortIdSubtype_OID = ".1.0.8802.1.1.2.1.3.7.1.2";
    public static final String lldpLocPortIdSubtype_SYMBOL =
            ".iso.std.iso8802.ieee802dot1.ieee802dot1mibs.lldpMIB.lldpObjects" +
                    ".lldpLocalSystemData.lldpLocPortTable.lldpLocPortEntry" +
                    ".lldpLocPortIdSubtype";

    public static enum LldpPortIdSubtype {
        IFALIAS(1),
        ENTPHYSICALALIAS(2),
        MACADDRESS(3),
        NETWORKADDRESS(4),
        IFNAME(5),
        CIRCUIT(6),
        LOCAL(7),;
        public final int id;

        private LldpPortIdSubtype(int id) {
            this.id = id;
        }

        public static LldpPortIdSubtype getByID(int id) {
            for (LldpPortIdSubtype instance : values()) {
                if (instance.id == id) {
                    return instance;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public static Map<Integer, Integer> getLldpXdot3LocLinkAggPortIds(final SnmpAccess snmp)
            throws AbortedException, IOException {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, lldpXdot3LocLinkAggPortId_OID);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                result.put(entry.oidSuffix[0].intValue(), entry.intValue());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static Map<Integer, LldpPortIdSubtype> getLldpLocPortIdSubtypes(final SnmpAccess snmp)
            throws AbortedException, IOException {
        Map<Integer, LldpPortIdSubtype> result = new HashMap<Integer, LldpPortIdSubtype>();
        try {
            List<SnmpUtil.IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, lldpLocPortIdSubtype_OID);
            for (SnmpUtil.IntSnmpEntry entry : entries) {
                result.put(entry.oidSuffix[0].intValue(), LldpPortIdSubtype.getByID(entry.intValue()));
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static Map<Integer, String> getLldpLocPortId(final SnmpAccess snmp)
            throws AbortedException, IOException {
        Map<Integer, String> result = new HashMap<Integer, String>();
        try {
            List<SnmpUtil.StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, lldpLocPortId_OID);
            for (SnmpUtil.StringSnmpEntry entry : entries) {
                result.put(entry.oidSuffix[0].intValue(), entry.getValue());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

}