package voss.discovery.agent.alaxala.profile;

import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;


public abstract class AlaxalaVendorProfile {
    public final static String OS_TYPE_ALAXALA = "AX(type-A)";
    public final static String OS_TYPE_CISCO_LIKE = "AX(type-C)";

    public abstract String getOsType();

    public abstract String getIfName(SnmpAccess snmp, AlaxalaPortEntry portEntry) throws IOException, AbortedException;

    public abstract String getAggregationName(int aggregationId);

    public abstract int getAggregationIdFromIfIndex(int ifindex);

    public abstract int getPortIfIndexOffset();

    public boolean isIfIndexPortIfIndex(int ifIndex) {
        return getPortIfIndexOffset() <= ifIndex
                && ifIndex < getVlanIfIndexOffset();
    }

    public abstract int getVlanIfIndexOffset();

    public boolean isIfIndexVlanIfIndex(int ifIndex) {
        if (ifIndex == getDefaultVlanIfIndex()) {
            return true;
        }

        return getVlanIfIndexOffset() < ifIndex
                && ifIndex <= (getVlanIfIndexOffset() + 4096);
    }

    public abstract int getDefaultVlanIfIndex();

    public abstract String getVendorIdOid();

    public abstract String getAlaxalaBaseOid();

    public abstract String getAlaxalaBaseVlanIdOid();

    public abstract String getAlaxalaVBBaseVlanTypeOid();

    public abstract String getAxsVBBaseVlanIfIndexOid();

    public abstract String getAxsVBBasePortIfIndexOid();

    public abstract String getVlanAndPortBindingStateOid();

    public abstract String getAxIfIndexOid();

    public abstract String getAxsIfStatsHighSpeedOid();

    public abstract String getQosFlowStatsInListName();

    public abstract String getFlowQosBaseOid();

    public abstract String getFlowQosInBaseOid();

    public abstract String getFlowQosInPremiumBaseOid();

    public abstract String getFlowQosOutBaseOid();

    public abstract String getFlowQosOutPremiumBaseOid();

    public abstract String getFlowQosStatsBaseOid();

    public abstract String getFlowQosStatsInBaseOid();

    public abstract String getFlowQosStatsInPremiumBaseOid();

    public abstract String getFlowQosStatsOutBaseOid();

    public abstract String getFlowQosStatsOutPremiumBaseOid();

    public abstract String getDeviceBaseOid();

    public abstract String getOsNameOid();

    public abstract String getOsVersionOid();

    public abstract String getNumberOfSlotOid();

    public abstract String getBoardNameOid();

    public abstract String getNumberOfPortOid();

    public abstract String getPhysLineTransceiverStatusOid();

    public abstract String getPhysLineConnectorTypeOid();

    public abstract String getModelName(SnmpAccess _snmp) throws IOException, AbortedException;
}