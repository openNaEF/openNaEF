package voss.discovery.agent.alaxala.profile;

import net.snmp.SnmpResponseException;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala1230SVendorProfile extends AlaxalaK0SeriesVendorProfile {

    private static final String ax2430s_prefix = Mib2.Snmpv2_SMI_enterprises
            + ".21839.2.2.6";
    private static final String axPhysLineConnectorType = ax2430s_prefix
            + ".2.2.1.1.2";

    private static final String AX2430S_OS_VERSION = ax2430s_prefix + ".1.2.3";

    public String getOsVersionOid() {
        return AX2430S_OS_VERSION;
    }

    private static final String AX2430S_NUMBER_OF_PORTS = ax2430s_prefix
            + ".2.1.2.1.19";

    public String getNumberOfPortOid() {
        return AX2430S_NUMBER_OF_PORTS;
    }

    private static final String ax2430sPhysLineTransceiverStatus = ax2430s_prefix
            + ".2.2.1.1.5";

    public String getPhysLineTransceiverStatusOid() {
        return ax2430sPhysLineTransceiverStatus;
    }

    public final String AX2430S_MODEL_OID = ax2430s_prefix + ".1.1.0";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelId = SnmpUtil.getInteger(_snmp, AX2430S_MODEL_OID);
            switch (modelId) {
                case 400:
                    return "AX2430S-24T";
                case 401:
                    return "AX2430S-48T";
                case 402:
                    return "AX2430S-24T2X";
                default:
                    break;
            }
            return "UNKNOWN";
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getDeviceBaseOid() {
        return ax2430s_prefix;
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
        return axPhysLineConnectorType;
    }
}