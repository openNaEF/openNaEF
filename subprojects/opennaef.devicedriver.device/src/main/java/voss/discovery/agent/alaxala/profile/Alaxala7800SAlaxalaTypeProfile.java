package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala7800SAlaxalaTypeProfile extends AlaxalaK1SeriesVendorProfile {

    private final String axIfIndex = alaxalaBaseOid + ".2.2.2.2.6.1.1.2";
    private final String axPhysLineConnectorType = alaxalaBaseOid + ".2.2.2.2.5.1.1.2";

    private final String ax7800sBasePrefix = ".2.2.2";
    private final String ax7800sModelType = alaxalaBaseOid + ".2.2.2.1.1";
    private final String ax7800sSoftwareName = alaxalaBaseOid + ".2.2.2.1.2.1";
    private final String ax7800sSoftwareVersion = alaxalaBaseOid + ".2.2.2.1.2.3";
    private final String nifBoardNumber = alaxalaBaseOid + ".2.2.2.2.1.2.1.10";

    private final String ax7800sNifBoardName = alaxalaBaseOid + ".2.2.2.2.4.1.1.4";
    private final String nifPhysicalLineNumber = alaxalaBaseOid + ".2.2.2.2.4.1.1.7";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, ax7800sModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 100:
                    return "AX7804 model 80E1";
                case 101:
                    return "AX7804 model 80E2";
                case 102:
                    return "AX7808 model 160E1";
                case 103:
                    return "AX7808 model 160E2";
                case 104:
                    return "AX7816 model 320E-DC";
                case 105:
                    return "AX7816 model 320E-AC";
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
        return alaxalaBaseOid + ax7800sBasePrefix;
    }

    @Override
    public String getOsVersionOid() {
        return this.ax7800sSoftwareVersion;
    }

    @Override
    public String getNumberOfPortOid() {
        return this.nifPhysicalLineNumber;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.axIfIndex;
    }

    @Override
    public String getOsNameOid() {
        return this.ax7800sSoftwareName;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.nifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.ax7800sNifBoardName;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.axPhysLineConnectorType;
    }

}