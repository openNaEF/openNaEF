package naef.mvo.vlan;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;
import tef.skelton.ValueException;

import java.util.HashSet;
import java.util.Set;

public class Vlan 
    extends AbstractNetwork
    implements Network.MemberPortConfigurable<VlanIf>, Network.Container, Network.LowerStackable
{
    public static class Attr {

        public static final IdPoolAttribute<VlanIdPool.Dot1q, Vlan> ID_POOL
            = new IdPoolAttribute<VlanIdPool.Dot1q, Vlan>("naef.vlan.id-pool", VlanIdPool.Dot1q.class);
        public static final Attribute<Integer, Vlan> ID
            = new IdAttribute<Integer, Vlan, VlanIdPool.Dot1q>("naef.vlan.id", AttributeType.INTEGER, ID_POOL);
    }

    private final S2<VlanIf> ports_ = new S2<VlanIf>(); 
    private final S2<VlanSegment> parts_ = new S2<VlanSegment>();
    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();

    public Vlan(MvoId id) {
        super(id);
    }

    public Vlan() {
    }

    @Override public Set<VlanIf> getCurrentMemberPorts() {
        Set<VlanIf> result = new HashSet<VlanIf>();
        result.addAll(ports_.get());
        for (VlanSegment part : getCurrentParts(false)) {
            result.addAll(part.getCurrentMemberPorts());
        }
        return result;
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (VlanIf port : getCurrentMemberPorts()) {
            result.addAll(port.getCurrentUntaggedPorts());
        }
        return result;
    }

    @Override public void addMemberPort(VlanIf port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(VlanIf port) {
        super.removeMemberPort(ports_, port);
    }

    @Override public void addPart(Network.Containee part) {
        if (! (part instanceof VlanSegment)) {
            throw new ValueException("containee type error.");
        }

        super.addPart(parts_, (VlanSegment) part);
    }

    @Override public void removePart(Network.Containee part) {
        if (! (part instanceof VlanSegment)) {
            throw new ValueException("containee type error.");
        }

        super.removePart(parts_, (VlanSegment) part);
    }

    @Override public Set<VlanSegment> getHereafterParts(boolean recursive) {
        return super.getHereafterParts(parts_, recursive);
    }

    @Override public Set<VlanSegment> getCurrentParts(boolean recursive) {
        return super.getCurrentParts(parts_, recursive);
    }

    @Override public void stackUnder(Network.UpperStackable upperLayer) {
        super.stackUnder(upperLayers_, upperLayer);
    }

    @Override public void unstackUnder(Network.UpperStackable upperLayer) {
        super.unstackUnder(upperLayers_, upperLayer);
    }

    @Override public Set<? extends Network.UpperStackable> getHereafterUpperLayers(boolean recursive) {
        return super.getHereafterUpperLayers(upperLayers_, recursive);
    }

    @Override public Set<? extends Network.UpperStackable> getCurrentUpperLayers(boolean recursive) {
        return super.getCurrentUpperLayers(upperLayers_, recursive);
    }
}
