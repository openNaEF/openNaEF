package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;


public class Alaxala7800SNecTypeProfile extends Alaxala7800SVendorProfile {

    private String vendorId = ".119";
    private String ip8800sBaseOid = Mib2.Snmpv2_SMI_enterprises + vendorId;

    private final String alaxalaIfStatsBase = ip8800sBaseOid + ".2.3.135.1.1.4.1.1";

    private String ip8800sVBBaseVlanIfIndex = ip8800sBaseOid + ".2.3.135.1.6.1.1.1.1.5";
    private String ip8800sVBBaseVlanType = ip8800sBaseOid + ".2.3.135.1.6.1.1.1.1.6";
    private String ip8800sBaseVlanId = ip8800sBaseOid + ".2.3.135.1.6.1.1.1.1.7";

    private String ip8800sVBBasePortIfIndex = ip8800sBaseOid + ".2.3.135.1.6.1.1.2.1.3";
    private String vlanAndPortBindingState = ip8800sBaseOid + ".2.3.135.1.6.1.1.2.1.8";

    private String deviceBaseOid = ip8800sBaseOid + ".2.3.135.100";
    private String ip8800sModelType = deviceBaseOid + ".1.1";
    private String ip8800sSoftwareName = deviceBaseOid + ".1.2.1";
    private String ip8800sSoftwareVersion = deviceBaseOid + ".1.2.3";
    private String nifBoardNumber = deviceBaseOid + ".2.1.2.1.10";

    private String ip8800sNifBoardName = deviceBaseOid + ".2.4.1.1.4";
    private String nifPhysicalLineNumber = deviceBaseOid + ".2.4.1.1.7";
    private String ip8800sPhysLineConnectorType = deviceBaseOid + ".2.5.1.1.2";
    private String ip8800sIfIndex = deviceBaseOid + ".2.6.1.1.2";

    private final String ip8800sFlowQosBase = ip8800sBaseOid + ".2.3.135.1.8.5";
    private final String ip8800sFlowQosInBase = ip8800sFlowQosBase + ".1";
    private final String ip8800sFlowQosInPremiumBase = ip8800sFlowQosBase + ".2";
    private final String ip8800sFlowQosOutBase = ip8800sFlowQosBase + ".3";
    private final String ip8800sFlowQosOutPremiumBase = ip8800sFlowQosBase + ".4";

    private final String ip8800sFlowQosStatsBase = ip8800sBaseOid + ".2.3.135.1.8.6";
    private final String ip8800sFlowQosStatsInBase = ip8800sFlowQosBase + ".1";
    private final String ip8800sFlowQosStatsInPremiumBase = ip8800sFlowQosBase + ".2";
    private final String ip8800sFlowQosStatsOutBase = ip8800sFlowQosBase + ".3";
    private final String ip8800sFlowQosStatsOutPremiumBase = ip8800sFlowQosBase + ".4";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, ip8800sModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 100:
                    return "IP8800/S401-DC";
                case 101:
                    return "IP8800/S401-AC";
                case 102:
                    return "IP8800/S402-DC";
                case 103:
                    return "IP8800/S402-AC";
                case 104:
                    return "IP8800/S403-DC";
                case 105:
                    return "IP8800/S403-AC";
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
    public String getOsVersionOid() {
        return this.ip8800sSoftwareVersion;
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
        return this.ip8800sBaseOid;
    }

    @Override
    public String getAlaxalaVBBaseVlanTypeOid() {
        return this.ip8800sVBBaseVlanType;
    }

    @Override
    public String getAlaxalaBaseVlanIdOid() {
        return this.ip8800sBaseVlanId;
    }

    @Override
    public String getAxsVBBasePortIfIndexOid() {
        return this.ip8800sVBBasePortIfIndex;
    }

    @Override
    public String getVlanAndPortBindingStateOid() {
        return this.vlanAndPortBindingState;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.ip8800sIfIndex;
    }

    @Override
    public String getOsNameOid() {
        return this.ip8800sSoftwareName;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.nifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.ip8800sNifBoardName;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.ip8800sPhysLineConnectorType;
    }

    @Override
    public String getAxsIfStatsHighSpeedOid() {
        return this.alaxalaIfStatsBase + ".11";
    }

    @Override
    public String getQosFlowStatsInListName() {
        return null;
    }

    @Override
    public String getAxsVBBaseVlanIfIndexOid() {
        return ip8800sVBBaseVlanIfIndex;
    }
    @Override
    public String getFlowQosBaseOid() {
        return this.ip8800sFlowQosBase;
    }

    @Override
    public String getFlowQosInBaseOid() {
        return this.ip8800sFlowQosInBase;
    }

    @Override
    public String getFlowQosInPremiumBaseOid() {
        return this.ip8800sFlowQosInPremiumBase;
    }

    @Override
    public String getFlowQosOutBaseOid() {
        return this.ip8800sFlowQosOutBase;
    }

    @Override
    public String getFlowQosOutPremiumBaseOid() {
        return this.ip8800sFlowQosOutPremiumBase;
    }

    @Override
    public String getFlowQosStatsBaseOid() {
        return this.ip8800sFlowQosStatsBase;
    }

    @Override
    public String getFlowQosStatsInBaseOid() {
        return this.ip8800sFlowQosStatsInBase;
    }

    @Override
    public String getFlowQosStatsInPremiumBaseOid() {
        return this.ip8800sFlowQosStatsInPremiumBase;
    }

    @Override
    public String getFlowQosStatsOutBaseOid() {
        return this.ip8800sFlowQosStatsOutBase;
    }

    @Override
    public String getFlowQosStatsOutPremiumBaseOid() {
        return this.ip8800sFlowQosStatsOutPremiumBase;
    }
}