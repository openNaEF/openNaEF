package naef.mvo.of;

import naef.mvo.AbstractNetwork;
import naef.mvo.Network;
import naef.mvo.Port;
import tef.MvoHome;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.IdAttribute;
import tef.skelton.IdPool;
import tef.skelton.IdPoolAttribute;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class OfPatchLink
    extends AbstractNetwork
    implements Network.LowerStackable
{
    public static class PatchIdPool
        extends IdPool.SingleMap<PatchIdPool, String, OfPatchLink>
        implements NameConfigurableModel
    {
        public static final UniquelyNamedModelHome.Indexed<PatchIdPool> home
            = new UniquelyNamedModelHome.Indexed<PatchIdPool>(PatchIdPool.class);

        private final F1<String> name = new F1<String>(home.nameIndex());

        protected PatchIdPool(MvoId id) {
            super(id);
        }

        public PatchIdPool() {
        }

        @Override public void setName(String name) {
            this.name.set(name);
        }

        @Override public String getName() {
            return name.get();
        }

        @Override public String parseId(String str) {
            return str;
        }

        @Override public Range.String parseRange(String str) {
            return Range.String.gainByRangeStr(str);
        }
    }

    public static final MvoHome<OfPatchLink> home = new MvoHome<OfPatchLink>(OfPatchLink.class);

    public static final IdPoolAttribute<PatchIdPool, OfPatchLink> PATCH_ID_POOL
        = new IdPoolAttribute<PatchIdPool, OfPatchLink>("naef.patch-id-pool", PatchIdPool.class);
    public static final Attribute<String, OfPatchLink> PATCH_ID
        = new IdAttribute<String, OfPatchLink, PatchIdPool>("naef.of-patch-link.id", AttributeType.STRING, PATCH_ID_POOL);

    private final S2<Network.UpperStackable> upperLayers = new S2<Network.UpperStackable>();
    private final F2<Port> patchPort1 = new F2<Port>();
    private final F2<Port> patchPort2 = new F2<Port>();

    public OfPatchLink(MvoId id) {
        super(id);
    }

    public OfPatchLink() {
    }

    @Override public void stackUnder(UpperStackable upperLayer) {
        super.stackUnder(upperLayers, upperLayer);
    }

    @Override public void unstackUnder(UpperStackable upperLayer) {
        super.stackUnder(upperLayers, upperLayer);
    }

    @Override public Set<? extends UpperStackable> getHereafterUpperLayers(boolean recursive) {
        return super.getHereafterUpperLayers(upperLayers, recursive);
    }

    @Override public Set<? extends UpperStackable> getCurrentUpperLayers(boolean recursive) {
        return super.getCurrentUpperLayers(upperLayers, recursive);
    }

    @Override public Collection<? extends Port> getCurrentMemberPorts() {
        return Collections.<Port>emptySet();
    }

    @Override public Collection<? extends Port> getCurrentAttachedPorts() {
        Set<Port> result = new LinkedHashSet<Port>(Arrays.<Port>asList(getPatchPort1(), getPatchPort2()));
        result.remove(null);
        return result;
    }

    public Port getPatchPort1() {
        return patchPort1.get();
    }

    public void setPatchPort1(Port port) {
        setPatchPort(this, patchPort1, port);
    }

    public void resetPatchPort1() {
        resetAttachedPort(this, patchPort1);
    }

    public Port getPatchPort2() {
        return patchPort2.get();
    }

    public void setPatchPort2(Port port) {
        setPatchPort(this, patchPort2, port);
    }

    public void resetPatchPort2() {
        resetAttachedPort(this, patchPort2);
    }

    private static void setPatchPort(final Network network, final F2<Port> attachedPortField, final Port port) {
        if (port != null) {
            if (! port.getHereafterNetworks(OfPatchLink.class).isEmpty()) {
                throw new ValueException("このポートにはすでにOF-Patch Linkが割り当てられています.");
            }
        }
        setAttachedPort(network, attachedPortField, port);
    }
}
