package naef.dto.pos;

import naef.dto.SoftPortDto;

import java.util.Set;

public class PosApsIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SetRefAttr<PosPortDto, PosApsIfDto> POS_PORTS
            = new SetRefAttr<PosPortDto, PosApsIfDto>("naef.dto.pos-aps-if.pos-ports");
    }

    public PosApsIfDto() {
    }

    public Set<PosPortDto> getPosPorts() {
        return ExtAttr.POS_PORTS.deref(this);
    }
}
