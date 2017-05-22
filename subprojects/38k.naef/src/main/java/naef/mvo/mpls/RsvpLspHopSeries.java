package naef.mvo.mpls;

import naef.mvo.AbstractNetwork;
import naef.mvo.NaefMvoUtils;
import naef.mvo.Network;
import naef.mvo.Node;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.ConfigurationException;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RsvpLspHopSeries 
    extends AbstractNetwork
    implements Network.LowerStackable, Network.PathHopSeries<RsvpLspHop, Port, RsvpLspHopSeries>
{
    public static class Attr {

        public static final IdPoolAttribute<RsvpLspHopSeriesIdPool, RsvpLspHopSeries> ID_POOL
            = new IdPoolAttribute<RsvpLspHopSeriesIdPool, RsvpLspHopSeries>(
                "naef.rsvp-lsp-hop-series.id-pool",
                RsvpLspHopSeriesIdPool.class);
        public static final Attribute<String, RsvpLspHopSeries> ID
            = new IdAttribute<String, RsvpLspHopSeries, RsvpLspHopSeriesIdPool>(
                "naef.rsvp-lsp-hop-series.id",
                AttributeType.STRING, ID_POOL);

        public static final Attribute.SingleModel<Node, RsvpLspHopSeries> INGRESS_NODE
            = new Attribute.SingleModel<Node, RsvpLspHopSeries>(
                "naef.rsvp-lsp-hop-series.ingress-node",
                Node.class)
        {
            @Override public void validateValue(RsvpLspHopSeries model, Node value) {
                super.validateValue(model, value);

                if (model.get(this) != value) {
                    if (model.getLastHop() != null) {
                        throw new ConfigurationException("ホップが設定されているため変更できません.");
                    }
                }
            }
        };

        public static final Attribute.ListStringAttr<RsvpLspHopSeries> CONFIGURED_HOPS
            = new Attribute.ListStringAttr<RsvpLspHopSeries>("naef.rsvp-lsp-hop-series.configured-hops");
    }

    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();
    private final F2<RsvpLspHop> lastHop_ = new F2<RsvpLspHop>();

    public RsvpLspHopSeries(MvoId id) {
        super(id);
    }

    public RsvpLspHopSeries() {
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

    @Override public void addPart(Network.Containee hop) {
        throw new UnsupportedOperationException();
    }

    @Override public void removePart(Network.Containee part) {
        throw new UnsupportedOperationException();
    }

    @Override public Set<RsvpLspHop> getHereafterParts(boolean recursive) {
        throw new RuntimeException("未実装");
    }

    @Override public Set<RsvpLspHop> getCurrentParts(boolean recursive) {
        return new HashSet<RsvpLspHop>(NaefMvoUtils.getHops(getLastHop()));
    }

    @Override public void setLastHop(RsvpLspHop lastHop) {
        if (getLastHop() == null && lastHop != null) {
            Node ingressNode = get(Attr.INGRESS_NODE);
            if (ingressNode == null) {
                throw new ConfigurationException(Attr.INGRESS_NODE.getName() + " を設定してください.");
            }

            if (ingressNode != lastHop.getSrcPort().getNode()) {
                throw new ConfigurationException(Attr.INGRESS_NODE.getName() + " が不整合です.");
            }
        }

        lastHop_.set(lastHop);
    }

    @Override public RsvpLspHop getLastHop() {
        return lastHop_.get();
    }

    public RsvpLspHop getFirstHop() {
        return NaefMvoUtils.getFirstHop(getLastHop());
    }

    public Port getSrcPort() {
        RsvpLspHop firstHop = getFirstHop();
        return firstHop == null
            ? null
            : firstHop.getSrcPort();
    }

    public Node getIngressNode() {
        return get(Attr.INGRESS_NODE);
    }

    public Port getDstPort() {
        RsvpLspHop lastHop = getLastHop();
        return lastHop == null
            ? null
            : lastHop.getDstPort();
    }

    public Node getEgressNode() {
        return getDstPort() == null ? null : getDstPort().getNode();
    }
}
