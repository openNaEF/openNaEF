package tef.skelton;

public class AttributeInterlock {

    public static void interlockSingle(Attribute.SingleAttr<?, ?> attr1, Attribute.SingleAttr<?, ?> attr2) {

        final Attribute.SingleAttr<Model, Model> a1 = (Attribute.SingleAttr<Model, Model>) attr1;
        final Attribute.SingleAttr<Model, Model> a2 = (Attribute.SingleAttr<Model, Model>) attr2;

        a1.addPostProcessor(new Attribute.SingleAttr.PostProcessor<Model, Model>() {

            @Override public void set(Model model, Model oldValue, Model newValue) {
                if (oldValue == newValue) {
                    return;
                }

                if (oldValue != null) {
                    a2.set(oldValue, null);
                }
                if (newValue != null) {
                    a2.set(newValue, model);
                }
            }
        });
        a2.addPostProcessor(new Attribute.SingleAttr.PostProcessor<Model, Model>() {

            @Override public void set(Model model, Model oldValue, Model newValue) {
                if (oldValue == newValue) {
                    return;
                }

                if (oldValue != null) {
                    a1.set(oldValue, null);
                }
                if (newValue != null) {
                    a1.set(newValue, model);
                }
            }
        });
    }
}
