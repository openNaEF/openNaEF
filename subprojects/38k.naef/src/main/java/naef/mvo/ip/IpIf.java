package naef.mvo.ip;

import naef.mvo.AbstractPort;
import naef.mvo.Node;
import naef.mvo.NodeType;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;
import tef.skelton.ValueResolver;

import java.util.Set;

public class IpIf extends AbstractPort {

    /**
     * @deprecated {@link Attr#BOUND_PORTS} に置き換えられました.
     */
    @Deprecated public static final Attribute.SetAttr<Port, IpIf> ASSOCIATED_PORTS = new Attribute.SetAttr<Port, IpIf>(
        "naef.ip.ip-if.associated-ports",
        new AttributeType.MvoSetType<Port>(Port.class) {

            @Override public Port parseElement(String str) {
                return ValueResolver.<Port>resolve(Port.class, null, str);
            }
        });

    public static class Attr {

        public static final IdPoolAttribute<IpSubnetAddress, IpIf> IP_SUBNET_ADDRESS
            = new IdPoolAttribute<IpSubnetAddress, IpIf>("naef.subnet-address", IpSubnetAddress.class);

        public static final Attribute<IpAddress, IpIf> IP_ADDRESS
            = new IdAttribute<IpAddress, IpIf, IpSubnetAddress>("naef.ip-address", IpAddress.TYPE, IP_SUBNET_ADDRESS);

        public static final Attribute.SingleInteger<IpIf> SUBNET_MASK_LENGTH
            = new Attribute.SingleInteger<IpIf>("naef.subnet-mask-length");

        /**
         * ip-if に関連付けられた port を priority 値付きで保持します.
         * <p>
         * {@link naef.mvo.Node.Attr#OBJECT_TYPE node の node-type} に 
         * {@link naef.mvo.NodeType.Attr#PORT_IPIF_PRIORITY_PATTERN 優先度パターン} 
         * が設定されていた場合, priority 値の設定可否はその制約に従います.
         * <p>
         * {@link naef.mvo.AbstractPort.Attr#BOUND_IPIFS} は逆参照で, 値の変更は連動します.
         * <p>
         * <dl>
         *  <dt>attribute name: <dd>"naef.ip-if.bound-ports"
         *  <dt>dto transcript: <dd>{@link naef.dto.ip.IpIfDto.ExtAttr#BOUND_PORTS}
         * </dl>
         *
         * @see naef.mvo.AbstractPort.Attr#BOUND_IPIFS
         * @see naef.mvo.NodeType.PortIpifPriorityPattern
         * @see naef.mvo.NodeType.Attr#PORT_IPIF_PRIORITY_PATTERN
         * @see naef.dto.ip.IpIfDto.ExtAttr#BOUND_PORTS
         */
        public static final Attribute.MapAttr<Port, Integer, IpIf> BOUND_PORTS
            = new Attribute.MapAttr<Port, Integer, IpIf>(
                "naef.ip-if.bound-ports",
                new AttributeType.MvoMapType<Port, Integer>(
                    new ValueResolver.Model<Port>(Port.class),
                    ValueResolver.INTEGER))
        {
            @Override protected void validatePut(IpIf model, Port key, Integer value) {
                super.validatePut(model, key, value);

                NodeType nodetype = Node.Attr.OBJECT_TYPE.get(model.getNode());
                if (nodetype == null) {
                    return;
                }

                NodeType.PortIpifPriorityPattern pattern = NodeType.Attr.PORT_IPIF_PRIORITY_PATTERN.get(nodetype);
                if (pattern == null) {
                    return;
                }

                pattern.validate(key, model, value);
            }
        };
        static {
            BOUND_PORTS.addPostProcessor(new Attribute.MapAttr.PostProcessor<Port, Integer, IpIf>() {

                @Override public void put(IpIf model, Port key, Integer oldValue, Integer newValue) {
                    if (! equals(newValue, AbstractPort.Attr.BOUND_IPIFS.get(key, model))) {
                        AbstractPort.Attr.BOUND_IPIFS.put(key, model, newValue);
                    }
                }

                @Override public void remove(IpIf model, Port key, Integer oldValue) {
                    if (AbstractPort.Attr.BOUND_IPIFS.containsKey(key, model)) {
                        AbstractPort.Attr.BOUND_IPIFS.remove(key, model);
                    }
                }

                private boolean equals(Object o1, Object o2) {
                    return o1 == null ? o2 == null : o1.equals(o2);
                }
            });
        }
    }

    public IpIf(MvoId id) {
        super(id);
    }

    public IpIf() {
    }

    public IpSubnet getSubnet() {
        Set<IpSubnet> subnets = getCurrentNetworks(IpSubnet.class);
        if (subnets.size() > 1) {
            throw new IllegalStateException("多重度異常: " + getFqn());
        }
        return subnets.size() == 0 ? null : subnets.iterator().next();
    }
}
