package naef.mvo;

import naef.NaefTefService;
import tef.MvoHome;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.ConstraintException;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueException;
import tef.skelton.ValueResolver;

public class Node extends AbstractNodeElement {

    public enum VirtualizationHostedType {

        NONE, SINGLE_HOST, MULTI_HOST
    }

    public static class Attr {

        public static final Attribute.SingleString<Node> SQN_DELIMITER
            = new Attribute.SingleString<Node>("naef.delimiter.sqn");

        public static final Attribute.SingleString<Node> FQN_PRIMARY_DELIMITER
            = new Attribute.SingleString<Node>("naef.delimiter.fqn-primary");
        public static final Attribute.SingleString<Node> FQN_SECONDARY_DELIMITER
            = new Attribute.SingleString<Node>("naef.delimiter.fqn-secondary");

        /**
         * この node の種類を表す node-type の属性定義です.
         * <p>
         * <dl>
         *  <dt>attribute name: <dd>"naef.object-type"
         * </dl>
         */
        public static final Attribute.SingleModel<NodeType, Node> OBJECT_TYPE
            = new Attribute.SingleModel<NodeType, Node>(
                AbstractNodeElement.Attr.OBJECT_TYPE_FIELD_NAME, 
                NodeType.class);

        public static final Attribute.MapAttr<String, Port, Node>IFNAME_MAP = new Attribute.MapAttr<String, Port, Node>(
            "naef.node.ifname-map",
            new AttributeType.MvoMapType<String, Port>(
                ValueResolver.STRING,
                new ValueResolver.Model<Port>(Port.class)));
        static {
            IFNAME_MAP.addPostProcessor(new Attribute.MapAttr.PostProcessor<String, Port, Node>() {

                @Override public void put(Node model, String ifname, Port oldPort, Port newPort) {
                    if (ifname == null) {
                        throw new ValueException("null key is not allowed.");
                    }

                    if (oldPort != null) {
                        String oldPortIfname = AbstractPort.Attr.IFNAME.get(oldPort);
                        if (ifname.equals(oldPortIfname)) {
                            AbstractPort.Attr.IFNAME.set(oldPort, null);
                        }
                    }

                    if (newPort != null) {
                        String newPortIfname = AbstractPort.Attr.IFNAME.get(newPort);
                        if (! ifname.equals(newPortIfname)) {
                            AbstractPort.Attr.IFNAME.set(newPort, ifname);
                        }
                    }
                }

                @Override public void remove(Node model, String ifname, Port oldPort) {
                    if (ifname == null) {
                        throw new ValueException("null key is not allowed.");
                    }

                    if (oldPort != null) {
                        String oldPortIfname = AbstractPort.Attr.IFNAME.get(oldPort);
                        if (ifname.equals(oldPortIfname)) {
                            AbstractPort.Attr.IFNAME.set(oldPort, null);
                        }
                    }
                }
            });
        }

        public static final Attribute.MapAttr<Long, Port, Node> IFINDEX_MAP = new Attribute.MapAttr<Long, Port, Node>(
            "naef.node.ifindex-map",
            new AttributeType.MvoMapType<Long, Port>(ValueResolver.LONG, new ValueResolver.Model<Port>(Port.class)));
        static {
            IFINDEX_MAP.addPostProcessor(new Attribute.MapAttr.PostProcessor<Long, Port, Node>() {

                @Override public void put(Node model, Long ifindex, Port oldPort, Port newPort) {
                    if (ifindex == null) {
                        throw new ValueException("null key is not allowed.");
                    }

                    if (oldPort != null) {
                        Long oldPortIfindex = AbstractPort.Attr.IFINDEX.get(oldPort);
                        if (ifindex.equals(oldPortIfindex)) {
                            AbstractPort.Attr.IFINDEX.set(oldPort, null);
                        }
                    }

                    if (newPort != null) {
                        Long newPortIfindex = AbstractPort.Attr.IFINDEX.get(newPort);
                        if (! ifindex.equals(newPortIfindex)) {
                            AbstractPort.Attr.IFINDEX.set(newPort, ifindex);
                        }
                    }
                }

                @Override public void remove(Node model, Long ifindex, Port oldPort) {
                    if (ifindex == null) {
                        throw new ValueException("null key is not allowed.");
                    }

                    if (oldPort != null) {
                        Long oldPortIfindex = AbstractPort.Attr.IFINDEX.get(oldPort);
                        if (ifindex.equals(oldPortIfindex)) {
                            AbstractPort.Attr.IFINDEX.set(oldPort, null);
                        }
                    }
                }
            });
        }

