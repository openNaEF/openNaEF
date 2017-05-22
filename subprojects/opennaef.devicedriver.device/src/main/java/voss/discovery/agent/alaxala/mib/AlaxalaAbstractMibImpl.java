package voss.discovery.agent.alaxala.mib;

import net.snmp.*;
import voss.discovery.iolib.snmp.SnmpEntry;

import java.io.IOException;

public abstract class AlaxalaAbstractMibImpl {
    protected final String qosFlowEntryKey;

    public AlaxalaAbstractMibImpl(String qosFlowEntryKey) {
        checkKey(qosFlowEntryKey);
        this.qosFlowEntryKey = qosFlowEntryKey;
    }

    public AlaxalaAbstractMibImpl() {
        this.qosFlowEntryKey = null;
    }

    protected void assertNotNull(Object target) {
        if (target == null) {
            throw new IllegalArgumentException();
        }
    }

    protected String getStringValue(SnmpClient client, String oid) {
        assertNotNull(client);
        assertNotNull(oid);

        try {
            VarBind varbind = client.snmpGet(OidTLV.getInstance(oid));
            if (varbind == null) {
                throw new IllegalStateException("No such Mib.");
            }
            String value = varbind.getValueAsString();
            return value;
        } catch (SnmpResponseException sre) {
            sre.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected abstract void checkKey(String key);

    protected long getCounterValue(SnmpClient client, String oid) throws SnmpResponseException, IOException {
        assertNotNull(client);
        assertNotNull(oid);

        OidTLV targetOid = OidTLV.getInstance(oid);
        SnmpResponse.Get responses = client.snmpGet(new OidTLV[]{targetOid});
        VarBind varbind = responses.getResponseVarBind(targetOid);
        if (varbind == null) {
            throw new IllegalStateException("No such mib: " + oid);
        }
        return new TypeCounter64(oid, varbind).getValueAsLong();

    }

    @SuppressWarnings("serial")
    public class TypeCounter64 extends SnmpEntry {
        public TypeCounter64(String baseOid, VarBind varbind) {
            super(baseOid, varbind);
        }

        public long getValueAsLong() {
            return getValueAsBigInteger().longValue();
        }
    }
}