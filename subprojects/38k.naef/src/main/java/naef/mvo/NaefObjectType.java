package naef.mvo;

import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.NameConfigurableModel;
import tef.skelton.UiTypeName;
import tef.skelton.UniquelyNamedModelHome;

public class NaefObjectType extends AbstractModel implements NameConfigurableModel {

    public static class Attr {

        public static final Attribute.SetAttr<UiTypeName, NaefObjectType> ACCEPTABLE_DECLARING_TYPES
            = new Attribute.SetAttr<UiTypeName, NaefObjectType>(
                "naef.acceptable-declaring-types",
                AttributeType.MvoSetType.TYPE);
    }

    public static final UniquelyNamedModelHome.Indexed<NaefObjectType> home
        = new UniquelyNamedModelHome.Indexed<NaefObjectType>(NaefObjectType.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public NaefObjectType(MvoId id) {
        super(id);
    }

    public NaefObjectType(String name) {
        setName(name);
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }
}
