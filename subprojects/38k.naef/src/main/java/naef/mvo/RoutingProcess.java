package naef.mvo;

import tef.skelton.ConstraintException;

public abstract class RoutingProcess extends AbstractNodeElement {

    private final F2<NodeElement> owner_ = new F2<NodeElement>();
    private final F1<String> name_ = new F1<String>();

    public RoutingProcess(MvoId id) {
        super(id);
    }

    public RoutingProcess(String name) throws ConstraintException {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        setName(name);
    }

    @Override public Node getNode() {
        return getOwner() == null ? null : getOwner().getNode();
    }

    @Override public void setOwner(NodeElement newOwner) throws ConstraintException {
        if (owner_.getFutureChanges().size() > 0) {
            throw new ConstraintException("他の変更が予定されています.");
        }

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
}
