package naef.mvo;

import tef.skelton.Attribute;
import tef.skelton.Attribute.ValueTypeConstraintAttr;
import tef.skelton.UniquelyNamedModelHome;

import java.util.Collection;

public class NodeElementType extends NaefObjectType {

    public static class Attr {

        public static final ValueTypeConstraintAttr.Single<NodeElementType, NodeElement, NodeElement>
            ACCEPTABLE_OWNER_TYPES = new ValueTypeConstraintAttr.Single<NodeElementType, NodeElement, NodeElement>(
                "naef.acceptable-owner-types")
        {
            @Override public NodeElement getExistingValue(NodeElement model) {
                return model.getOwner();
            }

            @Override public Attribute.SingleAttr<NodeElementType, NodeElement> getConstraintAttr() {
                return AbstractNodeElement.Attr.OBJECT_TYPE;
            }
        };

        public static final ValueTypeConstraintAttr.Multi<NodeElementType, NodeElement, NodeElement>
            ACCEPTABLE_ELEMENT_TYPES = new ValueTypeConstraintAttr.Multi<NodeElementType, NodeElement, NodeElement>(
                "naef.acceptable-element-types")
        {
            @Override public Collection<NodeElement> getExistingValues(NodeElement model) {
                return model.getHereafterSubElements();
            }

            @Override public Attribute.SingleAttr<NodeElementType, NodeElement> getConstraintAttr() {
                return AbstractNodeElement.Attr.OBJECT_TYPE;
            }
        };
    }

    public static final UniquelyNamedModelHome.SharedNamespace<NodeElementType> home
        = new UniquelyNamedModelHome.SharedNamespace<NodeElementType>(NaefObjectType.home, NodeElementType.class);

    public NodeElementType(MvoId id) {
        super(id);
    }

    public NodeElementType(String name) {
        super(name);
    }
}
