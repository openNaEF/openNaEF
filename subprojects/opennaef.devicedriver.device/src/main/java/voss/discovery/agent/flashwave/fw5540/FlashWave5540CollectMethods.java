package voss.discovery.agent.flashwave.fw5540;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.GenericEthernetSwitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlashWave5540CollectMethods {
    private static final Logger log = LoggerFactory.getLogger(FlashWave5540CollectMethods.class);

    public static final int LAG_BASE_ID = 10000000;
    public static final int IFINDEX_SLOT_MULTIPLIER = 100000;
    public static final int IFINDEX_PORT_MULTIPLIER = 1000;

    public static final String VENDOR_ID = "211";
    public static final String ENTERPRISE_BASE_OID = Mib2.Snmpv2_SMI_enterprises
            + "." + VENDOR_ID;

    private final SnmpAccess snmp;

    public FlashWave5540CollectMethods(SnmpAccess snmp) {
        this.snmp = snmp;
    }

    public String getSysName() throws IOException, AbortedException {
        return Mib2Impl.getSysName(snmp);
    }

    public String getManagementIpAddress() {
        return snmp.getSnmpAgentAddress().getAddress().getHostAddress();
    }

    public String getModelTypeName() {
        return null;
    }

    public String getOsVersion() {
        try {
            String sysDescr = Mib2Impl.getSysDescr(snmp);
            String[] arr = sysDescr.split("' '");
            if (arr.length >= 2) {
                return arr[1];
            }
        } catch (Exception e) {
            log.error("failed to get sysDescr.", e);
        }
        return null;
    }

    public static final String fwEtherPortType = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.3.1.1.2";

    public List<Integer> getPorts() throws IOException, AbortedException {
        try {
            List<Integer> result = new ArrayList<Integer>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, fwEtherPortType);
            for (IntSnmpEntry entry : entries) {
                int ifindex = entry.getLastOIDIndex().intValue();
                result.add(ifindex);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public String getConfigName(GenericEthernetSwitch device, int ifindex) {
        int slotId = getSlotId(ifindex);
        int portId = getPortId(ifindex);
        log.debug("build config-name: slot-id=" + slotId + ", port-id=" + portId);
        String prefix = "";
        String liuTypeName = device.getSlotBySlotIndex(slotId).getModule().getSystemDescription();
        if (liuTypeName.startsWith("fe")) {
            prefix = "fastethernet";
        } else if (liuTypeName.startsWith("ge")) {
            prefix = "gigabitethernet";
        } else {
            prefix = "interface";
        }
        String ifname = prefix + " " + slotId + "/" + portId;
        return ifname;
    }

    public int getSlotId(int ifindex) {
        if (ifindex < IFINDEX_SLOT_MULTIPLIER) {
            throw new IllegalArgumentException();
        }
        return ifindex / IFINDEX_SLOT_MULTIPLIER;
    }

    public int getPortId(int ifindex) {
        if (ifindex < IFINDEX_SLOT_MULTIPLIER) {
            throw new IllegalArgumentException();
        }
        return (ifindex % IFINDEX_SLOT_MULTIPLIER) / IFINDEX_PORT_MULTIPLIER;
    }

    public String getLinkAggregationName(int ifindex) {
        if (ifindex < LAG_BASE_ID) {
            throw new IllegalArgumentException();
        }
        return "link-aggregation " + (ifindex - LAG_BASE_ID);
    }

    public int getIfIndex(int slot, int port) {
        if (slot <= 0 || port <= 0) {
            throw new IllegalArgumentException();
        }
        return slot * IFINDEX_SLOT_MULTIPLIER + port * IFINDEX_PORT_MULTIPLIER + 1;
    }

    public static int getLagIfIndex(int lagId) {
        return lagId + LAG_BASE_ID;
    }

    public static final String fwVlanName = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.2.2.1.2";

    public Map<Integer, String> getVlanIfs() throws IOException,
            AbortedException {
        try {
            Map<Integer, String> result = new LinkedHashMap<Integer, String>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp,
                    fwVlanName);
            for (StringSnmpEntry entry : entries) {
                int vlanId = entry.getLastOIDIndex().intValue();
                String vlanName = entry.getValue();

                if ("null".equals(vlanName)) {
                    vlanName = null;
                }

                result.put(vlanId, vlanName);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static final String fwTagVwanTagList = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.4.1.1.1";

    public List<Integer> getTaggedVlanList(int ifindex) throws IOException,
            AbortedException {
        try {
            List<Integer> result = new ArrayList<Integer>();
            String oid = fwTagVwanTagList + "." + ifindex;
            try {
                byte[] tagVwanTagList = SnmpUtil.getByte(snmp, oid);
                int[] vlanList = SnmpUtil.decodeBitList(tagVwanTagList);
                for (int vlan : vlanList) {
                    vlan--;
                    result.add(vlan);
                }
            } catch (NoSuchMibException nsme) {
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public static final String fwVportUserVlanTag = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.5.1.1.2";

    public Map<Integer, List<Integer>> getVports() throws IOException,
            AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    fwVportVwanTag);
            Map<Integer, List<Integer>> result = new LinkedHashMap<Integer, List<Integer>>();
            for (IntSnmpEntry entry : entries) {
                if (entry.oidSuffix.length != 2) {
                    throw new IllegalStateException(
                            "illegal oidsuffix length: "
                                    + entry.oidSuffix.length);
                }
                int vPortIndex = entry.oidSuffix[entry.oidSuffix.length - 1]
                        .intValue();
                int ifindex = entry.oidSuffix[entry.oidSuffix.length - 2]
                        .intValue();

                List<Integer> vports = result.get(ifindex);
                if (vports == null) {
                    vports = new ArrayList<Integer>();
                    result.put(ifindex, vports);
                }
                vports.add(vPortIndex);
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public int getInnerVlanId(int ifindex, int vportId) throws IOException,
            AbortedException {
        try {
            String oid = fwVportVwanTag + "." + ifindex + "." + vportId;
            int outerVlanId = SnmpUtil.getInteger(snmp, oid);
            return outerVlanId;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static final String fwVportVwanTag = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.5.1.1.3";

    public int getOuterVlanId(int ifindex, int vportId) throws IOException,
            AbortedException {
        try {
            String oid = fwVportUserVlanTag + "." + ifindex + "." + vportId;
            int innerVlanId = SnmpUtil.getInteger(snmp, oid);
            return innerVlanId;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static final String fwVwanRowStatus = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.5.1.1.4";

    public static final String fwVportDescriptionAlias = ENTERPRISE_BASE_OID
            + ".1.24.7.1.1.2.5.5.2.1.2";

    public String getVportDescription(int ifindex, int vportId)
            throws IOException, AbortedException {
        try {
            String oid = fwVportDescriptionAlias + "." + ifindex + "."
                    + vportId;
            String description = SnmpUtil.getString(snmp, oid);
            return description;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            return "";
        }
    }

    public int getFfpWorkingIfIndex(int ffpIndex) throws AbortedException, IOException {
        try {
            final String fwEtherFfpWork = ".1.3.6.1.4.1.211.1.24.7.1.1.2.9.1.1.1";
            final String fwEtherFfpProt = ".1.3.6.1.4.1.211.1.24.7.1.1.2.9.1.1.2";
            final String fwEtherFfpSwitchSelStatus = ".1.3.6.1.4.1.211.1.24.7.1.1.2.9.2.1.2";

            int selStatus = SnmpUtil.getInteger(snmp, fwEtherFfpSwitchSelStatus + "." + ffpIndex);
            switch (selStatus) {
                case 1:
                    return SnmpUtil.getInteger(snmp, fwEtherFfpWork + "." + ffpIndex);
                case 2:
                    return SnmpUtil.getInteger(snmp, fwEtherFfpProt + "." + ffpIndex);
                default:
                    throw new IllegalArgumentException();
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }

    }
}