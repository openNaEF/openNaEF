package naef.dto;

import java.util.Set;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;

public abstract class NodeElementDto extends NaefDto implements NamedModel {

    public static class ExtAttr {

        public static final Attribute.SingleString<NodeElementDto> SQN
            = new Attribute.SingleString<NodeElementDto>("sqn");
        public static final Attribute.SingleString<NodeElementDto> NODE_LOCAL_NAME
            = new Attribute.SingleString<NodeElementDto>("node local name");
        public static final SingleRefAttr<NodeDto, NodeElementDto> NODE
            = new SingleRefAttr<NodeDto, NodeElementDto>("node");
        public static final SingleRefAttr<NodeElementDto, NodeElementDto> OWNER
            = new SingleRefAttr<NodeElementDto, NodeElementDto>("owner");
        public static final SetRefAttr<NodeElementDto, NodeElementDto> SUBELEMENTS
            = new SetRefAttr<NodeElementDto, NodeElementDto>("subelements");
    }

    public NodeElementDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public String getSqn() {
        return ExtAttr.SQN.get(this);
    }

    public String getNodeLocalName() {
        return ExtAttr.NODE_LOCAL_NAME.get(this);
    }

    public NodeDto getNode() {
        return ExtAttr.NODE.deref(this);
    }

    public NodeElementDto getOwner() {
        return ExtAttr.OWNER.deref(this);
    }

    public Set<NodeElementDto> getSubElements() {
        return ExtAttr.SUBELEMENTS.deref(this);
    }
}
