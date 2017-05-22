package naef.mvo;

import tef.skelton.ConstraintException;

public class Module extends AbstractHardware {

    public Module(MvoId id) {
        super(id);
    }

    public Module(Slot owner, String name) throws ConstraintException {
        super(owner, name);
    }
}
