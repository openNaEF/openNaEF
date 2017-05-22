package naef.mvo.ip;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.MvoHome;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IpSubnet 
    extends AbstractNetwork
    implements Network.Exclusive, Network.MemberPortConfigurable<Port>, Network.LowerStackable, Network.UpperStackable
{
    public static class Attr {

        public static final IdPoolAttribute<IpSubnetNamespace, IpSubnet> NAMESPACE
            = new IdPoolAttribute<IpSubnetNamespace, IpSubnet>("naef.ip.subnet-namespace", IpSubnetNamespace.class);

        public static final Attribute<String, IpSubnet> SUBNET_NAME
            = new IdAttribute<String, IpSubnet, IpSubnetNamespace>(
                "naef.ip.subnet-name",
                AttributeType.STRING, NAMESPACE);

        public static final Attribute<IpSubnetAddress, IpSubnet> SUBNET_ADDRESS
            = new Attribute.SingleModel<IpSubnetAddress, IpSubnet>("naef.subnet-address", IpSubnetAddress.class)
        {
            @Override public void set(IpSubnet model, IpSubnetAddress newValue) {
                IpSubnetAddress oldValue = get(model);

                if (oldValue == newValue) {
                    return;
                }

                super.set(model, newValue);

                if (oldValue != null) {
                    IpSubnetAddress.Attr.IP_SUBNET.set(oldValue, null);
                }
                if (newValue != null) {
                    IpSubnetAddress.Attr.IP_SUBNET.set(newValue, model);
                }
            }
        };
    }

    public static final MvoHome<IpSubnet> home = new MvoHome<IpSubnet>(IpSubnet.class);

    private final S2<Port> ports_ = new S2<Port>();
    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();

    public IpSubnet(MvoId id) {
        super(id);
    }

    public IpSubnet() {
    }

    @Override public Set<Port> getCurrentMemberPorts() {
        return new HashSet<Port>(ports_.get());
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public void addMemberPort(Port port) {
        super.addMemberPort(ports_, port);
    }

    @Override public void removeMemberPort(Port port) {
        super.removeMemberPort(ports_, port);
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
