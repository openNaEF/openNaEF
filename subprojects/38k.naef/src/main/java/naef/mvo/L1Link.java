package naef.mvo;

public class L1Link
    extends P2pLink<Port>
    implements Network.Exclusive, Network.Containee
{
    private final F2<L2Link> owner_ = new F2<L2Link>();

    public L1Link(MvoId id) {
        super(id);
    }

    public L1Link(Port port1, Port port2) {
        super(port1, port2);
    }

    @Override public void setContainer(Container container) {
        if (container != null) {
            if (! (container instanceof L2Link)) {
                throw new IllegalArgumentException("container type error.");
            }
        }

        super.setOwner(owner_, (L2Link) container);
    }

    @Override public L2Link getCurrentContainer() {
        return super.getCurrentOwner(owner_);
    }
}
