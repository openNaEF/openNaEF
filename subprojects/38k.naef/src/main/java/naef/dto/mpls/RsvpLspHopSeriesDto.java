package naef.dto.mpls;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PathHopDto;
import naef.mvo.mpls.RsvpLspHopSeries;
import tef.skelton.Attribute;
import tef.skelton.NamedModel;

import java.util.List;
import java.util.Set;

public class RsvpLspHopSeriesDto extends NetworkDto implements NamedModel {

    public static class ExtAttr {

        public static final SingleRefAttr<RsvpLspHopSeriesIdPoolDto, RsvpLspHopSeriesDto> ID_POOL
            = new SingleRefAttr<RsvpLspHopSeriesIdPoolDto, RsvpLspHopSeriesDto>(
                "naef.dto.mpls.rsvp-lsp-hop-series.id-pool");
        public static final ListRefAttr<PathHopDto, RsvpLspHopSeriesDto> HOPS
            = new ListRefAttr<PathHopDto, RsvpLspHopSeriesDto>("hops");
        public static final SetRefAttr<RsvpLspDto, RsvpLspHopSeriesDto> RSVPLSPS
            = new SetRefAttr<RsvpLspDto, RsvpLspHopSeriesDto>("naef.dto.mpls.rsvp-lsp-hop-series.rsvp-lsps");
        public static final SingleRefAttr<NodeDto, RsvpLspHopSeriesDto> INGRESS_NODE
            = new SingleRefAttr<NodeDto, RsvpLspHopSeriesDto>("naef.dto.mpls.rsvp-lsp-hop-series.ingress-node");
        public static final SingleRefAttr<NodeDto, RsvpLspHopSeriesDto> EGRESS_NODE
            = new SingleRefAttr<NodeDto, RsvpLspHopSeriesDto>("naef.dto.mpls.rsvp-lsp-hop-series.egress-node");
    }

    public RsvpLspHopSeriesDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public RsvpLspHopSeriesIdPoolDto getIdPool() {
        return ExtAttr.ID_POOL.deref(this);
    }

    public List<PathHopDto> getHops() {
        return ExtAttr.HOPS.deref(this);
    }

    public Set<RsvpLspDto> getRsvpLsps() {
        return ExtAttr.RSVPLSPS.deref(this);
    }

    public NodeDto getIngressNode() {
        return ExtAttr.INGRESS_NODE.deref(this);
    }

    public NodeDto getEgressNode() {
        return ExtAttr.EGRESS_NODE.deref(this);
    }

    public List<String> getConfiguredHops() {
        return (List<String>) getValue(RsvpLspHopSeries.Attr.CONFIGURED_HOPS.getName());
    }
}
