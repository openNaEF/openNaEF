package voss.discovery.agent.alaxala.mib;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.*;

public class AlaxalaVlanMibImpl implements AlaxalaVlanMib {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(AlaxalaVlanMibImpl.class);

    private final SnmpAccess snmp;
    private final AlaxalaVendorProfile profile;
    protected final Map<Integer, Integer> portIndexToIfIndexMap = new HashMap<Integer, Integer>();

    protected final Map<Integer, Integer> vlanIdToIfIndexMap = new HashMap<Integer, Integer>();

    public AlaxalaVlanMibImpl(SnmpAccess snmp, AlaxalaVendorProfile profile) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        if (profile == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.profile = profile;
    }

    public void prepare() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> axsVBBaseVlanIfIndices = SnmpUtil.getIntSnmpEntries(
                    snmp, profile.getAxsVBBaseVlanIfIndexOid());
            for (IntSnmpEntry axsVBBaseVlanIfIndex : axsVBBaseVlanIfIndices) {
                int vlanId = axsVBBaseVlanIfIndex.oidSuffix[0].intValue();
                int ifIndex = axsVBBaseVlanIfIndex.intValue();
                assert !vlanIdToIfIndexMap.containsKey(vlanId);
                vlanIdToIfIndexMap.put(vlanId, ifIndex);
            }

            List<IntSnmpEntry> axsVBBasePortIfIndices = SnmpUtil.getIntSnmpEntries(
                    snmp, profile.getAxsVBBasePortIfIndexOid());
            for (IntSnmpEntry axsVBBasePortIfIndex : axsVBBasePortIfIndices) {
                assert axsVBBasePortIfIndex.oidSuffix.length == 2;
                int portIndex = axsVBBasePortIfIndex.oidSuffix[1].intValue();
                int ifIndex = axsVBBasePortIfIndex.intValue();
                if (!portIndexToIfIndexMap.containsKey(portIndex)) {
                    portIndexToIfIndexMap.put(portIndex, ifIndex);
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    public void createVlanIf(VlanDevice device) throws IOException,
            AbortedException {
        Map<Integer, Integer> vlanIdToIfIndex = getAllVlanIfIndex();
        Set<Map.Entry<Integer, Integer>> entries = vlanIdToIfIndex.entrySet();
        for (Map.Entry<Integer, Integer> entry : entries) {
            int vlanId = entry.getKey();
            int ifIndex = entry.getValue();
            AlaxalaVlanIfImpl vlan = (AlaxalaVlanIfImpl) device
                    .getVlanIfByVlanId(vlanId);
            if (vlan == null) {
                createVlanIf(device, vlanId, ifIndex);
            }
        }

        AlaxalaVlanPortBindingState[] bindings = getVlanAndPortBindingState();

        for (AlaxalaVlanPortBindingState binding : bindings) {
            AlaxalaVlanIfImpl vlan = (AlaxalaVlanIfImpl) device
                    .getVlanIfByVlanId(binding.getVlanId());
            if (vlan == null) {
                throw new IllegalStateException(
                        "vlan/if bind found, but no vlan found. what trouble is going on ?");
            }

            bindVlanIfVsEthernet(device, vlan, binding);
        }

    }

    private AlaxalaVlanIfImpl createVlanIf(VlanDevice device, int vlanid,
                                           int ifIndex) throws IOException, AbortedException {
        AlaxalaVlanIfImpl vlan = new AlaxalaVlanIfImpl();
        device.addPort(vlan);
        vlan.initIfIndex(ifIndex);
        vlan.initVlanIfIndex(ifIndex);
        vlan.initVlanId(vlanid);
        String name = getVlanName(vlanid);
        vlan.initIfName(name);
        vlan.setVlanName(name);
        vlan.setVlanType(AlaxalaVlanIfImpl.acceptableVlanTypes[getVlanType(vlanid)]);
        return vlan;
    }

    protected void bindVlanIfVsEthernet(VlanDevice device, VlanIf vlan,
                                        AlaxalaVlanPortBindingState bindings) {
        Port port = device.getPortByIfIndex(bindings.getBoundPortIfIndex());
        if (port == null) {
            throw new IllegalArgumentException("no port found: ifindex="
                    + bindings.getBoundPortIfIndex());
        } else {
            if (port instanceof EthernetPort) {
                bindVlanIfVsEthernet(vlan, (EthernetPort) port, bindings);
            } else if (port instanceof EthernetPortsAggregator) {
                if (bindings.isTagged()) {
                    vlan.addTaggedPort((EthernetPortsAggregator) port);
                } else if (bindings.isUntagged()) {
                    vlan.addUntaggedPort((EthernetPortsAggregator) port);
                } else {
                    System.out.println("Unknown bind type found on: ifindex="
                            + bindings.getBoundPortIfIndex());
                }
            }
        }
    }

    private void bindVlanIfVsEthernet(VlanIf vlan, EthernetPort port,
                                      AlaxalaVlanPortBindingState bindings) {
        if (port != null && vlan.getVlanId() > 0) {
            if (bindings.isTagged()) {
                DeviceInfoUtil.addTaggedPort(vlan, port);
            } else if (bindings.isUntagged()) {
                DeviceInfoUtil.addUntaggedPort(vlan, port);
            }
        }
    }

    private AlaxalaVlanPortBindingState[] getVlanAndPortBindingState()
            throws IOException, AbortedException {
        assert portIndexToIfIndexMap.size() > 0 : "portIndexToIfIndexMap is not initialized.";

        try {
            List<AlaxalaVlanPortBindingState> result = new ArrayList<AlaxalaVlanPortBindingState>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    profile.getVlanAndPortBindingStateOid());
            for (IntSnmpEntry entry : entries) {
                AlaxalaVlanPortBindingState bindingState = new AlaxalaVlanPortBindingState(
                        entry, portIndexToIfIndexMap);
                result.add(bindingState);
            }
            return result.toArray(new AlaxalaVlanPortBindingState[0]);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private Map<Integer, Integer> getAllVlanIfIndex() {
        return Collections.unmodifiableMap(vlanIdToIfIndexMap);
    }

    private String getVlanName(int vlanId) throws IOException, AbortedException {
        int ifIndex;
        if (vlanId == 1) {
            ifIndex = 3;
        } else {
            ifIndex = vlanId + profile.getVlanIfIndexOffset();
        }
        String name = Mib2Impl.getIfAlias(snmp, ifIndex);
        return name.trim();
    }

    private int getVlanType(int vlanid) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp,
                    profile.getAlaxalaVBBaseVlanTypeOid());
            for (IntSnmpEntry entry : entries) {
                if (entry.getLastOIDIndex().intValue() == vlanid) {
                    return entry.intValue();
                }
            }
            throw new IllegalStateException(
                    "Cannot determine vlan type of vlan id : " + vlanid);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

}