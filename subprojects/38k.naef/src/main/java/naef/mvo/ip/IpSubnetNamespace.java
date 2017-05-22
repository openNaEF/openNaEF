package naef.mvo.ip;

import tef.skelton.Attribute;
import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;

public class IpSubnetNamespace
    extends IdPool.SingleMap<IpSubnetNamespace, String, IpSubnet>
    implements NameConfigurableModel
{
    public static class Attr {

        public static final Attribute.SingleModel<IpSubnetAddress, IpSubnetNamespace> IP_SUBNET_ADDRESS
            = new Attribute.SingleModel<IpSubnetAddress, IpSubnetNamespace>(
                "naef.ip-subnet-address",
                IpSubnetAddress.class);
        static {
            IP_SUBNET_ADDRESS.addPostProcessor(
                new Attribute.SingleAttr.PostProcessor<IpSubnetAddress, IpSubnetNamespace>() {

                    @Override public void set(
                        IpSubnetNamespace model, IpSubnetAddress oldValue, IpSubnetAddress newValue)
                    {
                        if (oldValue == newValue) {
                            return;
                        }

                        if (oldValue != null) {
                            IpSubnetAddress.Attr.IP_SUBNET_NAMESPACE.set(oldValue, null);
                        }
                        if (newValue != null) {
                            IpSubnetAddress.Attr.IP_SUBNET_NAMESPACE.set(newValue, model);
                        }
                    }
                });
        }
    }

    public static final UniquelyNamedModelHome.Indexed<IpSubnetNamespace> home
        = new UniquelyNamedModelHome.Indexed<IpSubnetNamespace>(IpSubnetNamespace.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected IpSubnetNamespace(MvoId id) {
        super(id);
    }

    public IpSubnetNamespace() {
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public String parseId(String str) {
        return str;
    }

    @Override public Range.String parseRange(String str) throws FormatException {
        return Range.String.gainByRangeStr(str);
    }
}
