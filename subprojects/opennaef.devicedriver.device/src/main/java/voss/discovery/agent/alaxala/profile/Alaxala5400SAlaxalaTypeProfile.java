package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala5400SAlaxalaTypeProfile extends AlaxalaK1SeriesVendorProfile {

    private static final String ax5400sBasePrefix = ".2.2.3";
    private static final String ax5400sBaseOid = alaxalaBaseOid + ax5400sBasePrefix;
    private static final String ax5400sModelType = ax5400sBaseOid + ".1.1";
    private static final String ax5400sSoftwareName = ax5400sBaseOid + ".1.2.1";
    private static final String ax5400sSoftwareVersion = ax5400sBaseOid + ".1.2.3";

    private static final String ax5400sNifBoardNumber = ax5400sBaseOid + ".2.1.2.1.10";
    private static final String ax5400sNifBoardName = ax5400sBaseOid + ".2.4.1.1.4";
    private static final String ax5400sNifPhysicalLineNumber = ax5400sBaseOid + ".2.4.1.1.7";
    private static final String ax5400sPhysLineConnectorType = ax5400sBaseOid + ".2.5.1.1.2";
    private static final String ax5400sIfIndex = ax5400sBaseOid + ".2.6.1.1.2";

    @Override
    public String getOsType() {
        return OS_TYPE_ALAXALA;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            System.out.println(ax5400sModelType);
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, ax5400sModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 200:
                    return "AX5402S";
                case 201:
                    return "AX5404S";
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
        return alaxalaBaseOid + ax5400sBasePrefix;
    }

    @Override
    public String getOsVersionOid() {
        return this.ax5400sSoftwareVersion;
    }

    @Override
    public String getNumberOfPortOid() {
        return this.ax5400sNifPhysicalLineNumber;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.ax5400sIfIndex;
    }

    @Override
    public String getOsNameOid() {
        return this.ax5400sSoftwareName;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.ax5400sNifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.ax5400sNifBoardName;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.ax5400sPhysLineConnectorType;
    }

}