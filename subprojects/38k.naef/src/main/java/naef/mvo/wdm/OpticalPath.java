package naef.mvo.wdm;

import naef.mvo.AbstractNetwork;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OpticalPath 
    extends AbstractNetwork 
    implements Network.Containee, Network.PathHopSeries<OpticalPathHop, WdmPort, OpticalPath>
{
    public static class Attr {

        public static final Attribute.SingleModel<OpticalPathIdPool, OpticalPath> ID_POOL
            = new Attribute.SingleModel<OpticalPathIdPool, OpticalPath>(
                "naef.optical-path.id-pool",
                OpticalPathIdPool.class);
    }

    private final F2<Network.Container> owner_ = new F2<Network.Container>();
    private final F2<OpticalPathHop> lastHop_ = new F2<OpticalPathHop>();

    public OpticalPath(MvoId id) {
        super(id);
    }

    public OpticalPath() {
    }

    @Override public Set<WdmPort> getCurrentMemberPorts() {
        Set<WdmPort> result = new HashSet<WdmPort>();
        for (OpticalPathHop hop : getCurrentParts(false)) {
            result.addAll(hop.getCurrentMemberPorts());
        }
        return result;
    }

    @Override public Collection<Port> getCurrentAttachedPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public void setContainer(Network.Container container) {
        owner_.set(container);
    }

    @Override public Network.Container getCurrentContainer() {
        return owner_.get();
    }

    @Override public void addPart(Network.Containee part) {
        throw new UnsupportedOperationException();
    }

    @Override public void removePart(Network.Containee part) {
        throw new UnsupportedOperationException();
    }

    @Override public Set<OpticalPathHop> getHereafterParts(boolean recursive) {
        throw new RuntimeException("未実装");
    }

    @Override public Set<OpticalPathHop> getCurrentParts(boolean recursive) {
        return new HashSet<OpticalPathHop>(NaefMvoUtils.getHops(getLastHop()));
    }

    @Override public void setLastHop(OpticalPathHop hop) {
        lastHop_.set(hop);
    }

    @Override public OpticalPathHop getLastHop() {
        return lastHop_.get();
    }
}
