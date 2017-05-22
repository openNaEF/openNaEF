package naef.mvo.mpls;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.NodeElement;
import naef.mvo.Port;
import tef.MvoHome;
import tef.TransactionContext;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPoolAttribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Pseudowire 
    extends AbstractNetwork 
    implements Network.UpperStackable, Network.LowerStackable
{
    public enum TransportType {

        ETHERNET("Ethernet", naef.mvo.eth.EthPort.class, naef.mvo.vlan.VlanIf.class),
        ATM_AAL5("ATM AAL5", naef.mvo.atm.AtmPvcIf.class, naef.mvo.eth.EthPort.class, naef.mvo.vlan.VlanIf.class),
        ATM_CELL_RELAY("ATM Cell Relay", naef.mvo.atm.AtmPvpIf.class, naef.mvo.atm.AtmPvcIf.class);

        public final String displayName;
        private final List<Class<? extends Port>> allowedAcTypes_;

        TransportType(String displayName, Class<? extends Port>... allowedAcTypes) {
            this.displayName = displayName;
            allowedAcTypes_ = Collections.unmodifiableList(Arrays.asList(allowedAcTypes));
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<Class<? extends Port>> getAllowedAcTypes() {
            return allowedAcTypes_;
        }

        public boolean isAllowedAcType(Class<? extends Port> type) {
            return allowedAcTypes_.contains(type);
        }

        @Override public String toString() {
            return getDisplayName();
        }
    }

    public static class Attr {

        public static final Attribute.SingleBoolean<NodeElement> PSEUDOWIRE_ENABLED
            = new Attribute.SingleBoolean<NodeElement>("naef.enabled-networking-function.pseudowire");

        @Deprecated public static final Attribute.SingleAttr<PseudowireIdPool, Pseudowire> ID_POOL
            = new Attribute.SingleAttr<PseudowireIdPool, Pseudowire>("naef.pseudowire.id-pool", null);

        public static final IdPoolAttribute<PseudowireIdPool, Pseudowire> LONG_IDPOOL
            = new IdPoolAttribute<PseudowireIdPool, Pseudowire>(
                "naef.pseudowire.id-pool.long-type",
                PseudowireIdPool.class);
        public static final Attribute<Long, Pseudowire> LONG_ID
            = new IdAttribute<Long, Pseudowire, PseudowireIdPool>(
                "naef.pseudowire.id.long-type",
                AttributeType.LONG, LONG_IDPOOL);

        public static final IdPoolAttribute<PseudowireStringIdPool, Pseudowire> STRING_IDPOOL
            = new IdPoolAttribute<PseudowireStringIdPool, Pseudowire>(
                "naef.pseudowire.id-pool.string-type",
                PseudowireStringIdPool.class);
        public static final Attribute<String, Pseudowire> STRING_ID
            = new IdAttribute<String, Pseudowire, PseudowireStringIdPool>(
                "naef.pseudowire.id.string-type",
                AttributeType.STRING, STRING_IDPOOL);

        public static final Attribute.SingleEnum<TransportType, Pseudowire> TRANSPORT_TYPE
            = new Attribute.SingleEnum<TransportType, Pseudowire>(
                "naef.pseudowire.transport-type",
                TransportType.class);
        public static final Attribute.SingleString<Pseudowire> ROUTE_DISTINGUISHER
            = new Attribute.SingleString<Pseudowire>("naef.pseudowire.route-distinguisher");
    }

    public static final MvoHome<Pseudowire> home = new MvoHome<Pseudowire>(Pseudowire.class);

    private final S2<Network.UpperStackable> upperLayers_ = new S2<Network.UpperStackable>();
    private final S2<Network.LowerStackable> lowerLayers_ = new S2<Network.LowerStackable>();
    private final F2<Port> attachmentCircuit1_ = new F2<Port>();
    private final F2<Port> attachmentCircuit2_ = new F2<Port>();

    protected Pseudowire(MvoId id) {
        super(id);
    }

    public Pseudowire() {
    }

    @Override public Set<Port> getCurrentMemberPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public Set<Port> getCurrentAttachedPorts() {
        Set<Port> result = new HashSet<Port>();
        if (getAttachmentCircuit1() != null) {
            result.add(getAttachmentCircuit1());
        }
        if (getAttachmentCircuit2() != null) {
            result.add(getAttachmentCircuit2());
        }
        return result;
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
        super.stackOver(lowerLayers_, lowerLayer);
    }

    @Override public void unstackOver(Network.LowerStackable lowerLayer) {
        super.unstackOver(lowerLayers_, lowerLayer);
    }

    @Override public Set<? extends Network.LowerStackable> getHereafterLowerLayers(boolean recursive) {
        return super.getHereafterLowerLayers(lowerLayers_, recursive);
    }

    @Override public Set<? extends Network.LowerStackable> getCurrentLowerLayers(boolean recursive) {
        return super.getCurrentLowerLayers(lowerLayers_, recursive);
    }

    public Port getAttachmentCircuit1() {
        return attachmentCircuit1_.get();
    }

    public void setAttachmentCircuit1(Port port) {
        setAttachedPort(this, attachmentCircuit1_, port);
    }

    public void resetAttachmentCircuit1() {
        resetAttachedPort(this, attachmentCircuit1_);
    }

    public Port getAttachmentCircuit2() {
        return attachmentCircuit2_.get();
    }

    public void setAttachmentCircuit2(Port port) {
        setAttachedPort(this, attachmentCircuit2_, port);
    }

    public void resetAttachmentCircuit2() {
        resetAttachedPort(this, attachmentCircuit2_);
    }

    public TransportType getTransportType() {
        return get(Attr.TRANSPORT_TYPE);
    }
}
