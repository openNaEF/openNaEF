package naef.mvo;

import tef.skelton.ConstraintException;

public class Jack extends AbstractHardware {

    public Jack(MvoId id) {
        super(id);
    }

    public Jack(Hardware owner, String name) throws ConstraintException {
        super(owner, name);
    }

    public Port getPort() {
        return getHereafterSubElement(Port.class, "");
    }
}
