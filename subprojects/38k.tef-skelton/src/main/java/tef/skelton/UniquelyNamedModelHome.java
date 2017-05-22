package tef.skelton;

import tef.MVO;
import tef.MvoHome;

import java.util.List;

public interface UniquelyNamedModelHome<T extends MVO & NamedModel> {

    public Class<T> getType();
    public List<T> list();
    public T getByName(String name);

    public class Indexed<T extends MVO & NamedModel>
        extends MvoHome<T>
        implements UniquelyNamedModelHome<T>
    {
        private final F1UniqueIndex nameIndex_ = new F1UniqueIndex();

        public Indexed(Class<T> type) {
            super(type);
        }

        @Override public T getByName(String name) {
            return nameIndex_.get(name);
        }

        public F1UniqueIndex nameIndex() {
            return nameIndex_;
        }
    }

    public class SharedNamespace<T extends MVO & NamedModel>
        extends MvoHome<T>
        implements UniquelyNamedModelHome<T>
    {
        private final UniquelyNamedModelHome<? super T> sharedNamespace_;

        public SharedNamespace(UniquelyNamedModelHome<? super T> sharedNamespace, Class<T> type){
            super(type);

            sharedNamespace_ = sharedNamespace;
        }

        @Override public T getByName(String name) {
            MVO namedObj = sharedNamespace_.getByName(name);
            return getType().isInstance(namedObj) ? getType().cast(namedObj) : null;
        }
    }
}
