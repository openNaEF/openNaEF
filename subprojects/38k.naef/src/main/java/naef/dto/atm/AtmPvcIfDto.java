package naef.dto.atm;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.atm.AtmPvcIf;

public class AtmPvcIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final SingleRefAttr<AtmPvcIfDto, AtmPvcIfDto> NEIGHBOR_PVC
            = new SingleRefAttr<AtmPvcIfDto, AtmPvcIfDto>("neighborPVC");
    }

    public AtmPvcIfDto() {
    }

    public PortDto getPhysicalPort() {
        if (getOwner() instanceof AtmPvpIfDto) {
            return ((AtmPvpIfDto) getOwner()).getPhysicalPort();
        } else {
            return null;
        }
    }

    public Integer getVpi() {
        if (getOwner() instanceof AtmPvpIfDto) {
            return ((AtmPvpIfDto) getOwner()).getVpi();
        } else {
            return null;
        }
    }

    public Integer getVci() {
        return get(AtmPvcIf.Attr.VCI);
    }

    public AtmPvcIfDto getNeighborPvc() {
        return ExtAttr.NEIGHBOR_PVC.deref(this);
    }
}
