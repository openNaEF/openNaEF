package voss.discovery.agent.alcatel.mib;

import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

public class TimetraSystemMibImpl implements TimetraSystemMib {

    private final SnmpAccess snmp;
    public TimetraSystemMibImpl(Alcatel7710SRDiscovery discovery) {
        this.snmp = discovery.getSnmpAccess();
    }

    public String getSwVersion() {
        String swVersion;
        try {
            int swMajorVersion = SnmpUtil.getInteger(snmp, sgiSwMajorVersion + ".0");
            int swMinorVersion = SnmpUtil.getInteger(snmp, sgiSwMinorVersion + ".0");
            String swVersionModifier = SnmpUtil.getString(snmp, sgiSwVersionModifier + ".0");
            swVersion = swMajorVersion + "." + swMinorVersion + "." + swVersionModifier;
        } catch (Exception e) {
            swVersion = "unknown";
        }

        return swVersion;
    }
}