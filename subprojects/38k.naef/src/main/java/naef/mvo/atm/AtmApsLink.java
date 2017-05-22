package naef.mvo.atm;

import naef.mvo.Network;
import naef.mvo.P2pLink;

public class AtmApsLink extends P2pLink<AtmApsIf> implements Network.Exclusive {

    public AtmApsLink(MvoId id) {
        super(id);
    }

    public AtmApsLink(AtmApsIf port1, AtmApsIf port2) {
        super(port1, port2);
    }
}
