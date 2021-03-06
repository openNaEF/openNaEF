package naef.mvo.vrf;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.NameConfigurableModel;
import tef.skelton.Range;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueResolver;

public abstract class VrfIdPool<T extends Object & Comparable<T>> 
    extends IdPool.SingleMap<VrfIdPool<T>, T, Vrf> 
{
    public static class StringType extends VrfIdPool<String> implements NameConfigurableModel {

        public static final UniquelyNamedModelHome.Indexed<StringType> home
            = new UniquelyNamedModelHome.Indexed<StringType>(StringType.class);

        private final F1<String> name_ = new F1<String>(home.nameIndex());

        protected StringType(MvoId id) {
            super(id);
        }

        public StringType() {
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

    public static class IntegerType extends VrfIdPool<Integer> implements NameConfigurableModel {

        public static final UniquelyNamedModelHome.Indexed<IntegerType> home
            = new UniquelyNamedModelHome.Indexed<IntegerType>(IntegerType.class);

        private final F1<String> name_ = new F1<String>(home.nameIndex());

        protected IntegerType(MvoId id) {
            super(id);
        }

        public IntegerType() {
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

    protected VrfIdPool(MvoId id) {
        super(id);
    }

    protected VrfIdPool() {
    }
}
