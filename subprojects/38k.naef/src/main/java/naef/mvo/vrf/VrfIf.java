package naef.mvo.vrf;

import naef.mvo.AbstractPort;
import naef.mvo.NodeElement;
import tef.skelton.Attribute;

import java.util.Set;

public class VrfIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleBoolean<NodeElement> VRF_ENABLED
            = new Attribute.SingleBoolean<NodeElement>("naef.enabled-networking-function.vrf");
        public static final Attribute.SingleInteger<VrfIf> VRF_ID
            = new Attribute.SingleInteger<VrfIf>("VRF ID");
    }

    public VrfIf(MvoId id) {
        super(id);
    }

    public VrfIf() {
    }

    public Vrf getVrf() {
        Set<Vrf> vrfs = getCurrentNetworks(Vrf.class);
        if (vrfs.size() > 1) {
            throw new IllegalStateException();
        }
        return vrfs.size() == 0 ? null : vrfs.iterator().next();
    }
}
