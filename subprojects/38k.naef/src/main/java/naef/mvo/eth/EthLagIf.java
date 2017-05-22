package naef.mvo.eth;

import naef.mvo.AbstractPort;
import naef.mvo.Network;
import naef.mvo.Node;
import naef.mvo.Port;

import java.util.HashSet;
import java.util.Set;

public class EthLagIf extends AbstractPort {

    private final S2<Port> parts_ = new S2<Port>(); 

    public EthLagIf(MvoId id) {
        super(id);
    }

    public EthLagIf() {
    }

    @Override protected S2<Port> getPartsField() {
        return parts_;
    }

    public Set<EthPort> getCurrentBundlePorts() {
        Set<EthPort> result = new HashSet<EthPort>();

        for (Port part : getParts()) {
            if (part instanceof EthPort) {
                result.add((EthPort) part);
            }
        }

        Node node = getNode();
        for (EthLag link : getCurrentNetworks(EthLag.class)) {
            for (Network part : link.getCurrentParts(false)) {
                if (part instanceof EthLink) {
                    for (Port port : part.getCurrentMemberPorts()) {
                        if (port.getNode() == node && port instanceof EthPort) {
                            result.add((EthPort) port);
                        }
                    }
                }
            }
        }

        return result;
    }
}
