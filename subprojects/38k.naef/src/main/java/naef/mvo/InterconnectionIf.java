package naef.mvo;

import tef.skelton.ConfigurationException;
import tef.skelton.ValueException;

import java.util.HashSet;
import java.util.Set;

public class InterconnectionIf extends AbstractPort {

    public InterconnectionIf(MvoId id) {
        super(id);
    }

    public InterconnectionIf() {
    }

    @Override public void joinNetwork(Network network) {
        if (! (network instanceof CrossConnection)) {
            throw new ValueException("cross connection のみが接続可能です.");
        }
        if (getHereafterNetworks(CrossConnection.class).size() > 1) {
            throw new ConfigurationException("接続可能数を超えています.");
        }

        super.joinNetwork(network);
    }

    public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        for (CrossConnection xcon : getCurrentNetworks(CrossConnection.class)) {
            result.add(xcon.getOpposite(this));
        }
        return result;
    }
}
