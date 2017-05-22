package naef.dto.atm;

import naef.dto.SoftPortDto;

import java.util.Set;

public class AtmApsIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SetRefAttr<AtmPortDto, AtmApsIfDto> ATM_PORTS
            = new SetRefAttr<AtmPortDto, AtmApsIfDto>(
                "naef.dto.atm.atm-ports"); 
    }

    public AtmApsIfDto() {
    }

    public Set<AtmPortDto> getAtmPorts() {
        return ExtAttr.ATM_PORTS.deref(this);
    }
}
