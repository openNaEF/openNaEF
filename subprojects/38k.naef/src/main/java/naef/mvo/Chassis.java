package naef.mvo;

import tef.skelton.ConstraintException;

public class Chassis extends AbstractHardware {

    public Chassis(MvoId id) {
        super(id);
    }

    public Chassis(Node owner, String name) throws ConstraintException {
        super(owner, name);
    }
}
