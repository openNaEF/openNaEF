package naef.mvo;

import tef.skelton.Attribute;
import tef.skelton.ConstraintException;

public abstract class AbstractHardware extends AbstractNodeElement implements Hardware {

    public static class Attr {

        public static final Attribute.SingleModel<HardwareType, Hardware> OBJECT_TYPE
            = new Attribute.SingleModel<HardwareType, Hardware>(
                AbstractNodeElement.Attr.OBJECT_TYPE_FIELD_NAME, 
                HardwareType.class)
        {
            @Override public void validateValue(Hardware model, HardwareType newValue) {
                super.validateValue(model, newValue);

                AbstractNodeElement.Attr.OBJECT_TYPE.validateValue(model, newValue);
            }
        };
    }

    private final F2<NodeElement> owner_ = new F2<NodeElement>();
    private final F1<String> name_ = new F1<String>();

    public AbstractHardware(MvoId id) {
        super(id);
    }

    public AbstractHardware(NodeElement owner, String name) throws ConstraintException {
        if (owner == null || name == null) {
            throw new IllegalArgumentException();
        }

        setName(name);
        setOwner(owner);
    }

    @Override public void setName(String name) throws ConstraintException {
        if (NaefMvoUtils.equals(name, getName())) {
            return;
        }

        NodeElement owner = getOwner();
        if (owner != null) {
            owner.removeSubElement(this, OperationType.TRANSIENT);
        }

        name_.set(name);

        if (owner != null) {
            owner.addSubElement(this);
        }
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public Node getNode() {
        return getOwner() == null ? null : getOwner().getNode();
    }

    @Override public void setOwner(NodeElement newOwner) throws ConstraintException {
        if (owner_.getFutureChanges().size() > 0) {
            throw new ConstraintException("他の変更が予定されています.");
        }

        NodeElementType.Attr.ACCEPTABLE_OWNER_TYPES.validateValue(this, newOwner);

        NodeElement oldOwner = getOwner();
        if (NaefMvoUtils.equals(oldOwner, newOwner)) {
            return;
        }

        if (oldOwner != null) {
            oldOwner.removeSubElement(this, OperationType.TRANSIENT);
        }

        owner_.set(newOwner);

        if (newOwner != null) {
            newOwner.addSubElement(this);
        }
    }

    @Override public NodeElement getOwner() {
        return owner_.get();
    }
}
