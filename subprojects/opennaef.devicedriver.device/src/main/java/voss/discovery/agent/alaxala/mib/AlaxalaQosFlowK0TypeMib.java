package voss.discovery.agent.alaxala.mib;

import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;

import java.io.IOException;

public interface AlaxalaQosFlowK0TypeMib {

    public final static String alaxalaBaseOid = Mib2.Snmpv2_SMI_enterprises
            + ".21839";

    public final static String alaxalaQosFlowMib = alaxalaBaseOid
            + ".2.2.1.8.11";

    public final static String axsQosFlowStatsInEntry = alaxalaQosFlowMib
            + ".1.1";

    public final static String axsQosFlowStatsInListName = axsQosFlowStatsInEntry
            + ".5";

    public abstract String getQosFlowStatsInListName(SnmpClient client,
                                                     String tableEntryKey);

    public abstract String getQosFlowStatsInListName(SnmpClient client);

    public final static String axsQosFlowStatsInMatchedPackets = axsQosFlowStatsInEntry
            + ".6";

    public abstract long getQosFlowStatsInMatchedPackets(SnmpClient client,
                                                         String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getQosFlowStatsInMatchedPackets(SnmpClient client)
            throws SnmpResponseException, IOException;

    public final static String axsQosFlowStatsInMatchedPacketsMinUnder = axsQosFlowStatsInEntry
            + ".7";

    public abstract long getQosFlowStatsInMatchedPacketsMinUnder(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getQosFlowStatsInMatchedPacketsMinUnder(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsQosFlowStatsInMatchedPacketsMinOver = axsQosFlowStatsInEntry
            + ".8";

    public abstract long getQosFlowStatsInMatchedPacketsMinOver(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getQosFlowStatsInMatchedPacketsMinOver(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsQosFlowStatsInMatchedPacketsMaxUnder = axsQosFlowStatsInEntry
            + ".9";

    public abstract long getQosFlowStatsInMatchedPacketsMaxUnder(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getQosFlowStatsInMatchedPacketsMaxUnder(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsQosFlowStatsInMatchedPacketsMaxOver = axsQosFlowStatsInEntry
            + ".10";

    public abstract long getQosFlowStatsInMatchedPacketsMaxOver(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getQosFlowStatsInMatchedPacketsMaxOver(
            SnmpClient client) throws SnmpResponseException, IOException;

}