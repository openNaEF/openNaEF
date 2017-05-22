package naef.mvo;

import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.NameConfigurableModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.SkeltonUtils;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public class CustomerInfo extends AbstractModel implements NameConfigurableModel {

    public static class Attr {

        public static final Attribute.SetAttr<Model, CustomerInfo> REFERENCES
            = new Attribute.SetAttr<Model, CustomerInfo>(
                "naef.customer-info.references",
                new AttributeType.MvoSetType<Model>(Model.class) {

                    @Override public Model parseElement(String str) {
                        return ValueResolver.<Model>resolve(Model.class, null, str);
                    }
                });
        static {
            REFERENCES.addPostProcessor(new Attribute.SetAttr.PostProcessor<Model, CustomerInfo>() {

                @Override public void add(CustomerInfo model, Model value) {
                    if (! NaefAttributes.CUSTOMER_INFOS.containsValue(value, model)) {
                        NaefAttributes.CUSTOMER_INFOS.addValue(value, model);
                    }
                }

                @Override public void remove(CustomerInfo model, Model value) {
                    if (NaefAttributes.CUSTOMER_INFOS.containsValue(value, model)) {
                        NaefAttributes.CUSTOMER_INFOS.removeValue(value, model);
                    }
                }
            });
        }

        public static final Attribute.SingleModel<SystemUser, CustomerInfo> SYSTEM_USER
            = new Attribute.SingleModel<SystemUser, CustomerInfo>("naef.system-user", SystemUser.class);
        static {
            SYSTEM_USER.addPostProcessor(new Attribute.SingleAttr.PostProcessor<SystemUser, CustomerInfo>() {

                @Override public void set(CustomerInfo model, SystemUser oldValue, SystemUser newValue) {
                    if (oldValue == newValue) {
                        return;
                    }

                    if (oldValue != null) {
                        SystemUser.Attr.CUSTOMER_INFOS.removeValue(oldValue, model);
                    }
                    if (newValue != null) {
                        SystemUser.Attr.CUSTOMER_INFOS.addValue(newValue, model);
                    }
                }
            });
        }
    }

    public static class ModelAttr {
    }

    public static final UniquelyNamedModelHome.Indexed<CustomerInfo> home
        = new UniquelyNamedModelHome.Indexed<CustomerInfo>(CustomerInfo.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public CustomerInfo(MvoId id) {
        super(id);
    }

    public CustomerInfo(String name) {
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
