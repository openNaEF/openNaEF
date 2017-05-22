package tef;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 特定の型の MVO の集合を保持するオブジェクトです。
 * <p>
 * 1階層までの継承が可能です。
 * この制約は MvoField とインデックスを結びつけるメカニズムの動作を保証するために必要なものです。
 */
public class MvoHome<T extends MVO> {

    public static class HomeConstraintsException extends RuntimeException {

        public HomeConstraintsException() {
        }

        public HomeConstraintsException(String reason) {
            super(reason);
        }
    }

    public static class UniqueIndexDuplicatedKeyFoundException extends HomeConstraintsException {

        public UniqueIndexDuplicatedKeyFoundException() {
        }

        public UniqueIndexDuplicatedKeyFoundException(String message) {
            super(message);
        }
    }

    public abstract class UniqueIndex {

        private IndexEngine<Object, T> indexBody_ = new IndexEngine<Object, T>() {

            protected Object getKey(T value) {
                return MvoHome.UniqueIndex.this.getKey(value);
            }
        };

        T get(Object key) {
            T result = null;
            for (T value : indexBody_.get(key)) {
                if (!isActive(value)) {
                    continue;
                }
                if (result != null) {
                    throw new UniqueIndexDuplicatedKeyFoundException
                            ("key:" + key + ", value:" + result.getMvoId() + "," + value.getMvoId());
                } else {
                    result = value;
                }
            }
            return result;
        }

        void put(T mvo) throws UniqueIndexDuplicatedKeyFoundException {
            if (!TransactionContext.getContextTransaction().isRestoredTransaction()) {
                Transaction.validateWriteContext();

                Object key = getKey(mvo);
                T current = get(key);
                if (current != null && current != mvo) {
                    throw new UniqueIndexDuplicatedKeyFoundException
                            ("key:" + key + ", value:" + current.getMvoId() + "," + mvo.getMvoId());
                }
            }

            indexBody_.put(mvo);
        }

        abstract Object getKey(T mvo);
    }

    /**
     * <p>F0 に対して設定できる一意制約です。
     * <p>MvoHome のサブタイプ内に final フィールドとして宣言し、インデクシング対象の
     * F0 のコンストラクタ パラメータに指定します。
     */
    public class F0UniqueIndex extends UniqueIndex {

        private Field f0_;

        final synchronized void initialize(Field f0) {
            if (f0_ == null) {
                f0_ = f0;
            } else {
                if (f0_ != f0) {
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        final synchronized Object getKey(T mvo) {
            if (f0_ == null) {
                TefService.instance().getIndexes().processInitializedMvoFields();

                if (f0_ == null) {
                    throw new IllegalStateException();
                }
            }

            try {
                return ((MVO.F0) f0_.get(mvo)).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public final T get(Object key) {
            return super.get(key);
        }

        final void addInitializedF0(MVO.F0 f0) {
            TefService.instance().getIndexes().addInitializedF0(f0, this);
        }
    }

    /**
     * <p>F1 に対して設定できる一意制約です。
     * <p>MvoHome のサブタイプ内に final フィールドとして宣言し、
     * インデクシング対象の F1 のコンストラクタ パラメータに指定します。
     */
    public class F1UniqueIndex extends UniqueIndex {

        private Field f1_;

        final synchronized void initialize(Field f1) {
            if (f1_ == null) {
                f1_ = f1;
            } else {
                if (f1_ != f1) {
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        final synchronized Object getKey(T mvo) {
            if (f1_ == null) {
                TefService.instance().getIndexes().processInitializedMvoFields();

                if (f1_ == null) {
                    throw new IllegalStateException();
                }
            }

            try {
                return ((MVO.F1) f1_.get(mvo)).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public T get(Object key) {
            return super.get(key);
        }

        final void addInitializedF1(MVO.F1 f1) {
            TefService.instance().getIndexes().addInitializedF1(f1, this);
        }
    }

    private Class<T> type_;
    private List<T> committeds_;
    private List<T> uncommitteds_;

    private Transaction lock_;

    private Transaction lastLock_;

    public MvoHome(Class<T> type) {
        if (!(this.getClass() == MvoHome.class
                || this.getClass().getSuperclass() == MvoHome.class)) {
            throw new MvoClassFormatException("MvoHome hierarchy error.");
        }

        if (type == null) {
            throw new IllegalArgumentException();
        }

        type_ = type;
        committeds_ = new ArrayList<T>();
        uncommitteds_ = new ArrayList<T>();

        TefService.instance().getMvoRegistry().registHome(MvoHome.this);
    }

    public final Class<T> getType() {
        return type_;
    }

    private final void checkConcurrentWriteTransaction() throws ConcurrentTransactionException {
        Transaction transaction = TransactionContext.getContextTransaction();

        if (transaction.isReadOnly()
                || TefService.instance().isInitializing()
                || TefService.instance().getRunningMode() != TefService.RunningMode.MASTER) {
            return;
        }

        synchronized (this) {
            if (lock_ == null) {
                if (lastLock_ != null
                        && lastLock_.getId().serial > transaction.getId().serial) {
                    throw new SuperSerializableException();
                }

                lock_ = transaction;
            } else if (lock_ != transaction) {
                throw new ConcurrentTransactionException();
            }
        }
    }

    final void registForTransactionRestoration(T mvo) {
        TefService.instance().checkTransactionRestoringThread();

        committeds_.add(mvo);
    }

    final synchronized void regist(T mvo) {
        checkConcurrentWriteTransaction();

        if (!type_.isInstance(mvo)) {
            throw new IllegalArgumentException();
        }

        uncommitteds_.add(mvo);
    }

    final synchronized void commitRegist() {
        checkConcurrentWriteTransaction();

        committeds_.addAll(uncommitteds_);
        uncommitteds_.clear();

        lastLock_ = lock_;
        lock_ = null;
    }

    final synchronized void rollbackRegist() {
        checkConcurrentWriteTransaction();

        uncommitteds_.clear();

        lock_ = null;
    }

    public synchronized List<T> list() {
        List<T> result = new ArrayList<T>();

        for (T mvo : committeds_) {
            if (MvoUtils.isExistingAt(mvo) && isActive(mvo)) {
                result.add(mvo);
            }
        }

        if (!TransactionContext.getContextTransaction().isReadOnly()) {
            checkConcurrentWriteTransaction();

            if (TransactionContext.getContextTransaction().getId().serial
                    <= TransactionContext.getTargetVersion().serial) {
                for (T mvo : uncommitteds_) {
                    if (isActive(mvo)) {
                        result.add(mvo);
                    }
                }
            }
        }

        return result;
    }

    synchronized final Transaction getLock() {
        return lock_;
    }

    /**
     * <p>
     * リストおよびインデックスの対象から除外したい場合、サブタイプで
     * <code>false</code>を返すように定義します。<code>false</code>の場合、{@link #list()},
     * {@link F0UniqueIndex#get(Object)}, {@link F1UniqueIndex#get(Object)}
     * の結果から除外されます。
     */
    protected boolean isActive(T mvo) {
        return true;
    }
}
