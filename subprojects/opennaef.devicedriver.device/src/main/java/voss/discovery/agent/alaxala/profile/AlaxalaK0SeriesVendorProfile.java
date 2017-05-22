package voss.discovery.agent.alaxala.profile;

import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;


public abstract class AlaxalaK0SeriesVendorProfile extends AlaxalaVendorProfile {

    protected final String alaxalaBaseOid = Mib2.Snmpv2_SMI_enterprises + getVendorIdOid();
    private final String axIfIndex = alaxalaBaseOid + ".2.2.2.2.6.1.1.2.1";
    protected final int vlanIfIndexOffset = 200;
    protected final String alaxalaIfStatsBase = alaxalaBaseOid + ".2.2.1.1.4.1.1";
    protected final String alaxalaIfStatsHighSpeed = alaxalaIfStatsBase + ".11";
    protected final String alaxalaVlanBridgeBase = alaxalaBaseOid + ".2.2.1.6.1.1";
    protected final String alaxalaVBBaseVlanIfIndex = alaxalaVlanBridgeBase + ".1.1.5";
    protected final String alaxalaVBBaseVlanType = alaxalaVlanBridgeBase + ".1.1.6";
    protected final String alaxalaBaseVlanId = alaxalaVlanBridgeBase + ".1.1.7";
    protected final String axsVBBasePortIfIndex = alaxalaVlanBridgeBase + ".2.1.3";
    protected final String vlanAndPortBindingState = alaxalaVlanBridgeBase + ".2.1.8";
    protected final String alaxalaQosFlowStatBase = alaxalaBaseOid + ".2.2.1.8.11";

    @Override
    public String getIfName(SnmpAccess snmp, AlaxalaPortEntry entry) throws IOException, AbortedException {
        return AlaxalaVendorProfileUtil.getIosLikeIfName(snmp, entry);
    }

    @Override
    public String getAggregationName(int aggregationId) {
        return AlaxalaVendorProfileUtil.getIosLikeAggregationName(aggregationId);
    }

    @Override
    public int getAggregationIdFromIfIndex(int ifindex) {
        assert ifindex > 60;
        return ifindex - 60;
    }

    @Override
    public int getDefaultVlanIfIndex() {
        return 3;
    }

    @Override
    public int getPortIfIndexOffset() {
        return 10;
    }

    public String getFlowQosBaseOid() {
        throw new IllegalStateException("MIB axsFlowQos not supported.");
    }

    public String getFlowQosInBaseOid() {
        throw new IllegalStateException("MIB axsFlowQos not supported.");
    }

    public String getFlowQosInPremiumBaseOid() {
        throw new IllegalStateException("MIB axsFlowQos not supported.");
    }

    public String getFlowQosOutBaseOid() {
        throw new IllegalStateException("MIB axsFlowQos not supported.");
    }

    public String getFlowQosOutPremiumBaseOid() {
        throw new IllegalStateException("MIB axsFlowQos not supported.");
    }

    public String getFlowQosStatsBaseOid() {
        throw new IllegalStateException("MIB axsFlowQosStats not supported.");
    }

    public String getFlowQosStatsInBaseOid() {
        throw new IllegalStateException("MIB axsFlowQosStats not supported.");
    }

    public String getFlowQosStatsInPremiumBaseOid() {
        throw new IllegalStateException("MIB axsFlowQosStats not supported.");
    }

    public String getFlowQosStatsOutBaseOid() {
        throw new IllegalStateException("MIB axsFlowQosStats not supported.");
    }

    public String getFlowQosStatsOutPremiumBaseOid() {
        throw new IllegalStateException("MIB axsFlowQosStats not supported.");
    }

    public final String getAxIfIndexOid() {
        return this.axIfIndex;
    }

    public final String getVendorIdOid() {
        return ".21839";
    }

    public int getVlanIfIndexOffset() {
        return this.vlanIfIndexOffset;
    }

    public String getAlaxalaBaseOid() {
        return this.alaxalaBaseOid;
    }

    public String getAlaxalaVBBaseVlanTypeOid() {
        return this.alaxalaVBBaseVlanType;
    }

    public String getAlaxalaBaseVlanIdOid() {
        return this.alaxalaBaseVlanId;
    }

    public String getAxsVBBasePortIfIndexOid() {
        return this.axsVBBasePortIfIndex;
    }

    public String getVlanAndPortBindingStateOid() {
        return this.vlanAndPortBindingState;
    }

    public String getAxsIfStatsHighSpeedOid() {
        return this.alaxalaIfStatsHighSpeed;
    }

    public String getQosFlowStatsInListName() {
        return this.alaxalaQosFlowStatBase + ".1.1.5";
    }

    public String getAxsVBBaseVlanIfIndexOid() {
        return alaxalaVBBaseVlanIfIndex;
    }

}