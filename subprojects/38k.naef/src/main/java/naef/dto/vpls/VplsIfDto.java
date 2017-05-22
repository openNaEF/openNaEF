package naef.dto.vpls;

import java.util.Set;

import naef.dto.PortDto;
import naef.dto.SoftPortDto;
import naef.mvo.vpls.VplsIf;
import tef.skelton.Attribute;

public class VplsIfDto extends SoftPortDto {

    public static class ExtAttr {

        public static final Attribute.SingleString<VplsIfDto> VPLS_MEI
            = new Attribute.SingleString<VplsIfDto>("VPLS 名");
        public static final SetRefAttr<PortDto, VplsIfDto> SETSUZOKU_PORT
            = new SetRefAttr<PortDto, VplsIfDto>("接続ポート");
        public static final SingleRefAttr<VplsDto, VplsIfDto> TRAFFIC_DOMAIN
            = new SingleRefAttr<VplsDto, VplsIfDto>("trafficDomain");
    }

    public VplsIfDto() {
    }

    public Integer getVplsId() {
        return get(VplsIf.Attr.VPLS_ID);
    }

    public Set<PortDto> getAttachedPorts() {
        return ExtAttr.SETSUZOKU_PORT.deref(this);
    }

    public VplsDto getTrafficDomain() {
        return ExtAttr.TRAFFIC_DOMAIN.deref(this);
    }
}
