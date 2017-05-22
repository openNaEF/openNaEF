package naef.mvo.eth;

import naef.mvo.AbstractPort;
import naef.mvo.Port;

public class EthPort extends AbstractPort {

    private final F2<Port> container_ = new F2<Port>(); 

    public EthPort(MvoId id) {
        super(id);
    }

    public EthPort() {
    }

    @Override protected F2<Port> getContainerField() {
        return container_;
    }
}
