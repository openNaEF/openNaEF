package naef.mvo.atm;

import naef.mvo.Network;
import naef.mvo.P2pLink;

public class AtmPvp extends P2pLink<AtmPvpIf> implements Network.Exclusive {

    public AtmPvp(MvoId id) {
        super(id);
    }

    public AtmPvp(AtmPvpIf port1, AtmPvpIf port2) {
        super(port1, port2);
    }
}