        public static final Attribute.SetAttr<NodeGroup, Node> NODE_GROUPS = new Attribute.SetAttr<NodeGroup, Node>(
            "naef.node.node-groups",
            new AttributeType.MvoSetType<NodeGroup>(NodeGroup.class) {

                @Override public NodeGroup parseElement(String str) {
                    return ValueResolver.<NodeGroup>resolve(NodeGroup.class, null, str);
                }
            });
        static {
            NODE_GROUPS.addPostProcessor(new Attribute.SetAttr.PostProcessor<NodeGroup, Node>() {

                @Override public void add(Node model, NodeGroup value) {
                    if (! NodeGroup.Attr.MEMBERS.containsValue(value, model)) {
                        NodeGroup.Attr.MEMBERS.addValue(value, model);
                    }
                }

                @Override public void remove(Node model, NodeGroup value) {
                    if (NodeGroup.Attr.MEMBERS.containsValue(value, model)) {
                        NodeGroup.Attr.MEMBERS.removeValue(value, model);
                    }
                }
            });
        }

        public static final Attribute.SingleBoolean<Node> VIRTUALIZED_HOSTING_ENABLED
            = new Attribute.SingleBoolean<Node>("naef.node.virtualized-hosting-enabled")
        {
            @Override public void validateValue(Node model, Boolean newValue) {
                super.validateValue(model, newValue);

                if (! Boolean.TRUE.equals(newValue)) {
                    if (0 < VIRTUALIZATION_GUEST_NODES.snapshot(model).size()) {
                        throw new ConfigurationException(
                            VIRTUALIZATION_GUEST_NODES.getName() + " に値が設定されているため変更できません.");
                    }
                }
            }
        };

        public static final Attribute.SetAttr<Node, Node> VIRTUALIZATION_GUEST_NODES
            = new Attribute.SetAttr<Node, Node>(
                "naef.node.virtualization-guest-nodes",
                new AttributeType.MvoSetType<Node>(Node.class) {

                    @Override public Node parseElement(String str) {
                        return ValueResolver.<Node>resolve(Node.class, null, str);
                    }
                })
        {
            @Override public void validateAddValue(Node model, Node value) {
                super.validateAddValue(model, value);

                if (! Boolean.TRUE.equals(VIRTUALIZED_HOSTING_ENABLED.get(model))) {
                    throw new ConfigurationException(
                        VIRTUALIZED_HOSTING_ENABLED.getName() + " の設定による制約のため値を追加することができません.");
                }
            }
        };
        static {
            VIRTUALIZATION_GUEST_NODES.addPostProcessor(new Attribute.SetAttr.PostProcessor<Node, Node>() {

                @Override public void add(Node model, Node value) {
                    if (! VIRTUALIZATION_HOST_NODES.containsValue(value, model)) {
                        VIRTUALIZATION_HOST_NODES.addValue(value, model);
                    }
                }

                @Override public void remove(Node model, Node value) {
                    if (VIRTUALIZATION_HOST_NODES.containsValue(value, model)) {
                        VIRTUALIZATION_HOST_NODES.removeValue(value, model);
                    }
                }
            });
        }

