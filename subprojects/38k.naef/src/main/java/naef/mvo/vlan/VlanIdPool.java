package naef.mvo.vlan;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public abstract class VlanIdPool extends IdPool.SingleMap<VlanIdPool, Integer, Vlan> {

    public static class Dot1q  extends VlanIdPool implements NameConfigurableModel {

        public static final UniquelyNamedModelHome.Indexed<Dot1q> home
            = new UniquelyNamedModelHome.Indexed<Dot1q>(Dot1q.class);

        private final F1<String> name_ = new F1<String>(home.nameIndex());

        protected Dot1q(MvoId id) {
            super(id);
        }

        public Dot1q() {
        }

        @Override public void setName(String name) {
            name_.set(name);
        }

        @Override public String getName() {
            return name_.get();
        }

        @Override public Integer parseId(String str) throws FormatException {
            return ValueResolver.parseInteger(str, true);
        }

        @Override public Range.Integer parseRange(String str) throws FormatException {
            return Range.Integer.gainByRangeStr(str);
        }
    }

    protected VlanIdPool(MvoId id) {
        super(id);
    }

    protected VlanIdPool() {
    }
}
