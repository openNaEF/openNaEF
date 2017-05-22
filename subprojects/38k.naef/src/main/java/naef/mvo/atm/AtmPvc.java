package naef.mvo.atm;

import naef.mvo.Network;
import naef.mvo.P2pLink;

public class AtmPvc extends P2pLink<AtmPvcIf> implements Network.Exclusive {

    public AtmPvc(MvoId id) {
        super(id);
    }

    public AtmPvc(AtmPvcIf port1, AtmPvcIf port2) {
        super(port1, port2);
    }
}
