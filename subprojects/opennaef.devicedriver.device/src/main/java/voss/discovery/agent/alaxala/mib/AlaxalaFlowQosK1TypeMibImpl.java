package voss.discovery.agent.alaxala.mib;

import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;

import java.io.IOException;

public class AlaxalaFlowQosK1TypeMibImpl extends AlaxalaAbstractMibImpl
        implements AlaxalaFlowQosK1TypeMib {
    public AlaxalaFlowQosK1TypeMibImpl(String qosFlowEntryKey) {
        super(qosFlowEntryKey);
    }

    public AlaxalaFlowQosK1TypeMibImpl() {
        super();
    }

    public String getFlowQosStatsInListName(String key) {
        return makeQosFlowListName(key, "in", "");
    }

    public String getFlowQosStatsInPremListName(String key) {
        return makeQosFlowListName(key, "in", "[Important]");
    }

    public String getFlowQosStatsOutListName(String key) {
        return makeQosFlowListName(key, "out", "");
    }

    public String getFlowQosStatsOutPremListName(String key) {
        return makeQosFlowListName(key, "out", "[Important]");
    }

    public long getFlowQosStatsInHitsPackets(SnmpClient client,
                                             String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInHitsPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInHitsPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInHitsPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInMaxOverPackets(SnmpClient client,
                                                String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInMaxOverPackets + tableEntryKey;
        long name = getCounterValue(client, oid);
        return name;
    }

    public long getFlowQosStatsInMaxOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInMaxOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInMaxUnderPackets(SnmpClient client,
                                                 String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInMaxUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInMaxUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInMaxUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInMinOverPackets(SnmpClient client,
                                                String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInMinOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInMinOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInMinOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInInMinUnderPackets(SnmpClient client,
                                                   String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInMinUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInInMinUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInInMinUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInPremMaxOverPackets(SnmpClient client,
                                                    String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInPremMaxOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInPremMaxOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInPremMaxOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInPremMaxUnderPackets(SnmpClient client,
                                                     String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInPremMaxUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInPremMaxUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInPremMaxUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInPremMinOverPackets(SnmpClient client,
                                                    String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInPremMinOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInPremMinOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInPremMinOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsInInPremMinUnderPackets(SnmpClient client,
                                                       String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsInPremMinUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsInInPremMinUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsInInPremMinUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutHitsPackets(SnmpClient client,
                                              String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutHitsPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutHitsPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutHitsPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutMaxOverPackets(SnmpClient client,
                                                 String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutMaxOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutMaxOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutMaxOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutMaxUnderPackets(SnmpClient client,
                                                  String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutMaxUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutMaxUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutMaxUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutMinOverPackets(SnmpClient client,
                                                 String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutMinOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutMinOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutMinOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutMinUnderPackets(SnmpClient client,
                                                  String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutMinUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutMinUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutMinUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutPremMaxOverPackets(SnmpClient client,
                                                     String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutPremMaxOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutPremMaxOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutPremMaxOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutPremMaxUnderPackets(SnmpClient client,
                                                      String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutPremMaxUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutPremMaxUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutPremMaxUnderPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutPremMinOverPackets(SnmpClient client,
                                                     String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutPremMinOverPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutPremMinOverPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutPremMinOverPackets(client, qosFlowEntryKey);
    }

    public long getFlowQosStatsOutPremMinUnderPackets(SnmpClient client,
                                                      String tableEntryKey) throws SnmpResponseException, IOException {
        checkKey(tableEntryKey);
        String oid = axsFlowQosStatsOutPremMinUnderPackets + tableEntryKey;
        long value = getCounterValue(client, oid);
        return value;
    }

    public long getFlowQosStatsOutPremMinUnderPackets(SnmpClient client)
            throws SnmpResponseException, IOException {
        assert qosFlowEntryKey != null;
        return getFlowQosStatsOutPremMinUnderPackets(client, qosFlowEntryKey);
    }

    protected void checkKey(String key) {
        if (!key.startsWith(".")) {
            throw new IllegalArgumentException();
        }
        String[] elem = key.split("\\.");
        if (elem.length != 4) {
            throw new IllegalArgumentException();
        }
        return;
    }

    @SuppressWarnings("unused")
    private String getIfIndexPart(String key) {
        if (!key.startsWith(".")) {
            throw new IllegalArgumentException();
        }
        String[] elem = key.split("\\.");
        if (elem.length != 4) {
            throw new IllegalArgumentException();
        }
        return elem[1];
    }

    private String getIfTypePart(String key) {
        if (!key.startsWith(".")) {
            throw new IllegalArgumentException();
        }
        String[] elem = key.split("\\.");
        if (elem.length != 4) {
            throw new IllegalArgumentException();
        }
        return elem[2];
    }

    private String getListNumberPart(String key) {
        if (!key.startsWith(".")) {
            throw new IllegalArgumentException();
        }
        String[] elem = key.split("\\.");
        if (elem.length != 4) {
            throw new IllegalArgumentException();
        }
        return elem[3];
    }

    private String makeQosFlowListName(String key, String direction, String qosClass) {
        String prefix = "qos_";
        String ifType = resolveIfIndexType(getIfTypePart(key));
        String listNumber = getListNumberPart(key);
        return prefix + "_" + direction + qosClass + "_" + ifType + "_" + listNumber;
    }

    private String resolveIfIndexType(String type) {
        try {
            int type_id = Integer.parseInt(type);
            return IFINDEX_TYPES[type_id];
        } catch (NumberFormatException nfe) {
            return IFINDEX_TYPE_UNKNOWN;
        }
    }

}