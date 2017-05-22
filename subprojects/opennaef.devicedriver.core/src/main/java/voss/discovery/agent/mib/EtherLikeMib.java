package voss.discovery.agent.mib;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.model.EthernetPort;

public class EtherLikeMib {

    @SuppressWarnings("serial")
    public static class Dot3StatsIndexEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.10.7.2.1.1";
        public static final String SYMBOL =
                "transmission.dot3.dot3StatsTable.dot3StatsEntry.dot3StatsIndex";

        public Dot3StatsIndexEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIfIndex() {
            return getValueAsBigInteger().intValue();
        }
    }

    @SuppressWarnings("serial")
    public static class Dot3StatsDuplexStatusEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.10.7.2.1.19";
        public static final String SYMBOL =
                "transmission.dot3.dot3StatsTable.dot3StatsEntry.dot3StatsDuplexStatus";

        public Dot3StatsDuplexStatusEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIfIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public EthernetPort.Duplex getDuplex() {
            int duplexValue = getValueAsBigInteger().intValue();
            return duplexValue == 1 ? null
                    : duplexValue == 2 ? EthernetPort.Duplex.HALF
                    : duplexValue == 3 ? EthernetPort.Duplex.FULL
                    : null;
        }
    }
}