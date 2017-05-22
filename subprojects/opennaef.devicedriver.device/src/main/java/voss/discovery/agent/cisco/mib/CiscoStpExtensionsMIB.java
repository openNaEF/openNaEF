package voss.discovery.agent.cisco.mib;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.*;

public class CiscoStpExtensionsMIB {
    private final static Logger log = LoggerFactory.getLogger(CiscoStpExtensionsMIB.class);

    public static final String stpxVlanMISTPInstMapInstIndex = ".1.3.6.1.4.1.9.9.82.1.7.3.1.1";
    public static final String SYMBOL_stpxVlanMISTPInstMapInstIndex = "enterprises.cisco.ciscoMgmt.ciscoStpExtensionsMIB.stpxObjects.stpxMISTPObjects"
            + ".stpxVlanMISTPInstMapTable.stpxVlanMISTPInstMapEntry"
            + ".stpxVlanMISTPInstMapInstIndex";

    public static final String stpxLongStpPortPathCost = ".1.3.6.1.4.1.9.9.82.1.6.3.1.1";
    public static final String SYMBOL_stpxLongStpPortPathCost = "enterprises.cisco.ciscoMgmt.ciscoStpExtensionsMIB.stpxObjects"
            + ".stpxSpanningTreeObjects.stpxLongStpPortPathCostTable"
            + ".stpxLongStpPortPathCostEntry.stpxLongStpPortPathCost";

    public static final String stpxSpanningTreeType = ".1.3.6.1.4.1.9.9.82.1.6.1.0";
    public static final String SYMBOL_stpxSpanningTreeType = "enterprises.cisco.ciscoMgmt.ciscoStpExtensionsMIB.stpxObjects"
            + ".stpxSpanningTreeObjects.stpxSpanningTreeType.0";

    private final SnmpAccess snmp;

    public CiscoStpExtensionsMIB(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public SpanningTreeType getSpanningTreeType(VlanDevice device) throws IOException, AbortedException {
        try {
            int value = SnmpUtil.getInteger(snmp, stpxSpanningTreeType);
            log.debug("@ set spantree mode '" + SpanningTreeType.valueOf(value)
                    + "' on device '" + device.getDeviceName() + "'");
            return SpanningTreeType.valueOf(value);
        } catch (NoSuchMibException e) {
        } catch (SnmpResponseException e) {
        }
        return SpanningTreeType.UNDEFINED;
    }

    public void createMistpInstanceAndVlanMapping(VlanDevice device) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, stpxVlanMISTPInstMapInstIndex);

            Map<Integer, MistpInstance> mistpInstanceMap = new HashMap<Integer, MistpInstance>();
            Map<MistpInstance, Set<VlanIf>> mappings = new HashMap<MistpInstance, Set<VlanIf>>();

            for (int i = 1; i <= 16; i++) {
                MistpInstance instance = new MistpInstanceImpl();
                instance.initDevice(device);
                instance.setInstanceIndex(i);

                mistpInstanceMap.put(i, instance);
                Set<VlanIf> vlanIfs = new HashSet<VlanIf>();
                mappings.put(instance, vlanIfs);
            }

            for (IntSnmpEntry entry : entries) {
                int mistpId = entry.oidSuffix[0].intValue();
                int vlanId = entry.oidSuffix[1].intValue();
                VlanIf vlanIf = device.getVlanIfByVlanId(vlanId);
                MistpInstance instance = mistpInstanceMap.get(mistpId);
                Set<VlanIf> instanceVlanIfs = mappings.get(instance);
                instanceVlanIfs.add(vlanIf);
            }

            for (MistpInstance key : mappings.keySet()) {
                key.setVlanIfs(mappings.get(key).toArray(new VlanIf[0]));
            }

        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void vlanStp(VlanDevice device, CiscoStackMibImpl ciscoStackMib) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, stpxLongStpPortPathCost);
            for (IntSnmpEntry entry : entries) {
                int portCrossIndex = entry.getLastOIDIndex().intValue();
                EthernetPort port = ciscoStackMib.getEthernetPortByCrossIndex(portCrossIndex);
                if (port == null) {
                    throw new IllegalArgumentException();
                }
                int cost = entry.intValue();
                log.warn("port stp cost: port=" + port.getFullyQualifiedName() + ", cost=" + cost);
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

}