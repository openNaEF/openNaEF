package naef.dto.mpls;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import tef.skelton.Attribute;
import tef.skelton.NamedModel;

import java.util.Set;

public class RsvpLspDto extends NetworkDto implements NamedModel {

    public static class ExtAttr {

        public static final SingleRefAttr<RsvpLspIdPoolDto, RsvpLspDto> IDPOOL
            = new SingleRefAttr<RsvpLspIdPoolDto, RsvpLspDto>("naef.dto.mpls.rsvp-lsp.id-pool");
        public static final Attribute.SingleString<RsvpLspDto> ID
            = new Attribute.SingleString<RsvpLspDto>("naef.dto.mpls.rsvp-lsp.id");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> ACTIVE_HOP_SERIES
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("naef.dto.mpls.rsvp-lsp.active-hop-series");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> HOP_SERIES_1
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("hop-series-1");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> HOP_SERIES_2
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("hop-series-2");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> HOP_SERIES_3
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("hop-series-3");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> HOP_SERIES_4
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("hop-series-4");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> HOP_SERIES_5
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("hop-series-5");
        public static final SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto> ACTUAL_HOP_SERIES
            = new SingleRefAttr<RsvpLspHopSeriesDto, RsvpLspDto>("naef.dto.mpls.rsvp-lsp.hop-series.actual");
        public static final SetRefAttr<PseudowireDto, RsvpLspDto> PSEUDOWIRES
            = new SetRefAttr<PseudowireDto, RsvpLspDto>("pseudowires");
        public static final SingleRefAttr<NodeDto, RsvpLspDto> INGRESS_NODE
            = new SingleRefAttr<NodeDto, RsvpLspDto>("ingress");
        public static final SingleRefAttr<NodeDto, RsvpLspDto> EGRESS_NODE
            = new SingleRefAttr<NodeDto, RsvpLspDto>("egress");
    }

    public RsvpLspDto() {
    }

    @Override public String getName() {
        return Attribute.NAME.get(this);
    }

    public RsvpLspIdPoolDto getIdPool() {
        return ExtAttr.IDPOOL.deref(this);
    }

    public String getId() {
        return ExtAttr.ID.get(this);
    }

    public RsvpLspHopSeriesDto getActiveHopSeries() {
        return ExtAttr.ACTIVE_HOP_SERIES.deref(this);
    }

    public RsvpLspHopSeriesDto getHopSeries1() {
        return ExtAttr.HOP_SERIES_1.deref(this);
    }

    public RsvpLspHopSeriesDto getHopSeries2() {
        return ExtAttr.HOP_SERIES_2.deref(this);
    }

    public RsvpLspHopSeriesDto getHopSeries3() {
        return ExtAttr.HOP_SERIES_3.deref(this);
    }

    public RsvpLspHopSeriesDto getHopSeries4() {
        return ExtAttr.HOP_SERIES_4.deref(this);
    }

    public RsvpLspHopSeriesDto getHopSeries5() {
        return ExtAttr.HOP_SERIES_5.deref(this);
    }

    public RsvpLspHopSeriesDto getActualHopSeries() {
        return ExtAttr.ACTUAL_HOP_SERIES.deref(this);
    }

    public Set<PseudowireDto> getPseudowires() {
        return ExtAttr.PSEUDOWIRES.deref(this);
    }

    public NodeDto getIngressNode() {
        return ExtAttr.INGRESS_NODE.deref(this);
    }

    public NodeDto getEgressNode() {
        return ExtAttr.EGRESS_NODE.deref(this);
    }
}
