package naef.dto;

import tef.skelton.Attribute;
import tef.skelton.dto.Dto;

public class InterportLinkDto extends Dto {

    public static class ExAttr {

        public static final Attribute<PortDto, InterportLinkDto> PORT1
            = new Attribute.SingleAttr<PortDto, InterportLinkDto>("port1", PortDto.TYPE);
        public static final Attribute<PortDto, InterportLinkDto> PORT2
            = new Attribute.SingleAttr<PortDto, InterportLinkDto>("port2", PortDto.TYPE);
    }

    public InterportLinkDto(PortDto port1, PortDto port2) {
        set(ExAttr.PORT1, port1);
        set(ExAttr.PORT2, port2);
    }

    public PortDto getPort1() {
        return ExAttr.PORT1.get(this);
    }

    public PortDto getPort2() {
        return ExAttr.PORT2.get(this);
    }

    public PortDto getPort(NodeDto node) {
        if (DtoUtils.isSameEntity(getPort1().getNodeRef().oid(), node.getOid())) {
            return getPort1();
        }
        if (DtoUtils.isSameEntity(getPort2().getNodeRef().oid(), node.getOid())) {
            return getPort2();
        }
        return null;
    }
}
