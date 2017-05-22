package naef.mvo;

import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.ValueResolver;

public class NaefAttributes {

    public static final Attribute.SingleBoolean<Model> ENABLED = new Attribute.SingleBoolean<Model>("naef.enabled");
    public static final Attribute.SingleDateTime<Model> ENABLED_TIME
        = new Attribute.SingleDateTime<Model>("naef.enabled-time");
    public static final Attribute.SingleDateTime<Model> DISABLED_TIME
        = new Attribute.SingleDateTime<Model>("naef.disabled-time");

    public static final Attribute.SetAttr<CustomerInfo, Model> CUSTOMER_INFOS
        = new Attribute.SetAttr<CustomerInfo, Model>(
            "naef.customer-infos",
            new AttributeType.MvoSetType<CustomerInfo>(CustomerInfo.class) {

                @Override public CustomerInfo parseElement(String str) {
                    return ValueResolver.<CustomerInfo>resolve(CustomerInfo.class, null, str);
                }
            });
    static {
        CUSTOMER_INFOS.addPostProcessor(new Attribute.SetAttr.PostProcessor<CustomerInfo, Model>() {

            @Override public void add(Model model, CustomerInfo value) {
                if (! CustomerInfo.Attr.REFERENCES.containsValue(value, model)) {
                    CustomerInfo.Attr.REFERENCES.addValue(value, model);
                }
            }

            @Override public void remove(Model model, CustomerInfo value) {
                if (CustomerInfo.Attr.REFERENCES.containsValue(value, model)) {
                    CustomerInfo.Attr.REFERENCES.removeValue(value, model);
                }
            }
        });
    }
}
