package naef.mvo.wdm;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;

public class OpticalPathIdPool
    extends IdPool.SingleMap<OpticalPathIdPool, String, OpticalPath>
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<OpticalPathIdPool> home
        = new UniquelyNamedModelHome.Indexed<OpticalPathIdPool>(OpticalPathIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected OpticalPathIdPool(MvoId id) {
        super(id);
    }

    public OpticalPathIdPool() {
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
