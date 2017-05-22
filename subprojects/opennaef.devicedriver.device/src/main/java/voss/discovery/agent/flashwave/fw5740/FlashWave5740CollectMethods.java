package voss.discovery.agent.flashwave.fw5740;


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
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;

import java.io.IOException;
import java.util.*;

public class FlashWave5740CollectMethods {
    private static final Logger log = LoggerFactory.getLogger(FlashWave5740CollectMethods.class);

    public static final int LAG_BASE_ID = 10 * 1000 * 1000;
    public static final int IFINDEX_SLOT_MULTIPLIER = 100 * 1000;
    public static final int IFINDEX_PORT_MULTIPLIER = 1000;

    public static final String VENDOR_ID = "211";
    public static final String ENTERPRISE_BASE_OID = Mib2.Snmpv2_SMI_enterprises + "." + VENDOR_ID;
    public static final String DEVICE_BASE_OID = ENTERPRISE_BASE_OID + ".1.24.7.10";

    private final SnmpAccess snmp;

    public FlashWave5740CollectMethods(SnmpAccess snmp) {
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

    public static final String fwEqConfLiuType = DEVICE_BASE_OID + ".1.2.2.7.1.2";
    public static final String fwEqPILiuSerialNum = DEVICE_BASE_OID + ".1.2.2.8.1.4";

    public void getSlotAndModules(SnmpAccess snmp, Device device) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, fwEqConfLiuType);
            for (IntSnmpEntry entry : entries) {
                int slotID = entry.getLastOIDIndex().intValue();
                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotIndex(slotID);
                slot.initSlotId(String.valueOf(slotID));
                log.debug("@" + device.getDeviceName() + " add slot: slot-id=" + slotID);
                int value = entry.intValue();
                String moduleName = null;
                switch (value) {
                    case 0:
                        moduleName = null;
                        break;
                    case 1:
                        moduleName = "gexp10";
                        break;
                    case 11:
                        moduleName = "tengerp1";
                        break;
                    default:
                        moduleName = "Unknown(" + value + ")";
                }
                if (moduleName == null) {
                    continue;
                }
                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(moduleName);
                module.setSystemDescription(moduleName);
                log.debug("@" + device.getDeviceName() + " add module: slot-id=" + slotID + " type=" + moduleName);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void getModuleSerialNumber(SnmpAccess snmp, Device device) throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, fwEqPILiuSerialNum);
            for (StringSnmpEntry entry : entries) {
                int slotID = entry.getLastOIDIndex().intValue();
                Slot slot = device.getSlotBySlotIndex(slotID);
                if (slot == null || slot.getModule() == null) {
                    continue;
                }
                Module module = slot.getModule();
                module.setSerialNumber(entry.getValue());
                log.debug("@" + device.getDeviceName() + " set module=" + slotID + " serial=" + entry.getValue());
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public static final String fwEtherPortType = DEVICE_BASE_OID + ".1.2.3.1.1.2";

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

    public Map<Integer, Integer> getPortTypes() throws IOException, AbortedException {
        try {
            Map<Integer, Integer> result = new HashMap<Integer, Integer>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, fwEtherPortType);
            for (IntSnmpEntry entry : entries) {
                int ifindex = entry.getLastOIDIndex().intValue();
                int type = entry.intValue();
                result.put(Integer.valueOf(ifindex), Integer.valueOf(type));
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
        Slot slot = device.getSlotBySlotIndex(slotId);
        if (slot == null) {
            throw new IllegalStateException("no such slot: " + slotId);
        }
        Module module = slot.getModule();
        if (module == null) {
            throw new IllegalStateException("no such module: slot=" + slotId);
        }
        String prefix = "";
        String liuTypeName = module.getModelTypeName();
        if (liuTypeName.startsWith("fe")) {
            prefix = "fastethernet";
        } else if (liuTypeName.startsWith("ge")) {
            prefix = "gigabitethernet";
        } else if (liuTypeName.startsWith("te")) {
            prefix = "tengigabitethernet";
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

    public static final String fwEtherPortLagIndex = DEVICE_BASE_OID + ".1.2.3.1.1.8";

    public int getLagIfIndexOf(int portIfIndex) throws AbortedException, IOException {
        try {
            Integer lagIfIndex = SnmpUtil.getInteger(snmp, fwEtherPortLagIndex + "." + portIfIndex);
            if (lagIfIndex == null) {
                return -1;
            } else {
                return lagIfIndex.intValue();
            }
        } catch (Exception e) {
            log.warn("no such mib.", e);
        }
        return -1;
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

    public static final String fwDomainName = DEVICE_BASE_OID + ".1.2.5.2.2.1.2";

    public static final String fwDomainJoinPorts = DEVICE_BASE_OID + ".1.2.5.2.2.1.7";

    public Map<Integer, String> getVlanIfs() throws IOException,
            AbortedException {
        try {
            Map<Integer, String> result = new LinkedHashMap<Integer, String>();
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, fwDomainName);
            for (StringSnmpEntry entry : entries) {
                int vlanId = entry.getLastOIDIndex().intValue();
                String vlanName = entry.getValue();
                if ("null".equals(vlanName) || "".equals(vlanName)) {
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

    public static final String fwTagVwanTagList = DEVICE_BASE_OID + ".1.2.5.4.1.1.1";

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

    public static final String fwVportGroups = DEVICE_BASE_OID + ".1.2.5.6";

    public Map<Integer, List<Integer>> getVports() throws IOException,
            AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, fwVportVlanUserVlanTag);
            Map<Integer, List<Integer>> result = new LinkedHashMap<Integer, List<Integer>>();
            for (IntSnmpEntry entry : entries) {
                if (entry.oidSuffix.length != 2) {
                    throw new IllegalStateException("illegal oidsuffix length: " + entry.oidSuffix.length);
                }
                int vPortIndex = entry.oidSuffix[entry.oidSuffix.length - 1].intValue();
                int ifindex = entry.oidSuffix[entry.oidSuffix.length - 2].intValue();
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

    public static final String fwVportVlanDomainId = fwVportGroups + ".1.1.4";

    public int getInnerVlanId(int ifindex, int vportId) throws IOException,
            AbortedException {
        try {
            String oid = fwVportVlanDomainId + "." + ifindex + "." + vportId;
            int outerVlanId = SnmpUtil.getInteger(snmp, oid);
            return outerVlanId;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static final String fwVportVlanUserVlanTag = fwVportGroups + ".1.1.3";

    public int getOuterVlanId(int ifindex, int vportId) throws IOException,
            AbortedException {
        try {
            String oid = fwVportVlanUserVlanTag + "." + ifindex + "." + vportId;
            int innerVlanId = SnmpUtil.getInteger(snmp, oid);
            return innerVlanId;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static final String fwVportVlanRowStatus = DEVICE_BASE_OID + ".1.2.5.6.1.1.2";

    public static final String fwVportDescriptionAlias = DEVICE_BASE_OID + ".1.2.5.6.2.1.2";

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

    public static final String fwVportVlanInnerTagDomainId = fwVportGroups + ".4.1.4";

    public Map<Integer, Integer> getVportSecondaryTagTranslationMap(int parentIfIndex, int vportId,
                                                                    TagChanger tc)
            throws IOException, AbortedException {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        final String baseOid = fwVportVlanInnerTagDomainId + "." + parentIfIndex + "." + vportId;
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, baseOid);
            for (IntSnmpEntry entry : entries) {
                int secondaryTag = entry.getLastOIDIndex().intValue();
                int domainId = entry.intValue();
                log.debug("found secondary tag: domain(inner)=" + domainId + ", outer.secondary=" + secondaryTag);
                result.put(Integer.valueOf(domainId), Integer.valueOf(secondaryTag));
            }
            return result;
        } catch (SnmpResponseException e) {
            throw new IOException("snmp error.", e);
        } catch (RepeatedOidException e) {
            throw new IOException("snmp error.", e);
        }
    }

    public static final String fwVportVlanOperStatus = ".1.3.6.1.4.1.211.1.24.7.10.1.2.5.6.9.1.3";

    public int getFfpWorkingIfIndex(int ffpIndex) throws AbortedException, IOException {
        try {
            final String fwEtherFfpWork = DEVICE_BASE_OID + ".1.2.9.1.1.1";
            final String fwEtherFfpProt = DEVICE_BASE_OID + ".1.2.9.1.1.2";
            final String fwEtherFfpSwitchSelStatus = DEVICE_BASE_OID + ".1.2.9.2.1.2";

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