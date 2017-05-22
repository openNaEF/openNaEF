package voss.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class AbstractLogicalEthernetPort extends AbstractLogicalPort implements LogicalEthernetPort {
    private static final long serialVersionUID = 1L;

    public static class TagChangerImpl extends AbstractLogicalEthernetPort implements TagChanger {
        private static final long serialVersionUID = 1L;
        private LogicalEthernetPort logicaleth_;
        private Integer tagChangerId_;
        private Integer innerVlanId_;
        private Integer outerVlanId_;
        private Map<Integer, Integer> secondaryTagTranslationMap = new HashMap<Integer, Integer>();

        public TagChangerImpl() {
        }

        public synchronized void initLogicalEthernetPort(LogicalEthernetPort logicaleth) {
            if (logicaleth == null) {
                throw new NullArgumentIsNotAllowedException();
            }

            if (logicaleth == logicaleth_) {
                return;
            }

            try {
                getLogicalEthernetPort();

                throw new AlreadyInitializedException(getLogicalEthernetPort().getFullyQualifiedName(), logicaleth.getIfName());
            } catch (NotInitializedException nie) {
            }

            if (getDevice() == null) {
                initDevice(logicaleth.getDevice());
            }

            if (getDevice() != logicaleth.getDevice()) {
                throw new IllegalArgumentException("A device do not match: It tried to set "+ logicaleth.getFullyQualifiedName() + " to " + getFullyQualifiedName() + ".");
            }

            logicaleth_ = logicaleth;
            logicaleth_.addTagChanger(this);
        }

        public synchronized LogicalEthernetPort getLogicalEthernetPort() throws NotInitializedException {
            if (logicaleth_ == null) {
                throw new NotInitializedException();
            }

            return logicaleth_;
        }

        public synchronized EthernetPort[] getPhysicalPorts() throws NotInitializedException {
            return new EthernetPort[0];
        }

        public synchronized boolean isAggregated() {
            return false;
        }

        public synchronized void setTagChangerId(Integer tagChangerId) {
            tagChangerId_ = tagChangerId;
        }

        public synchronized Integer getTagChangerId() {
            return tagChangerId_;
        }

        public synchronized void setInnerVlanId(Integer vlanid) {
            innerVlanId_ = vlanid;
        }

        public synchronized Integer getInnerVlanId() {
            return innerVlanId_;
        }

        public synchronized void setOuterVlanId(Integer vlanid) {
            outerVlanId_ = vlanid;
        }

        public synchronized Integer getOuterVlanId() {
            return outerVlanId_;
        }

        @Override
        public synchronized void addSecondaryMap(Integer device, Integer link) {
            if (device == null || link == null) {
                return;
            }
            this.secondaryTagTranslationMap.put(device, link);
        }

        @Override
        public synchronized void removeSecondaryMap(Integer device) {
            this.secondaryTagTranslationMap.remove(device);
        }

        @Override
        public synchronized void resetSecondaryMap() {
            this.secondaryTagTranslationMap.clear();
        }

        @Override
        public synchronized Map<Integer, Integer> getSecondaryMap() {
            Map<Integer, Integer> result = new HashMap<Integer, Integer>();
            result.putAll(this.secondaryTagTranslationMap);
            return result;
        }
    }

    private TagChanger[] tagChangers_ = new TagChanger[0];
    private VlanPortUsage vlanPortUsage;
    private String qosClassificationKey;

    protected AbstractLogicalEthernetPort() {
    }

    public boolean isNeighborConsistent() {
        EthernetPort[] physicalPorts = getPhysicalPorts();

        if (physicalPorts.length == 0) {
            return true;
        }

        LogicalEthernetPort neighbor;
        if ((EthernetPort) physicalPorts[0].getNeighbor() == null) {
            neighbor = null;
        } else {
            neighbor = getLogicalEthernetPort((EthernetPort) physicalPorts[0].getNeighbor());
            if (neighbor != null && neighbor.getPhysicalPorts().length != physicalPorts.length) {
                return false;
            }
        }
        for (int i = 0; i < physicalPorts.length; i++) {
            EthernetPort neighborEth = (EthernetPort) physicalPorts[i].getNeighbor();
            LogicalEthernetPort testee = neighborEth == null ? null : getLogicalEthernetPort(neighborEth);
            if (neighbor != testee) {
                return false;
            }
        }

        return true;
    }

    public LogicalEthernetPort getNeighbor() {
        if (!isNeighborConsistent()) {
            return null;
        }
        if (getPhysicalPorts().length == 0) {
            return null;
        }
        EthernetPort neighborEthernetPort = (EthernetPort) getPhysicalPorts()[0].getNeighbor();
        return neighborEthernetPort == null ? null : getLogicalEthernetPort(neighborEthernetPort);
    }

    public VlanIf getUntaggedVlanIf() {
        VlanIf[] vlanIfs = ((VlanDevice) getDevice()).getVlanIfs();
        VlanIf result = null;
        for (int i = 0; i < vlanIfs.length; i++) {
            if (vlanIfs[i].isBindedAsUntagged(AbstractLogicalEthernetPort.this)) {
                if (result != null) {
                    throw new IllegalStateException("Duplicate untagged connection detected: " + getFullyQualifiedName() + " vlan-"
                            + result.getVlanId() + ", vlan-" + vlanIfs[i].getVlanId());
                }

                result = vlanIfs[i];
            }
        }

        return result;
    }

    public VlanIf[] getUntaggedVlanIfs() {
        VlanIf[] vlanIfs = ((VlanDevice) getDevice()).getVlanIfs();
        List<VlanIf> result = new ArrayList<VlanIf>();
        for (int i = 0; i < vlanIfs.length; i++) {
            if (vlanIfs[i].isBindedAsUntagged(AbstractLogicalEthernetPort.this)) {
                result.add(vlanIfs[i]);
            }
        }
        return (VlanIf[]) result.toArray(new VlanIf[0]);
    }

    public VlanIf[] getTaggedVlanIfs() {
        VlanIf[] vlanIfs = ((VlanDevice) getDevice()).getVlanIfs();
        List<VlanIf> result = new ArrayList<VlanIf>();
        for (int i = 0; i < vlanIfs.length; i++) {
            if (vlanIfs[i].isBindedAsTagged(AbstractLogicalEthernetPort.this)) {
                result.add(vlanIfs[i]);
            }
        }
        return (VlanIf[]) result.toArray(new VlanIf[0]);
    }

    public void setIngressClassOfService(IngressClassOfService value) {
        addProperty(value);
    }

    public IngressClassOfService getIngressClassOfService() {
        return (IngressClassOfService) selectConfigProperty(IngressClassOfService.class);
    }

    public void setQosIngressTrafficLimitation(QosIngressTrafficLimitation value) {
        addProperty(value);
    }

    public QosIngressTrafficLimitation getQosIngressTrafficLimitation() {
        return (QosIngressTrafficLimitation) selectConfigProperty(QosIngressTrafficLimitation.class);
    }

    public void setQosEgressTrafficLimitation(QosEgressTrafficLimitation value) {
        addProperty(value);
    }

    public QosEgressTrafficLimitation getQosEgressTrafficLimitation() {
        return (QosEgressTrafficLimitation) selectConfigProperty(QosEgressTrafficLimitation.class);
    }

    public void setQosClassificationKey(String value) {
        this.qosClassificationKey = value;
    }

    public String getQosClassificationKey() {
        return this.qosClassificationKey;
    }

    static LogicalEthernetPort getLogicalEthernetPort(EthernetPort ethPort) {
        if (ethPort.getDevice() instanceof VlanDevice) {
            return ((VlanDevice) ethPort.getDevice()).getLogicalEthernetPort(ethPort);
        } else {
            return null;
        }
    }

    public synchronized void addTagChanger(TagChanger tagChanger) {
        if (tagChanger == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        if (VlanModelUtils.containsComparedByEquals(tagChangers_, tagChanger)) {
            return;
        }

        tagChangers_ = (TagChanger[]) VlanModelUtils.arrayadd(tagChangers_, new TagChanger[]{tagChanger});
        tagChanger.initLogicalEthernetPort(this);
    }

    public synchronized TagChanger[] getTagChangers() {
        return (TagChanger[]) VlanModelUtils.arraycopy(tagChangers_);
    }

    public synchronized void setVlanPortUsage(VlanPortUsage usage) {
        if (usage == null) {
            throw new IllegalStateException();
        }
        this.vlanPortUsage = usage;
    }

    public synchronized VlanPortUsage getVlanPortUsage() {
        return this.vlanPortUsage;
    }

    public synchronized void resetVlanPortUsage() {
        this.vlanPortUsage = null;
    }
}