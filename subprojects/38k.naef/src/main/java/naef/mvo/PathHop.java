package naef.mvo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class PathHop<S extends PathHop<S, T, U>, T extends Port, U extends Network.PathHopSeries<S, T, U>>
    extends AbstractNetwork
    implements Network.Containee, Network.UpperStackable
{
    private final F0<U> owner_ = new F0<U>();
    private final F0<S> previous_ = new F0<S>();
    private final F0<T> srcPort_ = new F0<T>();
    private final F0<T> dstPort_ = new F0<T>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();

    protected PathHop(MvoId id) {
        super(id);
    }

    protected PathHop(U owner, T srcPort, T dstPort) {
        if (srcPort == null || dstPort == null) {
            throw new IllegalArgumentException();
        }

        S previous = owner.getLastHop();
        if (previous != null && previous.getDstPort().getNode() != srcPort.getNode()) {
            throw new IllegalArgumentException("前ホップと不連続です.");
        }

        owner_.initialize(owner);
        previous_.initialize(previous);
        srcPort_.initialize(srcPort);
        dstPort_.initialize(dstPort);

        srcPort.joinNetwork(this);
        dstPort.joinNetwork(this);

        owner.setLastHop((S) this);
    }

    public void dispose() {
        if (getCurrentContainer().getLastHop() == this) {
            getCurrentContainer().setLastHop(null);
        }

        for (Network.LowerStackable lowerLayer : getHereafterLowerLayers(false)) {
            unstackOver(lowerLayer);
        }

        getSrcPort().disjoinNetwork(this);
        getDstPort().disjoinNetwork(this);
    }

    @Override public void setContainer(Container container) {
        throw new UnsupportedOperationException();
    }

    @Override public U getCurrentContainer() {
        return owner_.get();
    }

    public S getPrevious() {
        return previous_.get();
    }

    public T getSrcPort() {
        return srcPort_.get();
    }

    public T getDstPort() {
        return dstPort_.get();
    }

    @Override public Set<T> getCurrentMemberPorts() {
        Set<T> result = new HashSet<T>();
        result.add(getSrcPort());
        result.add(getDstPort());
        return result;
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public void stackOver(Network.LowerStackable lowerLayer) {
        super.stackOver(lowerLayers_, lowerLayer);
    }

    @Override public void unstackOver(Network.LowerStackable lowerLayer) {
        super.unstackOver(lowerLayers_, lowerLayer);
    }

    @Override public Set<? extends Network.LowerStackable> getHereafterLowerLayers(boolean recursive) {
        return super.getHereafterLowerLayers(lowerLayers_, recursive);
    }

    @Override public Set<? extends Network.LowerStackable> getCurrentLowerLayers(boolean recursive) {
        return super.getCurrentLowerLayers(lowerLayers_, recursive);
    }
}
