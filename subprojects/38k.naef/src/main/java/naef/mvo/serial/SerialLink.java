package naef.mvo.serial;

import naef.mvo.L2Link;
import naef.mvo.Network;
import naef.mvo.Port;

public class SerialLink
    extends L2Link<Port>
    implements Network.Exclusive
{
    public SerialLink(MvoId id) {
        super(id);
    }

    public SerialLink(Port port1, Port port2) {
        super(port1, port2);
    }
}
