package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala7800SHitachiTypeProfile extends
        Alaxala7800SVendorProfile {

    private String vendorId = ".116";
    private String gs4kBaseOid = Mib2.Snmpv2_SMI_enterprises + vendorId;

    private String gs4kIfStatsBase = gs4kBaseOid + ".6.25.1.1.1.4.1.1";

    private final String gs4kVBBaseVlanIfIndex = gs4kBaseOid
            + ".6.25.1.1.6.1.1.1.1.6";
    private final String gs4kVBBaseVlanType = gs4kBaseOid
            + ".6.25.1.1.6.1.1.1.1.6";
    private String gs4kBaseVlanId = gs4kBaseOid
            + ".6.25.1.1.6.1.1.1.1.7";

    private String gs4kVBBasePortIfIndex = gs4kBaseOid
            + ".6.25.1.1.6.1.1.2.1.3";
    private String vlanAndPortBindingState = gs4kBaseOid
            + ".6.25.1.1.6.1.1.2.1.8";

    private String deviceBaseOid = gs4kBaseOid + ".6.25.1.3";
    private String gs4kModelType = deviceBaseOid + ".1.1";
    private String gs4kSoftwareName = deviceBaseOid + ".1.2.1";
    private String gs4kSoftwareVersion = deviceBaseOid + ".1.2.3";
    private String nifBoardNumber = deviceBaseOid + ".2.1.2.1.10";

    private String gs4kNifBoardName = deviceBaseOid + ".2.4.1.1.4";
    private String nifPhysicalLineNumber = deviceBaseOid + ".2.4.1.1.7";
    private String axIfIndex = deviceBaseOid + ".2.6.1.1.2";
    private String axPhysLineConnectorType = deviceBaseOid + ".2.5.1.1.2";

    private final String gs4kFlowQosBase = ".6.25.1.1.8.5";
    private final String gs4kFlowQosInBase = gs4kFlowQosBase + ".1";
    private final String gs4kFlowQosInPremiumBase = gs4kFlowQosBase + ".2";
    private final String gs4kFlowQosOutBase = gs4kFlowQosBase + ".3";
    private final String gs4kFlowQosOutPremiumBase = gs4kFlowQosBase + ".4";

    private final String gs4kFlowQosStatsBase = ".6.25.1.1.8.6";
    private final String gs4kFlowQosStatsInBase = gs4kFlowQosStatsBase + ".1";
    private final String gs4kFlowQosStatsInPremiumBase = gs4kFlowQosStatsBase + ".2";
    private final String gs4kFlowQosStatsOutBase = gs4kFlowQosStatsBase + ".3";
    private final String gs4kFlowQosStatsOutPremiumBase = gs4kFlowQosStatsBase + ".4";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, gs4kModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 100:
                    return "GS4000-80E1";
                case 101:
                    return "GS4000-80E2";
                case 102:
                    return "GS4000-160E1";
                case 103:
                    return "GS4000-160E2";
                case 104:
                    return "GS4000-320E-DC";
                case 105:
                    return "GS4000-320E-AC";
            }
            return "UNKNOWN";
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getDeviceBaseOid() {
        return this.deviceBaseOid;
    }

    @Override
    public String getOsNameOid() {
        return this.gs4kSoftwareName;
    }

    @Override
    public String getOsVersionOid() {
        return this.gs4kSoftwareVersion;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.nifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.gs4kNifBoardName;
    }

    @Override
    public String getNumberOfPortOid() {
        return this.nifPhysicalLineNumber;
    }

    @Override
    public String getVendorIdOid() {
        return this.vendorId;
    }

    @Override
    public String getAlaxalaBaseOid() {
        return this.gs4kBaseOid;
    }

    @Override
    public String getAlaxalaVBBaseVlanTypeOid() {
        return this.gs4kVBBaseVlanType;
    }

    @Override
    public String getAlaxalaBaseVlanIdOid() {
        return this.gs4kBaseVlanId;
    }

    @Override
    public String getAxsVBBasePortIfIndexOid() {
        return this.gs4kVBBasePortIfIndex;
    }

    @Override
    public String getVlanAndPortBindingStateOid() {
        return this.vlanAndPortBindingState;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.axIfIndex;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.axPhysLineConnectorType;
    }

    @Override
    public String getAxsIfStatsHighSpeedOid() {
        return this.gs4kIfStatsBase + ".11";
    }

    @Override
    public String getQosFlowStatsInListName() {
        return null;
    }

    @Override
    public String getAxsVBBaseVlanIfIndexOid() {
        return gs4kVBBaseVlanIfIndex;
    }

    @Override
    public String getFlowQosBaseOid() {
        return this.gs4kFlowQosBase;
    }

    @Override
    public String getFlowQosInBaseOid() {
        return this.gs4kFlowQosInBase;
    }

    @Override
    public String getFlowQosInPremiumBaseOid() {
        return this.gs4kFlowQosInPremiumBase;
    }

    @Override
    public String getFlowQosOutBaseOid() {
        return this.gs4kFlowQosOutBase;
    }

    @Override
    public String getFlowQosOutPremiumBaseOid() {
        return this.gs4kFlowQosOutPremiumBase;
    }

    @Override
    public String getFlowQosStatsBaseOid() {
        return this.gs4kFlowQosStatsBase;
    }

    @Override
    public String getFlowQosStatsInBaseOid() {
        return this.gs4kFlowQosStatsInBase;
    }

    @Override
    public String getFlowQosStatsInPremiumBaseOid() {
        return this.gs4kFlowQosStatsInPremiumBase;
    }

    @Override
    public String getFlowQosStatsOutBaseOid() {
        return this.gs4kFlowQosStatsOutBase;
    }

    @Override
    public String getFlowQosStatsOutPremiumBaseOid() {
        return this.gs4kFlowQosStatsOutPremiumBase;
    }
}