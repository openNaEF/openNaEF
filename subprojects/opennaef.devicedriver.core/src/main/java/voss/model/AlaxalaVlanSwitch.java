package voss.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AlaxalaVlanSwitch extends GenericEthernetSwitch {
    private static final long serialVersionUID = 1L;
    private List<AlaxalaQosFlowProfile> qosFlowProfiles = new ArrayList<AlaxalaQosFlowProfile>();

    public AlaxalaVlanSwitch() {
        super();
    }

    public synchronized void addQosFlowProfile(AlaxalaQosFlowProfile _profile) {
        if (_profile == null || isQosFlowProfileRegistered(_profile)) {
            return;
        }
        qosFlowProfiles.add(_profile);
    }

    private synchronized boolean isQosFlowProfileRegistered(AlaxalaQosFlowProfile target) {
        for (Iterator<AlaxalaQosFlowProfile> it = qosFlowProfiles.iterator(); it.hasNext(); ) {
            if (it.next().getQosFlowProfileName().equals(target.getQosFlowProfileName())) {
                return true;
            }
        }
        return false;
    }

    public synchronized AlaxalaQosFlowProfile[] getQosFlowProfiles() {
        return qosFlowProfiles.toArray(new AlaxalaQosFlowProfile[qosFlowProfiles.size()]);
    }

    public AlaxalaQosFlowProfile getQosFlowProfileByName(AlaxalaQosFlowListEntry entry) {
        for (Iterator<AlaxalaQosFlowProfile> it = qosFlowProfiles.iterator(); it.hasNext(); ) {
            AlaxalaQosFlowProfile profile = it.next();
            if (profile.getQosFlowProfileName().equals(entry.getQosFlowListName())) {
                return profile;
            }
        }
        AlaxalaQosFlowProfile newProfle =
                new AlaxalaQosFlowProfile(entry.getQosFlowListName(), entry.getQosFlowListKey());
        qosFlowProfiles.add(newProfle);
        return newProfle;
    }
}