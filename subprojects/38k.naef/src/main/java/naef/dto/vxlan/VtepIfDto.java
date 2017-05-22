package naef.dto.vxlan;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;

public class VtepIfDto extends SoftPortDto {

    public static final SetRefAttr<PortDto, VtepIfDto> CONNECTED_PORTS
        = new SetRefAttr<PortDto, VtepIfDto>("naef.dto.vtep-if.connected-ports");

    public VtepIfDto() {
    }
}
