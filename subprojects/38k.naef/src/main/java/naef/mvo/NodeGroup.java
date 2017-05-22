package naef.mvo;

import tef.MvoHome;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.NameConfigurableModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.SkeltonUtils;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueException;
import tef.skelton.ValueResolver;

public class NodeGroup extends AbstractModel implements NameConfigurableModel {

    public static class Attr {

        public static final Attribute.SetAttr<Node, NodeGroup> MEMBERS = new Attribute.SetAttr<Node, NodeGroup>(
            "naef.node-group.members",
            new AttributeType.MvoSetType<Node>(Node.class) {

                @Override public Node parseElement(String valueStr) {
                    return ValueResolver.<Node>resolve(Node.class, null, valueStr);
                }
            });
        static {
            MEMBERS.addPostProcessor(new Attribute.SetAttr.PostProcessor<Node, NodeGroup>() {

                @Override public void add(NodeGroup model, Node value) {
                    if (! Node.Attr.NODE_GROUPS.containsValue(value, model)) {
                        Node.Attr.NODE_GROUPS.addValue(value, model);
                    }
                }

                @Override public void remove(NodeGroup model, Node value) {
                    if (Node.Attr.NODE_GROUPS.containsValue(value, model)) {
                        Node.Attr.NODE_GROUPS.removeValue(value, model);
                    }
                }
            });
        }
    }

    public static final UniquelyNamedModelHome.Indexed<NodeGroup> home
        = new UniquelyNamedModelHome.Indexed<NodeGroup>(NodeGroup.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public NodeGroup(MvoId id) {
        super(id);
    }

    public NodeGroup(String name) {
        setName(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public void setName(String name) {
        try {
            name_.set(name);
        } catch (MvoHome.UniqueIndexDuplicatedKeyFoundException uidkfe) {
            throw new ValueException(
                "名前の重複が検出されました: " + SkeltonTefService.instance().uiTypeNames().getName(getClass())
                + ", " + name);
        }
    }

    public String getFqn() {
        return SkeltonTefService.instance().uiTypeNames().getName(getClass())
            + SkeltonTefService.instance().getFqnPrimaryDelimiter()
            + SkeltonUtils.fqnEscape(getName());
    }
}
