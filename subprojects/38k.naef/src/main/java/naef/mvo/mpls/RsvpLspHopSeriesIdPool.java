package naef.mvo.mpls;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;

public class RsvpLspHopSeriesIdPool
    extends IdPool.SingleMap<RsvpLspHopSeriesIdPool, String, RsvpLspHopSeries>
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<RsvpLspHopSeriesIdPool> home
        = new UniquelyNamedModelHome.Indexed<RsvpLspHopSeriesIdPool>(RsvpLspHopSeriesIdPool.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected RsvpLspHopSeriesIdPool(MvoId id) {
        super(id);
    }

    public RsvpLspHopSeriesIdPool() {
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
