package naef.mvo.bgp;

import naef.mvo.RoutingProcess;
import tef.skelton.ConstraintException;

public class BgpProcess extends RoutingProcess {

    public BgpProcess(MvoId id) {
        super(id);
    }

    public BgpProcess(String name) throws ConstraintException {
        super(name);
    }
}
