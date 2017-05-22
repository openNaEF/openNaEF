package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.agent.dsl.VlanModelBuilder;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class CiscoVtpMibImpl implements CiscoVtpMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoVtpMibImpl.class);
    private final SnmpAccess snmp;
    private final VlanDevice device;
    private final VlanModelBuilder builder;
    private final Map<Integer, String> ifNameMap;

    public CiscoVtpMibImpl(SnmpAccess snmp, VlanDevice device) throws IOException, AbortedException {
        if (snmp == null || device == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.device = device;
        this.builder = new VlanModelBuilder(this.device);
        this.ifNameMap = Mib2Impl.getIfNameMap(snmp);
    }

    public void createVlanIf() throws IOException, AbortedException {
        Map<CiscoVtpVlanKey, StringSnmpEntry> vlanNames =
                SnmpUtil.getWalkResult(snmp, vtpVlanName, stringEntryBuilder,
                        vtpVlanKeyCreator);
        Map<CiscoVtpVlanKey, IntSnmpEntry> vlanIfIndices =
                SnmpUtil.getWalkResult(snmp, vtpVlanIfIndex, intEntryBuilder,
                        vtpVlanKeyCreator);

        for (Map.Entry<CiscoVtpVlanKey, StringSnmpEntry> entry : vlanNames.entrySet()) {
            int vlanID = entry.getKey().getVlanId();
            String vlanName = entry.getValue().getValue();
            String status = "enabled";
            builder.buildVlanIf(null, vlanID);
            builder.setVlanOperationalStatus(null, vlanID, status);
            IntSnmpEntry ifindex_ = vlanIfIndices.get(entry.getKey());
            if (ifindex_ != null && ifindex_.intValue() > 0) {
                int ifIndex = ifindex_.intValue();
                log.debug("vlanID=" + vlanID + ", ifIndex=" + ifIndex);
                String ifName = this.ifNameMap.get(Integer.valueOf(ifIndex));
                if (isIllegalIfName(ifName, vlanID)) {
                    log.warn("ifName/vlanName mismatch: [" + ifIndex
                            + "]->{vlanName=" + vlanName + ", ifName=" + ifName + "}");
                    continue;
                }
                Port p = this.device.getPortByIfIndex(ifIndex);
                if (p != null) {
                    log.warn("duplicated ifIndex found: [" + ifIndex
                            + "]->{" + p.getIfName() + ", " + vlanName + "}");
                    continue;
                }
                builder.setVlanIfIndex(null, vlanID, ifIndex);
                builder.setVlanName(null, vlanID, ifName);
            } else {
                builder.setVlanName(null, vlanID, vlanName);
            }
        }
    }

    private boolean isIllegalIfName(String ifName, int vlanID) {
        if (ifName == null) {
            return true;
        } else if (ifName.contains("default")) {
            return false;
        } else if (!ifName.contains(String.valueOf(vlanID))) {
            return true;
        }
        return false;
    }

    public void createTaggedVlan() throws IOException, AbortedException {
        Map<IfIndexKey, IntSnmpEntry> vlanTrunkPortDynamicStatuses = SnmpUtil
                .getWalkResult(snmp, vlanTrunkPortDynamicStatus,
                        intEntryBuilder, ifIndexKeyCreator);

        Map<Integer, Set<Integer>> trunkVlanMap = getTaggedBoundVlanList();

        for (IfIndexKey key : vlanTrunkPortDynamicStatuses.keySet()) {
            int ifindex = key.getIfIndex();
            int vlanTrunkPortDynamicStatusId = vlanTrunkPortDynamicStatuses.get(key).intValue();
            log.trace("// ifindex=" + ifindex + ", status=" + vlanTrunkPortDynamicStatusId);

            LogicalEthernetPort logical =
                    DeviceInfoUtil.
                            getLogicalEthernetPortByEthernetPortIfIndexOrEthernetPortsAggregatorIfIndex(
                                    device, ifindex);
            if (logical == null) {
                continue;
            }

            switch (vlanTrunkPortDynamicStatusId) {
                case VALUE_NOT_TRUNKING:
                    break;
                case VALUE_TRUNKING:
                    builder.setVlanPortUsage(logical, VlanPortUsage.TRUNK);
                    Set<Integer> trunkVlanSet = trunkVlanMap.get(ifindex);
                    for (Integer vlan : trunkVlanSet) {
                        builder.addTaggedVlan(null, vlan.intValue(), logical);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown logical="
                            + logical.getFullyQualifiedName()
                            + ", value=" + vlanTrunkPortDynamicStatusId);
            }
        }
    }

    public void createUntaggedVlan() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> untaggedVlans = SnmpUtil.getIntSnmpEntries(snmp, vlanTrunkPortNativeVlan);

            for (IntSnmpEntry entry : untaggedVlans) {
                int ifindex = entry.getLastOIDIndex().intValue();
                int vlanID = entry.intValue();
                Port port = device.getPortByIfIndex(ifindex);
                if (port instanceof EthernetPort) {
                    continue;
                }
                LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) port);
                if (logical == null) {
                    throw new IllegalStateException();
                }
                builder.setUntaggedVlan(null, vlanID, logical);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    protected Map<Integer, Set<Integer>> getTaggedBoundVlanList() throws IOException, AbortedException {
        Map<IfIndexKey, ByteSnmpEntry> vlan1000 = SnmpUtil
                .getWalkResult(snmp, vlanTrunkPortVlansXmitJoined, byteEntryBuilder,
                        ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vlan2000 = SnmpUtil
                .getWalkResult(snmp, vlanTrunkPortVlansXmitJoined2k, byteEntryBuilder,
                        ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vlan3000 = SnmpUtil
                .getWalkResult(snmp, vlanTrunkPortVlansXmitJoined3k, byteEntryBuilder,
                        ifIndexKeyCreator);
        Map<IfIndexKey, ByteSnmpEntry> vlan4000 = SnmpUtil
                .getWalkResult(snmp, vlanTrunkPortVlansXmitJoined4k, byteEntryBuilder,
                        ifIndexKeyCreator);

        Map<Integer, Set<Integer>> trunkVlanMap = new HashMap<Integer, Set<Integer>>();
        for (IfIndexKey key : vlan1000.keySet()) {
            int ifIndex = key.getIfIndex();
            Set<Integer> trunkVlanSet = new HashSet<Integer>();

            makeTrunkVlanSet(trunkVlanSet, vlan1000.get(key).getValue(), 0);
            if (vlan2000.get(key) != null) {
                makeTrunkVlanSet(trunkVlanSet, vlan2000.get(key).getValue(), 1024);
                makeTrunkVlanSet(trunkVlanSet, vlan3000.get(key).getValue(), 2048);
                makeTrunkVlanSet(trunkVlanSet, vlan4000.get(key).getValue(), 3072);
            }
            trunkVlanMap.put(ifIndex, trunkVlanSet);
        }
        return Collections.unmodifiableMap(trunkVlanMap);
    }

    private static void makeTrunkVlanSet(final Set<Integer> trunkVlanSet,
                                         final byte[] list, final int offset) {
        int[] vlans = SnmpUtil.decodeBitList(list);
        for (int vlan : vlans) {
            int vlanID = vlan - 1 + offset;
            trunkVlanSet.add(vlanID);
        }
    }

    private final static class CiscoVtpVlanKey {
        public final int managementDomainIndex;
        public final int vlanId;

        public CiscoVtpVlanKey(BigInteger[] oidSuffix) {
            if (oidSuffix == null || oidSuffix.length != 2) {
                throw new IllegalArgumentException();
            }
            this.managementDomainIndex = oidSuffix[0].intValue();
            this.vlanId = oidSuffix[1].intValue();
        }

        @SuppressWarnings("unused")
        public int getManagementDomainIndex() {
            return this.managementDomainIndex;
        }

        public int getVlanId() {
            return this.vlanId;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof CiscoVtpVlanKey) {
                return this.vlanId == ((CiscoVtpVlanKey) o).vlanId
                        && this.managementDomainIndex == ((CiscoVtpVlanKey) o).managementDomainIndex;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.toString().hashCode();
            return i * i + i + 41;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + ":"
                    + this.managementDomainIndex + ":" + this.vlanId;
        }
    }

    @SuppressWarnings("serial")
    private final static KeyCreator<CiscoVtpVlanKey> vtpVlanKeyCreator = new KeyCreator<CiscoVtpVlanKey>() {
        public CiscoVtpVlanKey getKey(BigInteger[] oidSuffix) {
            return new CiscoVtpVlanKey(oidSuffix);
        }
    };

}