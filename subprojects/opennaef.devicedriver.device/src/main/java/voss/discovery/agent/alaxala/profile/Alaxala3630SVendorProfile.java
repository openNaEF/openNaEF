package voss.discovery.agent.alaxala.profile;

import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala3630SVendorProfile extends AlaxalaK0SeriesVendorProfile {

    private final String ax3630s_prefix = Mib2.Snmpv2_SMI_enterprises
            + ".21839.2.2.7";
    private final String ax3630sPhysLineConnectorType = ax3630s_prefix
            + ".2.2.1.1.2";

    private final String AX3630S_OS_VERSION = ax3630s_prefix + ".1.2.3";

    public String getOsVersionOid() {
        return this.AX3630S_OS_VERSION;
    }

    private final String AX3630S_NUMBER_OF_PORTS = ax3630s_prefix
            + ".2.1.2.1.19";

    public String getNumberOfPortOid() {
        return this.AX3630S_NUMBER_OF_PORTS;
    }

    private final String ax3630sPhysLineTransceiverStatus = ax3630s_prefix
            + ".2.2.1.1.5";

    public String getPhysLineTransceiverStatusOid() {
        return this.ax3630sPhysLineTransceiverStatus;
    }

    public final String AX3630S_MODEL_OID = ax3630s_prefix + ".1.1.0";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            int modelId = SnmpUtil.getInteger(snmp, AX3630S_MODEL_OID);
            switch (modelId) {
                case 500:
                    return "AX3630S-24T";
                case 501:
                    return "AX3630S-24T2X";
                case 502:
                    return "AX3630S-24P";
                default:
                    break;
            }
            return "UNKNOWN AX3630S Type";
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getDeviceBaseOid() {
        return this.ax3630s_prefix;
    }

    @Override
    public String getOsNameOid() {
        return null;
    }

    @Override
    public String getNumberOfSlotOid() {
        return null;
    }

    @Override
    public String getBoardNameOid() {
        return null;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.ax3630sPhysLineConnectorType;
    }
}