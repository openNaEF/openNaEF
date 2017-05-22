package voss.discovery.agent.cisco.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.LogicalEthernetPort;
import voss.model.VlanDevice;
import voss.model.VlanIf;
import voss.model.VlanIfImpl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class CiscoVlanMembershipMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoVlanMembershipMib.class);
    public static final String vmMembershipEntry = ".1.3.6.1.4.1.9.9.68.1.2.2.1";
    public static final String SYMBOL_vmMembershipEntry =
            "enterprises.cisco.ciscoMgmt.ciscoVlanMembershipMIB.ciscoVlanMembershipMIBObjects"
                    + ".vmMembership.vmMembershipTable.vmMembershipEntry";

    public static final String vmVlanType = vmMembershipEntry + ".1";
    public static final String vmVlan = vmMembershipEntry + ".2";
    public static final String vmVlans = vmMembershipEntry + ".4";
    public static final String vmVlans2k = vmMembershipEntry + ".5";
    public static final String vmVlans3k = vmMembershipEntry + ".6";
    public static final String vmVlans4k = vmMembershipEntry + ".7";

    private final SnmpAccess snmp;

    public CiscoVlanMembershipMib(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public void createUntaggedVlan(VlanDevice device) throws IOException, AbortedException {
        Map<IfIndexKey, IntSnmpEntry> vmVlanTypes =
                SnmpUtil.getWalkResult(snmp, vmVlanType, intEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, IntSnmpEntry> vmVlanIds =
                SnmpUtil.getWalkResult(snmp, vmVlan, intEntryBuilder, ifIndexKeyCreator);

        for (IfIndexKey key : vmVlanTypes.keySet()) {
            int ifindex = key.getIfIndex();
            LogicalEthernetPort port =
                    DeviceInfoUtil.
                            getLogicalEthernetPortByEthernetPortIfIndexOrEthernetPortsAggregatorIfIndex(device, ifindex);

            if (port == null) {
                throw new IllegalStateException("unknown ifindex: "
                        + ifindex + " on " + device.getDeviceName());
            }

            int type = vmVlanTypes.get(key).intValue();
            switch (type) {
                case 1:
                    int vlanId = vmVlanIds.get(key).intValue();
                    VlanIf vlanIf = device.getVlanIfByVlanId(vlanId);
                    if (vlanIf == null) {
                        vlanIf = createVlanIf(device, vlanId);
                    }
                    vlanIf.addUntaggedPort(port);
                    break;
                case 2:
                    break;
                case 3:
                    break;
            }
        }
    }

    public void createVlanConfiguration(VlanDevice device) throws IOException, AbortedException {
        log.info("createVlanIf: device=" + device.getDeviceName());

        Map<IfIndexKey, IntSnmpEntry> vmVlanTypes =
                SnmpUtil.getWalkResult(snmp, vmVlanType, intEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, IntSnmpEntry> vmVlanIds =
                SnmpUtil.getWalkResult(snmp, vmVlan, intEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vmVlansList =
                SnmpUtil.getWalkResult(snmp, vmVlans, byteEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vmVlans2kList =
                SnmpUtil.getWalkResult(snmp, vmVlans2k, byteEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vmVlans3kList =
                SnmpUtil.getWalkResult(snmp, vmVlans3k, byteEntryBuilder, ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vmVlans4kList =
                SnmpUtil.getWalkResult(snmp, vmVlans4k, byteEntryBuilder, ifIndexKeyCreator);

        for (IfIndexKey key : vmVlanTypes.keySet()) {
            int ifindex = key.getIfIndex();
            LogicalEthernetPort port =
                    DeviceInfoUtil.
                            getLogicalEthernetPortByEthernetPortIfIndexOrEthernetPortsAggregatorIfIndex(device, ifindex);

            if (port == null) {
                throw new IllegalStateException("unknown ifindex: "
                        + ifindex + " on " + device.getDeviceName());
            }

            int type = vmVlanTypes.get(key).intValue();
            switch (type) {
                case 1:
                    int vlanId = vmVlanIds.get(key).intValue();
                    VlanIf vlanIf = device.getVlanIfByVlanId(vlanId);
                    if (vlanIf == null) {
                        vlanIf = createVlanIf(device, vlanId);
                    }
                    vlanIf.addUntaggedPort(port);
                    break;
                case 2:
                    break;
                case 3:
                    Set<Integer> trunkVlanSet = new HashSet<Integer>();
                    makeTrunkVlanSet(trunkVlanSet, vmVlansList.get(key).getValue(), 1);
                    makeTrunkVlanSet(trunkVlanSet, vmVlans2kList.get(key).getValue(), 1025);
                    makeTrunkVlanSet(trunkVlanSet, vmVlans3kList.get(key).getValue(), 2049);
                    makeTrunkVlanSet(trunkVlanSet, vmVlans4kList.get(key).getValue(), 3073);
                    for (Integer vlanId_ : trunkVlanSet) {
                        VlanIf vlanIf_ = device.getVlanIfByVlanId(vlanId_);
                        if (vlanIf_ == null) {
                            vlanIf_ = createVlanIf(device, vlanId_);
                        }
                        vlanIf_.addTaggedPort(port);
                    }
                    break;
            }
        }

    }

    private VlanIf createVlanIf(VlanDevice device, int vlanId) {
        VlanIf vlanIf = new VlanIfImpl();
        vlanIf.initDevice(device);
        vlanIf.initVlanId(vlanId);
        vlanIf.initIfName("vlan" + vlanId);
        vlanIf.setVlanName("vlan" + vlanId);
        device.addPort(vlanIf);
        return vlanIf;
    }

    private static void makeTrunkVlanSet(final Set<Integer> trunkVlanSet,
                                         final byte[] list, final int offset) {
        int[] vlans = SnmpUtil.decodeBitList(list);
        for (int vlan : vlans) {
            trunkVlanSet.add(vlan - 1 + offset);
        }
    }

}