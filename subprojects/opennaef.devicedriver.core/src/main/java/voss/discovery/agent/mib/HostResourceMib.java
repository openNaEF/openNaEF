package voss.discovery.agent.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HostResourceMib {

    public static final String hrSWInstalledName_OID = ".1.3.6.1.2.1.25.6.3.1.2";
    public static final String hrSWInstalledName_SYMBOL = ".iso.org.dod.internet.mgmt.mib-2.host" +
            ".hrSWInstalled.hrSWInstalledTable.hrSWInstalledEntry.hrSWInstalledName";

    public static List<String> getHrSWInstalledName(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, hrSWInstalledName_OID);
            List<String> result = new ArrayList<String>();
            for (StringSnmpEntry entry : entries) {
                result.add(entry.getValue());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }
}