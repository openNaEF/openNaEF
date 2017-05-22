package naef.mvo.pos;

import naef.mvo.AbstractPort;
import naef.mvo.Port;

public class PosApsIf extends AbstractPort {

    private final S2<Port> parts_ = new S2<Port>(); 

    public PosApsIf(MvoId id) {
        super(id);
    }

    public PosApsIf() {
    }

    @Override protected S2<Port> getPartsField() {
        return parts_;
    }
}
