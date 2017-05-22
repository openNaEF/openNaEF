package voss.discovery.agent.alaxala.mib;

import voss.discovery.iolib.snmp.SnmpEntry;

import java.util.Map;

public class AlaxalaVlanPortBindingState {

    public static final String TAGGED = "tagged";
    public static final String UNTAGGED = "untagged";
    public static final String UNKNOWN = "unknown";
    public static final int ifIndexOffsetForVlanId = 20000;

    private int vlanId;
    private int boundPortIfIndex;
    private String taggingState;

    public AlaxalaVlanPortBindingState(SnmpEntry entry,
                                       Map<Integer, Integer> portIndexToIfIndex) {
        assert entry.oidSuffix.length == 2;
        Integer bindedPortIfIndex = portIndexToIfIndex.get(new Integer(
                entry.oidSuffix[1].intValue()));
        assert bindedPortIfIndex != null;

        setup(entry.oidSuffix[0].intValue(), bindedPortIfIndex.intValue(),
                entry.getValueAsBigInteger().intValue());
    }

    public void setup(int vlanId, int boundPortIfIndex, int taggingState) {
        this.vlanId = vlanId;
        this.boundPortIfIndex = boundPortIfIndex;
        this.taggingState = resolveTaggingState(taggingState);
    }

    public int getVlanId() {
        return vlanId;
    }

    public int getBoundPortIfIndex() {
        return boundPortIfIndex;
    }

    public boolean isTagged() {
        return taggingState.equals(TAGGED);
    }

    public boolean isUntagged() {
        return taggingState.equals(UNTAGGED);
    }

    public static String resolveTaggingState(int taggingState) {
        switch (taggingState) {
            case 1:
                return UNTAGGED;
            case 2:
                return TAGGED;
            default:
                return UNKNOWN;
        }
    }

    public String toString() {
        return "VlanId: " + this.vlanId + ", portIfIndex: " + boundPortIfIndex
                + ", taggingState: " + taggingState;
    }

}