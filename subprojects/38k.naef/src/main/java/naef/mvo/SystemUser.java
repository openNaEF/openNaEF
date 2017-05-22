package naef.mvo;

import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.NameConfigurableModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.SkeltonUtils;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public class SystemUser extends AbstractModel implements NameConfigurableModel {

    public static class Attr {

        public static final Attribute.SetAttr<CustomerInfo, SystemUser>
            CUSTOMER_INFOS = new Attribute.SetAttr<CustomerInfo, SystemUser>(
            "naef.customer-infos",
            new AttributeType.MvoSetType<CustomerInfo>(CustomerInfo.class) {

                @Override public CustomerInfo parseElement(String str) {
                    return ValueResolver.<CustomerInfo>resolve(CustomerInfo.class, null, str);
                }
            });
        static {
            CUSTOMER_INFOS.addPostProcessor(new Attribute.SetAttr.PostProcessor<CustomerInfo, SystemUser>() {

                @Override public void add(SystemUser model, CustomerInfo value) {
                    if (CustomerInfo.Attr.SYSTEM_USER.get(value) != model) {
                        CustomerInfo.Attr.SYSTEM_USER.set(value, model);
                    }
                }

                @Override public void remove(SystemUser model, CustomerInfo value) {
                    if (CustomerInfo.Attr.SYSTEM_USER.get(value) == model) {
                        CustomerInfo.Attr.SYSTEM_USER.set(value, null);
                    }
                }
            });
        }
    }

    public static final UniquelyNamedModelHome.Indexed<SystemUser> home
        = new UniquelyNamedModelHome.Indexed<SystemUser>(SystemUser.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public SystemUser(MvoId id) {
        super(id);
    }

    public SystemUser(String name) {
        setName(name);
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    public String getFqn() {
        return SkeltonTefService.instance().uiTypeNames().getName(getClass())
            + SkeltonTefService.instance().getFqnPrimaryDelimiter()
            + SkeltonUtils.fqnEscape(getName());
    }
}
