package voss.model;

public interface EsrpGroup extends VlanStpElement {

    public static interface MasterSlaveRelationship {

        public static class StateType extends VlanModelConstants {
            private static final long serialVersionUID = 1L;

            public static final StateType NEUTRAL = new StateType("neutral");
            public static final StateType MASTER = new StateType("master");
            public static final StateType SLAVE = new StateType("slave");

            private StateType() {
            }

            private StateType(String id) {
                super(id);
            }
        }

        public VlanIf getVlanIf();

        public void setVlanIf(VlanIf vlanIf);

        public EsrpGroup getEsrpGroup();

        public void setEsrpGroup(EsrpGroup esrpGroup);

        public StateType getStateType();

        public void setStateType(StateType stateType);

        public int getPriority();

        public void setPriority(int priority);
    }

    public int getGroupIndex();

    public void setGroupIndex(int groupIndex);

    public MasterSlaveRelationship[] getMasterSlaveRelationships();

    public void setMasterSlaveRelationships(MasterSlaveRelationship[] values);
}