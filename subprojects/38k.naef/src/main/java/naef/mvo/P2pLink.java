package naef.mvo;

import java.util.HashSet;
import java.util.Set;

public abstract class P2pLink<T extends Port> extends AbstractNetwork {

    private final F0<T> port1_ = new F0<T>();
    private final F0<T> port2_ = new F0<T>();

    public P2pLink(MvoId id) {
        super(id);
    }

    public P2pLink(T port1, T port2) {
        if (port1 == null) {
            throw new IllegalArgumentException();
        }
        if (port1 == port2) {
            throw new IllegalArgumentException();
        }
        if (port1 == port2) {
            throw new IllegalArgumentException();
        }

        port1_.initialize(port1);
        port2_.initialize(port2);

        port1.joinNetwork(this);
        if (port2 != null) {
            port2.joinNetwork(this);
        }
    }

    @Override public Set<T> getCurrentMemberPorts() {
        Set<T> result = new HashSet<T>();
        result.add(getPort1());
        if (getPort2() != null) {
            result.add(getPort2());
        }
        return result;
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (T port : getCurrentMemberPorts()) {
            result.addAll(port.getCurrentCrossConnectedPorts());
        }
        return result;
    }

    public T getPort1() {
        return port1_.get();
    }

    public T getPort2() {
        return port2_.get();
    }

    public T getPeer(T port) {
        if (port == getPort1()) {
            return getPort2();
        }
        if (port == getPort2()) {
            return getPort1();
        }
        throw new IllegalArgumentException();
    }

    public static <T extends Port> T getPeer(Class<? extends P2pLink> linkClass, T port) {
        Set<? extends P2pLink> links = port.getCurrentNetworks(linkClass);
        if (links.size() > 1) {
            throw new IllegalStateException("multi-links found, " + port.getFqn());
        }

        P2pLink<T> link = links.size() == 0 ? null : (P2pLink<T>) links.iterator().next();

        return link == null ? null : link.getPeer(port);
    }
}
