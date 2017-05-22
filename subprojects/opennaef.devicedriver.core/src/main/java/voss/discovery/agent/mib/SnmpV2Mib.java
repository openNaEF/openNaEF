package voss.discovery.agent.mib;


import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpEntry;

import java.net.InetAddress;

public class SnmpV2Mib {

    @SuppressWarnings("serial")
    public static class SnmpTargetAddrTAddressEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.6.3.12.1.2.1.3";
        public static final String SYMBOL =
                ".iso.org.dod.internet.snmpV2.snmpModules.snmpTargetMIB.snmpTargetObjects"
                        + ".snmpTargetAddrTable.snmpTargetAddrEntry.snmpTargetAddrTAddress";

        public SnmpTargetAddrTAddressEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public String getIpAddress() {
            try {
                byte[] rawAddress =
                        new byte[]{value[0], value[1], value[2], value[3]};
                return InetAddress.getByAddress(rawAddress).getHostAddress();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}