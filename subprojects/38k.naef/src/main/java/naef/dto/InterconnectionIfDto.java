package naef.dto;

import java.util.Set;

public class InterconnectionIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SetRefAttr<PortDto, InterconnectionIfDto> ATTACHED_PORTS
            = new SetRefAttr<PortDto, InterconnectionIfDto>("naef.dto.i13n-if.attached-ports");
    }

    public InterconnectionIfDto() {
    }

    public Set<PortDto> getAttachedPorts() {
        return ExtAttr.ATTACHED_PORTS.deref(this);
    }
}
