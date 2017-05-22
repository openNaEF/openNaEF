package naef.mvo;

import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.ConstraintException;
import tef.skelton.Model;
import tef.skelton.SkeltonTefService;
import tef.skelton.SkeltonUtils;
import tef.skelton.UiTypeName;
import tef.skelton.ValueException;

import java.util.List;
import java.util.Set;

public abstract class AbstractNodeElement extends AbstractModel implements NodeElement {

    public static class Attr {

        public static final String OBJECT_TYPE_FIELD_NAME = "naef.object-type";

        public static final Attribute.SingleModel<NodeElementType, NodeElement> OBJECT_TYPE
            = new Attribute.SingleModel<NodeElementType, NodeElement>(OBJECT_TYPE_FIELD_NAME, NodeElementType.class)
        {
            @Override public void validateValue(NodeElement model, NodeElementType newValue) {
                super.validateValue(model, newValue);

                if (newValue != null) {
                    validateDeclaringTypeAcceptability(newValue, model);

                    NodeElementType.Attr.ACCEPTABLE_OWNER_TYPES.validateConstraint(model, newValue);
                    NodeElementType.Attr.ACCEPTABLE_ELEMENT_TYPES.validateConstraint(model, newValue);
                }
            }

            private void validateDeclaringTypeAcceptability(NodeElementType type, NodeElement model) {
                for (UiTypeName acceptableType : NaefObjectType.Attr.ACCEPTABLE_DECLARING_TYPES.snapshot(type)) {
                    if (acceptableType.type().isInstance(model)) {
                        return;
                    }
                }

                throw new ValueException(
                    "型制約 " + NaefObjectType.Attr.ACCEPTABLE_DECLARING_TYPES.getName() + " に適合しません.");
            }
        };
    }

    private final N2<String, NodeElement> subElements_ = new N2<String, NodeElement>();

    public AbstractNodeElement(MvoId id) {
        super(id);
    }

    public AbstractNodeElement() {
    }

    @Override public String getFqn() {
        String simpleName 
            = SkeltonTefService.instance().uiTypeNames().getName(getClass())
            + SkeltonTefService.instance().getFqnPrimaryDelimiter()
            + SkeltonUtils.fqnEscape(getName());
        if (getOwner() == null) { 
            return simpleName;
        } else {
            return getOwner().getFqn() + getNode().getFqnSecondaryDelimiter() + simpleName;
        }
    }

    @Override public final void addSubElement(NodeElement subelement)
        throws ConstraintException 
    {
        if (subelement.getOwner() != this) {
            throw new IllegalArgumentException();
        }

        if (getHereafterSubElement(subelement.getClass(), subelement.getName()) != null) {
            throw new ConstraintException("名前の重複が検出されました: " + this.getFqn() + " " + subelement.getName());
        }

        NodeElementType.Attr.ACCEPTABLE_ELEMENT_TYPES.validateValue(this, subelement);

        subElements_.add(subelement.getName(), subelement);
    }

    @Override public final void removeSubElement(NodeElement subelement, OperationType opType)
        throws ConstraintException 
    {
        if (! subElements_.getHereafterValues().contains(subelement)) {
            return;
        }

        if (opType == OperationType.FINAL) {
            if (subelement.getHereafterSubElements().size() > 0) {
                throw new ConstraintException("廃止対象 " + subelement.getFqn() + " に子要素があります.");
            }

            if (subelement instanceof Port) {
                Port port = (Port) subelement;
                if (port.getHereafterNetworks(Network.class).size() > 0) {
                    throw new ConstraintException("廃止対象 " + subelement.getFqn() + " に接続が存在します.");
                }
            }

            for (Attribute<?, ?> attr : Attribute.getAttributes(subelement.getClass())) {
                if (attr instanceof Attribute.SingleAttr<?, ?>) {
                    subelement.set((Attribute.SingleAttr<?, Model>) attr, null);
                } else if (attr instanceof Attribute.CollectionAttr<?, ?, ?, ?>) {
                    Attribute.CollectionAttr<Object, NodeElement, ?, ?> collectionattr
                        = (Attribute.CollectionAttr<Object, NodeElement, ?, ?>) attr;
                    for (Object value : collectionattr.snapshot(subelement)) {
                        collectionattr.removeValue(subelement, value);
                    }
                } else if (attr instanceof Attribute.MapAttr<?, ?, ?>) {
                    Attribute.MapAttr<Object, ?, NodeElement> mappingattr
                        = (Attribute.MapAttr<Object, ?, NodeElement>) attr;
                    for (Object key : mappingattr.getKeys(subelement)) {
                        mappingattr.remove(subelement, key);
                    }
                }
            }
        }

        subElements_.remove(subelement.getName(), subelement);

        if (opType == OperationType.FINAL) {
            subelement.setOwner(null);
        }
    }

    @Override public final Set<NodeElement> getCurrentSubElements() {
        return subElements_.getValues();
    }

    @Override public final Set<NodeElement> getHereafterSubElements() {
        return subElements_.getHereafterValues();
    }

    @Override public <T extends NodeElement> T getHereafterSubElement(Class<T> klass, String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        List<NodeElement> elems = subElements_.get(name);
        if (elems == null) {
            return null;
        }

        T result = null;
        for (NodeElement elem : elems) {
            if (klass.isInstance(elem)) {
                if (result == null) {
                    result = (T) elem;
                } else {
                    throw new IllegalStateException(getFqn() + ", " + name);
                }
            }
        }
        return result;
    }
}
