package naef.mvo.pos;

import naef.mvo.Network;
import naef.mvo.P2pLink;

public class PosApsLink extends P2pLink<PosApsIf> implements Network.Exclusive {

    public PosApsLink(MvoId id) {
        super(id);
    }

    public PosApsLink(PosApsIf port1, PosApsIf port2) {
        super(port1, port2);
    }
}
