package naef.mvo.vxlan;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.HashSet;
import java.util.Set;

public class Vxlan
    extends AbstractNetwork
    implements Network.Exclusive, Network.MemberPortConfigurable<VtepIf>
{
    public static final IdPoolAttribute<VxlanIdPool, Vxlan> ID_POOL
        = new IdPoolAttribute<VxlanIdPool, Vxlan>("naef.vxlan.id-pool", VxlanIdPool.class);
    public static final Attribute<Long, Vxlan> ID
        = new IdAttribute<Long, Vxlan, VxlanIdPool>("naef.vxlan.id",AttributeType.LONG, ID_POOL);

    private final S2<VtepIf> ports_ = new S2<VtepIf>();

    public Vxlan(MvoId id) {
        super(id);
    }

    public Vxlan() {
    }

    @Override public Set<VtepIf> getCurrentMemberPorts() {
        return new HashSet<VtepIf>(ports_.get());
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        final Set<Port> result = new HashSet<Port>();
        for (VtepIf port : getCurrentMemberPorts()) {
            result.addAll(port.getCurrentCrossConnectedPorts());
        }
        return result;
    }

    @Override public void addMemberPort(VtepIf port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(VtepIf port) {
        super.removeMemberPort(ports_, port);
    }
}
