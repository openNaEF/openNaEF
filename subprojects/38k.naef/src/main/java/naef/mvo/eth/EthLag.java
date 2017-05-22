package naef.mvo.eth;

import naef.mvo.Network;
import naef.mvo.P2pLink;
import tef.skelton.ValueException;

import java.util.Set;

public class EthLag 
    extends P2pLink<EthLagIf>
    implements Network, Network.Exclusive, Network.LowerStackable, Network.Container
{
    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();
    private final S2<EthLink> parts_ = new S2<EthLink>();

    public EthLag(MvoId id) {
        super(id);
    }

    public EthLag(EthLagIf port1, EthLagIf port2) {
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

    @Override public void addPart(Network.Containee part) {
        if (! (part instanceof EthLink)) {
            throw new ValueException("containee type error.");
        }

        super.addPart(parts_, (EthLink) part);
    }

    @Override public void removePart(Network.Containee part) {
        if (! (part instanceof EthLink)) {
            throw new ValueException("containee type error.");
        }

        super.removePart(parts_, (EthLink) part);
    }

    @Override public Set<EthLink> getHereafterParts(boolean recursive) {
        return super.getHereafterParts(parts_, recursive);
    }

    @Override public Set<EthLink> getCurrentParts(boolean recursive) {
        return super.getCurrentParts(parts_, recursive);
    }
}
