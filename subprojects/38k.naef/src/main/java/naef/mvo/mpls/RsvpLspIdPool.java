package naef.mvo.mpls;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;

public class RsvpLspIdPool
    extends IdPool.SingleMap<RsvpLspIdPool, String, RsvpLsp>
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<RsvpLspIdPool> home
        = new UniquelyNamedModelHome.Indexed<RsvpLspIdPool>(RsvpLspIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected RsvpLspIdPool(MvoId id) {
        super(id);
    }

    public RsvpLspIdPool() {
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public String parseId(String str) {
        return str;
    }

    @Override public Range.String parseRange(String str) throws FormatException {
        return Range.String.gainByRangeStr(str);
    }
}
