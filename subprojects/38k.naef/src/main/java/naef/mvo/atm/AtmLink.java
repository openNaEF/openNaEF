package naef.mvo.atm;

import naef.mvo.L2Link;
import naef.mvo.Network;
import naef.mvo.Port;

public class AtmLink
    extends L2Link<Port>
    implements Network.Exclusive
{
    public AtmLink(MvoId id) {
        super(id);
    }

    public AtmLink(Port port1, Port port2) {
        super(port1, port2);
    }
}
