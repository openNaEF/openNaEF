package naef.mvo.vlan;

import naef.mvo.Network;
import tef.skelton.ValueException;

import java.util.HashSet;
import java.util.Set;

public class VlanLink extends VlanSegment {

    private final F2<Vlan> owner_ = new F2<Vlan>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();
    private final F0<VlanIf> port1_ = new F0<VlanIf>();
    private final F0<VlanIf> port2_ = new F0<VlanIf>();

    public VlanLink(MvoId id) {
        super(id);
    }

    public VlanLink(VlanIf port1, VlanIf port2) {
        if (port1 == null) {
            throw new IllegalArgumentException();
        }
        if (port1 == port2) {
            throw new IllegalArgumentException();
        }

        port1_.initialize(port1);
        port2_.initialize(port2);

        port1.joinNetwork(this);
        if (port2 != null) {
            port2.joinNetwork(this);
        }
    }

    @Override public Set<VlanIf> getCurrentMemberPorts() {
        Set<VlanIf> result = new HashSet<VlanIf>();
        result.add(port1_.get());
        if (port2_.get() != null) {
            result.add(port2_.get());
        }
        return result;
    }

    private void assertContainerType(Container owner) {
        if (owner == null
            || owner instanceof Vlan)
        {
            return;
        }

        throw new ValueException("container type error.");
    }

    @Override public void setContainer(Container container) {
        assertContainerType(container);
        super.setOwner(owner_, (Vlan) container);
    }

    @Override public Vlan getCurrentContainer() {
        return super.getCurrentOwner(owner_);
    }

    @Override public void stackOver(Network.LowerStackable lowerLayer) {
        super.stackOver(lowerLayers_, lowerLayer);
    }

    @Override public void unstackOver(Network.LowerStackable lowerLayer) {
        super.unstackOver(lowerLayers_, lowerLayer);
    }

    @Override public Set<? extends Network.LowerStackable> getHereafterLowerLayers(boolean recursive) {
        return super.getHereafterLowerLayers(lowerLayers_, recursive);
    }

    @Override public Set<? extends Network.LowerStackable> getCurrentLowerLayers(boolean recursive) {
        return super.getCurrentLowerLayers(lowerLayers_, recursive);
    }
}
