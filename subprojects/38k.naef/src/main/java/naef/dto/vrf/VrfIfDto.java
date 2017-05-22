package naef.dto.vrf;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.vrf.VrfIf;
import tef.skelton.Attribute;

import java.util.Set;

public class VrfIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final Attribute.SingleString<VrfIfDto> VRF_MEI
            = new Attribute.SingleString<VrfIfDto>("VRF 名");
        public static final SetRefAttr<PortDto, VrfIfDto> SETSUZOKU_PORT
            = new SetRefAttr<PortDto, VrfIfDto>( "接続ポート");
        public static final SingleRefAttr<VrfDto, VrfIfDto> TRAFFIC_DOMAIN
            = new SingleRefAttr<VrfDto, VrfIfDto>("trafficDomain");
    }

    public VrfIfDto() {
    }

    public Integer getVrfId() {
        return get(VrfIf.Attr.VRF_ID);
    }

    public Set<PortDto> getAttachedPorts() {
        return ExtAttr.SETSUZOKU_PORT.deref(this);
    }

    public VrfDto getTrafficDomain() {
        return ExtAttr.TRAFFIC_DOMAIN.deref(this);
    }
}
