package naef.mvo.mpls;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;

public class PseudowireStringIdPool 
    extends IdPool.SingleMap<PseudowireStringIdPool, String, Pseudowire> 
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<PseudowireStringIdPool> home
        = new UniquelyNamedModelHome.Indexed<PseudowireStringIdPool>(PseudowireStringIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected PseudowireStringIdPool(MvoId id) {
        super(id);
    }

    public PseudowireStringIdPool() {
    }

    @Override public void setName(String name) {
        name_.set(name);
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public String parseId(String str) throws FormatException {
        return str;
    }

    @Override public Range.String parseRange(String str) throws FormatException {
        return Range.String.gainByRangeStr(str);
    }
}
