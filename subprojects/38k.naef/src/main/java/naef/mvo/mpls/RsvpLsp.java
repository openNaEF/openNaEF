package naef.mvo.mpls;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Node;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;
import tef.skelton.ValueException;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class RsvpLsp 
    extends AbstractNetwork
    implements Network.LowerStackable, Network.UpperStackable
{
    public static class Attr {

        public static final Attribute.SingleBoolean<NodeElement> RSVP_LSP_ENABLED
            = new Attribute.SingleBoolean<NodeElement>("naef.enabled-networking-function.rsvp-lsp");

        public static final IdPoolAttribute<RsvpLspIdPool, RsvpLsp> IDPOOL
            = new IdPoolAttribute<RsvpLspIdPool, RsvpLsp>("naef.mpls.rsvp-lsp.id-pool", RsvpLspIdPool.class);
        public static final Attribute<String, RsvpLsp> ID
            = new IdAttribute<String, RsvpLsp, RsvpLspIdPool>("naef.mpls.rsvp-lsp.id", AttributeType.STRING, IDPOOL);

        public static final Attribute.SingleModel<Node, RsvpLsp> INGRESS_NODE
            = new Attribute.SingleModel<Node, RsvpLsp>("naef.mpls.rsvp-lsp.ingress-node", Node.class)
        {
            @Override public void validateValue(RsvpLsp model, Node value)
                throws ValueException
            {
                super.validateValue(model, value);

                if (model.get(this) != value) {
                    if (model.get(HOP_SERIES_1) != null
                        || model.get(HOP_SERIES_2) != null
                        || model.get(HOP_SERIES_3) != null
                        || model.get(HOP_SERIES_4) != null
                        || model.get(HOP_SERIES_5) != null)
                    {
                        throw new ConfigurationException("ホップ列が設定されているため変更できません.");
                    }
                }
            }
        };

        private static class HopSeriesAttribute
            extends Attribute.SingleModel<RsvpLspHopSeries, RsvpLsp>
        {
            HopSeriesAttribute(String name) {
                super(name, RsvpLspHopSeries.class);
            }

            @Override public void validateValue(RsvpLsp model, RsvpLspHopSeries value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                Node ingressNode = model.get(INGRESS_NODE);
                if (ingressNode == null) {
                    throw new ConfigurationException(INGRESS_NODE.getName() + " を設定してください.");
                }
                if (value != null && value.getIngressNode() != ingressNode) {
                    throw new ValueException(INGRESS_NODE.getName() + " が不整合です.");
                }

                RsvpLspHopSeries currentValue = model.get(this);
                if (value == null) {
                    if (model.get(ACTIVE_HOP_SERIES) == currentValue) {
                        throw new ValueException("active に設定されているためリセットできません.");
                    }
                }
            }
        }

        private static class HopSeriesPostProcessor 
            implements Attribute.SingleAttr.PostProcessor<RsvpLspHopSeries, RsvpLsp>
        {
            @Override public void set(RsvpLsp model, RsvpLspHopSeries oldValue, RsvpLspHopSeries newValue) {
                if (oldValue != null) {
                    model.unstackOver(oldValue);
                }
                if (newValue != null) {
                    model.stackOver(newValue);
                }
            }
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> HOP_SERIES_1
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series-1");
        static {
            HOP_SERIES_1.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> HOP_SERIES_2
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series-2");
        static {
            HOP_SERIES_2.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> HOP_SERIES_3
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series-3");
        static {
            HOP_SERIES_3.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> HOP_SERIES_4
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series-4");
        static {
            HOP_SERIES_4.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> HOP_SERIES_5
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series-5");
        static {
            HOP_SERIES_5.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleAttr<RsvpLspHopSeries, RsvpLsp> ACTUAL_HOP_SERIES
            = new HopSeriesAttribute("naef.mpls.rsvp-lsp.hop-series.actual");
        static {
            ACTUAL_HOP_SERIES.addPostProcessor(new HopSeriesPostProcessor());
        }

        public static final Attribute.SingleModel<RsvpLspHopSeries, RsvpLsp> ACTIVE_HOP_SERIES
            = new Attribute.SingleModel<RsvpLspHopSeries, RsvpLsp>(
                "naef.mpls.rsvp-lsp.active-hop-series",
                RsvpLspHopSeries.class)
        {
            @Override public void validateValue(RsvpLsp model, RsvpLspHopSeries value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (value != null) {
                    if (model.get(HOP_SERIES_1) != value
                        && model.get(HOP_SERIES_2) != value
                        && model.get(HOP_SERIES_3) != value
                        && model.get(HOP_SERIES_4) != value
                        && model.get(HOP_SERIES_5) != value)
                    {
                        throw new ValueException("設定された hop-series を指定してください.");
                    }
                }
            }
        };
    }

    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();

    public RsvpLsp(MvoId id) {
        super(id);
    }

    public RsvpLsp() {
    }

    @Override public Collection<Port> getCurrentMemberPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public Collection<Port> getCurrentAttachedPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public void stackUnder(Network.UpperStackable upperLayer) {
        super.stackUnder(upperLayers_, upperLayer);
    }

    @Override public void unstackUnder(Network.UpperStackable upperLayer) {
        super.unstackUnder(upperLayers_, upperLayer);
    }

    @Override public Set<? extends Network.UpperStackable> getHereafterUpperLayers(boolean recursive) {
        return super.getHereafterUpperLayers(upperLayers_, recursive);
    }

    @Override public Set<? extends Network.UpperStackable> getCurrentUpperLayers(boolean recursive) {
        return super.getCurrentUpperLayers(upperLayers_, recursive);
    }

    @Override public void stackOver(Network.LowerStackable lowerLayer) {
        if (! (lowerLayer instanceof RsvpLspHopSeries)) {
            throw new IllegalArgumentException("rsvp-lsp の下層には rsvp-lsp-hop-series を指定してください.");
        }

        super.stackOver(lowerLayers_, lowerLayer);
    }

    @Override public void unstackOver(Network.LowerStackable lowerLayer) {
        if (! (lowerLayer instanceof RsvpLspHopSeries)) {
            throw new IllegalArgumentException("rsvp-lsp の下層には rsvp-lsp-hop-series を指定してください.");
        }

        super.unstackOver(lowerLayers_, lowerLayer);
    }

    @Override public Set<? extends Network.LowerStackable> getHereafterLowerLayers(boolean recursive) {
        return super.getHereafterLowerLayers(lowerLayers_, recursive);
    }

    @Override public Set<? extends Network.LowerStackable> getCurrentLowerLayers(boolean recursive) {
        return super.getCurrentLowerLayers(lowerLayers_, recursive);
    }

    public Node getIngressNode() {
        return get(Attr.INGRESS_NODE);
    }

    public Node getEgressNode() {
        return get(Attr.HOP_SERIES_1) == null
            ? null
            : get(Attr.HOP_SERIES_1).getEgressNode();
    }
}
