package naef.dto.ospf;

import java.util.Set;

import naef.dto.RoutingProcessDto;
import naef.mvo.ospf.OspfAreaId;
import naef.mvo.ospf.OspfProcess;

public class OspfProcessDto extends RoutingProcessDto {

    public OspfProcessDto() {
    }

    public Set<OspfAreaId> getOspfAreaIds() {
        return unnull((Set<OspfAreaId>) getValue(OspfProcess.Attr.AREA_ID.getName()));
    }

    public Set<String> getShamLinks() {
        return unnull((Set<String>) getValue(OspfProcess.Attr.SHAM_LINKS.getName()));
    }
}
