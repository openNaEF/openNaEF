package naef.mvo.mpls;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public class PseudowireIdPool 
    extends IdPool.SingleMap<PseudowireIdPool, Long, Pseudowire> 
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<PseudowireIdPool> home
        = new UniquelyNamedModelHome.Indexed<PseudowireIdPool>(PseudowireIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected PseudowireIdPool(MvoId id) {
        super(id);
    }

    public PseudowireIdPool() {
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public Long parseId(String str) throws FormatException {
        return ValueResolver.parseLong(str, true);
    }

    @Override public Range.Long parseRange(String str) throws FormatException {
        return Range.Long.gainByRangeStr(str);
    }
}
