package voss.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class AbstractVlanDevice extends AbstractDevice implements
        VlanDevice {
    private boolean eoeEnabled = false;
    private String spanningTreeType_;
    private VlanStpElement[] vlanStpElements_ = new VlanStpElement[0];

    private transient PortIndex<String, VlanIf> extendedVlanIdIndex;

    private synchronized PortIndex<String, VlanIf> getVlanifs() {
        if (extendedVlanIdIndex == null) {
            extendedVlanIdIndex = new PortIndex<String, VlanIf>(
                    "vlandevice:eoeid+vlanid-vlanif") {

                @Override
                protected boolean isTargetPort(Port port) {
                    return port instanceof VlanIf;
                }

                @Override
                protected boolean isInitializable(VlanIf vif) {
                    try {
                        return vif.getExtendedVlanId() != null;
                    } catch (NotInitializedException nie) {
                        return false;
                    }
                }

                @Override
                protected boolean isUniqueKey(String key) {
                    return true;
                }

                @Override
                protected boolean isMultipleKeyEntity() {
                    return false;
                }

                @Override
                protected List<String> getKeys(VlanIf vlanif) {
                    throw new IllegalStateException();
                }

                @Override
                protected String getKey(VlanIf vif) {
                    return vif.getExtendedVlanId();
                }

                @Override
                protected Set<VlanIf> getInitialValues() {
                    Set<VlanIf> result = new HashSet<VlanIf>();
                    for (VlanIf vif : AbstractVlanDevice.this.getVlanIfs()) {
                        result.add(vif);
                    }
                    return result;
                }

                @Override
                protected String getKeyString(String key) {
                    return key;
                }
            };
        }
        return extendedVlanIdIndex;
    }

    protected AbstractVlanDevice() {
    }

    public synchronized void setEoeEnable(boolean enable) {
        this.eoeEnabled = enable;
        this.extendedVlanIdIndex = null;
    }

    public synchronized boolean isEoeEnable() {
        return this.eoeEnabled;
    }

    public synchronized String getSpanningTreeType() {
        return spanningTreeType_;
    }

    public synchronized void setSpanningTreeType(String spanningTreeType) {
        spanningTreeType_ = spanningTreeType;
    }

    public VlanIf[] getVlanIfs() {
        return selectPorts(VlanIf.class);
    }

    public EthernetPort[] getEthernetPorts() {
        return selectPorts(EthernetPort.class);
    }

    public LogicalEthernetPort[] getLogicalEthernetPorts() {
        return selectPorts(LogicalEthernetPort.class);
    }

    public EthernetPortsAggregator[] getEthernetPortsAggregators() {
        return selectPorts(EthernetPortsAggregator.class);
    }

    public LogicalEthernetPort.TagChanger[] getTagChangers() {
        return selectPorts(LogicalEthernetPort.TagChanger.class);
    }

    public VlanIf getUntaggedVlanIf(EthernetPort ethernetPort) {
        VlanIf result = null;
        for (VlanIf vlanIf : getVlanIfs()) {
            for (LogicalEthernetPort untaggedPort : vlanIf.getUntaggedPorts()) {
                for (EthernetPort ethPort : untaggedPort.getPhysicalPorts()) {
                    if (ethPort == ethernetPort) {
                        if (result != null && result != vlanIf) {
                            throw new IllegalStateException(
                                    "Duplicate untagged connection detected: "
                                            + ethernetPort
                                            .getFullyQualifiedName()
                                            + " vlan-" + result.getVlanId()
                                            + ", vlan-"
                                            + vlanIf.getVlanId());
                        }
                        result = vlanIf;
                    }
                }
            }
        }
        return result;
    }

    public LogicalEthernetPort getLogicalEthernetPort(EthernetPort ethernetPort) {
        LogicalEthernetPort result = null;
        for (LogicalEthernetPort logical : getLogicalEthernetPorts()) {
            try {
                for (EthernetPort ether : logical.getPhysicalPorts()) {
                    if (ether == ethernetPort) {
                        if (result != null && result != logical) {
                            throw new IllegalStateException("The configuration of the logical Ethernet port for [" +
                                    ethernetPort.getFullyQualifiedName()
                                    + "] does not match.");
                        }
                        result = logical;
                    }
                }
            } catch (NotInitializedException nie) {
                continue;
            }
        }
        return result;
    }

    public EthernetPortsAggregator getEthernetPortsAggregatorByAggregationGroupId(
            int aggregationGroupId) {
        EthernetPortsAggregator result = null;

        for (EthernetPortsAggregator aggregator : getEthernetPortsAggregators()) {
            if (aggregator.getAggregationGroupId() != null
                    && aggregator.getAggregationGroupId().intValue() == aggregationGroupId) {
                if (result != null) {
                    throw new IllegalStateException(
                            "An eth-aggregator with duplicate aggregation-group-id was detected: "
                                    + "device:" + getDeviceName()
                                    + " aggregation-group-id:"
                                    + aggregationGroupId + " "
                                    + result.getIfName() + ", "
                                    + aggregator.getIfName());
                }

                result = aggregator;
            }
        }

        return result;
    }

    public VlanIf getVlanIfByVlanId(int vlanid) {
        if (vlanid == VlanIf.NULL_VLAN_ID) {
            throw new IllegalArgumentException();
        } else if (eoeEnabled) {
            throw new IllegalStateException("use getVlanIfsByVlanId() " +
                    "if EoE feature is enable.");
        }
        String key = VlanIfImpl.getExtendedVlanId(null, Integer.valueOf(vlanid));
        return (VlanIf) getVlanifs().getUniqueValue(key);
    }

    public VlanIf getVlanIfBy(Integer eoeID, int vlanID) {
        String key = VlanIfImpl.getExtendedVlanId(eoeID, vlanID);
        return (VlanIf) getVlanifs().getUniqueValue(key);
    }

    public VlanIf[] getVlanifsHavingNullVlanid() {
        Set<VlanIf> result = getVlanifs().getNonUniqueValues(
                VlanIfImpl.getExtendedVlanId(null, Integer.valueOf(VlanIf.NULL_VLAN_ID)));
        if (result == null) {
            return new VlanIf[0];
        } else {
            return result.toArray(new VlanIf[0]);
        }
    }

    public synchronized VlanStpElement[] getVlanStpElements() {
        return vlanStpElements_;
    }

    public synchronized void addVlanStpElement(VlanStpElement vlanStpElement) {
        vlanStpElements_ = (VlanStpElement[]) VlanModelUtils
                .arrayaddNoDuplicate(vlanStpElements_, vlanStpElement);
    }

    @Override
    protected synchronized <T extends Port> void sortPorts(Class<T> type,
                                                           List<Port> ports) {
        if (VlanIf.class.isAssignableFrom(type)) {
            VlanModelUtils.sortVlanIf(ports);
        } else if (LogicalEthernetPort.class.isAssignableFrom(type)) {
            VlanModelUtils.sortLogicalEthernetPort(ports);
        } else {
            super.sortPorts(type, ports);
        }
    }
}