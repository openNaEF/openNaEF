package naef.mvo.vlan;

import naef.mvo.Network;
import tef.skelton.ValueException;

import java.util.HashSet;
import java.util.Set;

public class MultipointVlanSegment
    extends VlanSegment
    implements Network.MemberPortConfigurable<VlanIf>
{
    private final F2<Vlan> owner_ = new F2<Vlan>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();
    private final S2<VlanIf> ports_ = new S2<VlanIf>();

    public MultipointVlanSegment(MvoId id) {
        super(id);
    }

    public MultipointVlanSegment() {
    }

    @Override public Set<VlanIf> getCurrentMemberPorts() {
        return new HashSet<VlanIf>(ports_.get());
    }

    @Override public void addMemberPort(VlanIf port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(VlanIf port) {
        super.removeMemberPort(ports_, port);
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
