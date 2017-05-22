package voss.model;

import java.util.Map;


public interface LogicalEthernetPort extends LogicalPort {

    public static interface TagChanger extends LogicalEthernetPort {

        public void initLogicalEthernetPort(LogicalEthernetPort logicaleth);

        public LogicalEthernetPort getLogicalEthernetPort() throws NotInitializedException;

        public void setTagChangerId(Integer tagChangerId);

        public Integer getTagChangerId();

        public void setInnerVlanId(Integer vlanid);

        public Integer getInnerVlanId();

        public void setOuterVlanId(Integer vlanid);

        public Integer getOuterVlanId();

        public void addSecondaryMap(Integer device, Integer link);

        public void removeSecondaryMap(Integer device);

        public void resetSecondaryMap();

        public Map<Integer, Integer> getSecondaryMap();
    }

    public static class StpPortStatus extends VlanModelConstants {
        private static final long serialVersionUID = 1L;

        public static final StpPortStatus FORWARDING = new StpPortStatus("forwarding");
        public static final StpPortStatus BLOCKING = new StpPortStatus("blocking");
        public static final StpPortStatus NOT_CONFIGURED = new StpPortStatus("not configured");
        public static final StpPortStatus OTHER = new StpPortStatus("other");

        private StpPortStatus(String id) {
            super(id);
        }
    }

    public static class IngressClassOfService extends IntegerConfigProperty {
        private static final long serialVersionUID = 1L;

        protected IngressClassOfService() {
        }

        public IngressClassOfService(Integer cosValue) {
            super(cosValue);
        }

        public Integer getCosValue() {
            return getValue();
        }
    }

    public static class QosIngressTrafficLimitation extends LongConfigProperty {
        private static final long serialVersionUID = 1L;

        protected QosIngressTrafficLimitation() {
        }

        public QosIngressTrafficLimitation(Long value) {
            super(value);
            if (value == null) {
                throw new NullArgumentIsNotAllowedException();
            }
        }

        public Long getBpsValue() {
            return getValue();
        }
    }

    public static class QosEgressTrafficLimitation extends LongConfigProperty {
        private static final long serialVersionUID = 1L;

        protected QosEgressTrafficLimitation() {
        }

        public QosEgressTrafficLimitation(Long value) {
            super(value);

            if (value == null) {
                throw new NullArgumentIsNotAllowedException();
            }
        }

        public Long getBpsValue() {
            return getValue();
        }
    }

    public boolean isAggregated();

    public EthernetPort[] getPhysicalPorts() throws NotInitializedException;

    public LogicalEthernetPort getNeighbor();

    public VlanIf getUntaggedVlanIf();

    public VlanIf[] getUntaggedVlanIfs();

    public VlanIf[] getTaggedVlanIfs();

    public void setIngressClassOfService(IngressClassOfService value);

    public IngressClassOfService getIngressClassOfService();

    public void setQosIngressTrafficLimitation(QosIngressTrafficLimitation value);

    public QosIngressTrafficLimitation getQosIngressTrafficLimitation();

    public void setQosEgressTrafficLimitation(QosEgressTrafficLimitation value);

    public QosEgressTrafficLimitation getQosEgressTrafficLimitation();

    public void setQosClassificationKey(String value);

    public String getQosClassificationKey();

    public boolean isNeighborConsistent();

    public void addTagChanger(TagChanger tagChanger);

    public TagChanger[] getTagChangers();

    public void setVlanPortUsage(VlanPortUsage usage);

    public VlanPortUsage getVlanPortUsage();

    public void resetVlanPortUsage();
}