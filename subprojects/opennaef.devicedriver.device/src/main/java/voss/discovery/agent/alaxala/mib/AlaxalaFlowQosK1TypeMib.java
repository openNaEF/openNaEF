package voss.discovery.agent.alaxala.mib;

import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;

import java.io.IOException;

public interface AlaxalaFlowQosK1TypeMib {

    public final static String IFINDEX_TYPE_UNKNOWN = "UNKNOWN";
    public final static String IFINDEX_TYPE_PORT = "Port";
    public final static String IFINDEX_TYPE_IF = "IF";
    public final static String[] IFINDEX_TYPES =
            {IFINDEX_TYPE_UNKNOWN, IFINDEX_TYPE_PORT, IFINDEX_TYPE_IF};

    public final static String alaxalaBaseOid = Mib2.Snmpv2_SMI_enterprises
            + ".21839";

    public final static String alaxalaFlowQosMib = alaxalaBaseOid
            + ".2.2.1.8.5";

    public final static String alaxalaFlowQosStatsMib = alaxalaBaseOid
            + ".2.2.1.8.6";

    public final static String axsFlowQosStatsInEntry = alaxalaFlowQosStatsMib
            + ".1.1";

    public final static String axsFlowQosStatsInPremiumEntry = alaxalaFlowQosStatsMib
            + ".2.1";

    public final static String axsFlowQosStatsOutEntry = alaxalaFlowQosStatsMib
            + ".3.1";

    public final static String axsFlowQosStatsOutPremiumEntry = alaxalaFlowQosStatsMib
            + ".4.1";

    public abstract String getFlowQosStatsInListName(String tableEntryKey);

    public abstract String getFlowQosStatsInPremListName(String tableEntryKey);

    public abstract String getFlowQosStatsOutListName(String tableEntryKey);

    public abstract String getFlowQosStatsOutPremListName(String tableEntryKey);

    public final static String axsFlowQosStatsInHitsPackets = axsFlowQosStatsInEntry
            + ".4";

    public abstract long getFlowQosStatsInHitsPackets(SnmpClient client,
                                                      String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInHitsPackets(SnmpClient client)
            throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInMaxOverPackets = axsFlowQosStatsInEntry
            + ".5";

    public abstract long getFlowQosStatsInMaxOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInMaxOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInMaxUnderPackets = axsFlowQosStatsInEntry
            + ".6";

    public abstract long getFlowQosStatsInMaxUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInMaxUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInMinOverPackets = axsFlowQosStatsInEntry
            + ".7";

    public abstract long getFlowQosStatsInMinOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInMinOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInMinUnderPackets = axsFlowQosStatsInEntry
            + ".8";

    public abstract long getFlowQosStatsInInMinUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInInMinUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInPremMaxOverPackets = axsFlowQosStatsInPremiumEntry
            + ".4";

    public abstract long getFlowQosStatsInPremMaxOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInPremMaxOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInPremMaxUnderPackets = axsFlowQosStatsInPremiumEntry
            + ".5";

    public abstract long getFlowQosStatsInPremMaxUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInPremMaxUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInPremMinOverPackets = axsFlowQosStatsInPremiumEntry
            + ".6";

    public abstract long getFlowQosStatsInPremMinOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInPremMinOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsInPremMinUnderPackets = axsFlowQosStatsInPremiumEntry
            + ".7";

    public abstract long getFlowQosStatsInInPremMinUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsInInPremMinUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;


    public final static String axsFlowQosStatsOutHitsPackets = axsFlowQosStatsOutEntry
            + ".4";

    public abstract long getFlowQosStatsOutHitsPackets(SnmpClient client,
                                                       String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutHitsPackets(SnmpClient client)
            throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutMaxOverPackets = axsFlowQosStatsOutEntry
            + ".5";

    public abstract long getFlowQosStatsOutMaxOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutMaxOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutMaxUnderPackets = axsFlowQosStatsOutEntry
            + ".6";

    public abstract long getFlowQosStatsOutMaxUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutMaxUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutMinOverPackets = axsFlowQosStatsOutEntry
            + ".7";

    public abstract long getFlowQosStatsOutMinOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutMinOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutMinUnderPackets = axsFlowQosStatsOutEntry
            + ".8";

    public abstract long getFlowQosStatsOutMinUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutMinUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutPremMaxOverPackets = axsFlowQosStatsOutPremiumEntry
            + ".4";

    public abstract long getFlowQosStatsOutPremMaxOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutPremMaxOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutPremMaxUnderPackets = axsFlowQosStatsOutPremiumEntry
            + ".5";

    public abstract long getFlowQosStatsOutPremMaxUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutPremMaxUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutPremMinOverPackets = axsFlowQosStatsOutPremiumEntry
            + ".6";

    public abstract long getFlowQosStatsOutPremMinOverPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutPremMinOverPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

    public final static String axsFlowQosStatsOutPremMinUnderPackets = axsFlowQosStatsOutPremiumEntry
            + ".7";

    public abstract long getFlowQosStatsOutPremMinUnderPackets(
            SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException;

    public abstract long getFlowQosStatsOutPremMinUnderPackets(
            SnmpClient client) throws SnmpResponseException, IOException;

}