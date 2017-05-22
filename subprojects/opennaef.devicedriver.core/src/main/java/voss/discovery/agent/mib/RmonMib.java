package voss.discovery.agent.mib;

import net.snmp.IpAddressTLV;
import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpEntry;

public class RmonMib {

    public static final String RmonMibBasePrefix = ".16";
    public static final String RmonMibBaseOid = Mib2.Snmpv2_MIB + RmonMibBasePrefix;

    public static final String NetDefaultGateway = RmonMibBaseOid + ".19.12";

    @SuppressWarnings("serial")
    public static class NetDefaultGatewayEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.16.19.12";
        public static final String SYMBOL =
                "rmon.probeConfig.netDefaultGateway";

        public NetDefaultGatewayEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public String getGatewayAddress() {
            return IpAddressTLV.getStringExpression(value);
        }
    }

}