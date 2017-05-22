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
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.AlaxalaQosFlowListEntry;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Alaxala7800SMibImpl extends AlaxalaMibImpl {
    private AlaxalaVendorProfile profile;

    public Alaxala7800SMibImpl(SnmpAccess snmp, AlaxalaVendorProfile profile) {
        super(snmp, profile);
        this.profile = profile;
    }

    public AlaxalaPortEntry[] getPhysicalPorts() throws IOException, AbortedException {
        try {
            List<AlaxalaPortEntry> result = new ArrayList<AlaxalaPortEntry>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, profile.getAxIfIndexOid());
            for (IntSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 4;

                int slotIndex = entry.oidSuffix[1].intValue() - 1;
                int portIndex = entry.oidSuffix[2].intValue() - 1;
                int ifIndex = entry.intValue();
                result.add(new Alaxala7800SPortEntryImpl(ifIndex, slotIndex, portIndex));
            }
            return result.toArray(new AlaxalaPortEntry[0]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public String getModelName() throws IOException, AbortedException {
        return profile.getModelName(snmp);
    }

    public String getOsType() throws IOException, AbortedException {
        try {
            String osType =
                    SnmpUtil.getString(snmp, profile.getOsNameOid() + ".0");
            return osType;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getOsVersion() throws IOException, AbortedException {
        try {
            String OsVersion =
                    SnmpUtil.getString(snmp, profile.getOsVersionOid() + ".0");
            return OsVersion;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public int getNumberOfSlot() throws IOException, AbortedException {
        try {
            int numberOfSlot = SnmpUtil.getInteger(snmp, profile.getNumberOfSlotOid() + ".1");
            return numberOfSlot;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getNifBoardName(int slotId) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries
                    = SnmpUtil.getIntSnmpEntries(snmp, profile.getBoardNameOid());
            for (IntSnmpEntry entry : entries) {
                if (entry.oidSuffix[1].intValue() == slotId) {
                    return new String(entry.value);
                }
            }
            return "?";
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public int getNumberOfPort(int slotId) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries
                    = SnmpUtil.getIntSnmpEntries(snmp, profile.getNumberOfPortOid());
            for (IntSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 2;
                if (entry.oidSuffix[1].intValue() == slotId) {
                    return entry.intValue();
                }
            }
            return -1;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static InterfaceType getInterfaceTypeByIfIndex(int ifIndex) {
        if (isLoopback(ifIndex)) {
            return InterfaceType.LOOPBACK;
        } else if (isRmEther(ifIndex)) {
            return InterfaceType.RMETHER;
        } else if (isDefaultVlan(ifIndex)) {
            return InterfaceType.DEFAULTVLAN;
        } else if (isLine(ifIndex)) {
            return InterfaceType.PORT;
        } else if (isTagVlan(ifIndex)) {
            return InterfaceType.TAGVLAN;
        } else if (isVlanIf(25000)) {
            return InterfaceType.VLAN;
        } else if (isLinkAggregation(ifIndex)) {
            return InterfaceType.LAG;
        } else if (isTunnel(ifIndex)) {
            return InterfaceType.TUNNEL;
        } else {
            return InterfaceType.UNKNOWN;
        }
    }

    static class InterfaceType {
        public final static InterfaceType LOOPBACK = new InterfaceType("loopback");
        public final static InterfaceType RMETHER = new InterfaceType("rmether");
        public final static InterfaceType DEFAULTVLAN = new InterfaceType("defaultvlan");
        public final static InterfaceType PORT = new InterfaceType("port");
        public final static InterfaceType TAGVLAN = new InterfaceType("tag-vlan");
        public final static InterfaceType VLAN = new InterfaceType("vlan");
        public final static InterfaceType LAG = new InterfaceType("LAG");
        public final static InterfaceType TUNNEL = new InterfaceType("tunnel");
        public final static InterfaceType UNKNOWN = new InterfaceType("unknown");

        private String interfaceType;

        InterfaceType(String _type) {
            interfaceType = _type;
        }

        public String getInterfaceTypeName() {
            return interfaceType;
        }
    }

    protected static final int ifIndexForLoopback = 1;
    protected static final int ifIndexForRmEther = 2;
    protected static final int ifIndexForDefaultVlan = 3;
    protected static final int ifIndexForLine = 101;
    protected static final int ifIndexForTagVlan = 2001;
    protected static final int ifIndexForVlanIf = 20001;
    protected static final int ifIndexForLinkAggregation = 25001;
    protected static final int ifIndexForTunnl = 30001;
    protected static final int ifIndexEnd = 40000;
    public static boolean isLoopback(int ifIndex) {
        return (ifIndexForLoopback <= ifIndex) && (ifIndex < ifIndexForRmEther);
    }

    public static boolean isRmEther(int ifIndex) {
        return (ifIndexForRmEther <= ifIndex) && (ifIndex < ifIndexForDefaultVlan);
    }

    public static boolean isDefaultVlan(int ifIndex) {
        return (ifIndexForDefaultVlan <= ifIndex) && (ifIndex < ifIndexForLine);
    }

    public static boolean isLine(int ifIndex) {
        return (ifIndexForLine <= ifIndex) && (ifIndex < ifIndexForTagVlan);
    }

    public static boolean isTagVlan(int ifIndex) {
        return (ifIndexForTagVlan <= ifIndex) && (ifIndex < ifIndexForVlanIf);
    }

    public static boolean isVlanIf(int ifIndex) {
        return (ifIndexForVlanIf <= ifIndex) && (ifIndex < ifIndexForLinkAggregation);
    }

    public static boolean isLinkAggregation(int ifIndex) {
        return (ifIndexForLinkAggregation <= ifIndex) && (ifIndex < ifIndexForTunnl);
    }

    public static boolean isTunnel(int ifIndex) {
        return (ifIndexForTunnl <= ifIndex) && (ifIndex < ifIndexEnd);
    }

    @Override
    public AlaxalaQosFlowListEntry[] getQosFlowEntries() throws IOException, AbortedException {
        assert profile.getFlowQosInBaseOid() != null;
        assert profile.getFlowQosInPremiumBaseOid() != null;
        assert profile.getFlowQosOutBaseOid() != null;
        assert profile.getFlowQosOutPremiumBaseOid() != null;

        List<AlaxalaQosFlowListEntry> result = new ArrayList<AlaxalaQosFlowListEntry>();
        try {
            List<StringSnmpEntry> entries =
                    SnmpUtil.getStringSnmpEntries(snmp, profile.getFlowQosInBaseOid() + ".1.4");
            for (StringSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 3;
                int ifIndex = entry.oidSuffix[0].intValue();
                String flowKey =
                        "." + entry.oidSuffix[0]
                                + "." + entry.oidSuffix[1]
                                + "." + entry.oidSuffix[2];
                String flowName = makeFlowName(ifIndex,
                        AlaxalaQosFlowListEntry.DIRECTION_IN,
                        AlaxalaQosFlowListEntry.QOS_CLASS_NORMAL,
                        entry.oidSuffix[2]);
                AlaxalaQosFlowListEntry profile =
                        new AlaxalaQosFlowListEntry(flowName, flowKey, ifIndex,
                                AlaxalaQosFlowListEntry.DIRECTION_IN,
                                AlaxalaQosFlowListEntry.QOS_CLASS_NORMAL);
                result.add(profile);
            }

            List<StringSnmpEntry> inPremEntries =
                    SnmpUtil.getStringSnmpEntries(snmp, profile.getFlowQosInPremiumBaseOid() + ".1.4");
            for (StringSnmpEntry entry : inPremEntries) {
                assert entry.oidSuffix.length == 3;
                int ifIndex = entry.oidSuffix[0].intValue();
                String flowKey =
                        "." + entry.oidSuffix[0]
                                + "." + entry.oidSuffix[1]
                                + "." + entry.oidSuffix[2];
                String flowName = makeFlowName(ifIndex,
                        AlaxalaQosFlowListEntry.DIRECTION_IN,
                        AlaxalaQosFlowListEntry.QOS_CLASS_PREMIUM,
                        entry.oidSuffix[2]);
                AlaxalaQosFlowListEntry profile =
                        new AlaxalaQosFlowListEntry(flowName, flowKey, ifIndex,
                                AlaxalaQosFlowListEntry.DIRECTION_IN,
                                AlaxalaQosFlowListEntry.QOS_CLASS_PREMIUM);
                result.add(profile);
            }
            List<StringSnmpEntry> outEntries =
                    SnmpUtil.getStringSnmpEntries(snmp, profile.getFlowQosOutBaseOid() + ".1.4");
            for (StringSnmpEntry entry : outEntries) {
                assert entry.oidSuffix.length == 3;
                int ifIndex = entry.oidSuffix[0].intValue();
                String flowKey =
                        "." + entry.oidSuffix[0]
                                + "." + entry.oidSuffix[1]
                                + "." + entry.oidSuffix[2];
                String flowName = makeFlowName(ifIndex,
                        AlaxalaQosFlowListEntry.DIRECTION_OUT,
                        AlaxalaQosFlowListEntry.QOS_CLASS_NORMAL,
                        entry.oidSuffix[2]);
                AlaxalaQosFlowListEntry profile =
                        new AlaxalaQosFlowListEntry(flowName, flowKey, ifIndex,
                                AlaxalaQosFlowListEntry.DIRECTION_OUT,
                                AlaxalaQosFlowListEntry.QOS_CLASS_NORMAL);
                result.add(profile);
            }
            List<StringSnmpEntry> outPremEntries =
                    SnmpUtil.getStringSnmpEntries(snmp, profile.getFlowQosOutPremiumBaseOid() + ".1.4");
            for (StringSnmpEntry entry : outPremEntries) {
                assert entry.oidSuffix.length == 3;
                int ifIndex = entry.oidSuffix[0].intValue();
                String flowKey =
                        "." + entry.oidSuffix[0]
                                + "." + entry.oidSuffix[1]
                                + "." + entry.oidSuffix[2];
                String flowName = makeFlowName(ifIndex,
                        AlaxalaQosFlowListEntry.DIRECTION_OUT,
                        AlaxalaQosFlowListEntry.QOS_CLASS_PREMIUM,
                        entry.oidSuffix[2]);
                AlaxalaQosFlowListEntry profile =
                        new AlaxalaQosFlowListEntry(flowName, flowKey, ifIndex,
                                AlaxalaQosFlowListEntry.DIRECTION_OUT,
                                AlaxalaQosFlowListEntry.QOS_CLASS_PREMIUM);
                result.add(profile);
            }

            return result.toArray(new AlaxalaQosFlowListEntry[result.size()]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private String makeFlowName(int ifIndex, String direction, String qosClass, BigInteger listId) {
        assert direction != null;
        assert listId != null;

        StringBuffer result = new StringBuffer();
        result.append("[");
        result.append(direction);
        result.append("]");
        if (qosClass != null && !qosClass.equals("")) {
            result.append("[");
            result.append(qosClass);
            result.append("]");
        }
        result.append(listId);
        return result.toString();
    }

}