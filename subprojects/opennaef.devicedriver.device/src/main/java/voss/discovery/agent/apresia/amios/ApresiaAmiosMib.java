package voss.discovery.agent.apresia.amios;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.apresia.apware.ApresiaMmrpMib;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.agent.util.DiscoveryUtils;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.*;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.discovery.utils.ByteArrayUtil;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApresiaAmiosMib {
    private final static Logger log = LoggerFactory.getLogger(ApresiaAmiosMib.class);

    public final static int PORT_BASE_IFINDEX = 10000;

    public final static int MGMT_PORT_BASE_IFINDEX = 20000;

    public final static int LAG_BASE_IFINDEX = 50000;

    public final static int MMRP_BASE_IFINDEX = 60000;

    public final static int VLAN_BASE_IFINDEX = 100 * 1000;
    public final static int EOE_MULTIPLIER = 10 * 1000;

    protected final SnmpAccess snmp;

    public ApresiaAmiosMib(SnmpAccess snmp) {
        this.snmp = snmp;
    }

    public static final String Snmpv2_SMI_hitachiCable_mibs = Mib2.Snmpv2_SMI_enterprises
            + ".278.2";
    public static final String agentIpAddress = Snmpv2_SMI_hitachiCable_mibs
            + ".1.1.2";
    public static final String agentNetMask = Snmpv2_SMI_hitachiCable_mibs
            + ".1.1.3";
    public static final String agentDGate = Snmpv2_SMI_hitachiCable_mibs
            + ".1.1.5";
    public static final String hclSyslogServerAddress = Snmpv2_SMI_hitachiCable_mibs
            + ".1.4.5.2.1.3";
    public static final String hclSwitchSlotType = Snmpv2_SMI_hitachiCable_mibs
            + ".5.4.2.1.2";
    public static final String hclSwitchSlotStatus = Snmpv2_SMI_hitachiCable_mibs
            + ".5.4.2.1.5";
    public static final String hclSwitchSlotSerialNo = Snmpv2_SMI_hitachiCable_mibs
            + ".5.4.2.1.7";
    public static final String hclIfMediaType = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.5";
    public static final String hclIfAdminStatus = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.6";
    public static final String hclIfName = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.22";
    public static final String hclIfFixSpeedDuplex = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.10";
    public static final String hclIfLinkStatus = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.7";
    public static final String hclIfAutoNegAdminStatus = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.8";
    public static final String hclIfLagId = Snmpv2_SMI_hitachiCable_mibs
            + ".5.3.1.1.23";
    public static final String hclIfLagName = Snmpv2_SMI_hitachiCable_mibs
            + ".30.5.2.1.1.7";

    public static final String hclIntVlan = Snmpv2_SMI_hitachiCable_mibs + ".30.26";
    public static final String hclIntVlanMapEntry = hclIntVlan + ".2.1";
    public static final String hclIntVlanMapNameSuffix = "4";
    public static final String hclIntVlanMapEoeModeSuffix = "5";
    public static final String hclIntVlanMapName = hclIntVlanMapEntry + "." + hclIntVlanMapNameSuffix;
    public static final String hclIntVlanMapEoeMode = hclIntVlanMapEntry + "." + hclIntVlanMapEoeModeSuffix;

    public static final String hclIntVlanLagEntry = hclIntVlan + ".3.1";

    public static final String hclIntVlanLagPortModeSuffix = "3";
    public static final String hclIntVlanLagPortMode = hclIntVlanLagEntry + "." + hclIntVlanLagPortModeSuffix;

    public static final String hclIntVlanMmrpPortEntry = hclIntVlan + ".4.1";

    public static final String hclIntVlanMmrpPortModeSuffix = "4";

    public static final String hclIntVlanMmrpPortMode = hclIntVlanMmrpPortEntry + "." + hclIntVlanMmrpPortModeSuffix;
    public static final int hclIntVlanMmrpPortMode_UNKNOWN = 0;
    public static final int hclIntVlanMmrpPortMode_EOE = 1;
    public static final int hclIntVlanMmrpPortMode_TAGGED = 2;
    public static final int hclIntVlanMmrpPortMode_UNTAGGED = 3;
    public static final int hclIntVlanMmrpPortMode_B_TAGGED = 4;

    public static final String hclIntVlanPortEntry = hclIntVlan + ".5.1";

    public static final String hclIntVlanPortModeSuffix = "4";
    public static final String hclIntVlanPortMode = hclIntVlanPortEntry + "." + hclIntVlanPortModeSuffix;

    public static final String hclVdrIndex = Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.1";

    public static final String hclVdrRevertiveMode = Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.3";

    public static final String hclVdrAdminStatus = Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.7";

    public static final String hclVdrName = Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.8";

    public static final String hclVdrUplinkIndex = Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.1";

    public static final String hclVdrUplinkInterface = Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.4";

    public static final String hclVdrUplinkIfFailureStatus = Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.6";

    private String getIpAddress(String oid) throws IOException,
            AbortedException {
        try {
            String result = SnmpUtil.getNextIpAddress(snmp, oid);
            if (result == null) {
                return null;
            }
            Matcher macher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+).*")
                    .matcher(result);
            if (macher.matches()) {
                result = macher.group(1);
            }
            return result;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getModelName() throws IOException, AbortedException {
        try {
            byte[] sysObjectID = SnmpUtil.getByte(snmp, Mib2.sysObjectID + ".0");
            log.debug(ByteArrayUtil.byteArrayToHexString(sysObjectID));
            switch (sysObjectID[sysObjectID.length - 1]) {
                case 31:
                    return "Apresia 18020";
                case 48:
                    return "Apresia 18005";
                case 61:
                    return "Apresia 16003";
                case 62:
                    return "Apresia 16006";
                case 63:
                    return "Apresia 16012";
                case 79:
                    return "Apresia 16012XL";
                case 87:
                    return "Apresia 26010";
                default:
                    return "Unknonw Apresia AMIOS Switch["
                            + sysObjectID[sysObjectID.length - 1] + ")";
            }
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getOsTypeName() throws IOException, AbortedException {
        return "AMIOS";
    }

    public CidrAddress getIpAddressWithSubnetmask() throws IOException,
            AbortedException {
        try {
            InetAddress inetAddress = InetAddress.getByName(getIpAddress(agentIpAddress));
            int masklen = SnmpUtil.getMaskLength(getIpAddress(agentNetMask));
            CidrAddress addr = new CidrAddress(inetAddress, masklen);
            return addr;
        } catch (UnknownHostException e) {
            throw new IOException("illegal agentIpAddress", e);
        }
    }

    public String getOSVersion() throws IOException, AbortedException {
        try {
            String description = SnmpUtil.getString(snmp, Mib2.sysDescr + ".0");
            int index = description.lastIndexOf("Ver.");
            if (index == -1) {
                return "unknown";
            } else {
                return description.substring(index + "Ver.".length());
            }
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getGatewayAddress() throws IOException, AbortedException {
        return getIpAddress(agentDGate);
    }

    public String[] getSyslogServerAddresses() throws IOException,
            AbortedException {
        try {
            List<String> result = SnmpUtil.getStringByWalk(snmp,
                    hclSyslogServerAddress);
            for (int i = result.size() - 1; 0 <= i; i--) {
                String each = result.get(i);
                if (each == null || "".equals(each)) {
                    result.remove(i);
                }
            }
            return result.toArray(new String[0]);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public List<SlotType> getSlotTypes() throws IOException, AbortedException {
        try {
            ArrayList<SlotType> result = new ArrayList<SlotType>();

            AbstractWalkProcessor<ArrayList<SlotType>> walker
                    = new AbstractWalkProcessor<ArrayList<SlotType>>(result) {
                private static final long serialVersionUID = 1L;

                public void process(VarBind varbind) {
                    result.add(new SlotType(hclSwitchSlotType, varbind));
                }
            };
            snmp.walk(hclSwitchSlotType, walker);
            result = walker.getResult();

            return result;
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public final static int SLOT_UNKNOWN = 1;
    public final static int SLOT_NOT_PRESENCE = 2;
    public final static int SLOT_IN_SERVICE = 3;
    public final static int SLOT_OUT_SERVICE = 4;
    public final static int SLOT_INITIALIZING = 5;
    public final static int SLOT_BOOTING = 6;
    public final static int SLOT_POWER_DOWN = 7;

    public Map<Integer, Integer> getSlotStatus() throws IOException,
            AbortedException {
        try {
            final Map<Integer, Integer> result = new HashMap<Integer, Integer>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclSwitchSlotStatus);
            for (IntSnmpEntry entry : entries) {
                int slotId = entry.oidSuffix[0].intValue();
                int status = entry.getValueAsBigInteger().intValue();
                result.put(slotId, status);
            }
            return result;
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public Map<Integer, String> getModuleSerialNumber() throws IOException,
            AbortedException {
        try {
            final Map<Integer, String> result = new HashMap<Integer, String>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp,
                    hclSwitchSlotSerialNo);
            for (StringSnmpEntry entry : entries) {
                int slotId = entry.oidSuffix[0].intValue();
                String serialNumber = entry.getValue();
                result.put(slotId, serialNumber);
            }
            return result;
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public List<PhysicalPortType> getPhysicalPortTypes(final int slotIndex)
            throws IOException, AbortedException {
        try {
            ArrayList<PhysicalPortType> ports = new ArrayList<PhysicalPortType>();

            AbstractWalkProcessor<ArrayList<PhysicalPortType>> walker
                    = new AbstractWalkProcessor<ArrayList<PhysicalPortType>>(ports) {
                private static final long serialVersionUID = 1L;

                public void process(VarBind varbind) {
                    result.add(new PhysicalPortType(hclIfMediaType, varbind));
                }
            };
            snmp.walk(hclIfMediaType, walker);
            ports = walker.getResult();

            List<PhysicalPortType> result = setupPhysicalPortType(ports,
                    slotIndex);
            return result;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private List<PhysicalPortType> setupPhysicalPortType(
            List<PhysicalPortType> ports, int slotIndex)
            throws UnexpectedVarBindException {
        ArrayList<PhysicalPortType> result = new ArrayList<PhysicalPortType>();
        for (PhysicalPortType each : ports) {
            each.setupValue();
            if (each.getSlotIndex() == slotIndex) {
                result.add(each);
            }
        }
        return result;
    }

    public List<PhysicalPortType> getPhysicalPortTypes()
            throws AbortedException, IOException {
        try {
            ArrayList<PhysicalPortType> result = new ArrayList<PhysicalPortType>();

            AbstractWalkProcessor<ArrayList<PhysicalPortType>> walker =
                    new AbstractWalkProcessor<ArrayList<PhysicalPortType>>(result) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new PhysicalPortType(hclIfMediaType, varbind));
                        }
                    };
            snmp.walk(hclIfMediaType, walker);
            result = walker.getResult();

            setupPhysicalPortType(result);
            return result;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setupPhysicalPortType(List<PhysicalPortType> result)
            throws UnexpectedVarBindException {
        for (PhysicalPortType each : result) {
            each.setupValue();
        }
    }

    public String getPortName(int ifIndex) throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp,
                    hclIfName);
            for (StringSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    return entry.getValue();
                }
            }
            return null;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getAdminStatus(int ifIndex) throws IOException,
            AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclIfAdminStatus);
            for (IntSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    if (entry.intValue() == 1) {
                        return "up";
                    } else if (entry.intValue() == 2) {
                        return "down";
                    } else {
                        return "unknown state(" + entry.intValue() + ")";
                    }
                }
            }
            return null;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public PortSpeedValue.Oper getOperationalSpeed(int ifIndex)
            throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclIfLinkStatus);
            for (IntSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    switch (entry.intValue()) {
                        case 1:
                        case 2:
                            return new PortSpeedValue.Oper(
                                    10 * 1000 * 1000, "10");
                        case 3:
                        case 4:
                            return new PortSpeedValue.Oper(
                                    100 * 1000 * 1000, "100");
                        case 5:
                            return new PortSpeedValue.Oper(
                                    1000 * 1000 * 1000, "1000");
                        case 6:
                            return new PortSpeedValue.Oper(
                                    10 * 1000 * 1000 * 1000, "10000");
                        case 21:
                            return new PortSpeedValue.Oper(0, "0");
                        case 22:
                            return new PortSpeedValue.Oper(0, "0");
                        default:
                            return new PortSpeedValue.Oper("unknown state("
                                    + entry.intValue() + ")");
                    }
                }
            }
            return null;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public PortSpeedValue.Admin getAdminSpeed(int ifIndex)
            throws IOException, AbortedException {
        if (isAutoNego(ifIndex)) {
            return PortSpeedValue.Admin.AUTO;
        }

        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclIfFixSpeedDuplex);
            for (IntSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    switch (entry.intValue()) {
                        case 1:
                        case 2:
                            return new PortSpeedValue.Admin(
                                    10 * 1000 * 1000, "10");
                        case 3:
                        case 4:
                            return new PortSpeedValue.Admin(
                                    100 * 1000 * 1000, "100");
                        case 5:
                            return new PortSpeedValue.Admin(
                                    1000 * 1000 * 1000, "1000");
                        case 6:
                            return new PortSpeedValue.Admin(
                                    10 * 1000 * 1000 * 1000, "10000");
                        default:
                            return new PortSpeedValue.Admin(
                                    "unknown state(" + entry.intValue() + ")");
                    }
                }
            }
            return null;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public EthernetPort.Duplex getDuplex(int ifIndex) throws IOException,
            AbortedException {
        if (isAutoNego(ifIndex)) {
            return EthernetPort.Duplex.AUTO;
        }
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclIfFixSpeedDuplex);
            for (IntSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    switch (entry.intValue()) {
                        case 1:
                        case 3:
                            return EthernetPort.Duplex.HALF;
                        case 2:
                        case 4:
                        case 5:
                        case 6:
                            return EthernetPort.Duplex.FULL;
                        default:
                            return new EthernetPort.Duplex("unknown state("
                                    + entry.intValue() + ")");
                    }
                }
            }
            return null;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private boolean isAutoNego(int ifIndex) throws IOException,
            AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    hclIfAutoNegAdminStatus);
            for (IntSnmpEntry entry : entries) {
                if (ifIndex == entry.getOIDSuffix(0)) {
                    if (entry.intValue() == 1) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public int getAggregationID(int ifIndex) throws IOException,
            AbortedException {
        try {
            Integer id = SnmpUtil.getInteger(snmp, hclIfLagId + "." + ifIndex + ".1");
            if (0 < id) {
                return id;
            } else {
                return -1;
            }
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getAggregationName(int aggregationID) throws IOException,
            AbortedException {
        try {
            String aggregationName = SnmpUtil.getString(snmp, hclIfLagName
                    + "." + aggregationID);
            if (aggregationName != null) {
                aggregationName = aggregationName.replaceAll("\"", "");
            }
            return aggregationName;
        } catch (NoSuchMibException e) {
            return "N/A";
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public List<VlanNameEntry> getVlanNames() throws IOException, AbortedException {
        try {
            ArrayList<VlanNameEntry> vlans = new ArrayList<VlanNameEntry>();

            AbstractWalkProcessor<ArrayList<VlanNameEntry>> walker =
                    new AbstractWalkProcessor<ArrayList<VlanNameEntry>>(vlans) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new VlanNameEntry(hclIntVlanMapName, varbind));
                        }
                    };
            snmp.walk(hclIntVlanMapName, walker);
            for (VlanNameEntry each : vlans) {
                each.setup();
            }
            return vlans;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public List<IntVlanLagPortModeEntry> getVlanLagPortModes() throws IOException, AbortedException {
        try {
            ArrayList<IntVlanLagPortModeEntry> vlans = new ArrayList<IntVlanLagPortModeEntry>();

            AbstractWalkProcessor<ArrayList<IntVlanLagPortModeEntry>> walker =
                    new AbstractWalkProcessor<ArrayList<IntVlanLagPortModeEntry>>(vlans) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new IntVlanLagPortModeEntry(hclIntVlanMapName, varbind));
                        }
                    };
            snmp.walk(hclIntVlanLagPortMode, walker);
            for (IntVlanLagPortModeEntry each : vlans) {
                each.setup();
            }
            return vlans;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public List<IntVlanPortModeEntry> getVlanPortModes() throws IOException, AbortedException {
        try {
            ArrayList<IntVlanPortModeEntry> vlans = new ArrayList<IntVlanPortModeEntry>();

            AbstractWalkProcessor<ArrayList<IntVlanPortModeEntry>> walker =
                    new AbstractWalkProcessor<ArrayList<IntVlanPortModeEntry>>(vlans) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            result.add(new IntVlanPortModeEntry(hclIntVlanMapName, varbind));
                        }
                    };
            snmp.walk(hclIntVlanPortMode, walker);
            for (IntVlanPortModeEntry each : vlans) {
                each.setup();
            }
            return vlans;
        } catch (UnexpectedVarBindException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void buildMmrp(Device device) throws IOException, AbortedException {
        MibTable mmrpTable = new MibTable(snmp, "hclMmrpV2RingTable", ApresiaMmrpMib.hclMmrpV2RingTable);
        mmrpTable.addColumn("1", "hclMmrpV2RingIndex");
        mmrpTable.addColumn("3", "hclMmrpV2RingName");
        mmrpTable.addColumn("9", "hclMmrpV2AdminStatus");
        mmrpTable.walk();

        Map<Integer, ApresiaMmrpRing> rings = new HashMap<Integer, ApresiaMmrpRing>();
        for (TableRow row : mmrpTable.getRows()) {
            IntSnmpEntry ringIDEntry = row.getColumnValue("1", SnmpHelper.intEntryBuilder);
            int ringID = ringIDEntry.intValue();
            StringSnmpEntry ringNameEntry = row.getColumnValue("3", SnmpHelper.stringEntryBuilder);
            String ringName = ringNameEntry.getValue();
            if (ringName == null || ringName.isEmpty()) {
                ringName = "unnamed_mmrp[" + ringID + "]";
            }
            IntSnmpEntry ringStatusEntry = row.getColumnValue("9", SnmpHelper.intEntryBuilder);
            int ringStatus = ringStatusEntry.intValue();
            ApresiaMmrpRing ring = new ApresiaMmrpRing();
            ring.initDevice(device);
            ring.initIfName("mmrp-" + ringID);
            ring.setRingName(ringName);
            ring.setMmrpRingId(ringID);
            switch (ringStatus) {
                case 1:
                    ring.setAdminStatus("Enabled");
                    break;
                case 2:
                    ring.setAdminStatus("Disabled");
                    break;
                default:
                    log.warn("unexpected response: mmrp-adminstatus: " + ringStatus);
            }
            rings.put(ringID, ring);
        }
        buildMmrpPortBinding(device, rings);
        buildMmrpVlanBinding(device, rings);
    }

    private void buildMmrpVlanBinding(Device device, Map<Integer, ApresiaMmrpRing> rings) throws IOException {
        if (!(device instanceof GenericEthernetSwitch)) {
            return;
        }
        GenericEthernetSwitch sw = (GenericEthernetSwitch) device;
        try {
            List<IntSnmpEntry> bindings = SnmpUtil.getIntSnmpEntries(snmp, hclIntVlanMmrpPortMode);
            for (IntSnmpEntry binding : bindings) {
                int ringID = binding.oidSuffix[0].intValue();
                int eoeID = binding.oidSuffix[2].intValue();
                int vlanID = binding.oidSuffix[3].intValue();
                log.debug("Target MMRP ring is [" + ringID + "]");
                ApresiaMmrpRing ring = rings.get(ringID);
                if (ring == null) {
                    log.debug("- Unknown mmrp: " + ringID);
                    continue;
                }
                log.debug("- Bound to vlan: " + eoeID + "." + vlanID);
                VlanIf vlanIf = sw.getVlanIfBy(eoeID, vlanID);
                if (vlanIf == null) {
                    log.debug("- Unknown mmrp member vlan: " + eoeID + "." + vlanID);
                    continue;
                }
                int mmrpMode = binding.intValue();
                switch (mmrpMode) {
                    case hclIntVlanMmrpPortMode_EOE:
                        log.debug("- Bind as eoe (" + mmrpMode + ")");
                        bindVlanToMmrp(ring, vlanIf);
                        break;
                    case hclIntVlanMmrpPortMode_TAGGED:
                        log.debug("- Bind as tagged (" + mmrpMode + ")");
                        bindVlanToMmrp(ring, vlanIf);
                        break;
                    case hclIntVlanMmrpPortMode_UNTAGGED:
                        log.debug("- Bind as untagged (" + mmrpMode + ")");
                        bindVlanToMmrp(ring, vlanIf);
                        break;
                    case hclIntVlanMmrpPortMode_B_TAGGED:
                        log.debug("- Bind as b_tagged (" + mmrpMode + ")");
                        bindVlanToMmrp(ring, vlanIf);
                        break;
                    default:
                        log.warn("- Not bound: unexpected bindingMode:" + mmrpMode);
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void bindVlanToMmrp(ApresiaMmrpRing ring, VlanIf vlanIf) {
        ring.addMemberVlanIf(vlanIf);
        bindVlanToLogicalEthernetPort(vlanIf, ring.getMasterPort());
        bindVlanToLogicalEthernetPort(vlanIf, ring.getSlavePort());
        for (Port aware : ring.getAwarePorts()) {
            bindVlanToLogicalEthernetPort(vlanIf, aware);
        }
    }

    private void bindVlanToLogicalEthernetPort(VlanIf vlanIf, Port port) {
        LogicalEthernetPort le = DiscoveryUtils.getLogicalEthernetPort(port);
        if (le != null) {
            vlanIf.addTaggedPort(le);
        }
    }

    private void buildMmrpPortBinding(Device device, Map<Integer, ApresiaMmrpRing> rings) throws IOException {
        if (!(device instanceof GenericEthernetSwitch)) {
            return;
        }
        GenericEthernetSwitch sw = (GenericEthernetSwitch) device;
        try {
            List<IntSnmpEntry> bindings = SnmpUtil.getIntSnmpEntries(snmp, ApresiaMmrpMib.hclMmrpV2IfMode);
            for (IntSnmpEntry binding : bindings) {
                int ringID = binding.oidSuffix[0].intValue();
                int index = binding.oidSuffix[1].intValue();
                ApresiaMmrpRing ring = rings.get(ringID);
                if (ring == null) {
                    log.debug("unknown mmrp: " + ringID);
                    continue;
                }
                Port port = null;
                if (index >= 50000) {
                    int lagID = index - LAG_BASE_IFINDEX;
                    port = sw.getEthernetPortsAggregatorByAggregationGroupId(lagID);
                } else if (index > 30000) {
                    port = device.getPortByIfIndex(index);
                }
                if (port == null) {
                    log.debug("unknown mmrp member port: index=" + index);
                    continue;
                }
                int ifMode = binding.intValue();
                switch (ifMode) {
                    case 1:
                        log.warn("mode is unknown: ignored:" + port.getFullyQualifiedName());
                        break;
                    case 2:
                        ring.setMasterPort(port);
                        break;
                    case 3:
                        ring.setSlavePort(port);
                        break;
                    case 4:
                        ring.addAwarePort(port);
                        break;
                    default:
                        log.warn("unexpected ifMode:" + ifMode + " on " + port.getFullyQualifiedName());
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void close() throws IOException, voss.discovery.iolib.console.ConsoleException {
        snmp.close();
    }
}