        public static final Attribute.SingleEnum<VirtualizationHostedType, Node> VIRTUALIZATION_HOSTED_TYPE
            = new Attribute.SingleEnum<VirtualizationHostedType, Node>(
                "naef.node.virtualization-hosted-type",
                VirtualizationHostedType.class)
        {
            @Override public VirtualizationHostedType get(Node model) {
                return escapeNull(super.get(model));
            }

            @Override public void validateValue(Node model, VirtualizationHostedType newValue) {
                super.validateValue(model, newValue);

                newValue = escapeNull(newValue);
                VirtualizationHostedType oldValue = escapeNull(get(model));

                if (newValue != oldValue
                    && 0 < VIRTUALIZATION_HOST_NODES.snapshot(model).size())
                {
                    throw new ConfigurationException(
                        VIRTUALIZATION_HOST_NODES.getName() + " に値が設定されているため変更できません.");
                }
            }

            private VirtualizationHostedType escapeNull(VirtualizationHostedType value) {
                return value == null ? VirtualizationHostedType.NONE : value;
            }
        };

        public static final Attribute.SetAttr<Node, Node> VIRTUALIZATION_HOST_NODES = new Attribute.SetAttr<Node, Node>(
            "naef.node.virtualization-host-nodes",
            new AttributeType.MvoSetType<Node>(Node.class) {

                @Override public Node parseElement(String str) {
                    return ValueResolver.<Node>resolve(Node.class, null, str);
                }
            })
        {
            @Override public void validateAddValue(Node model, Node value) {
                super.validateAddValue(model, value);

                VirtualizationHostedType type = VIRTUALIZATION_HOSTED_TYPE.get(model);
                if (type == VirtualizationHostedType.NONE
                    || (type == VirtualizationHostedType.SINGLE_HOST
                        && 0 < snapshot(model).size()))
                {
                    throw new ConfigurationException(
                        VIRTUALIZATION_HOSTED_TYPE.getName() + " の設定による制約のため値を追加することができません.");
                }
            }
        };
        static {
            VIRTUALIZATION_HOST_NODES.addPostProcessor(new Attribute.SetAttr.PostProcessor<Node, Node>() {

                @Override public void add(Node model, Node value) {
                    if (! VIRTUALIZATION_GUEST_NODES.containsValue(value, model)) {
                        VIRTUALIZATION_GUEST_NODES.addValue(value, model);
                    }
                }

                @Override public void remove(Node model, Node value) {
                    if (VIRTUALIZATION_GUEST_NODES.containsValue(value, model)) {
                        VIRTUALIZATION_GUEST_NODES.removeValue(value, model);
                    }
                }
            });
        }
    }

    public static final UniquelyNamedModelHome.Indexed<Node> home
        = new UniquelyNamedModelHome.Indexed<Node>(Node.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public Node(MvoId id) {
        super(id);
    }

    public Node(String name) throws ConstraintException {
        setName(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public void setName(String name) throws ConstraintException {
        try {
            name_.set(name);
        } catch (MvoHome.UniqueIndexDuplicatedKeyFoundException uidkfe) {
            throw new ConstraintException("名前の重複が検出されました: " + name);
        }
    }

    @Override public Node getNode() {
        return this;
    }

    @Override public void setOwner(NodeElement owner) {
        throw new UnsupportedOperationException();
    }

    @Override public NodeElement getOwner() {
        return null;
    }

    public String getSqnDelimiter() {
        String result = Attr.SQN_DELIMITER.get(this);
        return result == null 
            ? NaefTefService.instance().getSqnDelimiter()
            : result;
    }

    public String getFqnPrimaryDelimiter() {
        String result = Attr.FQN_PRIMARY_DELIMITER.get(this);
        return result == null 
            ? NaefTefService.instance().getFqnPrimaryDelimiter()
            : result;
    }

    public String getFqnSecondaryDelimiter() {
        String result = Attr.FQN_SECONDARY_DELIMITER.get(this);
        return result == null 
            ? NaefTefService.instance().getFqnSecondaryDelimiter()
            : result;
    }
}
