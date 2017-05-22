package naef.mvo.atm;

import naef.mvo.AbstractPort;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.skelton.Attribute;

public class AtmPort extends AbstractPort {

    public static class Attr {

        public static final Attribute.SingleBoolean<NodeElement> ATM_ENABLED
            = new Attribute.SingleBoolean<NodeElement>("naef.enabled-networking-function.atm");
    }

    private final F2<Port> container_ = new F2<Port>(); 

    public AtmPort(MvoId id) {
        super(id);
    }

    public AtmPort() {
    }

    @Override protected F2<Port> getContainerField() {
        return container_;
    }
}
