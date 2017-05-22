package naef.mvo.vrf;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.HashSet;
import java.util.Set;

public class Vrf
    extends AbstractNetwork
    implements Network.Exclusive, Network.MemberPortConfigurable<VrfIf>
{
    public static class Attr {

        @Deprecated public static final Attribute.SingleAttr<VrfIdPool<?>, Vrf> ID_POOL
            = new Attribute.SingleAttr<VrfIdPool<?>, Vrf>("naef.vrf.id-pool", null);

        public static final IdPoolAttribute<VrfIdPool.IntegerType, Vrf> INTEGER_IDPOOL
            = new IdPoolAttribute<VrfIdPool.IntegerType, Vrf>(
                "naef.vrf.id-pool.integer-type",
                VrfIdPool.IntegerType.class);
        public static final Attribute<Integer, Vrf> INTEGER_ID
            = new IdAttribute<Integer, Vrf, VrfIdPool.IntegerType>(
                "naef.vrf.id.integer-type",
                AttributeType.INTEGER, INTEGER_IDPOOL);

        public static final IdPoolAttribute<VrfIdPool.StringType, Vrf> STRING_IDPOOL
            = new IdPoolAttribute<VrfIdPool.StringType, Vrf>(
                "naef.vrf.id-pool.string-type",
                VrfIdPool.StringType.class);
        public static final Attribute<String, Vrf> STRING_ID
            = new IdAttribute<String, Vrf, VrfIdPool.StringType>(
                "naef.vrf.id.string-type",
                AttributeType.STRING, STRING_IDPOOL);
    }

    private final S2<VrfIf> ports_ = new S2<VrfIf>();

    public Vrf(MvoId id) {
        super(id);
    }

    public Vrf() {
    }

    @Override public Set<VrfIf> getCurrentMemberPorts() {
        return new HashSet<VrfIf>(ports_.get());
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (VrfIf port : getCurrentMemberPorts()) {
            result.addAll(port.getCurrentCrossConnectedPorts());
        }
        return result;
    }

    @Override public void addMemberPort(VrfIf port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(VrfIf port) {
        super.removeMemberPort(ports_, port);
    }
}
