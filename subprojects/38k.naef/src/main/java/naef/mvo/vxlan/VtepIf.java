package naef.mvo.vxlan;

import naef.mvo.AbstractPort;

import java.util.Set;

public class VtepIf extends AbstractPort {

    public VtepIf(MvoId id) {
        super(id);
    }

    public VtepIf() {
    }

    public Vxlan getVxlan() {
        final Set<Vxlan> vxlans = getCurrentNetworks(Vxlan.class);
        switch (vxlans.size()) {
            case 0:
                return null;
            case 1:
                return vxlans.iterator().next();
            default:
                throw new IllegalStateException();
        }
    }
}
