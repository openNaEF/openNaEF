package naef.mvo;

import tef.skelton.ConstraintException;

public class Slot extends AbstractHardware {

    public Slot(MvoId id) {
        super(id);
    }

    public Slot(Hardware owner, String name) throws ConstraintException {
        super(owner, name);
    }

    public Module getCurrentModule() {
        Module result = null;
        for (Module module : NaefMvoUtils.getCurrentSubElements(this, Module.class, false)) {
            if (result != null) {
                throw new RuntimeException();
            } else {
                result = module;
            }
        }
        return result;
    }
}
