package voss.discovery.agent.alaxala.mib;

import net.snmp.RealSnmpClientFactory;
import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlaxalaQosFlowK0TypeMibImpl extends AlaxalaAbstractMibImpl
        implements AlaxalaQosFlowK0TypeMib {
    public AlaxalaQosFlowK0TypeMibImpl(String qosFlowEntryKey) {
        super(qosFlowEntryKey);
    }

    public AlaxalaQosFlowK0TypeMibImpl() {
        super();
    }

    public String getQosFlowStatsInListName(SnmpClient client, String tableEntryKey) {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInListName + tableEntryKey;
        String name = getStringValue(client, oid);
        return name;
    }

    public String getQosFlowStatsInListName(SnmpClient client) {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInListName(client, qosFlowEntryKey);
    }

    public long getQosFlowStatsInMatchedPackets(SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInMatchedPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getQosFlowStatsInMatchedPackets(SnmpClient client) throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInMatchedPackets(client, qosFlowEntryKey);
    }

    public long getQosFlowStatsInMatchedPacketsMinUnder(SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInMatchedPacketsMinUnder + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getQosFlowStatsInMatchedPacketsMinUnder(SnmpClient client) throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInMatchedPacketsMinUnder(client, qosFlowEntryKey);
    }

    public long getQosFlowStatsInMatchedPacketsMinOver(SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInMatchedPacketsMinOver + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getQosFlowStatsInMatchedPacketsMinOver(SnmpClient client) throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInMatchedPacketsMinOver(client, qosFlowEntryKey);
    }

    public long getQosFlowStatsInMatchedPacketsMaxUnder(SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInMatchedPacketsMaxUnder + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getQosFlowStatsInMatchedPacketsMaxUnder(SnmpClient client) throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInMatchedPacketsMaxUnder(client, qosFlowEntryKey);
    }

    public long getQosFlowStatsInMatchedPacketsMaxOver(SnmpClient client, String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsQosFlowStatsInMatchedPacketsMaxOver + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getQosFlowStatsInMatchedPacketsMaxOver(SnmpClient client) throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getQosFlowStatsInMatchedPacketsMaxOver(client, qosFlowEntryKey);
    }

    protected void checkKey(String key) {
        if (!key.startsWith(".")) {
            throw new IllegalArgumentException();
        }
        String[] elem = key.split("\\.");
        if (elem.length != 5) {
            throw new IllegalArgumentException();
        }
        return;
    }


    public static void main(String[] args) {
        String oid = ".1.3.6.1.2.1.2.2.1.10.1";
        AlaxalaQosFlowK0TypeMibImpl mib = new AlaxalaQosFlowK0TypeMibImpl(".28.1.0.10");
        try {
            SnmpClient client = new RealSnmpClientFactory().createSnmpClient(
                    InetAddress.getByName("172.20.0.115"), "public".getBytes());
            long result = mib.getCounterValue(client, oid);
            String header = SimpleDateFormat.getInstance().format(new Date());
            System.out.println(header + ":ifInOctet=" + result);

            header = SimpleDateFormat.getInstance().format(new Date());
            try {
                String name = mib.getQosFlowStatsInListName(client);
                System.out.println(header + ":getQosFlowStatsInListName=" + name);
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
            }

            header = SimpleDateFormat.getInstance().format(new Date());
            try {
                long matched = mib.getQosFlowStatsInMatchedPackets(client);
                System.out.println(header + ":getQosFlowStatsInMatchedPackets=" + matched);
            } catch (IllegalStateException e) {
                System.out.println(header + ":getQosFlowStatsInMatchedPackets=" + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(1);
    }

}