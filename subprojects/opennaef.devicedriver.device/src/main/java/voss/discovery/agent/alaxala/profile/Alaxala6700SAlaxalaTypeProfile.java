package voss.discovery.agent.alaxala.profile;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;

import java.io.IOException;

public class Alaxala6700SAlaxalaTypeProfile extends AlaxalaK1SeriesVendorProfile {

    private final String ax6700sBasePrefix = ".2.2.9";
    private final String ax6700sBaseOid = alaxalaBaseOid + ax6700sBasePrefix;
    private final String ax6700sModelType = ax6700sBaseOid + ".1.1";
    private final String ax6700sSoftwareName = ax6700sBaseOid + ".1.2.1";
    private final String ax6700sSoftwareVersion = ax6700sBaseOid + ".1.2.3";

    private final String ax6700sNifBoardNumber = ax6700sBaseOid + ".2.1.2.1.28";
    private final String ax6700sNifBoardName = ax6700sBaseOid + ".2.4.1.1.4";
    private final String ax6700sNifPhysicalLineNumber = ax6700sBaseOid + ".2.4.1.1.7";
    private final String ax6700sPhysLineConnectorType = ax6700sBaseOid + ".2.5.1.1.2";
    private final String ax6700sIfIndex = ax6700sBaseOid + ".2.6.1.1.2";

    @Override
    public String getOsType() {
        return OS_TYPE_CISCO_LIKE;
    }

    @Override
    public String getModelName(SnmpAccess _snmp) throws IOException, AbortedException {
        try {
            int modelTypeId = SnmpUtil.getIntSnmpEntries(_snmp, ax6700sModelType).get(0).intValue();
            switch (modelTypeId) {
                case 1:
                    return "Other";
                case 701:
                    return "AX6708S";
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
        return alaxalaBaseOid + ax6700sBasePrefix;
    }

    @Override
    public String getOsVersionOid() {
        return this.ax6700sSoftwareVersion;
    }

    @Override
    public String getNumberOfPortOid() {
        return this.ax6700sNifPhysicalLineNumber;
    }

    @Override
    public String getAxIfIndexOid() {
        return this.ax6700sIfIndex;
    }

    @Override
    public String getOsNameOid() {
        return this.ax6700sSoftwareName;
    }

    @Override
    public String getNumberOfSlotOid() {
        return this.ax6700sNifBoardNumber;
    }

    @Override
    public String getBoardNameOid() {
        return this.ax6700sNifBoardName;
    }

    @Override
    public String getPhysLineConnectorTypeOid() {
        return this.ax6700sPhysLineConnectorType;
    }

}