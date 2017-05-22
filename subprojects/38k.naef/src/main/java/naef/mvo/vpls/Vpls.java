package naef.mvo.vpls;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.HashSet;
import java.util.Set;

public class Vpls
    extends AbstractNetwork
    implements Network.Exclusive, Network.MemberPortConfigurable<VplsIf>
{
    public static class Attr {

        @Deprecated public static final Attribute.SingleAttr<VplsIdPool<?>, Vpls> ID_POOL
            = new Attribute.SingleAttr<VplsIdPool<?>, Vpls>("naef.vpls.id-pool", null);

        public static final IdPoolAttribute<VplsIdPool.IntegerType, Vpls> INTEGER_IDPOOL
            = new IdPoolAttribute<VplsIdPool.IntegerType, Vpls>(
                "naef.vpls.id-pool.integer-type",
                VplsIdPool.IntegerType.class);
        public static final Attribute<Integer, Vpls> INTEGER_ID
            = new IdAttribute<Integer, Vpls, VplsIdPool.IntegerType>(
                "naef.vpls.id.integer-type",
                AttributeType.INTEGER, INTEGER_IDPOOL);

        public static final IdPoolAttribute<VplsIdPool.StringType, Vpls> STRING_IDPOOL
            = new IdPoolAttribute<VplsIdPool.StringType, Vpls>(
                "naef.vpls.id-pool.string-type",
                VplsIdPool.StringType.class);
        public static final Attribute<String, Vpls> STRING_ID
            = new IdAttribute<String, Vpls, VplsIdPool.StringType>(
                "naef.vpls.id.string-type",
                AttributeType.STRING, STRING_IDPOOL);
    }

    private final S2<VplsIf> ports_ = new S2<VplsIf>();

    public Vpls(MvoId id) {
        super(id);
    }

    public Vpls() {
    }

    @Override public Set<VplsIf> getCurrentMemberPorts() {
        return new HashSet<VplsIf>(ports_.get());
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (VplsIf port : getCurrentMemberPorts()) {
            result.addAll(port.getCurrentCrossConnectedPorts());
        }
        return result;
    }

    @Override public void addMemberPort(VplsIf port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(VplsIf port) {
        super.removeMemberPort(ports_, port);
    }
}
