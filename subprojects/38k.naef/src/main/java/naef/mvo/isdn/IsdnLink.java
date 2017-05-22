package naef.mvo.isdn;

import naef.mvo.L2Link;
import naef.mvo.Network;
import naef.mvo.Port;

public class IsdnLink 
    extends L2Link<Port>
    implements Network.Exclusive
{
    public IsdnLink(MvoId id) {
        super(id);
    }

    public IsdnLink(Port port1, Port port2) {
        super(port1, port2);
    }
}
