package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.NamedModel;

import java.util.Set;

public class LocationDto extends NaefDto implements NamedModel {

    public static class ExtAttr {

        public static final SingleRefAttr<LocationDto, LocationDto> PARENT
            = new SingleRefAttr<LocationDto, LocationDto>("parent");
        public static final SetRefAttr<LocationDto, LocationDto> CHILDREN
            = new SetRefAttr<LocationDto, LocationDto>("children");
    }

    public LocationDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public LocationDto getParent() {
        return ExtAttr.PARENT.deref(this);
    }

    public Set<LocationDto> getChildren() {
        return ExtAttr.CHILDREN.deref(this);
    }
}
