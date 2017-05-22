package voss.model;


import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class POSAPSImpl extends AbstractLogicalPort implements LogicalPort, APS<POSImpl>, POS {
    private Feature feature = null;
    private List<POSImpl> members = new ArrayList<POSImpl>();
    private POSImpl workingPort = null;

    @Override
    public Feature getLogicalFeature() {
        return this.feature;
    }

    @Override
    public void setLogicalFeature(Feature feature) {
        this.feature = feature;
        feature.setParentPort(this);
    }

    @Override
    public void addMemberPort(POSImpl member) {
        for (POSImpl m : members) {
            if (m.equals(member)) {
                return;
            }
        }
        this.members.add(member);
        VlanModelUtils.sortPhysicalPort(this.members);
    }

    @Override
    public List<POSImpl> getMemberPort() {
        List<POSImpl> result = new ArrayList<POSImpl>();
        result.addAll(this.members);
        return result;
    }

    @Override
    public void resetMemberPort() {
        this.members.clear();
    }

    @Override
    public POSImpl getWorkingPort() {
        return this.workingPort;
    }

    @Override
    public void setWorkingPort(POSImpl workingPort) {
        for (POSImpl m : members) {
            if (m.equals(workingPort)) {
                this.workingPort = workingPort;
                return;
            }
        }
        throw new IllegalArgumentException();
    }

}