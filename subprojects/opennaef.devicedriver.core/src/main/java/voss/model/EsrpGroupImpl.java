package voss.model;

public class EsrpGroupImpl extends AbstractVlanStpElement implements EsrpGroup {
    private static final long serialVersionUID = 1L;

    public static class MasterSlaveRelationshipImpl implements MasterSlaveRelationship {
        private VlanIf vlanIf_;
        private EsrpGroup esrpGroup_;
        private StateType stateType_;
        private int priority_;

        public MasterSlaveRelationshipImpl() {
        }

        public synchronized VlanIf getVlanIf() {
            return vlanIf_;
        }

        public synchronized void setVlanIf(VlanIf vlanIf) {
            vlanIf_ = vlanIf;
        }

        public synchronized EsrpGroup getEsrpGroup() {
            return esrpGroup_;
        }

        public synchronized void setEsrpGroup(EsrpGroup esrpGroup) {
            esrpGroup_ = esrpGroup;
        }

        public synchronized StateType getStateType() {
            return stateType_;
        }

        public synchronized void setStateType(StateType stateType) {
            stateType_ = stateType;
        }

        public synchronized int getPriority() {
            return priority_;
        }

        public synchronized void setPriority(int priority) {
            priority_ = priority;
        }
    }

    private int groupIndex_;
    private MasterSlaveRelationship[] masterSlaveRelationships_ = new MasterSlaveRelationship[0];

    public EsrpGroupImpl() {
    }

    public String getVlanStpElementId() {
        return Integer.toString(getGroupIndex());
    }

    public synchronized int getGroupIndex() {
        return groupIndex_;
    }

    public synchronized void setGroupIndex(int groupIndex) {
        groupIndex_ = groupIndex;
    }

    public synchronized MasterSlaveRelationship[] getMasterSlaveRelationships() {
        return masterSlaveRelationships_;
    }

    public synchronized void setMasterSlaveRelationships(MasterSlaveRelationship[] value) {
        masterSlaveRelationships_ = value;
    }
}