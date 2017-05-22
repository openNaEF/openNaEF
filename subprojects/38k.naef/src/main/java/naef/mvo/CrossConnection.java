package naef.mvo;

import tef.MVO;
import tef.skelton.Attribute;
import tef.skelton.Model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class CrossConnection extends MVO implements Network {

    private final F0<Port> port1_ = new F0<Port>();
    private final F0<Port> port2_ = new F0<Port>();

    public CrossConnection(MvoId id) {
        super(id);
    }

    public CrossConnection(Port port1, Port port2) {
        if (port1 == null || port2 == null) {
            throw new IllegalArgumentException();
        }
        if (port1.getNode() != port2.getNode()) {
            throw new IllegalArgumentException();
        }
        if (port1 == port2) {
            throw new IllegalArgumentException();
        }
        for (CrossConnection xcon : port1.getHereafterNetworks(CrossConnection.class)) {
            if (xcon.getPort1() == port2 || xcon.getPort2() == port2) {
                throw new IllegalStateException("接続済です: " + port1.getFqn() + ", " + port2.getFqn());
            }
        }
        for (CrossConnection xcon : port2.getHereafterNetworks(CrossConnection.class)) {
            if (xcon.getPort1() == port1 || xcon.getPort2() == port1) {
                throw new IllegalStateException("接続済です: " + port1.getFqn() + ", " + port2.getFqn());
            }
        }

        PortType.Attr.ACCEPTABLE_XCONNECT_TYPES.validateValue(port1, port2);
        PortType.Attr.ACCEPTABLE_XCONNECT_TYPES.validateValue(port2, port1);

        port1_.initialize(port1);
        port2_.initialize(port2);

        port1.joinNetwork(this);
        port2.joinNetwork(this);
    }

    @Override public SortedSet<String> getAttributeNames() {
        return new TreeSet<String>();
    }

    @Override public <S, T extends Model> void set(Attribute<S, ? super T> attr, S value) {
        throw new UnsupportedOperationException();
    }

    @Override public <S, T extends Model> S get(Attribute<S, ? super T> attr) {
        throw new UnsupportedOperationException();
    }

    @Override public void putValue(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override public Object getValue(String key) {
        return null;
    }

    @Override public Set<Port> getCurrentMemberPorts() {
        Set<Port> result = new HashSet<Port>();
        result.add(getPort1());
        result.add(getPort2());
        return result;
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        return Collections.<Port>emptySet();
    }

    public Port getPort1() {
        return port1_.get();
    }

    public Port getPort2() {
        return port2_.get();
    }

    public void disconnect() {
        getPort1().disjoinNetwork(this);
        getPort2().disjoinNetwork(this);
    }

    public Port getOpposite(Port port) {
        if (port == getPort1()) {
            return getPort2();
        }
        if (port == getPort2()) {
            return getPort1();
        }
        throw new IllegalArgumentException("not a member port.");
    }
}
