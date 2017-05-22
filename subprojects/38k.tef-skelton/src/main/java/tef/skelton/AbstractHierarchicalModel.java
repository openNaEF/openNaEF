package tef.skelton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractHierarchicalModel<T extends AbstractHierarchicalModel> 
    extends AbstractModel 
{
    private final F1<T> parent_ = new F1<T>();
    private final S1<T> children_ = new S1<T>();

    protected AbstractHierarchicalModel(MvoId id) {
        super(id);
    }

    protected AbstractHierarchicalModel() {
    }

    public void setParent(T parent) {
        if (parent == this) {
            throw new ValueException("[階層構造制約違反] 自分自身を親に設定することはできません.");
        }

        if (getParent() == parent) {
            return;
        }

        if (parent != null && parent.getClass() != this.getClass()) {
            throw new ValueException("[階層構造制約違反] " + uiTypeName() + " を指定してください.");
        }
        if (parent.getAncestors().contains(this)) {
            throw new ValueException("[階層構造制約違反] 循環する構造はサポートされていません.");
        }

        if (getParent() != null) {
            getParent().removeChild((T) this);
        }

        parent_.set(parent);

        if (getParent() != null) {
            getParent().addChild((T) this);
        }
    }

    public List<T> getAncestors() {
        T parent = getParent();
        if (parent == null) {
            return new ArrayList<T>();
        } else {
            List<T> result = parent.getAncestors();
            result.add(getParent());
            return result;
        }
    }

    public T getParent() {
        return parent_.get();
    }

    public T getRoot() {
        return getParent() == null
            ? (T) this
            : (T) getParent().getRoot();
    }

    void addChild(T subLocation) {
        children_.add(subLocation);
    }

    void removeChild(T subLocation) {
        children_.remove(subLocation);
    }

    public Set<T> getChildren() {
        return children_.get();
    }
}
