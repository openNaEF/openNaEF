package naef.mvo.pos;

import naef.mvo.AbstractPort;
import naef.mvo.Port;

public class PosPort extends AbstractPort {

    private final F2<Port> container_ = new F2<Port>(); 

    public PosPort(MvoId id) {
        super(id);
    }

    public PosPort() {
    }

    @Override protected F2<Port> getContainerField() {
        return container_;
    }
}
