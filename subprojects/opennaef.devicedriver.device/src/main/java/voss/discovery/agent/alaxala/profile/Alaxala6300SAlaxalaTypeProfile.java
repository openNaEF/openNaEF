package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala6300SAlaxalaTypeProfile extends AlaxalaK1SeriesVendorProfile {

    private final String ax6300sBasePrefix = ".2.2.8";
    private final String ax6300sBaseOid = alaxalaBaseOid + ax6300sBasePrefix;
    private final String ax6300sModelType = ax6300sBaseOid + ".1.1";
    private final String ax6300sSoftwareName = ax6300sBaseOid + ".1.2.1";
    private final String ax6300sSoftwareVersion = ax6300sBaseOid + ".1.2.3";

    private final String ax6300sNifBoardNumber = ax6300sBaseOid + ".2.1.2.1.28";
    private final String ax6300sNifBoardName = ax6300sBaseOid + ".2.4.1.1.4";
    private final String ax6300sNifPhysicalLineNumber = ax6300sBaseOid + ".2.4.1.1.7";
    private final String ax6300sPhysLineConnectorType = ax6300sBaseOid + ".2.5.1.1.2";
    private final String ax6300sIfIndex = ax6300sBaseOid + ".2.6.1.1.2";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, ax6300sModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 600:
                    return "AX6302S";
                case 601:
                    return "AX6304S";
            }
            return "UNKNOWN";
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getAggregationIdFromIfIndex(int ifindex) {
        assert ifindex > 1000;
        return ifindex - 1000;
    }

    @Override
    public int getVlanIfIndexOffset() {
        return 1200;
    }

    @Override
    public String getIfName(SnmpAccess snmp, AlaxalaPortEntry entry) throws IOException, AbortedException {
        return AlaxalaVendorProfileUtil.getIosLikeIfName(snmp, entry);
    }

    @Override
    public String getAggregationName(int aggregationId) {
        return AlaxalaVendorProfileUtil.getIosLikeAggregationName(aggregationId);
    }

    @Override
    public String getDeviceBaseOid() {
        return alaxalaBaseOid + ax6300sBasePrefix;
    }

    @Override
    public String getOsVersionOid() {
        return this.ax6300sSoftwareVersion;
    }

    @Override
    public String getNumberOfPortOid() {
        return this.ax6300sNifPhysicalLineNumber;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.ax6300sIfIndex;
    }

    @Override
    public String getOsNameOid() {
        return this.ax6300sSoftwareName;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.ax6300sNifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.ax6300sNifBoardName;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.ax6300sPhysLineConnectorType;
    }

}