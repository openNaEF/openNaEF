package naef.dto.fr;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.fr.FrPvcIf;

public class FrPvcIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<PortDto, FrPvcIfDto> BUTSURI_PORT
            = new SingleRefAttr<PortDto, FrPvcIfDto>("物理ポート");
        public static final SingleRefAttr<FrPvcIfDto, FrPvcIfDto> NEIGHBOR_PVC
            = new SingleRefAttr<FrPvcIfDto, FrPvcIfDto>("neighborPVC");
    }

    public FrPvcIfDto() {
    }

    public Integer getDlci() {
        return get(FrPvcIf.Attr.DLCI);
    }

    public PortDto getPhysicalPort() {
        return ExtAttr.BUTSURI_PORT.deref(this);
    }

    public FrPvcIfDto getNeighborPvc() {
        return ExtAttr.NEIGHBOR_PVC.deref(this);
    }
}
