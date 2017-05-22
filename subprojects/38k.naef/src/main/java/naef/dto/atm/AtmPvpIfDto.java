package naef.dto.atm;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.atm.AtmPvpIf;

public class AtmPvpIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<PortDto, AtmPvpIfDto> BUTSURI_PORT
            = new SingleRefAttr<PortDto, AtmPvpIfDto>("物理ポート");
    }

    public AtmPvpIfDto() {
    }

    public Integer getVpi() {
        return get(AtmPvpIf.Attr.VPI);
    }

    public PortDto getPhysicalPort() {
        return ExtAttr.BUTSURI_PORT.deref(this);
    }
}
