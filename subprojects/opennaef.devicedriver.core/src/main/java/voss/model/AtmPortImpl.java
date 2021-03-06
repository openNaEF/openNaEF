package voss.model;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class AtmPortImpl extends AbstractPhysicalPort implements AtmPort {
    private final Set<AtmVp> atmVps = new HashSet<AtmVp>();

    public AtmPortImpl() {
        super();
        this.atmVps.clear();
    }

    @Override
    public Port getParentPort() {
        return this;
    }

    @Override
    public void setParentPort(Port port) {
    }

    @Override
    public void addVp(AtmVp vp) {
        AtmVp vp_ = getVp(vp.getVpi());
        if (vp_ != null) {
            throw new IllegalStateException("already added. vpi=" + vp.getVpi());
        }
        this.atmVps.add(vp);
    }

    @Override
    public AtmVp getVp(int vpi) {
        for (AtmVp vp : atmVps) {
            if (vp.getVpi() == vpi) {
                return vp;
            }
        }
        return null;
    }

    @Override
    public AtmVp[] getVps() {
        List<AtmVp> result = new ArrayList<AtmVp>();
        result.addAll(this.atmVps);
        return result.toArray(new AtmVp[0]);
    }

}