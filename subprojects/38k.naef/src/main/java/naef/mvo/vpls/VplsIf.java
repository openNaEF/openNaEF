package naef.mvo.vpls;

import naef.mvo.AbstractPort;
import naef.mvo.NodeElement;
import tef.skelton.Attribute;

import java.util.Set;

public class VplsIf extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleBoolean<NodeElement> VPLS_ENABLED
            = new Attribute.SingleBoolean<NodeElement>("naef.enabled-networking-function.vpls");

        public static final Attribute.SingleInteger<VplsIf> VPLS_ID
            = new Attribute.SingleInteger<VplsIf>("VPLS ID");
    }

    public VplsIf(MvoId id) {
        super(id);
    }

    public VplsIf() {
    }

    public Vpls getVpls() {
        Set<Vpls> vplses = getCurrentNetworks(Vpls.class);
        if (vplses.size() > 1) {
            throw new IllegalStateException();
        }
        return vplses.size() == 0 ? null : vplses.iterator().next();
    }
}
