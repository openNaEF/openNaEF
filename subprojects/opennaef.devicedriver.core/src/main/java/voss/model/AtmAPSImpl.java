package voss.model;


import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class AtmAPSImpl extends AbstractLogicalPort implements LogicalPort, APS<AtmPortImpl>, AtmPort {
    private AtmPortFeatureImpl feature = new AtmPortFeatureImpl();
    private List<AtmPortImpl> members = new ArrayList<AtmPortImpl>();
    private AtmPortImpl workingPort = null;

    @Override
    public Port getParentPort() {
        return this;
    }

    @Override
    public void setParentPort(Port port) {
    }

    @Override
    public void addMemberPort(AtmPortImpl member) {
        for (AtmPortImpl m : members) {
            if (m.equals(member)) {
                return;
            }
        }
        this.members.add(member);
        VlanModelUtils.sortPhysicalPort(this.members);
    }

    @Override
    public List<AtmPortImpl> getMemberPort() {
        List<AtmPortImpl> result = new ArrayList<AtmPortImpl>();
        result.addAll(this.members);
        return result;
    }

    @Override
    public void resetMemberPort() {
        this.members.clear();
    }

    @Override
    public AtmPortImpl getWorkingPort() {
        return this.workingPort;
    }

    @Override
    public void setWorkingPort(AtmPortImpl workingPort) {
        for (AtmPortImpl m : members) {
            if (m.equals(workingPort)) {
                this.workingPort = workingPort;
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public AtmVp[] getVps() {
        return this.feature.getVps();
    }

    @Override
    public void addVp(AtmVp vp) {
        this.feature.addVp(vp);
    }

    @Override
    public AtmVp getVp(int vpi) {
        return this.feature.getVp(vpi);
    }

}