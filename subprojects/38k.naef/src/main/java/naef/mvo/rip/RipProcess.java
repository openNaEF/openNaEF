package naef.mvo.rip;

import naef.mvo.RoutingProcess;
import tef.skelton.ConstraintException;

public class RipProcess extends RoutingProcess {

    public RipProcess(MvoId id) {
        super(id);
    }

    public RipProcess(String name) throws ConstraintException {
        super(name);
    }
}
