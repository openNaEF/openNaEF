package voss.discovery.agent.common;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;

public class EntityMibAnalyzer {
    private final static Logger log = LoggerFactory.getLogger(EntityMibAnalyzer.class);
    private final Map<Integer, PhysicalEntry> physicalEntries = new HashMap<Integer, PhysicalEntry>();
    private final Map<String, String> oid2name = new HashMap<String, String>();

    public final static String OID_EntPhysicalClass = ".1.3.6.1.2.1.47.1.1.1.1.5";
    private final Set<EntityType> filterList = new HashSet<EntityType>();

    public final static String OID_entPhysicalContainedIn = ".1.3.6.1.2.1.47.1.1.1.1.4";

    public final static String OID_entPhysicalDescr = ".1.3.6.1.2.1.47.1.1.1.1.2";

    public final static String OID_entPhysicalVendorType = ".1.3.6.1.2.1.47.1.1.1.1.3";

    public final static String OID_entPhysicalParentRelPos = ".1.3.6.1.2.1.47.1.1.1.1.6";

    public final static String OID_entPhysicalName = ".1.3.6.1.2.1.47.1.1.1.1.7";

    public final static String OID_entPhysicalHardwareRev = ".1.3.6.1.2.1.47.1.1.1.1.8";

    public final static String OID_entPhysicalFirmwareRev = ".1.3.6.1.2.1.47.1.1.1.1.9";

    public final static String OID_entPhysicalSoftwareRev = ".1.3.6.1.2.1.47.1.1.1.1.10";

    public final static String OID_entPhysicalSerialNum = ".1.3.6.1.2.1.47.1.1.1.1.11";

    public final static String OID_entAliasMappingIdentifier = ".1.3.6.1.2.1.47.1.3.2.1.2";

    public EntityMibAnalyzer(Map<String, String> list) {
        this.oid2name.clear();
        this.oid2name.putAll(list);
        this.filterList.clear();
    }

    public Map<Integer, PhysicalEntry> getEntities() {
        return Collections.unmodifiableMap(this.physicalEntries);
    }

    public PhysicalEntry getTopmostEntity() {
        int topmost = getTopIndex();
        return this.physicalEntries.get(Integer.valueOf(topmost));
    }

    public void addFilter(EntityType filterId) {
        this.filterList.add(filterId);
    }

    public void analyze(SnmpAccess snmp) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException, SnmpResponseException {
        physicalEntries.clear();
        List<Integer> targetEntityIds = new ArrayList<Integer>();

        List<IntSnmpEntry> entityTypes = SnmpUtil.getIntSnmpEntries(snmp, OID_EntPhysicalClass);
        for (IntSnmpEntry entry : entityTypes) {
            EntityType type = EntityType.valueOf(entry.intValue());
            if (filterList.size() > 0 && !filterList.contains(type)) {
                continue;
            }
            targetEntityIds.add(entry.getLastOIDIndex().intValue());
            PhysicalEntry pe = new PhysicalEntry(entry.getLastOIDIndex().intValue(), type);
            physicalEntries.put(entry.getLastOIDIndex().intValue(), pe);
        }

        List<ByteSnmpEntry> entityVendorTypes = SnmpUtil.getByteSnmpEntries(snmp, OID_entPhysicalVendorType);
        for (ByteSnmpEntry entry : entityVendorTypes) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            String oidString = SnmpUtil.oidToOidString(entry.getValue());
            String name = oid2name.get(oidString);
            pe.name = (name == null ? "unknown" : name);
        }

        List<StringSnmpEntry> entityRelationalNames = SnmpUtil.getStringSnmpEntries(snmp, OID_entPhysicalName);
        for (StringSnmpEntry entry : entityRelationalNames) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            pe.physicalName = entry.getValue();
        }

        List<StringSnmpEntry> entityRelationalDescrs = SnmpUtil.getStringSnmpEntries(snmp, OID_entPhysicalDescr);
        for (StringSnmpEntry entry : entityRelationalDescrs) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            pe.descr = entry.getValue();
        }

        List<StringSnmpEntry> entityRelationalHardwareRevs = SnmpUtil.getStringSnmpEntries(snmp, OID_entPhysicalHardwareRev);
        for (StringSnmpEntry entry : entityRelationalHardwareRevs) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            pe.hardwareRev = entry.getValue();
        }

        List<StringSnmpEntry> entityRelationalSerialNumbers = SnmpUtil.getStringSnmpEntries(snmp, OID_entPhysicalSerialNum);
        for (StringSnmpEntry entry : entityRelationalSerialNumbers) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            pe.serialNumber = entry.getValue();
        }

        List<IntSnmpEntry> entityRelationalPositions = SnmpUtil.getIntSnmpEntries(snmp, OID_entPhysicalParentRelPos);
        for (IntSnmpEntry entry : entityRelationalPositions) {
            int suffix = entry.getLastOIDIndex().intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(entry.getLastOIDIndex().intValue());
            pe.position = entry.intValue();
        }

        List<SnmpEntry> entAliasMappingIdentifiers = SnmpUtil.getSnmpEntries(snmp, OID_entAliasMappingIdentifier);
        for (SnmpEntry entry : entAliasMappingIdentifiers) {
            int suffix = entry.oidSuffix[0].intValue();
            if (!targetEntityIds.contains(suffix)) {
                continue;
            }
            PhysicalEntry pe = physicalEntries.get(suffix);
            String oid = entry.getVarBind().getValueAsString();
            String[] elem = oid.split("\\.");
            pe.ifindex = Integer.parseInt(elem[elem.length - 1]);
        }

        List<IntSnmpEntry> entityDependencies = SnmpUtil.getIntSnmpEntries(snmp, OID_entPhysicalContainedIn);
        for (IntSnmpEntry entry : entityDependencies) {
            int index = entry.getLastOIDIndex().intValue();
            int parentIndex = entry.intValue();
            PhysicalEntry pe = physicalEntries.get(index);
            PhysicalEntry parent = physicalEntries.get(parentIndex);
            if (!targetEntityIds.contains(index)) {
                continue;
            }
            if (parentIndex == 0) {
            } else {
                if (parent == null) {
                    throw new IllegalStateException();
                }
                log.trace("physical structure: " + parentIndex + " <- " + index);
                parent.addChild(index, pe);
            }
        }
        int topIndex = getTopIndex();
        PhysicalEntry topmost = physicalEntries.get(topIndex);
        topmost.alignDepth();
        print(topmost);
    }

    private int getTopIndex() {
        Integer top = null;
        for (Integer key : this.physicalEntries.keySet()) {
            if (key == null) {
                throw new IllegalStateException("illegal physicalentity index[null]");
            } else if (top == null) {
                top = key;
                continue;
            } else if (key.intValue() < top.intValue()) {
                top = key;
                continue;
            }
        }
        if (top == null) {
            throw new IllegalStateException("no topmost physicalentity index.");
        }
        return top.intValue();
    }

    private void print(PhysicalEntry pe) {
        log.debug(pe.toString());
        List<PhysicalEntry> children = pe.getChildren();
        for (PhysicalEntry child : children) {
            print(child);
        }
    }

}