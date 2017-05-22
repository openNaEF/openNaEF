package naef.dto;

import java.util.Set;

public class NetworkDto extends NaefDto {

    public static class ExtAttr {

        public static final SetRefAttr<PortDto, NetworkDto> MEMBER_PORTS
            = new SetRefAttr<PortDto, NetworkDto>("member ports");
        public static final SetRefAttr<NetworkDto, NetworkDto> UPPER_LAYERS
            = new SetRefAttr<NetworkDto, NetworkDto>("upper layers");
        public static final SetRefAttr<NetworkDto, NetworkDto> LOWER_LAYER_LINKS
            = new SetRefAttr<NetworkDto, NetworkDto>("lower layer links");
        public static final SingleRefAttr<NetworkDto, NetworkDto> OWNER
            = new SingleRefAttr<NetworkDto, NetworkDto>("owner");
    }

    public NetworkDto() {
    }

    public Set<PortDto> getMemberPorts() {
        return ExtAttr.MEMBER_PORTS.deref(this);
    }

    public boolean isDemarcationLink() {
        return ExtAttr.MEMBER_PORTS.get(this).size() == 1;
    }

    public Set<NetworkDto> getUpperLayers() {
        return ExtAttr.UPPER_LAYERS.deref(this);
    }

    public Set<NetworkDto> getLowerLayerLinks() {
        return ExtAttr.LOWER_LAYER_LINKS.deref(this);
    }

    public NetworkDto getOwner() {
        return ExtAttr.OWNER.deref(this);
    }
}
