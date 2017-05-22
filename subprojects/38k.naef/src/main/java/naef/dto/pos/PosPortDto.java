package naef.dto.pos;

import naef.dto.HardPortDto;

public class PosPortDto extends HardPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<PosApsIfDto, PosPortDto> POS_APS_IF
            = new SingleRefAttr<PosApsIfDto, PosPortDto>("naef.dto.pos-port.pos-aps-if");
    }

    public PosPortDto() {
    }

    public PosApsIfDto getPosApsIf() {
        return ExtAttr.POS_APS_IF.deref(this);
    }
}
