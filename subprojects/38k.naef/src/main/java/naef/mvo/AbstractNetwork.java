package naef.mvo;

import tef.TransactionContext;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.UiTypeName;
import tef.skelton.ValueException;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractNetwork extends AbstractModel implements Network {

    public static class Attr {

        public static final Attribute.SingleModel<NetworkType, Network> OBJECT_TYPE
            = new Attribute.SingleModel<NetworkType, Network>("naef.object-type", NetworkType.class)
        {
            @Override public void validateValue(Network model, NetworkType newValue) {
                super.validateValue(model, newValue);

                if (newValue != null) {
                    validateDeclaringTypeAcceptability(newValue, model);

                    NetworkType.Attr.ACCEPTABLE_PORT_TYPES.validateConstraint(model, newValue);
                    NetworkType.Attr.ACCEPTABLE_LOWER_TYPES.validateConstraint(model, newValue);
                    NetworkType.Attr.ACCEPTABLE_UPPER_TYPES.validateConstraint(model, newValue);
                    NetworkType.Attr.ACCEPTABLE_CONTAINER_TYPES.validateConstraint(model, newValue);
                    NetworkType.Attr.ACCEPTABLE_PART_TYPES.validateConstraint(model, newValue);
                }
            }

            private void validateDeclaringTypeAcceptability(NetworkType type, Network model) {
                for (UiTypeName acceptableType : NaefObjectType.Attr.ACCEPTABLE_DECLARING_TYPES.snapshot(type)) {
                    if (acceptableType.type().isInstance(model)) {
                        return;
                    }
                }

                throw new ValueException(
                    "型制約 " + NaefObjectType.Attr.ACCEPTABLE_DECLARING_TYPES.getName() + " に適合しません.");
            }
        };

        public static final Attribute.SingleInteger<Network> MAX_CONFIGURABLE_PORTS
            = new Attribute.SingleInteger<Network>("naef.network.max-configurable-ports")
        {
            @Override public void validateValue(Network model, Integer value)
                throws ValueException, ConfigurationException
            {
                super.validateValue(model, value);

                if (! (model instanceof Network.MemberPortConfigurable)) {
                    throw new ConfigurationException("ポート構成変更不可です.");
                }
            }
        };
    }

    protected AbstractNetwork(MvoId id) {
        super(id);
    }

    protected AbstractNetwork() {
    }

    protected final <T extends Port> void addMemberPort(S2<T> ports, T port) {
        if (ports.contains(port) && ports.getFutureChanges(port).size() == 0) {
            return;
        }

        Integer maxConfigurablePorts = get(Attr.MAX_CONFIGURABLE_PORTS);
        if (maxConfigurablePorts != null
            && maxConfigurablePorts.intValue() <= ports.getAllHereafter().size())
        {
            throw new ConfigurationException("ポート数の上限に達しています: " + maxConfigurablePorts);
        }

        NetworkType.Attr.ACCEPTABLE_PORT_TYPES.validateValue(this, port);

        ports.add(port);
        port.joinNetwork(this);
    }

    protected final <T extends Port> void removeMemberPort(S2<T> ports, T port) {
        if (! ports.contains(port) && ports.getFutureChanges(port).size() == 0) {
            return;
        }

        ports.remove(port);
        port.disjoinNetwork(this);
    }

    protected final void stackUnder(S2<Network.UpperStackable> upperLayers, Network.UpperStackable upperLayer) {
        if (! (this instanceof LowerStackable)) {
            throw new UnsupportedOperationException();
        }
        if (upperLayer == this) {
            throw new IllegalArgumentException();
        }
        if (upperLayer instanceof Network.LowerStackable
            && ((Network.LowerStackable) upperLayer).getHereafterUpperLayers(true).contains(this))
        {
            throw new IllegalArgumentException();
        }

        if (upperLayers.contains(upperLayer)
            && upperLayers.getFutureChanges(upperLayer).size() == 0)
        {
            return;
        }

        NetworkType.Attr.ACCEPTABLE_UPPER_TYPES.validateValue(this, upperLayer);

        upperLayers.add(upperLayer);
        upperLayer.stackOver((LowerStackable) this);
    }

    protected final void unstackUnder(S2<Network.UpperStackable> upperLayers, Network.UpperStackable upperLayer) {
        if (! (this instanceof LowerStackable)) {
            throw new UnsupportedOperationException();
        }
        if (upperLayer == this) {
            throw new IllegalArgumentException();
        }

        if (! upperLayers.contains(upperLayer)
            && upperLayers.getFutureChanges(upperLayer).size() == 0)
        {
            return;
        }

        upperLayers.remove(upperLayer);
        upperLayer.unstackOver((LowerStackable) this);
    }

    protected final Set<? extends Network.UpperStackable> getHereafterUpperLayers(
        S2<Network.UpperStackable> upperLayers, boolean recursive)
    {
        Set<Network.UpperStackable> result = new HashSet<Network.UpperStackable>();
        for (Network.UpperStackable upperLayer : upperLayers.getAllHereafter()) {
            result.add(upperLayer);
            if (recursive && upperLayer instanceof Network.LowerStackable) {
                result.addAll(((Network.LowerStackable) upperLayer).getHereafterUpperLayers(true));
            }
        }
        return result;
    }

    protected final Set<? extends Network.UpperStackable> getCurrentUpperLayers(
        S2<Network.UpperStackable> upperLayers, boolean recursive)
    {
        Set<Network.UpperStackable> result = new HashSet<Network.UpperStackable>();
        for (Network.UpperStackable upperLayer : upperLayers.get()) {
            result.add(upperLayer);
            if (recursive && upperLayer instanceof Network.LowerStackable) {
                result.addAll(((Network.LowerStackable) upperLayer).getCurrentUpperLayers(true));
            }
        }
        return result;
    }

    protected final void stackOver(S2<Network.LowerStackable> lowerLayers, Network.LowerStackable lowerLayer) {
        if (! (this instanceof UpperStackable)) {
            throw new UnsupportedOperationException();
        }
        if (lowerLayer == this) {
            throw new IllegalArgumentException();
        }
        if (lowerLayer instanceof Network.UpperStackable
            && ((Network.UpperStackable) lowerLayer).getHereafterLowerLayers(true).contains(this))
        {
            throw new IllegalArgumentException();
        }

        if (lowerLayers.contains(lowerLayer)
            && lowerLayers.getFutureChanges(lowerLayer).size() == 0)
        {
            return;
        }

        NetworkType.Attr.ACCEPTABLE_LOWER_TYPES.validateValue(this, lowerLayer);

        lowerLayers.add(lowerLayer);
        lowerLayer.stackUnder((UpperStackable) this);
    }

    protected final void unstackOver(S2<Network.LowerStackable> lowerLayers, Network.LowerStackable lowerLayer) {
        if (! (this instanceof UpperStackable)) {
            throw new UnsupportedOperationException();
        }
        if (lowerLayer == this) {
            throw new IllegalArgumentException();
        }

        if (! lowerLayers.contains(lowerLayer)
            && lowerLayers.getFutureChanges(lowerLayer).size() == 0)
        {
            return;
        }

        lowerLayers.remove(lowerLayer);
        lowerLayer.unstackUnder((UpperStackable) this);
    }

    protected final Set<? extends Network.LowerStackable> getHereafterLowerLayers(
        S2<Network.LowerStackable> lowerLayers, boolean recursive)
    {
        Set<Network.LowerStackable> result = new HashSet<Network.LowerStackable>();
        for (Network.LowerStackable lowerLayer : lowerLayers.getAllHereafter()) {
            result.add(lowerLayer);
            if (recursive && lowerLayer instanceof Network.UpperStackable) {
                result.addAll(((Network.UpperStackable) lowerLayer).getHereafterLowerLayers(true));
            }
        }
        return result;
    }

    protected final Set<? extends Network.LowerStackable> getCurrentLowerLayers(
        S2<Network.LowerStackable> lowerLayers, boolean recursive)
    {
        Set<Network.LowerStackable> result = new HashSet<Network.LowerStackable>();
        for (Network.LowerStackable lowerLayer : lowerLayers.get()) {
            result.add(lowerLayer);
            if (recursive && lowerLayer instanceof Network.UpperStackable) {
                result.addAll(((Network.UpperStackable) lowerLayer).getCurrentLowerLayers(true));
            }
        }
        return result;
    }

    protected final <T extends Network.Containee> void addPart(S2<T> parts, T part) {
        if (! (this instanceof Container)) {
            throw new UnsupportedOperationException();
        }
        if (part == this) {
            throw new IllegalArgumentException();
        }

        if (part instanceof Network.Container
            && ((Network.Container) part).getHereafterParts(true).contains(this))
        {
            throw new IllegalArgumentException();
        }

        if (parts.contains(part)
            && parts.getFutureChanges(part).size() == 0)
        {
            return;
        }

        NetworkType.Attr.ACCEPTABLE_PART_TYPES.validateValue(this, part);

        parts.add(part);
        part.setContainer((Container) this);
    }

    protected final <T extends Network.Containee> void removePart(S2<T> parts, T part) {
        if (! (this instanceof Container)) {
            throw new UnsupportedOperationException();
        }
        if (! parts.contains(part)
            && parts.getFutureChanges(part).size() == 0)
        {
            return;
        }

        parts.remove(part);
        part.setContainer(null);
    }

    protected final <T extends Containee> Set<T> getHereafterParts(S2<T> parts, boolean recursive) {
        Set<T> result = new HashSet<T>();
        for (T part : parts.getAllHereafter()) {
            result.add(part);
            if (recursive && part instanceof Network.Container) {
                result.addAll((Set<T>) ((Network.Container) part).getHereafterParts(true));
            }
        }
        return result;
    }

    protected final <T extends Containee> Set<T> getCurrentParts(S2<T> parts, boolean recursive) {
        Set<T> result = new HashSet<T>();
        for (T part : parts.get()) {
            result.add(part);
            if (recursive && part instanceof Network.Container) {
                result.addAll((Set<T>) ((Network.Container) part).getCurrentParts(true));
            }
        }
        return result;
    }

    protected final <T extends Network.Container> void setOwner(F2<T> ownerField, T newOwner) {
        if (! (this instanceof Containee)) {
            throw new UnsupportedOperationException();
        }
        if (newOwner instanceof Network.Containee
            && ((Network.Containee) newOwner).getCurrentContainer() == this)
        {
            throw new IllegalArgumentException();
        }
        if (newOwner == null && ownerField.getFutureChanges().size() > 0) {
            throw new IllegalStateException();
        }

        if (ownerField.get() == newOwner
            && ownerField.getFutureChanges().size() == 0)
        {
            return;
        }

        NetworkType.Attr.ACCEPTABLE_CONTAINER_TYPES.validateValue(this, newOwner);

        Network.Container oldOwner = ownerField.get();

        ownerField.set(newOwner);

        if (oldOwner != null) {
            oldOwner.removePart((Containee) this);
        }
        if (newOwner != null) {
            newOwner.addPart((Containee) this);
        }
    }

    protected final <T extends Network.Container> T getCurrentOwner(F2<T> ownerField) {
        return ownerField.get();
    }

    protected static void setAttachedPort(final Network network, final F2<Port> attachedPortField, final Port port) {
        if (attachedPortField.get() == port && attachedPortField.getFutureChanges().size() == 0) {
            return;
        }

        resetAttachedPort(network, attachedPortField);

        attachedPortField.set(port);

        if (port != null) {
            port.joinNetwork(network);
        }
    }

    protected static void resetAttachedPort(final Network network, final F2<Port> attachedPortField) {
        for (Long time : attachedPortField.getHereafterChanges().keySet()) {
            long savedTime = TransactionContext.getTargetTime();
            try {
                TransactionContext.setTargetTime(time);

                if (attachedPortField.get() != null) {
                    attachedPortField.get().disjoinNetwork(network);

                    attachedPortField.set(null);
                }
            } finally {
                TransactionContext.setTargetTime(savedTime);
            }
        }
    }
}
