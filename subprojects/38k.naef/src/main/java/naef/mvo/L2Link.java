package naef.mvo;

import java.util.HashSet;
import java.util.Set;

import tef.skelton.ValueException;

public abstract class L2Link<T extends Port>
    extends P2pLink<T>
    implements Network.Containee, Network.Container
{
    private final F2<Network.Container> owner_ = new F2<Network.Container>();
    private final S2<Network.Containee> parts_ = new S2<Network.Containee>();

    public L2Link(MvoId id) {
        super(id);
    }

    public L2Link(T port1, T port2) {
        super(port1, port2);
    }

    protected void assertAcceptableContainerType(Network.Container owner) {
        if (owner == null
            || getClass().isInstance(owner)
            || owner.getClass().isInstance(this))
        {
            return;
        }

        throw new ValueException("container type error.");
    }

    @Override public void setContainer(Network.Container container) {
        assertAcceptableContainerType(container);
        super.setOwner(owner_, container);
    }

    @Override public Network.Container getCurrentContainer() {
        return super.getCurrentOwner(owner_);
    }

    public L2Link<?> getNestingContainer() {
        Network.Container container = getCurrentContainer();
        return container != null && isNestingLink(container)
            ? (L2Link) container
            : null;
    }

    public boolean isNestingOutermost() {
        return getNestedLinks().size() > 0
            && getNestingContainer() == null;
    }

    private boolean isNestingLink(Network link) {
        return getClass().isInstance(link)
            || link.getClass().isInstance(this);
    }

    private void assertAcceptableContaineeType(Network.Containee part) {
        if (part instanceof L1Link
            || getClass().isInstance(part)
            || part.getClass().isInstance(this))
        {
            return;
        }

        throw new ValueException("containee type error.");
    }

    @Override public void addPart(Network.Containee part) {
        assertAcceptableContaineeType(part);
        super.addPart(parts_, part);
    }

    @Override public void removePart(Network.Containee part) {
        assertAcceptableContaineeType(part);
        super.removePart(parts_, part);
    }

    @Override public Set<Network.Containee> getHereafterParts(boolean recursive) {
        return super.getHereafterParts(parts_, recursive);
    }

    @Override public Set<Network.Containee> getCurrentParts(boolean recursive) {
        return super.getCurrentParts(parts_, recursive);
    }

    public Set<L2Link<?>> getNestedLinks() {
        Set<L2Link<?>> result = new HashSet<L2Link<?>>();
        for (Network.Containee part : getCurrentParts(false)) {
            if (isNestingLink(part)) {
                result.add((L2Link<?>) part);
            }
        }
        return result;
    }

    public boolean isNestedInnermost() {
        return getNestingContainer() != null
            && getNestedLinks().size() == 0;
    }
}
