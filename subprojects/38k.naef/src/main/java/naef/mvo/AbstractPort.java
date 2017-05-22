package naef.mvo;

import naef.mvo.ip.IpIf;
import tef.MVO;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.ConstraintException;
import tef.skelton.SkeltonTefService;
import tef.skelton.ValueException;
import tef.skelton.ValueResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractPort extends AbstractNodeElement implements Port {

    public static class Attr {

        public static final Attribute.SingleModel<PortType, Port> OBJECT_TYPE
            = new Attribute.SingleModel<PortType, Port>(
                AbstractNodeElement.Attr.OBJECT_TYPE_FIELD_NAME, 
                PortType.class)
        {
            @Override public void validateValue(Port model, PortType newValue) {
                super.validateValue(model, newValue);

                AbstractNodeElement.Attr.OBJECT_TYPE.validateValue(model, newValue);

                if (newValue != null) {
                    PortType.Attr.ACCEPTABLE_NETWORK_TYPES.validateConstraint(model, newValue);
                    PortType.Attr.ACCEPTABLE_LOWER_TYPES.validateConstraint(model, newValue);
                    PortType.Attr.ACCEPTABLE_UPPER_TYPES.validateConstraint(model, newValue);
                    PortType.Attr.ACCEPTABLE_CONTAINER_TYPES.validateConstraint(model, newValue);
                    PortType.Attr.ACCEPTABLE_PART_TYPES.validateConstraint(model, newValue);
                    PortType.Attr.ACCEPTABLE_XCONNECT_TYPES.validateConstraint(model, newValue);
                }
            }
        };

        public static final Attribute.SingleString<Port> IFNAME = new Attribute.SingleString<Port>("naef.port.ifname");
        static {
            IFNAME.addPostProcessor(new Attribute.SingleAttr.PostProcessor<String, Port>() {

                @Override public void set(Port port, String oldIfname, String newIfname) {
                    Node node = port.getNode();

                    if (oldIfname != null) {
                        Port mapping = Node.Attr.IFNAME_MAP.get(node, oldIfname);
                        if (mapping == port) {
                            Node.Attr.IFNAME_MAP.remove(node, oldIfname);
                        } else if (mapping != null) {
                            throw new IllegalStateException(((MVO) mapping).getMvoId().getLocalStringExpression());
                        }
                    }

                    if (newIfname != null) {
                        Port mapping = Node.Attr.IFNAME_MAP.get(node, newIfname);
                        if (mapping == null) {
                            Node.Attr.IFNAME_MAP.put(node, newIfname, port);
                        } else if (mapping != port) {
                            throw new ValueException(newIfname + " は割当済です.");
                        }
                    }
                }
            });
        }

        public static final Attribute.SingleLong<Port> IFINDEX = new Attribute.SingleLong<Port>("naef.port.ifindex");
        static {
            IFINDEX.addPostProcessor(new Attribute.SingleAttr.PostProcessor<Long, Port>() {

                @Override public void set(Port port, Long oldIfindex, Long newIfindex) {
                    Node node = port.getNode();

                    if (oldIfindex != null) {
                        Port mapping = Node.Attr.IFINDEX_MAP.get(node, oldIfindex);
                        if (mapping == port) {
                            Node.Attr.IFINDEX_MAP.remove(node, oldIfindex);
                        } else if (mapping != null) {
                            throw new IllegalStateException(((MVO) mapping).getMvoId().getLocalStringExpression());
                        }
                    }

                    if (newIfindex != null) {
                        Port mapping = Node.Attr.IFINDEX_MAP.get(node, newIfindex);
                        if (mapping == null) {
                            Node.Attr.IFINDEX_MAP.put(node, newIfindex, port);
                        } else if (mapping != port) {
                            throw new ValueException(newIfindex + " は割当済です.");
                        }
                    }
                }
            });
        }

        public static final Attribute.SingleString<Port> BGP_SITENAME
            = new Attribute.SingleString<Port>("naef.port.bgp.site-name");

        /**
         * @deprecated {@link #BOUND_IPIFS} に置き換えられました.
         */
        @Deprecated public static final Attribute.SingleModel<IpIf, Port> PRIMARY_IPIF
            = new Attribute.SingleModel<IpIf, Port>("naef.port.primary-ipif", IpIf.class);
        static {
            PRIMARY_IPIF.addPostProcessor(new Attribute.SingleAttr.PostProcessor<IpIf, Port>() {

                @Override public void set(Port model, IpIf oldValue, IpIf newValue) {
                    if (oldValue == newValue) {
                        return;
                    }

                    if (oldValue != null) {
                        IpIf.ASSOCIATED_PORTS.removeValue(oldValue, model);
                    }

                    if (newValue != null) {
                        IpIf.ASSOCIATED_PORTS.addValue(newValue, model);
                    }
                }
            });
        }

        /**
         * @deprecated {@link #BOUND_IPIFS} に置き換えられました.
         */
        @Deprecated public static final Attribute.SingleModel<IpIf, Port> SECONDARY_IPIF
            = new Attribute.SingleModel<IpIf, Port>("naef.port.secondary-ipif", IpIf.class);
        static {
            SECONDARY_IPIF.addPostProcessor(new Attribute.SingleAttr.PostProcessor<IpIf, Port>() {

                @Override public void set(Port model, IpIf oldValue, IpIf newValue) {
                    if (oldValue == newValue) {
                        return;
                    }

                    if (oldValue != null) {
                        IpIf.ASSOCIATED_PORTS.removeValue(oldValue, model);
                    }

                    if (newValue != null) {
                        IpIf.ASSOCIATED_PORTS.addValue(newValue, model);
                    }
                }
            });
        }

        /**
         * port に関連付けられた ip-if を priority 値付きで保持します. 
         * <p>
         * {@link naef.mvo.Node.Attr#OBJECT_TYPE node の node-type} に 
         * {@link naef.mvo.NodeType.Attr#PORT_IPIF_PRIORITY_PATTERN 優先度パターン} 
         * が設定されていた場合, priority 値の設定可否はその制約に従います.
         * <p>
         * {@link naef.mvo.ip.IpIf.Attr#BOUND_PORTS} は逆参照で, 値の変更は連動します.
         * <p>
         * <dl>
         *  <dt>attribute name: <dd>"naef.port.bound-ipifs"
         *  <dt>dto transcript: <dd>{@link naef.dto.PortDto.ExtAttr#BOUND_IPIFS}
         * </dl>
         *
         * @see naef.mvo.ip.IpIf.Attr#BOUND_PORTS
         * @see naef.mvo.NodeType.PortIpifPriorityPattern
         * @see naef.mvo.NodeType.Attr#PORT_IPIF_PRIORITY_PATTERN
         * @see naef.dto.PortDto.ExtAttr#BOUND_IPIFS
         */
        public static final Attribute.MapAttr<IpIf, Integer, Port> BOUND_IPIFS
            = new Attribute.MapAttr<IpIf, Integer, Port>(
                "naef.port.bound-ipifs",
                new AttributeType.MvoMapType<IpIf, Integer>(
                    new ValueResolver.Model<IpIf>(IpIf.class),
                    ValueResolver.INTEGER))
        {
            @Override protected void validatePut(Port model, IpIf key, Integer value) {
                super.validatePut(model, key, value);

                NodeType nodetype = Node.Attr.OBJECT_TYPE.get(model.getNode());
                if (nodetype == null) {
                    return;
                }

                NodeType.PortIpifPriorityPattern pattern = NodeType.Attr.PORT_IPIF_PRIORITY_PATTERN.get(nodetype);
                if (pattern == null) {
                    return;
                }

                pattern.validate(model, key, value);
            }
        };
        static {
            BOUND_IPIFS.addPostProcessor(new Attribute.MapAttr.PostProcessor<IpIf, Integer, Port>() {

                @Override public void put(Port model, IpIf key, Integer oldValue, Integer newValue) {
                    if (! equals(newValue, IpIf.Attr.BOUND_PORTS.get(key, model))) {
                        IpIf.Attr.BOUND_PORTS.put(key, model, newValue);
                    }
                }

                @Override public void remove(Port model, IpIf key, Integer oldValue) {
                    if (IpIf.Attr.BOUND_PORTS.containsKey(key, model)) {
                        IpIf.Attr.BOUND_PORTS.remove(key, model);
                    }
                }

                private boolean equals(Object o1, Object o2) {
                    return o1 == null ? o2 == null : o1.equals(o2);
                }
            });
        }

        public static final Attribute.MapAttr<Port, CustomerInfo, Port> PORT_CUSTOMERINFOS
            = new Attribute.MapAttr<Port, CustomerInfo, Port>(
                "naef.port.port-customerinfos",
                new AttributeType.MvoMapType<Port, CustomerInfo>(
                    new ValueResolver.Model<Port>(Port.class),
                    new ValueResolver.Model<CustomerInfo>(CustomerInfo.class)));

        public static final Attribute.SingleEnum<PortMode, Port> PORT_MODE
            = new Attribute.SingleEnum<PortMode, Port>("naef.port-mode", PortMode.class);

        public static final Attribute.SingleBoolean<Port>ALIAS_SOURCEABLE
            = new Attribute.SingleBoolean<Port>("naef.port.alias-sourceable")
        {
            @Override public void validateValue(Port model, Boolean newValue) {
                super.validateValue(model, newValue);

                if (0 < ALIASES.snapshot(model).size()
                    && (newValue == null || newValue.equals(Boolean.FALSE)))
                {
                    throw new ConfigurationException(ALIASES.getName() + " に値が設定されているため変更できません.");
                }
            }
        };

        public static final Attribute.SetAttr<Port, Port> ALIASES = new Attribute.SetAttr<Port, Port>(
            "naef.port.aliases",
            new AttributeType.MvoSetType<Port>(Port.class) {

                @Override public Port parseElement(String str) {
                    return ValueResolver.<Port>resolve(Port.class, null, str);
                }
            })
        {
            @Override public void validateAddValue(Port model, Port value) {
                super.validateAddValue(model, value);

                if (! Node.Attr.VIRTUALIZATION_GUEST_NODES.containsValue(model.getNode(), value.getNode())) {
                    throw new ConfigurationException(
                        Node.Attr.VIRTUALIZATION_GUEST_NODES.getName() + " に登録がない node の port です.");
                }
                if (! Boolean.TRUE.equals(ALIAS_SOURCEABLE.get(model))) {
                    throw new ConfigurationException(
                        model.getFqn() + " の " + ALIAS_SOURCEABLE.getName() + " を true に設定してください.");
                }
                if (ALIAS_SOURCE.get(value) != null
                    && ALIAS_SOURCE.get(value) != model)
                {
                    throw new ValueException(value.getFqn() + " は他の port の alias に設定されています.");
                }
            }
        };
        static {
            ALIASES.addPostProcessor(new Attribute.SetAttr.PostProcessor<Port, Port>() {

                @Override public void add(Port model, Port value) {
                    if (ALIAS_SOURCE.get(value) != model) {
                        ALIAS_SOURCE.set(value, model);
                    }
                }

                @Override public void remove(Port model, Port value) {
                    if (ALIAS_SOURCE.get(value) == model) {
                        ALIAS_SOURCE.set(value, null);
                    }
                }
            });
        }

        public static final Attribute.SingleModel<Port, Port> ALIAS_SOURCE
            = new Attribute.SingleModel<Port, Port>("naef.port.alias-source", Port.class)
        {
            @Override public void validateValue(Port model, Port newValue) {
                super.validateValue(model, newValue);

                if (newValue != null
                    && ! Node.Attr.VIRTUALIZATION_HOST_NODES.containsValue(model.getNode(), newValue.getNode()))
                {
                    throw new ConfigurationException(
                        Node.Attr.VIRTUALIZATION_HOST_NODES.getName() + " に登録がない node の port です.");
                }
            }
        };
        static {
            ALIAS_SOURCE.addPostProcessor(new Attribute.SingleAttr.PostProcessor<Port, Port>() {

                @Override public void set(Port model, Port oldValue, Port newValue) {
                    if (oldValue != null && ALIASES.containsValue(oldValue, model)) {
                        ALIASES.removeValue(oldValue, model);
                    }
                    if (newValue != null && ! ALIASES.containsValue(newValue, model)) {
                        ALIASES.addValue(newValue, model);
                    }
                }
            });
        }
    }

    private final F2<NodeElement> owner_ = new F2<NodeElement>();
    private final F1<String> name_ = new F1<String>();
    private final S2<Network> networks_ = new S2<Network>();
    private final S2<Port> lowerLayerPorts_ = new S2<Port>();
    private final S2<Port> upperLayerPorts_ = new S2<Port>();

    protected AbstractPort(MvoId id) {
        super(id);
    }

    protected AbstractPort() {
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

        if (0 < getUpperLayerPorts().size()) {
            throw new ConstraintException("upper layer port を unstack してください.");
        }
        if (0 < getLowerLayerPorts().size()) {
            throw new ConstraintException("lower layer port を unstack してください.");
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

    @Override public void joinNetwork(Network network) {
        if (network instanceof CrossConnection) {
            PortType portType = AbstractPort.Attr.OBJECT_TYPE.get(this);
            if (portType != null
                && PortType.Attr.ACCEPTABLE_XCONNECT_TYPES.snapshot(portType).size() == 0)
            {
                throw new ValueException(
                    AbstractPort.Attr.OBJECT_TYPE.getName() + " の型制約に "
                    + PortType.Attr.ACCEPTABLE_XCONNECT_TYPES.getName() + " が定義されていません.");
            }
        } else {
            PortType.Attr.ACCEPTABLE_NETWORK_TYPES.validateValue(this, network);
        }

        if (network instanceof Network.Exclusive) {
            Set<? extends Network> hereafters = getHereafterNetworks(network.getClass());
            hereafters.remove(network);
            if (hereafters.size() > 0) {
                throw new ConfigurationException(
                    getFqn() + " には既に他の " + SkeltonTefService.instance().uiTypeNames().getName(network.getClass())
                    + " が接続されています.");
            }
        }

        if (networks_.contains(network)
            && networks_.getFutureChanges(network).size() == 0)
        {
            return;
        }

        networks_.add(network);
        if (network instanceof Network.MemberPortConfigurable) {
            ((Network.MemberPortConfigurable) network).addMemberPort(this);
        }
    }

    @Override public void disjoinNetwork(Network network) {
        if (! networks_.contains(network)
            && networks_.getFutureChanges(network).size() == 0)
        {
            return;
        }

        networks_.remove(network);
        if (network instanceof Network.MemberPortConfigurable) {
            ((Network.MemberPortConfigurable) network).removeMemberPort(this);
        }
    }

    @Override public <T extends Network> Set<T> getCurrentNetworks(Class<T> networkClass) {
        return selectNetworks(networks_.get(), networkClass);
    }

    @Override public <T extends Network> Set<T> getHereafterNetworks(Class<T> networkClass) {
        return selectNetworks(networks_.getAllHereafter(), networkClass);
    }

    @Override public Collection<? extends Port> getCurrentCrossConnectedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (CrossConnection xcon : getCurrentNetworks(CrossConnection.class)) {
            if (xcon.getPort1() == this) {
                result.add(xcon.getPort2());
            } else if (xcon.getPort2() == this) {
                result.add(xcon.getPort1());
            } else {
                throw new RuntimeException(xcon.getMvoId().getLocalStringExpression());
            }
        }
        return result;
    }

    @Override public void addLowerLayerPort(Port port) {
        if (port.getNode() != getNode()) {
            throw new IllegalArgumentException();
        }

        PortType.Attr.ACCEPTABLE_LOWER_TYPES.validateValue(this, port);

        lowerLayerPorts_.add(port);
    }

    @Override public void removeLowerLayerPort(Port port) {
        lowerLayerPorts_.remove(port);
    }

    @Override public Collection<? extends Port> getLowerLayerPorts() {
        return lowerLayerPorts_.get();
    }

    @Override public void addUpperLayerPort(Port port) {
        if (port.getNode() != getNode()) {
            throw new IllegalArgumentException();
        }

        PortType.Attr.ACCEPTABLE_UPPER_TYPES.validateValue(this, port);

        upperLayerPorts_.add(port);
    }

    @Override public void removeUpperLayerPort(Port port) {
        upperLayerPorts_.remove(port);
    }

    @Override public Collection<? extends Port> getUpperLayerPorts() {
        return upperLayerPorts_.get();
    }

    private <T extends Network> Set<T> selectNetworks(Collection<Network> networks, Class<T> networkClass) {
        Set<T> result = new HashSet<T>();
        for (Network network : networks) {
            if (networkClass.isInstance(network)) {
                result.add(networkClass.cast(network));
            }
        }
        return result;
    }

    @Override public void addPart(Port part) {
        S2<Port> partsField = getPartsField();
        if (partsField == null) {
            throw new UnsupportedOperationException(
                "包含部分要素を設定できません: " + SkeltonTefService.instance().uiTypeNames().getName(getClass()));
        }

        PortType.Attr.ACCEPTABLE_PART_TYPES.validateValue(this, part);

        partsField.add(part);
    }

    @Override public void removePart(Port part) {
        getPartsField().remove(part);
    }

    @Override public Collection<? extends Port> getParts() {
        return getPartsField() == null
            ? Collections.<Port>emptyList()
            : getPartsField().get();
    }

    protected S2<Port> getPartsField() {
        return null;
    }

    @Override public void setContainer(Port container) {
        F2<Port> containerField = getContainerField();
        if (containerField == null) {
            throw new UnsupportedOperationException(
                "包含全体要素を設定できません: " + SkeltonTefService.instance().uiTypeNames().getName(getClass()));
        }

        PortType.Attr.ACCEPTABLE_CONTAINER_TYPES.validateValue(this, container);

        containerField.set(container);
    }

    @Override public void resetContainer() {
        getContainerField().set(null);
    }

    @Override public Port getContainer() {
        return getContainerField() == null
            ? null
            : getContainerField().get();
    }

    protected F2<Port> getContainerField() {
        return null;
    }
}
