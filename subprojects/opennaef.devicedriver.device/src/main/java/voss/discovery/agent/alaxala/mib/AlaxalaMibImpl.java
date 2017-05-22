package voss.discovery.agent.alaxala.mib;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.alaxala.AlaxalaPortEntry;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.AlaxalaQosFlowListEntry;
import voss.model.EthernetPort;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AlaxalaMibImpl {
    protected AlaxalaVendorProfile profile;
    protected boolean onApiEnable = false;
    protected final SnmpAccess snmp;

    protected final Map<Integer, Integer> portIndexToIfIndexMap = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> vlanIdToIfIndexMap = new HashMap<Integer, Integer>();

    public AlaxalaMibImpl(SnmpAccess snmp, AlaxalaVendorProfile _profile) {
        this.snmp = snmp;
        this.profile = _profile;
    }

    public String getModelName() throws IOException, AbortedException {
        return profile.getModelName(snmp);
    }

    public abstract String getOsType() throws IOException, AbortedException;

    public String getOsVersion() throws IOException, AbortedException {
        try {
            return SnmpUtil.getString(snmp, profile.getOsVersionOid() + ".0");
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public abstract int getNumberOfPort(int _slotId) throws IOException, AbortedException;

    public abstract int getNumberOfSlot() throws IOException, AbortedException;

    public abstract String getNifBoardName(int slotId) throws IOException, AbortedException;

    public abstract AlaxalaPortEntry[] getPhysicalPorts() throws IOException, AbortedException;

    public String getSyslogServerAddress() throws IOException, AbortedException {
        return null;
    }

    public String getNifBoardSerialNumber(int slotId) throws IOException, AbortedException {
        return "not yet";
    }

    public String getIfName(SnmpAccess snmp, AlaxalaPortEntry portEntry) throws IOException, AbortedException {
        return this.profile.getIfName(snmp, portEntry);
    }

    public String getAggregationName(int aggregationId) {
        return this.profile.getAggregationName(aggregationId);
    }

    public PortSpeedValue.Admin getAdminSpeed(int _ifIndex) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries =
                    SnmpUtil.getIntSnmpEntries(snmp, profile.getAxsIfStatsHighSpeedOid());
            for (IntSnmpEntry entry : entries) {
                int ifIndex = entry.getLastOIDIndex().intValue();
                if (ifIndex == _ifIndex) {
                    String value = entry.intValue() + "Mbps";
                    long speed = (long) (entry.intValue()) * 1000L * 1000L;
                    return new PortSpeedValue.Admin(speed, value);
                }
            }

            return null;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public EthernetPort.Duplex getDuplex(int ifIndex) {
        return null;
    }

    protected static final String dot3adAggPortListPorts
            = ".1.2.840.10006.300.43.1.1.2.1.1";

    protected final Map<Integer, List<Integer>> aggregationMemberPortsMap =
            new HashMap<Integer, List<Integer>>();

    protected final Map<Integer, int[]> aggregatorMembers = new HashMap<Integer, int[]>();

    public void prepareAggregatorIfIndex() throws AbortedException, IOException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, dot3adAggPortListPorts);
            for (IntSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 1;
                int[] aggregationPortsIndices
                        = SnmpUtil.decodeBitList(entry.value);
                aggregatorMembers.put(new Integer(entry.oidSuffix[0].intValue()), aggregationPortsIndices);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public Integer getAggregatorIfIndex(int ifIndex) {
        Integer result = null;
        for (Integer key : aggregatorMembers.keySet()) {
            int[] aggregationPortsIndices = aggregatorMembers.get(key);
            if (SnmpUtil.contains(aggregationPortsIndices, ifIndex)) {
                if (result != null) {
                    throw new IllegalStateException("duplicated aggregator.");
                }
                result = key;

                List<Integer> aggregationMembers = aggregationMemberPortsMap.get(result);
                if (aggregationMembers == null) {
                    aggregationMembers = new ArrayList<Integer>();
                    aggregationMemberPortsMap.put(result, aggregationMembers);
                }
                aggregationMembers.add(ifIndex);
            }
        }
        return result;
    }

    public Integer getAggregationId(int ifindex) {
        return profile.getAggregationIdFromIfIndex(ifindex);
    }

    int[] getAggregationPortIfIndices(int aggregatorIfIndex) {
        List<Integer> list = aggregationMemberPortsMap.get(aggregatorIfIndex);
        if (list == null) {
            return null;
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Integer) list.get(i)).intValue();
        }
        return result;
    }

    public String getPhysicalLineConnectorType(int ifIndex) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, profile.getPhysLineConnectorTypeOid());
            for (IntSnmpEntry entry : entries) {
                int slotId = entry.oidSuffix[1].intValue();
                int portId = entry.oidSuffix[2].intValue();
                if (ifIndex == (slotId * 100 + portId)) {
                    BigInteger typeId = entry.getValueAsBigInteger();
                    if (typeId != null) {
                        switch (typeId.intValue()) {
                            case 1:
                                return "other";
                            case 301:
                                return "1000BASE-LX";
                            case 302:
                                return "1000BASE-SX";
                            case 303:
                                return "1000BASE-LH";
                            case 304:
                                return "1000BASE-BX10-D";
                            case 305:
                                return "1000BASE-BX10-U";
                            case 306:
                                return "1000BASE-BX40-D";
                            case 307:
                                return "1000BASE-BX40-U";
                            case 401:
                                return "10GBASE-SR";
                            case 402:
                                return "10GBASE-LR";
                            case 403:
                                return "10GBASE-ER";
                            case 404:
                                return "10GBASE-ZR";
                            case 103:
                                return "OC-48c/STM-16 POS 2km";
                            case 104:
                                return "OC-48c/STM-16 POS 40km";
                            default:
                                return "unknown";
                        }
                    }
                }
            }
            return "null";
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public String getPhysicalLineConnectorType(int slot, int port) throws IOException, AbortedException {
        try {
            slot = slot + 1;
            port = port + 1;
            String oid = profile.getPhysLineConnectorTypeOid() + ".1." + slot + "." + port;
            int typeId = SnmpUtil.getInteger(snmp, oid);

            switch (typeId) {
                case 1:
                    return "other";
                case 301:
                    return "1000BASE-LX";
                case 302:
                    return "1000BASE-SX";
                case 303:
                    return "1000BASE-LH";
                case 304:
                    return "1000BASE-BX10-D";
                case 305:
                    return "1000BASE-BX10-U";
                case 306:
                    return "1000BASE-BX40-D";
                case 307:
                    return "1000BASE-BX40-U";
                case 401:
                    return "10GBASE-SR";
                case 402:
                    return "10GBASE-LR";
                case 403:
                    return "10GBASE-ER";
                case 404:
                    return "10GBASE-ZR";
                case 103:
                    return "OC-48c/STM-16 POS 2km";
                case 104:
                    return "OC-48c/STM-16 POS 40km";
                default:
                    return "unknown";
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public abstract AlaxalaQosFlowListEntry[] getQosFlowEntries() throws IOException, AbortedException;

    public AlaxalaVendorProfile getVendorProfile() {
        return profile;
    }

}