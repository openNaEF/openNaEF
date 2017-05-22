package naef.mvo.wdm;

import naef.mvo.L2Link;
import naef.mvo.Network;

import java.util.Set;

public class WdmLink 
    extends L2Link<WdmPort> 
    implements Network, Network.Exclusive, Network.LowerStackable
{
    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();

    public WdmLink(MvoId id) {
        super(id);
    }

    public WdmLink(WdmPort port1, WdmPort port2) {
        super(port1, port2);
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
