package naef.mvo.ip;

import tef.skelton.Attribute;
import tef.skelton.FormatException;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;

public class IpSubnetAddress
    extends IdPool.SingleMap<IpSubnetAddress, IpAddress, IpIf>
    implements NameConfigurableModel
{
    public static class Attr {

        public static final Attribute.SingleModel<IpSubnetNamespace, IpSubnetAddress> IP_SUBNET_NAMESPACE
            = new Attribute.SingleModel<IpSubnetNamespace, IpSubnetAddress>(
                "naef.ip-subnet-namespace",
                IpSubnetNamespace.class);
        static {
            IP_SUBNET_NAMESPACE.addPostProcessor(
                new Attribute.SingleAttr.PostProcessor<IpSubnetNamespace, IpSubnetAddress>() {

                    @Override public void set(
                        IpSubnetAddress model, IpSubnetNamespace oldValue, IpSubnetNamespace newValue)
                    {
                        if (oldValue == newValue) {
                            return;
                        }

                        if (oldValue != null) {
                            IpSubnetNamespace.Attr.IP_SUBNET_ADDRESS.set(oldValue, null);
                        }
                        if (newValue != null) {
                            IpSubnetNamespace.Attr.IP_SUBNET_ADDRESS.set(newValue, model);
                        }
                    }
                });
        }

        public static final Attribute<IpSubnet, IpSubnetAddress> IP_SUBNET
            = new Attribute.SingleModel<IpSubnet, IpSubnetAddress>("naef.ip-subnet", IpSubnet.class)
        {
            @Override public void set(IpSubnetAddress model, IpSubnet newValue) {
                IpSubnet oldValue = get(model);

                if (oldValue == newValue) {
                    return;
                }

                super.set(model, newValue);

                if (oldValue != null) {
                    IpSubnet.Attr.SUBNET_ADDRESS.set(oldValue, null);
                }
                if (newValue != null) {
                    IpSubnet.Attr.SUBNET_ADDRESS.set(newValue, model);
                }
            }
        };
    }

    public static final UniquelyNamedModelHome.Indexed<IpSubnetAddress> home
        = new UniquelyNamedModelHome.Indexed<IpSubnetAddress>(IpSubnetAddress.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public IpSubnetAddress(MvoId id) {
        super(id);
    }

    public IpSubnetAddress() {
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public IpAddress parseId(String str) throws FormatException {
        return IpAddress.gain(str);
    }

    @Override public IpAddressRange parseRange(String str) throws FormatException {
        return IpAddressRange.gainByRangeStr(str);
    }
}
