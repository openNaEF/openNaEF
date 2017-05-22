package naef.mvo.fr;

import naef.mvo.L2Link;
import naef.mvo.Network;

public class FrPvc extends L2Link<FrPvcIf> implements Network.Exclusive {

    public FrPvc(MvoId id) {
        super(id);
    }

    public FrPvc(FrPvcIf port1, FrPvcIf port2) {
        super(port1, port2);
    }
}
