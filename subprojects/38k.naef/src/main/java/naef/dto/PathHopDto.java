package naef.dto;

public class PathHopDto extends NetworkDto {

    public static class Attr {

        public static final SingleRefAttr<PortDto, PathHopDto> SRC_PORT
            = new SingleRefAttr<PortDto, PathHopDto>("naef.dto.path-hop.src-port");
        public static final SingleRefAttr<PortDto, PathHopDto> DST_PORT
            = new SingleRefAttr<PortDto, PathHopDto>("naef.dto.path-hop.dst-port");
    }

    public PathHopDto() {
    }

    public PortDto getSrcPort() {
        return Attr.SRC_PORT.deref(this);
    }

    public PortDto getDstPort() {
        return Attr.DST_PORT.deref(this);
    }
}
