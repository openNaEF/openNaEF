package naef.dto.atm;

import naef.dto.HardPortDto;

public class AtmPortDto extends HardPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<AtmApsIfDto, AtmPortDto> ATM_APS_IF
            = new SingleRefAttr<AtmApsIfDto, AtmPortDto>("naef.dto.atm.atm-aps-if");
    }

    public AtmPortDto() {
    }

    public AtmApsIfDto getAtmApsIf() {
        return ExtAttr.ATM_APS_IF.deref(this);
    }
}
