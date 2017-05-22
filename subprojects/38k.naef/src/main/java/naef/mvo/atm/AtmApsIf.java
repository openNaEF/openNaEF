package naef.mvo.atm;

import naef.mvo.AbstractPort;
import naef.mvo.Port;

public class AtmApsIf extends AbstractPort {

    private final S2<Port> parts_ = new S2<Port>(); 

    public AtmApsIf(MvoId id) {
        super(id);
    }

    public AtmApsIf() {
    }

    @Override protected S2<Port> getPartsField() {
        return parts_;
    }
}
