package naef.mvo.vxlan;

import tef.skelton.ConstraintException;
import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public class VxlanIdPool
    extends IdPool.SingleMap<VxlanIdPool, Long, Vxlan>
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<VxlanIdPool> home
        = new UniquelyNamedModelHome.Indexed<VxlanIdPool>(VxlanIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    public VxlanIdPool(MvoId id) {
        super(id);
    }

    public VxlanIdPool() {
    }

    @Override public void setName(String name) throws ConstraintException {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public Long parseId(String str) throws FormatException {
        return ValueResolver.parseLong(str, true);
    }

    @Override public Range<Long> parseRange(String str) throws FormatException {
        return Range.Long.gainByRangeStr(str);
    }
}
