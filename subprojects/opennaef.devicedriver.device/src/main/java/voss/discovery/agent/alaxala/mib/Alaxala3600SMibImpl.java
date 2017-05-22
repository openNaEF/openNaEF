package voss.discovery.agent.alaxala.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.alaxala.Alaxala3600SPortEntryImpl;
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

public class Alaxala3600SMibImpl extends AlaxalaMibImpl {
    public Alaxala3600SMibImpl(SnmpAccess snmp, AlaxalaVendorProfile profile) {
        super(snmp, profile);
    }

    @Override
    public String getOsType() throws IOException, AbortedException {
        return null;
    }

    @Override
    public int getNumberOfPort(int slotId) throws IOException, AbortedException {
        try {
            return SnmpUtil.getInteger(snmp, profile.getNumberOfPortOid() + ".1");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }

    }

    @Override
    public int getNumberOfSlot() throws IOException, AbortedException {
        return 0;
    }

    @Override
    public String getNifBoardName(int slotId) throws IOException, AbortedException {
        return "";
    }

    @Override
    public String getPhysicalLineConnectorType(int slot, int port) throws IOException, AbortedException {
        try {
            String oid = profile.getPhysLineTransceiverStatusOid() + ".1." + port;
            int typeId = SnmpUtil.getInteger(snmp, oid);
            switch (typeId) {
                case 1:
                    return "10/100/1000BASE-T";
                case 20:
                    return getPhysicalLineConnectorTypeDetail(slot, port);
                case 21:
                    return "(SFP:Not used)";
                case 22:
                    return "(SFP:Unsupported)";
                case 23:
                    return "(SFP:Status unknown)";
                case 30:
                    return getPhysicalLineConnectorTypeDetail(slot, port);
                case 31:
                    return "(XFP:Not used)";
                case 32:
                    return "(XFP:Unsupported)";
                case 33:
                    return "(XFP:Status unknown)";
                default:
                    return null;
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getPhysicalLineConnectorTypeDetail(int slot, int port) throws IOException, AbortedException {
        try {
            int typeId = SnmpUtil.getInteger(snmp, profile.getPhysLineConnectorTypeOid() + ".1." + port);
            switch (typeId) {
                case 1:
                    return "other";
                case 301:
                    return "1000BASE-LX";
                case 302:
                    return "1000BASE-SX";
                case 303:
                    return "1000BASE-LH";
                case 402:
                    return "10GBASE-LR";
                default:
                    return "unknown";
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getPhysicalLineConnectorType(int ifIndex) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries =
                    SnmpUtil.getIntSnmpEntries(snmp, profile.getPhysLineTransceiverStatusOid());
            for (IntSnmpEntry entry : entries) {
                System.out.println(">" + entry.varbind.getOid().getOidString());
                int portId = entry.oidSuffix[1].intValue();
                if (ifIndex == Alaxala3600SPortEntryImpl.getIfIndex(portId)) {
                    switch (entry.getValueAsBigInteger().intValue()) {
                        case 1:
                            return "10/100/1000BASE-T";
                        case 20:
                            return getPhysicalLineConnectorTypeDetail(ifIndex);
                        case 21:
                            return "(SFP:Not used)";
                        case 22:
                            return "(SFP:Unsupported)";
                        case 23:
                            return "(SFP:Status unknown)";
                        case 30:
                            return getPhysicalLineConnectorTypeDetail(ifIndex);
                        case 31:
                            return "(XFP:Not used)";
                        case 32:
                            return "(XFP:Unsupported)";
                        case 33:
                            return "(XFP:Status unknown)";
                        default:
                            break;
                    }
                }
            }
            return null;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    protected String getTransceiverType(int _ifIndex) {
        return null;
    }

    public String getPhysicalLineConnectorTypeDetail(int ifIndex) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, profile.getPhysLineConnectorTypeOid());
            for (IntSnmpEntry entry : entries) {
                int portId = entry.oidSuffix[1].intValue();
                if (ifIndex == (9 + portId)) {
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
                            case 402:
                                return "10GBASE-LR";
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

    public static int getIfIndexByPortId(int portId) {
        return portId + 9;
    }

    @Override
    public AlaxalaPortEntry[] getPhysicalPorts() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    profile.getPhysLineTransceiverStatusOid());
            List<AlaxalaPortEntry> result = new ArrayList<AlaxalaPortEntry>();
            for (IntSnmpEntry entry : entries) {
                int portId = entry.oidSuffix[1].intValue();
                AlaxalaPortEntry port = new Alaxala3600SPortEntryImpl(portId);
                result.add(port);
            }
            return result.toArray(new AlaxalaPortEntry[result.size()]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AlaxalaQosFlowListEntry[] getQosFlowEntries() throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries =
                    SnmpUtil.getStringSnmpEntries(snmp, profile.getQosFlowStatsInListName());
            List<AlaxalaQosFlowListEntry> result = new ArrayList<AlaxalaQosFlowListEntry>();
            for (StringSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 4;
                String flowName = entry.getValue();
                int portIfIndex = entry.oidSuffix[0].intValue();
                String flowKey =
                        "." + entry.oidSuffix[0]
                                + "." + entry.oidSuffix[1]
                                + "." + entry.oidSuffix[2]
                                + "." + entry.oidSuffix[3];
                AlaxalaQosFlowListEntry profile = new AlaxalaQosFlowListEntry(flowName, flowKey, portIfIndex);
                result.add(profile);
            }
            return result.toArray(new AlaxalaQosFlowListEntry[result.size()]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

}