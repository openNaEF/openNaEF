package voss.discovery.agent.foundry;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.VossIfType;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.*;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.EthernetPort;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoundryCollectMethods {
    private final static Logger log = LoggerFactory.getLogger(FoundryCollectMethods.class);
    private final static Map<String, String> deviceTypeMap = new HashMap<String, String>();

    static {
        deviceTypeMap.put(".1.3.6.1.4.1.1991.1.3.40.1.2", "BigIron RX 16");
        deviceTypeMap.put(".1.3.6.1.4.1.1991.1.3.34.1.1.1.2", "FESX424-PREM");
        deviceTypeMap.put(".1.3.6.1.4.1.1991.1.3.34.2.1.1.2", "FESX448");
    }

    public final static int IGNORING_IFINDEX_VALUE = 1025;

    private int MAGIC_NUMBER_FOR_LAG_IFINDEX = 10000;

    protected final static String foundryOID = ".1991";
    protected final static String foundryEnterpriseOID = Mib2.Snmpv2_SMI_enterprises + foundryOID;
    protected final static String agentNetMask = ".1.3.6.1.2.1.4.20.1.3";

    protected final static String foundryDeviceModel = foundryEnterpriseOID
            + ".1.1.2.8.1.1.4";
    protected final static String foundryOsVersion = foundryEnterpriseOID
            + ".1.1.2.1.11";
    protected final static String foundrySyslogHost = foundryEnterpriseOID
            + ".1.1.2.6.1.9";

    protected final static String snSwPortInfoPortNum =
            foundryEnterpriseOID + ".1.1.3.3.1.1.38";
    protected final static String foundrySnIfIndexLookupInterfaceId =
            foundryEnterpriseOID + ".1.1.3.3.4.1.2";
    protected final static String snSwPortType5PortName =
            foundryEnterpriseOID + ".1.1.3.3.5.1.18";

    protected final static String foundryPortName = foundryEnterpriseOID
            + ".1.1.3.3.1.1.24";
    protected final static String foundryPortDuplex = foundryEnterpriseOID
            + ".1.1.3.3.1.1.4";
    protected final static String foundryPortAdminSpeed = foundryEnterpriseOID
            + ".1.1.3.3.1.1.5";
    protected final static String foundryPortType = foundryEnterpriseOID
            + ".1.1.3.3.1.1.6";
    protected final static String foundryPortLinkNegotiaion = foundryEnterpriseOID
            + ".1.1.3.3.1.1.34";

    protected final static String fondryTrunkIndex = foundryEnterpriseOID
            + ".1.1.3.6.1.1.1";
    protected final static String fondryTrunkPortMask = foundryEnterpriseOID
            + ".1.1.3.6.1.1.2";
    protected final static String fondryTrunkPortType = foundryEnterpriseOID
            + ".1.1.3.6.1.1.3";

    protected final static String foundryVlanName = foundryEnterpriseOID
            + ".1.1.3.2.7.1.21";
    protected final static String foundryPortTaggingState = foundryEnterpriseOID
            + ".1.1.3.3.1.1.3";
    protected final static String foundryPortTaggingStateV2 = foundryEnterpriseOID
            + ".1.1.3.2.6.1.4";
    protected final static String foundryBindedVlanByPort = foundryEnterpriseOID
            + ".1.1.3.2.6.1.1";

    public final static int TAGGIGN_STATE_TAGGED = 1;
    public final static int TAGGING_STATE_UNTAGGED = 2;
    public final static int TAGGING_STATE_AUTO = 3;
    public final static int TAGGING_STATE_DISABLED = 4;

    protected final static String foundrySnTrunkPortMask = foundryEnterpriseOID +
            ".1.1.3.6.1.1.2";
    protected final static String foundrySnMSTrunkPortList = foundryEnterpriseOID +
            ".1.1.3.6.2.1.2";
    protected final static String foundrySnTrunkIfList = foundryEnterpriseOID +
            ".1.1.3.6.3.1.2";

    protected final Map<Integer, Integer> ifIndexToPortIfIndexMap =
            new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> portIfIndexToIfIndexMap =
            new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> ifIndexToAggregationPortIfIndex =
            new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> primalIfIndexOfAggregationPort =
            new HashMap<Integer, Integer>();

    protected final SnmpAccess snmp;
    protected final Mib2Impl mib2;

    public FoundryCollectMethods(SnmpAccess snmp) {
        this.snmp = snmp;
        this.mib2 = new Mib2Impl(snmp);
    }

    public void close() throws IOException, ConsoleException {
        this.snmp.close();
    }

    public String getModelName() throws IOException, AbortedException {
        try {
            String sysObjectId = mib2.getSysObjectId();
            String modelType = deviceTypeMap.get(sysObjectId);
            if (modelType != null) {
                return modelType;
            }

            String result = SnmpUtil.getString(snmp, foundryDeviceModel + ".1");
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    protected String getIpAddress(String oid) throws IOException, AbortedException {
        try {
            String result = SnmpUtil.getIpAddress(snmp, oid);
            if (result == null) {
                return null;
            }
            Matcher macher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+).*").matcher(result);
            if (macher.matches()) {
                result = macher.group(1);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getIpAddressWithSubnetmask(String ipAddress) throws IOException, AbortedException {
        return ipAddress + "/" + getIpAddress(agentNetMask + "." + ipAddress);
    }

    public String getOSVersion() throws IOException, AbortedException {
        try {
            return SnmpUtil.getString(snmp, foundryOsVersion + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getGatewayAddress() {
        return null;
    }

    public List<String> getSyslogServerAddresses() throws IOException, AbortedException {
        try {
            return SnmpUtil.getStringByWalk(snmp, foundrySyslogHost);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public VlanType[] getVlanTypes() throws IOException, AbortedException {
        try {
            ArrayList<VlanType> result = new ArrayList<VlanType>();
            AbstractWalkProcessor<ArrayList<VlanType>> walker =
                    new AbstractWalkProcessor<ArrayList<VlanType>>(result) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new VlanType(foundryBindedVlanByPort, varbind));
                        }
                    };
            snmp.walk(foundryBindedVlanByPort, walker);
            result = walker.getResult();
            for (int i = 0; i < result.size(); i++) {
                result.get(i).setup();
            }
            return result.toArray(new VlanType[result.size()]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        }
    }

    public String getVlanIfDescr(int vlanId) throws IOException, AbortedException {
        try {
            String result = SnmpUtil.getString(snmp, foundryVlanName + "." + vlanId);
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public PhysicalPortType[] getPhysicalPortTypes() throws IOException, AbortedException {
        List<PhysicalPortType> type1Result = getType1PhysicalPortTypes();
        if (type1Result.size() > 0) {
            log.info("Using Type1 MIB");
            return type1Result.toArray(new PhysicalPortType[type1Result.size()]);
        }

        List<PhysicalPortType> type5Result = getType5PhysicalPortTypes();
        if (type5Result.size() > 0) {
            log.info("Using Type5 MIB");
            return type5Result.toArray(new PhysicalPortType[type5Result.size()]);
        }

        List<PhysicalPortType> mg8Result = getMg8TypePhysicalPortTypes();
        if (mg8Result.size() > 0) {
            log.info("Using Type MG8 MIB");
            return mg8Result.toArray(new PhysicalPortType[mg8Result.size()]);
        }

        throw new IllegalStateException("Cannot retrieve any physical port information.");
    }

    private List<PhysicalPortType> getType1PhysicalPortTypes() throws IOException, AbortedException {
        List<PhysicalPortType> result = new ArrayList<PhysicalPortType>();
        try {
            AbstractWalkProcessor<List<PhysicalPortType>> walker
                    = new AbstractWalkProcessor<List<PhysicalPortType>>(result) {
                private static final long serialVersionUID = 1L;

                public void process(VarBind varbind) {
                    Type1PhysicalPortTypeImpl port
                            = new Type1PhysicalPortTypeImpl(snSwPortInfoPortNum, varbind);
                    result.add(port);
                }
            };
            snmp.walk(snSwPortInfoPortNum, walker);
            result = walker.getResult();
            for (int i = 0; i < result.size(); i++) {
                PhysicalPortType port = result.get(i);
                port.setup();
                ifIndexToPortIfIndexMap.put(port.getIfIndex(), port.getPortIfIndex());
                portIfIndexToIfIndexMap.put(port.getPortIfIndex(), port.getIfIndex());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        }
    }

    private List<PhysicalPortType> getType5PhysicalPortTypes() throws IOException, AbortedException {
        List<PhysicalPortType> result = new ArrayList<PhysicalPortType>();
        try {
            AbstractWalkProcessor<List<PhysicalPortType>> walker =
                    new AbstractWalkProcessor<List<PhysicalPortType>>(result) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            Type5PhysicalPortTypeImpl port = new Type5PhysicalPortTypeImpl(snSwPortType5PortName, varbind);
                            result.add(port);
                        }
                    };
            snmp.walk(snSwPortType5PortName, walker);
            result = walker.getResult();
            for (int i = 0; i < result.size(); i++) {
                PhysicalPortType port = result.get(i);
                port.setup();
                if (port.getIfIndex() >= IGNORING_IFINDEX_VALUE) {
                    continue;
                }
                ifIndexToPortIfIndexMap.put(port.getIfIndex(), port.getPortIfIndex());
                portIfIndexToIfIndexMap.put(port.getPortIfIndex(), port.getIfIndex());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        }
    }

    private List<PhysicalPortType> getMg8TypePhysicalPortTypes() throws IOException, AbortedException {
        ArrayList<PhysicalPortType> result = new ArrayList<PhysicalPortType>();
        try {
            AbstractWalkProcessor<ArrayList<PhysicalPortType>> walker =
                    new AbstractWalkProcessor<ArrayList<PhysicalPortType>>(result) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new SnIfIndexLookupInterfaceId(snSwPortInfoPortNum, varbind));
                        }
                    };
            snmp.walk(foundrySnIfIndexLookupInterfaceId, walker);
            result = walker.getResult();
            for (int i = 0; i < result.size(); i++) {
                SnIfIndexLookupInterfaceId port = (SnIfIndexLookupInterfaceId) result.get(i);
                port.setup();
                if (port.getMediaType() != 1) {
                    continue;
                }
                ifIndexToPortIfIndexMap.put(port.getIfIndex(), port.getPortIfIndex());
                portIfIndexToIfIndexMap.put(port.getPortIfIndex(), port.getIfIndex());
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public String getPortTaggingState(int vlanid, int ifIndex) throws IOException, AbortedException {
        String taggingState;
        try {
            taggingState = getPortTaggingStateUsingNewMib(vlanid, ifIndex);
        } catch (NoSuchMibException nse) {
            if (ifIndexToPortIfIndexMap.size() == 0) {
                throw new IllegalStateException(
                        "map ifIndexToPortIfIndex isn't initialized.");
            }
            int portIfIndex = ifIndexToPortIfIndexMap.get(ifIndex);
            taggingState = getPortTaggingStateUsingOldMib(portIfIndex);
        }

        return taggingState;
    }

    private String getPortTaggingStateUsingOldMib(int portIfIndex) throws IOException, AbortedException {
        try {
            int taggingState = SnmpUtil.getInteger(snmp, foundryPortTaggingState + "." + portIfIndex);
            switch (taggingState) {
                case TAGGIGN_STATE_TAGGED:
                    return "Tagged";
                case TAGGING_STATE_UNTAGGED:
                    return "Untagged";
                case TAGGING_STATE_AUTO:
                    return "auto";
                case TAGGING_STATE_DISABLED:
                    return "disabled";
                default:
                    throw new IllegalStateException("unknown tagging state: "
                            + "portIfIndex = " + portIfIndex + " / tagging = "
                            + taggingState);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    private String getPortTaggingStateUsingNewMib(int vlanid, int ifIndex)
            throws IOException, AbortedException, NoSuchMibException {
        try {
            int taggingState = SnmpUtil.getInteger(snmp, foundryPortTaggingStateV2 + "." + vlanid + "." + ifIndex);
            switch (taggingState) {
                case TAGGIGN_STATE_TAGGED:
                    return "Tagged";
                case TAGGING_STATE_UNTAGGED:
                    return "Untagged";
                case TAGGING_STATE_AUTO:
                    return "auto";
                case TAGGING_STATE_DISABLED:
                    return "disabled";
                default:
                    throw new IllegalStateException("unknown tagging state: vlanid = "
                            + vlanid + ", ifIndex = " + ifIndex + " / tagging = " + taggingState);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getPortType(int portIfIndex) throws IOException, AbortedException {
        try {
            Integer entry = SnmpUtil.getInteger(snmp, foundryPortType + "." + portIfIndex);
            if (0 < entry.intValue()) {
                return getPortTypeName(entry.intValue());
            } else {
                return VossIfType.UNDEFINED.toString();
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return VossIfType.UNDEFINED.toString();
        }
    }

    public static String getPortTypeName(int portType) {
        switch (portType) {
            case 1:
                return VossIfType.OTHER.toString();
            case 2:
                return VossIfType.E100BASETX.toString();
            case 3:
                return VossIfType.E100BASEFX.toString();
            case 4:
                return VossIfType.E1000BASEFX.toString();
            case 5:
                return VossIfType.T3.toString();
            case 6:
                return VossIfType.ATM_OC3.toString();
            case 7:
                return VossIfType.E1000BASET.toString();
            case 8:
                return VossIfType.ATM_OC12.toString();
            case 9:
                return VossIfType.POS_OC12.toString();
            case 10:
                return VossIfType.POS_OC48.toString();
            case 11:
                return VossIfType.POS_OC192.toString();
            case 12:
                return VossIfType.E10GBASEFX.toString();
            default:
                log.warn("unknown port type: " + portType);
                return VossIfType.UNDEFINED.toString();
        }
    }

    public String getPortName(int portIfIndex) throws IOException, AbortedException {
        try {
            String result = SnmpUtil.getString(snmp, foundryPortName + "." + portIfIndex);
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public PortSpeedValue.Oper getOperationalSpeed(int ifIndex) {
        return null;
    }

    public PortSpeedValue.Admin getAdminSpeed(int portIfIndex) throws IOException, AbortedException {
        try {
            Integer entry = SnmpUtil.getInteger(snmp, foundryPortAdminSpeed + "." + portIfIndex);
            switch (entry.intValue()) {
                case 0:
                    return new PortSpeedValue.Admin("none");
                case 1:
                    return PortSpeedValue.Admin.AUTO;
                case 2:
                    return new PortSpeedValue.Admin(
                            10 * 1000 * 1000, "10M");
                case 3:
                    return new PortSpeedValue.Admin(
                            100 * 1000 * 1000, "100M");
                case 4:
                    return new PortSpeedValue.Admin(
                            1 * 1000 * 1000 * 1000, "1G");
                case 5:
                    return new PortSpeedValue.Admin(
                            45 * 1000 * 1000, "45M");
                case 6:
                    return new PortSpeedValue.Admin(
                            155 * 1000 * 1000, "155M");
                case 7:
                    return new PortSpeedValue.Admin(
                            10 * 1000 * 1000 * 1000, "10G");
                default: {
                    log.warn("Unknown foundryPortAdminSpeed (" + entry.intValue() + ")");
                    return null;
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public EthernetPort.Duplex getDuplex(int portIfIndex) throws IOException, AbortedException {
        try {
            Integer entry = SnmpUtil.getInteger(snmp, foundryPortDuplex + "." + portIfIndex);
            switch (entry.intValue()) {
                case 0:
                    return null;
                case 1:
                    return EthernetPort.Duplex.HALF;
                case 2:
                    return EthernetPort.Duplex.FULL;
                default: {
                    System.out.println("Unknown foundryPortDuplex value (" + entry.intValue() + ")");
                    return null;
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return null;
        }
    }

    public Integer getAggregationID(int portIfIndex) {
        return getAggregationIdWithMagicNumber(ifIndexToAggregationPortIfIndex.get(portIfIndex));
    }

    private Integer getAggregationIdWithMagicNumber(Integer aggregationId) {
        if (aggregationId == null) {
            return null;
        }
        int craftedAggregationId = aggregationId + MAGIC_NUMBER_FOR_LAG_IFINDEX;
        primalIfIndexOfAggregationPort.put(craftedAggregationId, aggregationId);
        return craftedAggregationId;
    }

    public void setupTrunkList() throws IOException, SnmpResponseException, RepeatedOidException, AbortedException {
        List<ByteSnmpEntry> entries = SnmpUtil.getByteSnmpEntries(snmp, foundrySnTrunkIfList);
        for (ByteSnmpEntry entry : entries) {
            int aggregationPortIfIndex = entry.getLastOIDIndex().intValue();
            byte[] ifIndices = entry.getValue();
            List<Integer> ifIndexList = craftAggregationPortIfIndex(ifIndices);
            for (Integer ifIndex : ifIndexList) {
                ifIndexToAggregationPortIfIndex.put(ifIndex, aggregationPortIfIndex);
            }
        }
        if (ifIndexToAggregationPortIfIndex.size() > 0) {
            return;
        }

        List<SnmpEntry> snMSTrunkPortList = SnmpUtil.getSnmpEntries(snmp, foundrySnMSTrunkPortList);
        for (SnmpEntry entry : snMSTrunkPortList) {
            int[] aggregationPortIfIndices = getAggregationPortIfIndices(entry.value);
            int trunk_id = entry.oidSuffix[0].intValue();
            for (int portIfIndex : aggregationPortIfIndices) {
                ifIndexToAggregationPortIfIndex.put(portIfIndex, trunk_id);
            }
        }
        if (ifIndexToAggregationPortIfIndex.size() > 0) {
            return;
        }

        List<IntSnmpEntry> snTrunkPortList = SnmpUtil.getIntSnmpEntries(snmp, foundrySnTrunkPortMask);
        for (IntSnmpEntry entry : snTrunkPortList) {
            int[] aggregationPortIfIndices = SnmpUtil.decodeBitListFromBigInteger(entry.getValueAsBigInteger());
            int trunk_id = entry.oidSuffix[0].intValue();
            for (int portIfIndex : aggregationPortIfIndices) {
                ifIndexToAggregationPortIfIndex.put(portIfIndex, trunk_id);
            }
        }
    }

    private List<Integer> craftAggregationPortIfIndex(byte[] bytes) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < bytes.length; i = i + 2) {
            int ifIndex = (int) bytes[i] * 256 + ((int) bytes[i + 1] & 0xFF);
            result.add(ifIndex);
        }
        return result;
    }

    private int[] getAggregationPortIfIndices(byte[] rawOctets) {
        if (rawOctets.length % 2 != 0) {
            throw new IllegalStateException("invalid octets length.");
        }
        int[] aggregationPortIfIndices = new int[rawOctets.length / 2];
        for (int i = 0; i < aggregationPortIfIndices.length; i++) {
            aggregationPortIfIndices[i] = rawOctets[i * 2] * 256 + rawOctets[i * 2 + 1];
        }
        return aggregationPortIfIndices;
    }

    protected final static String snChasNumSlots = ".1.3.6.1.4.1.1991.1.1.1.1.24.0";

    public int getSnChasNumSlots() throws SocketTimeoutException, SocketException, AbortedException, IOException, SnmpResponseException, NoSuchMibException {
        int numberOfSlots = SnmpUtil.getInteger(snmp, snChasNumSlots);
        return numberOfSlots;
    }

    protected final static String snAgentConfigModuleDescription =
            foundryEnterpriseOID + ".1.1.2.2.1.1.2";

    public String getModuleName(int slotid)
            throws SocketTimeoutException, SocketException, AbortedException, IOException, SnmpResponseException, NoSuchMibException {
        String moduleName = SnmpUtil.getString(snmp, snAgentConfigModuleDescription + "." + slotid);
        return moduleName;
    }
}