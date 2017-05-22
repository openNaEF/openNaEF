package voss.discovery.agent.alaxala.profile;

import voss.discovery.agent.mib.Mib2;

public abstract class AlaxalaK1SeriesVendorProfile extends Alaxala7800SVendorProfile {
    protected static final String vendorId = ".21839";
    protected static final String alaxalaBaseOid = Mib2.Snmpv2_SMI_enterprises + vendorId;

    protected static final String alaxalaIfStatsBase = alaxalaBaseOid + ".2.2.1.1.4.1.1";

    protected static final String alaxalaVlanBridgeBase = alaxalaBaseOid + ".2.2.1.6.1.1";

    protected static final String alaxalaVBBaseVlanIfIndex = alaxalaVlanBridgeBase + ".1.1.5";
    protected static final String alaxalaVBBaseVlanType = alaxalaVlanBridgeBase + ".1.1.6";
    protected static final String alaxalaBaseVlanId = alaxalaVlanBridgeBase + ".1.1.7";
    protected static final String axsVBBasePortIfIndex = alaxalaVlanBridgeBase + ".2.1.3";
    protected static final String vlanAndPortBindingState = alaxalaVlanBridgeBase + ".2.1.8";

    protected static final String alaxalaFlowQosBase = alaxalaBaseOid + ".2.2.1.8.5";
    protected static final String alaxalaFlowQosInBase = alaxalaFlowQosBase + ".1";
    protected static final String alaxalaFlowQosInPremiumBase = alaxalaFlowQosBase + ".2";
    protected static final String alaxalaFlowQosOutBase = alaxalaFlowQosBase + ".3";
    protected static final String alaxalaFlowQosOutPremiumBase = alaxalaFlowQosBase + ".4";

    protected static final String alaxalaFlowQosStatsBase = alaxalaBaseOid + ".2.2.1.8.6";
    protected static final String alaxalaFlowQosStatsInBase = alaxalaFlowQosStatsBase + ".1";
    protected static final String alaxalaFlowQosStatsInPremiumBase = alaxalaFlowQosStatsBase + ".2";
    protected static final String alaxalaFlowQosStatsOutBase = alaxalaFlowQosStatsBase + ".3";
    protected static final String alaxalaFlowQosStatsOutPremiumBase = alaxalaFlowQosStatsBase + ".4";

    protected static final String alaxalaQosFlowStatBase = alaxalaBaseOid + ".2.2.1.8.11";

    @Override
    public String getVendorIdOid() {
        return vendorId;
    }

    @Override
    public String getAlaxalaBaseOid() {
        return alaxalaBaseOid;
    }

    @Override
    public String getAlaxalaVBBaseVlanTypeOid() {
        return alaxalaVBBaseVlanType;
    }

    @Override
    public String getAlaxalaBaseVlanIdOid() {
        return alaxalaBaseVlanId;
    }

    @Override
    public String getAxsVBBasePortIfIndexOid() {
        return axsVBBasePortIfIndex;
    }

    @Override
    public String getVlanAndPortBindingStateOid() {
        return vlanAndPortBindingState;
    }

    @Override
    public String getAxsIfStatsHighSpeedOid() {
        return alaxalaIfStatsBase + ".11";
    }

    @Override
    public String getQosFlowStatsInListName() {
        return alaxalaQosFlowStatBase + ".1.1.5";
    }

    @Override
    public String getAxsVBBaseVlanIfIndexOid() {
        return alaxalaVBBaseVlanIfIndex;
    }

    @Override
    public String getFlowQosBaseOid() {
        return alaxalaFlowQosBase;
    }

    @Override
    public String getFlowQosInBaseOid() {
        return alaxalaFlowQosInBase;
    }

    @Override
    public String getFlowQosInPremiumBaseOid() {
        return alaxalaFlowQosInPremiumBase;
    }

    @Override
    public String getFlowQosOutBaseOid() {
        return alaxalaFlowQosOutBase;
    }

    @Override
    public String getFlowQosOutPremiumBaseOid() {
        return alaxalaFlowQosOutPremiumBase;
    }

    @Override
    public String getFlowQosStatsBaseOid() {
        return alaxalaFlowQosStatsBase;
    }

    @Override
    public String getFlowQosStatsInBaseOid() {
        return alaxalaFlowQosStatsInBase;
    }

    @Override
    public String getFlowQosStatsInPremiumBaseOid() {
        return alaxalaFlowQosStatsInPremiumBase;
    }

    @Override
    public String getFlowQosStatsOutBaseOid() {
        return alaxalaFlowQosStatsOutBase;
    }

    @Override
    public String getFlowQosStatsOutPremiumBaseOid() {
        return alaxalaFlowQosStatsOutPremiumBase;
    }

}