package naef.mvo.eth;

import naef.mvo.L2Link;
import naef.mvo.Network;

import java.util.Set;

public class EthLink
    extends L2Link<EthPort>
    implements Network.LowerStackable
{
    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();

    public EthLink(MvoId id) {
        super(id);
    }

    public EthLink(EthPort port1, EthPort port2) {
        super(port1, port2);
    }

    @Override protected void assertAcceptableContainerType(Network.Container owner) {
        if (owner instanceof EthLag) {
            return;
        }

        super.assertAcceptableContainerType(owner);
    }

    @Override public void stackUnder(Network.UpperStackable upperLayer) {
        super.stackUnder(upperLayers_, upperLayer);
    }

    @Override public void unstackUnder(Network.UpperStackable upperLayer) {
        super.unstackUnder(upperLayers_, upperLayer);
    }

    @Override public Set<? extends Network.UpperStackable> getHereafterUpperLayers(boolean recursive) {
        return super.getHereafterUpperLayers(upperLayers_, recursive);
    }

    @Override public Set<? extends Network.UpperStackable> getCurrentUpperLayers(boolean recursive) {
        return super.getCurrentUpperLayers(upperLayers_, recursive);
    }
}
