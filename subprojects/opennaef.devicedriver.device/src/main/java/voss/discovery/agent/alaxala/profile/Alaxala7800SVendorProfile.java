package voss.discovery.agent.alaxala.profile;

import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;


public abstract class Alaxala7800SVendorProfile extends AlaxalaVendorProfile {

    @Override
    public String getIfName(SnmpAccess snmp, AlaxalaPortEntry portEntry) throws IOException, AbortedException {
        assert portEntry != null;

        return portEntry.getSlotId() + "/" + portEntry.getPortId();
    }

    @Override
    public String getAggregationName(int aggregationId) {
        return AlaxalaVendorProfileUtil.getAlaxalaLikeAggregationName(aggregationId);
    }

    @Override
    public int getAggregationIdFromIfIndex(int ifindex) {
        assert ifindex > 25000;
        return ifindex - 25000;
    }

    @Override
    public int getDefaultVlanIfIndex() {
        return 3;
    }

    @Override
    public int getPortIfIndexOffset() {
        return 100;
    }

    @Override
    public int getVlanIfIndexOffset() {
        return 20000;
    }

    public String getPhysLineTransceiverStatusOid() {
        return null;
    }
}