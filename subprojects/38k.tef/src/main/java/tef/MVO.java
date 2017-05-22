package tef;

import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * TEF におけるオブジェクトの抽象基底クラスです。
 * <p>
 * サブタイプ内に定義できるフィールドのアクセス修飾子には以下の制約があります:
 * <ul>
 * <li><code>MvoField</code> 以外の型のフィールドの場合: <code>static</code> または
 * <code>transient</code> を指定する必要があります。
 * <li>{@link MvoField} の場合: <code>final</code> を指定する必要があります。
 * また、<code>static</code> および <code>transient</code> を指定することはできません。
 * </ul>
 * <p>
 * また、サブタイプには {@link MVO#MVO(MvoId) <code>MvoId</code>を引数に取るコンストラクタ}
 * とそれ以外のコンストラクタを最低一つ定義する必要があります。
 */
public abstract class MVO {

    /**
     * <p>
     * MVO を識別する TefService 内で一意な ID です。
     */
    public static final class MvoId implements java.io.Serializable {

        private static final long serialVersionUID = 0L;

        private static final char DELIMITER_CHAR = '-';
        private static final String DELIMITER_STR = "-";
        private static final int RADIX = 16;

        public static MvoId getInstanceByGlobalId(String mvoIdStr) {
            String[] tokens = mvoIdStr.split(DELIMITER_STR);
            if (tokens.length != 3) {
                throw new IllegalArgumentException("invalid global mvo-id format: '" + mvoIdStr + "'");
            }

            String tefServiceId = tokens[0];
            TransactionId.W transactionId = new TransactionId.W(Integer.parseInt(tokens[1], RADIX));
            int transactionLocalSerial = Integer.parseInt(tokens[2], RADIX);
            return new MvoId(tefServiceId, transactionId, transactionLocalSerial);
        }

        public static MvoId getInstanceByLocalId(final String mvoIdStr) {
            int delimiterCount = 0;
            int txId = 0;
            int txLocalSerial = 0;
            for (int i = 0; i < mvoIdStr.length(); i++) {
                final char c = mvoIdStr.charAt(i);
                if (c == DELIMITER_CHAR) {
                    delimiterCount++;
                } else {
                    final int digit = Character.digit(c, RADIX);

                    switch (delimiterCount) {
                        case 0:
                            txId *= RADIX;
                            txId += digit;
                            break;
                        case 1:
                            txLocalSerial *= RADIX;
                            txLocalSerial += digit;
                            break;
                    }
                }
            }
            if (delimiterCount != 1) {
                throw new IllegalArgumentException(mvoIdStr);
            }

            return new MvoId(new TransactionId.W(txId), txLocalSerial);
        }

        final String tefServiceId;
        final TransactionId.W transactionId;
        final int transactionLocalSerial;

        MvoId(TransactionId.W transactionId, int transactionLocalSerial) {
            this(TefService.instance().getServiceName(), transactionId, transactionLocalSerial);
        }

        private MvoId(String tefServiceId, TransactionId.W transactionId, int transactionLocalSerial) {
            this.tefServiceId = tefServiceId;
            this.transactionId = transactionId;
            this.transactionLocalSerial = transactionLocalSerial;

            if (this.tefServiceId == null) {
                throw new IllegalArgumentException();
            }
        }

        public boolean isLocalObjectId() {
            return tefServiceId.equals(TefService.instance().getServiceName());
        }

        public String getGlobalStringExpression() {
            return tefServiceId + DELIMITER_STR + getLocalStringExpression();
        }

        public String getLocalStringExpression() {
            return Integer.toString(transactionId.serial, RADIX)
                    + DELIMITER_STR
                    + Integer.toString(transactionLocalSerial, RADIX);
        }

        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }

            MvoId another = (MvoId) obj;
            return tefServiceId.equals(another.tefServiceId)
                    && transactionId.equals(another.transactionId)
                    && transactionLocalSerial == another.transactionLocalSerial;
        }

        public int hashCode() {
            return tefServiceId.hashCode() + transactionId.serial + transactionLocalSerial;
        }

        public String toString() {
            return getLocalStringExpression();
        }
    }

    /**
     * <p>
     * MVO のサブタイプ内に定義できる永続化フィールドです。アクセス修飾子は
     * <code>final</code> を指定する必要があります。また <code>static</code> および
     * <code>transient</code> を指定することはできません。
     * <p>
     * <code>MvoField</code> へは以下の種類のオブジェクトを代入することができます。
     * <ul>
     * <li> プリミティブ ラッパー
     * <li> <code>java.lang.String</code>
     * <li> 列挙型 (enum)
     * <li> {@link MVO}
     * <li> {@link DateTime}
     * </ul>
     * <p>
     * MvoField の操作の隔離性水準はシリアライザブルです。
     * また、MvoField の読取/書込操作はスレッド安全です。
     * <p>
     * MvoField が保持する値は書込値と読取値で同一性の保証はされないことに注意してください。
     * これはメモリ効率を上げるため、内部で <code>String.intern()</code> のように同値オブジェクト
     * への置換が行われる場合があるためです。
     * <p>
     * たとえば型引数 X の F1&lt;X&gt; フィールド f1 があり、X 型のオブジェクト x があったとします。
     * このとき、<code>f1.set(x)</code> とした後の同値性評価式 <code>x.equals(f1.get())</code>
     * の結果は必ず <code>true</code> になりますが、同一性評価式 <code>x==f1.get()</code> は
     * <code>false</code> になる場合があるということです。なお、X が MVO (のサブタイプ)
     * である場合は同一性評価も必ず <code>true</code> になることが保証されます。
     */
    public static interface MvoField {

        /**
         * <p>
         * この MvoField をフィールドとして定義している MVO を返します。
         */
        public MVO getParent();

        /**
         * <p>
         * この MvoField のフィールド名を返します。
         */
        public String getFieldName();

        /**
         * <p>
         * この MvoField が最後に変更されたバージョンを返します。
         */
        public TransactionId.W getLatestVersion();
    }

    static interface F1HistoryElementProcessor {

        public void preprocess(F1 target);

        public void postprocess(F1 target);

        public MonaxisEngine.HistoryElementProcessor getMonaxisProcessor();
    }

    /**
     * <p>
     * F0 フィールドまたは S0 フィールドに対するアクセスに問題がある場合に発生する例外です。
     */
    public static abstract class IllegalZeroDFieldAccessException extends RuntimeException {

        /**
         * <p>
         * 初期化されていない F0/S0 フィールドに対して読取を行った場合に発生する例外です。
         */
        public static class NotInitialized extends IllegalZeroDFieldAccessException {

            NotInitialized(F0 f0) {
                super(f0);
            }

            NotInitialized(S0 s0) {
                super(s0);
            }
        }

        /**
         * <p>
         * 既に初期化済の F0/S0 フィールドに対して書込を行った場合に発生する例外です。
         */
        public static class AlreadyInitialized extends IllegalZeroDFieldAccessException {

            AlreadyInitialized(F0 f0) {
                super(f0);
            }

            AlreadyInitialized(S0 s0) {
                super(s0);
            }
        }

        /**
         * <p>
         * 生成トランザクション以外から F0/S0 フィールドに対して書込を行った場合に発生する例外
         * です。
         */
        public static class ZeroDFieldIsImmutable extends IllegalZeroDFieldAccessException {

            ZeroDFieldIsImmutable(F0 f0) {
                super(f0);
            }

            ZeroDFieldIsImmutable(S0 s0) {
                super(s0);
            }
        }

        IllegalZeroDFieldAccessException(MvoField field) {
            super(field.getParent().getClass().getName() + "." + field.getFieldName());
        }
    }

    /**
     * <p>
     * 単一の値を保持する不変({@link tef.TimeDimension#ZERO 時間次元0})の MVO フィールドです。
     * <p>
     * F0 フィールドは生成されたトランザクション、すなわちこの F0 フィールドを定義する MVO
     * インスタンスを生成したトランザクション内で1回だけ値を設定する必要があります (0回でも複数回
     * でもエラーが発生します)。
     */
    protected final class F0<T> implements MvoField, AxislessField {

        private T value_;

        /**
         * <p>
         * インデクシングしない F0 フィールドを作成します。
         */
        public F0() {
        }

        /**
         * <p>
         * ユニーク インデックスを使用した F0 フィールドを作成します。
         */
        public F0(MvoHome.F0UniqueIndex index) {
            index.addInitializedF0(this);
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        public TransactionId.W getLatestVersion() {
            return getParent().getInitialVersion();
        }

        /**
         * <p>
         * このフィールドの値を返します。
         *
         * @throws IllegalZeroDFieldAccessException.NotInitialized 未初期化の場合
         */
        @TimeDimensioned(TimeDimension.ZERO)
        public T get() {
            mutexLock();
            try {
                Transaction transaction = TransactionContext.getContextTransaction();
                if (transaction.getId() == getParent().getInitialVersion()) {
                    if (!transaction.hasInitialized(this)) {
                        throw new IllegalZeroDFieldAccessException.NotInitialized(F0.this);
                    }
                }

                return TefUtils.mutableGuard(value_);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドの値を設定します。
         *
         * @throws IllegalZeroDFieldAccessException.AlreadyInitialized    既に値が初期化済である場合
         * @throws IllegalZeroDFieldAccessException.ZeroDFieldIsImmutable 生成トランザクション以外
         *                                                                の場合
         */
        @TimeDimensioned(TimeDimension.ZERO)
        public void initialize(T value) {
            mutexLock();
            try {
                getParent().validateAccess();

                Transaction transaction = TransactionContext.getContextTransaction();
                if (transaction.getId().serial > getParent().getInitialVersion().serial) {
                    throw new IllegalZeroDFieldAccessException.ZeroDFieldIsImmutable(F0.this);
                }

                if (transaction.hasInitialized(this)) {
                    throw new IllegalZeroDFieldAccessException.AlreadyInitialized(F0.this);
                }

                value_ = TefUtils.mutableGuard(value);

                transaction.addInitializedF0(this);

                MvoHome.F0UniqueIndex index = TefService.instance().getIndexes().getF0UniqueIndex(this);
                if (index != null) {
                    index.put(getParent());
                }
            } finally {
                mutex.unlock();
            }
        }
    }

    /**
     * <p>
     * 単一の値を保持する{@link tef.TimeDimension#ONE 時間次元1}の MVO フィールドです。
     */
    protected final class F1<T> extends MonaxisEngine<T> implements MvoField, MonaxisField<T> {

        /**
         * <p>
         * インデクシングしない F1 フィールドを作成します。
         */
        public F1() {
            super(TefService.instance().getRestoringMode());
        }

        /**
         * <p>
         * ユニーク インデックスを使用した F1 フィールドを作成します。
         */
        public F1(MvoHome.F1UniqueIndex index) {
            super(TefService.instance().getRestoringMode());

            index.addInitializedF1(this);
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreHistoryIfNeeded() {
            mutexLock();
            try {
                if (isHistoryInitialized()) {
                    return;
                }

                MvoBulkDump.restoreF1(this);
            } finally {
                mutex.unlock();
            }
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        /**
         * <p>
         * このフィールドの値を返します。
         * <p>
         * コンテキスト トランザクションの指定バージョンを設定することで過去の値を
         * 取得することができます。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final T get() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                return super.getValue();
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドの値を設定します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final void set(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                super.setValue(value);

                TransactionContext.getContextTransaction().addLog(this);

                MvoHome.F1UniqueIndex index
                        = TefService.instance().getIndexes().getF1UniqueIndex(this);
                if (index != null) {
                    index.put(getParent());
                }
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                HistoryElement commitTarget = getLastElement();
                if (commitTarget == null) {
                    throw new IllegalStateException();
                }
                if (commitTarget.transaction != TransactionContext.getContextTransaction()) {
                    throw new IllegalStateException();
                }
                logger.getF1Logger().write(this, commitTarget.getValue());

                checkStrictMonotonicIncreasing();
            } finally {
                mutex.unlock();
            }
        }

        final void rollback() {
            mutexLock();
            try {
                rollbackCurrentTxElems();

                checkStrictMonotonicIncreasing();
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * この F1 フィールドの変更履歴のビューを返します。
         *
         * @return 変化が起きたバージョンとそのバージョンにおけるフィールドの値のマップ。
         */
        @TimeDimensioned(TimeDimension.NONE)
        public final SortedMap<TransactionId.W, T> getVersions() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                final SortedMap<TransactionId.W, T> result = new TreeMap<TransactionId.W, T>();
                HistoryElementProcessor versionsBuilder = new HistoryElementProcessor() {

                    @Override
                    public void process(Transaction historyElementTransaction, Object historyElementValue) {
                        result.put((TransactionId.W) historyElementTransaction.getId(), (T) historyElementValue);
                    }
                };
                super.traverseHistory(
                        versionsBuilder,
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                return result;
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(F1HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                processor.preprocess(this);
                super.traverseHistory(
                        processor.getMonaxisProcessor(),
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocess(this);
            } finally {
                mutex.unlock();
            }
        }
    }

    static interface F2HistoryElementProcessor {

        public void preprocess(F2 target);

        public void postprocess(F2 target);

        public BinaxesEngine.HistoryElementProcessor getBinaxesProcessor();
    }

    /**
     * <p>
     * 単一の値を保持する{@link tef.TimeDimension#TWO 時間次元2}の MVO フィールドです。
     */
    protected final class F2<T> extends BinaxesEngine<T> implements MvoField, BinaxesField<T> {

        public F2() {
            super(TefService.instance().getRestoringMode());
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreHistoryIfNeeded() {
            mutexLock();
            try {
                if (isHistoryInitialized()) {
                    return;
                }

                MvoBulkDump.restoreF2(this);
            } finally {
                mutex.unlock();
            }
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        /**
         * <p>
         * このフィールドの値を返します。
         * <p>
         * コンテキスト トランザクションで指定された指定バージョンと指定時刻の影響を受けます。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final T get() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                return super.getValue();
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドの値を設定します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final void set(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                super.setValue(value);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                JournalWriter.F2Logger f2Logger = logger.getF2Logger();
                for (HistoryElement commitTarget : super.getCommitTargets()) {
                    f2Logger.write(this, commitTarget.binaxesArg.time, commitTarget.getValue());
                }
            } finally {
                mutex.unlock();
            }
        }

        final void rollback() {
            mutexLock();
            try {
                super.rollback();
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * この F2 フィールドの時間変化のビューを返します。
         *
         * @return 変化が起きる時間とその時間におけるフィールドの値のマップ。
         */
        @TimeDimensioned(TimeDimension.ONE)
        @Override
        public final SortedMap<Long, T> getChanges() {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                return super.getChanges();
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.TWO)
        public final SortedMap<Long, T> getHereafterChanges() {
            return getChanges(TimeBoundaryType.HEREAFTER);
        }

        @TimeDimensioned(TimeDimension.TWO)
        public final SortedMap<Long, T> getFutureChanges() {
            return getChanges(TimeBoundaryType.AFTER);
        }

        @TimeDimensioned(TimeDimension.TWO)
        private final SortedMap<Long, T> getChanges(TimeBoundaryType timeBoundaryType) {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                if (timeBoundaryType == null
                        || (!(timeBoundaryType.equals(TimeBoundaryType.HEREAFTER)
                        || timeBoundaryType.equals(TimeBoundaryType.AFTER)))) {
                    throw new IllegalArgumentException();
                }

                SortedMap<Long, T> allChanges = getChanges();

                long targetTime = TransactionContext.getTargetTime();

                SortedMap<Long, T> result = new TreeMap<Long, T>();
                if (timeBoundaryType.equals(TimeBoundaryType.HEREAFTER)) {
                    result.put(targetTime, get());
                }
                for (Map.Entry<Long, T> entry : allChanges.entrySet()) {
                    long time = entry.getKey();
                    T value = entry.getValue();

                    if (targetTime < time) {
                        result.put(time, value);
                    }
                }

                return result;
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.TWO)
        public final Set<T> getAllHereafter() {
            Set<T> result = new HashSet<T>();
            for (Map.Entry<Long, T> entry : getHereafterChanges().entrySet()) {
                result.add(entry.getValue());
            }
            return result;
        }

        @TimeDimensioned(TimeDimension.TWO)
        public final Set<T> getAllFuture() {
            Set<T> result = new HashSet<T>();
            for (Map.Entry<Long, T> entry : getFutureChanges().entrySet()) {
                result.add(entry.getValue());
            }
            return result;
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(F2HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreHistoryIfNeeded();

                processor.preprocess(this);
                super.traverseHistory(
                        processor.getBinaxesProcessor(),
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocess(this);
            } finally {
                mutex.unlock();
            }
        }
    }

    protected final class S0<T> implements MvoField {

        private T[] values_;

        public S0() {
        }

        @Override
        public final MVO getParent() {
            return MVO.this;
        }

        @Override
        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        @Override
        public TransactionId.W getLatestVersion() {
            return getParent().getInitialVersion();
        }

        @TimeDimensioned(TimeDimension.ZERO)
        T[] getAsArray() {
            mutexLock();
            try {
                Transaction transaction = TransactionContext.getContextTransaction();
                if (transaction.getId() == getParent().getInitialVersion()) {
                    if (!transaction.hasInitialized(this)) {
                        throw new IllegalZeroDFieldAccessException.NotInitialized(S0.this);
                    }
                }

                return (T[]) TefUtils.arrayShallowCopy(values_);
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.ZERO)
        public List<T> get() {
            mutexLock();
            try {
                Transaction transaction = TransactionContext.getContextTransaction();
                if (transaction.getId() == getParent().getInitialVersion()) {
                    if (!transaction.hasInitialized(this)) {
                        throw new IllegalZeroDFieldAccessException.NotInitialized(S0.this);
                    }
                }

                return Arrays.asList(values_);
            } finally {
                mutex.unlock();
            }
        }

        public void initialize(T[] values) {
            initializeImpl(values);
        }

        void initializeImpl(Object values) {
            mutexLock();
            try {
                getParent().validateAccess();

                Transaction transaction = TransactionContext.getContextTransaction();
                if (transaction.getId().serial > getParent().getInitialVersion().serial) {
                    throw new IllegalZeroDFieldAccessException.ZeroDFieldIsImmutable(S0.this);
                }

                if (transaction.hasInitialized(this)) {
                    throw new IllegalZeroDFieldAccessException.AlreadyInitialized(S0.this);
                }

                values_ = (T[]) TefUtils.arrayShallowCopy(values);

                transaction.addInitializedS0(this);
            } finally {
                mutex.unlock();
            }
        }
    }

    static interface S1HistoryElementProcessor
            extends MonaxisMapEngine.MonaxisMapHistoryElementProcessor {
        public void preprocessS1(S1 target);

        public void postprocessS1(S1 target);
    }

    /**
     * <p>
     * 集合を保持する{@link tef.TimeDimension#ONE 時間次元1}の MVO フィールドです。
     */
    protected final class S1<T>
            extends MonaxisMapEngine<T, Boolean>
            implements MvoField, MonaxisSet<T> {
        public S1() {
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreElementsIfNeeded() {
            mutexLock();
            try {
                if (isEntriesInitialized()) {
                    return;
                }

                MvoBulkDump.restoreS1(this);
            } finally {
                mutex.unlock();
            }
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        /**
         * <p>
         * このフィールドが保持する集合を返します。
         * <p>
         * コンテキスト トランザクションの指定バージョンを設定することで過去の値を
         * 取得することができます。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final Set<T> get() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getKeysSetOf(Boolean.TRUE);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合に要素を追加します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final void add(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(value, Boolean.TRUE);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void addAll(T[] values) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                for (int i = 0; i < values.length; i++) {
                    add(values[i]);
                }
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合から要素を削除します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final void remove(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(value, Boolean.FALSE);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合から全ての要素を削除します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final void clear() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                for (T value : get()) {
                    remove(value);
                }
            } finally {
                mutex.unlock();
            }
        }

        public final void replaceAll(T[] values) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                clear();
                addAll(values);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * 指定された要素がこのフィールドが保持する集合に含まれる場合に true を返します。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final boolean contains(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                Boolean contains = super.getValue(value);
                return contains != null && contains.booleanValue();
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                super.commit(this, logger.getS1Logger());
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(S1HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                processor.preprocessS1(this);
                super.traverseHistory(
                        processor,
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocessS1(this);
            } finally {
                mutex.unlock();
            }
        }
    }

    static interface M1HistoryElementProcessor
            extends MonaxisMapEngine.MonaxisMapHistoryElementProcessor {
        public void preprocessM1(M1 target);

        public void postprocessM1(M1 target);
    }

    protected final class M1<K, V>
            extends MonaxisMapEngine<K, V>
            implements MvoField, MonaxisMap<K, V> {
        public M1() {
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreElementsIfNeeded() {
            mutexLock();
            try {
                if (isEntriesInitialized()) {
                    return;
                }

                MvoBulkDump.restoreM1(this);
            } finally {
                mutex.unlock();
            }
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        public final Set<K> getKeys() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getKeysSet();
            } finally {
                mutex.unlock();
            }
        }

        public final boolean containsKey(K key) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.hasEntry(key);
            } finally {
                mutex.unlock();
            }
        }

        public final V get(K key) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getValue(key);
            } finally {
                mutex.unlock();
            }
        }

        public final void put(K key, V value) {
            if (key != null && key.getClass().isArray()) {
                throw new IllegalArgumentException();
            }

            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(key, value);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void remove(K key) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                if (!containsKey(key)) {
                    return;
                }

                super.removeEntry(key);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void clear() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                for (K key : getKeys()) {
                    remove(key);
                }
            } finally {
                mutex.unlock();
            }
        }

        public final List<V> getValues() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                List<V> result = new ArrayList<V>();
                for (K key : getKeys()) {
                    result.add(get(key));
                }
                return result;
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                super.commit(this, logger.getM1Logger());
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(M1HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                processor.preprocessM1(this);
                super.traverseHistory(
                        processor,
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocessM1(this);
            } finally {
                mutex.unlock();
            }
        }
    }

    static interface S2HistoryElementProcessor
            extends BinaxesMapEngine.BinaxesMapHistoryElementProcessor {
        public void preprocessS2(S2 target);

        public void postprocessS2(S2 target);
    }

    /**
     * <p>
     * 集合を保持する{@link tef.TimeDimension#TWO 時間次元2}の MVO フィールドです。
     * 時間変化する集合を表現できます。
     */
    protected final class S2<T>
            extends BinaxesMapEngine<T, Boolean>
            implements MvoField, BinaxesSet<T> {
        public S2() {
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreElementsIfNeeded() {
            mutexLock();
            try {
                if (isEntriesInitialized()) {
                    return;
                }

                MvoBulkDump.restoreS2(this);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合を返します。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final List<T> get() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getKeysListOf(Boolean.TRUE);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合に要素を追加します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final void add(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(value, Boolean.TRUE);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void addAll(T[] values) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                for (int i = 0; i < values.length; i++) {
                    add(values[i]);
                }
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合から要素を削除します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final void remove(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(value, Boolean.FALSE);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * このフィールドが保持する集合から全ての要素を削除します。
         * <p>
         * コンテキスト トランザクションの指定バージョンは実行中のトランザクションで
         * なければなりません(過去のバージョンを指定した値の設定はできません)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final void clear() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                List<T> currentValues = get();
                for (T value : currentValues) {
                    remove(value);
                }
            } finally {
                mutex.unlock();
            }
        }

        public final void replaceAll(T[] values) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                clear();
                addAll(values);
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                super.commit(this, logger.getS2Logger());
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(S2HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                processor.preprocessS2(this);
                super.traverseHistory(
                        processor,
                        TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocessS2(this);
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * 指定された要素がコンテキスト トランザクションの指定時刻における集合に
         * 含まれる場合 true を返します。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final boolean contains(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                Boolean contains = super.getValue(value);
                return contains != null && contains.booleanValue();
            } finally {
                mutex.unlock();
            }
        }

        public final Set<T> getComprehensiveSet() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                Set<T> result = new HashSet<T>();
                long backupedTargetTime = TransactionContext.getTargetTime();
                try {
                    for (Long time : getChangeTimes()) {
                        TransactionContext.setTargetTime(time);
                        result.addAll(get());
                    }
                } finally {
                    TransactionContext.setTargetTime(backupedTargetTime);
                }

                return result;
            } finally {
                mutex.unlock();
            }
        }

        public final Set<T> getAllFuture() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                long backupedTargetTime = TransactionContext.getTargetTime();
                try {
                    Set<T> result = new HashSet<T>();
                    for (Long time : getFutureChangeTimes()) {
                        TransactionContext.setTargetTime(time);
                        result.addAll(get());
                    }
                    return result;
                } finally {
                    TransactionContext.setTargetTime(backupedTargetTime);
                }
            } finally {
                mutex.unlock();
            }
        }

        public final Set<T> getAllHereafter() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                Set<T> result = new HashSet<T>();
                result.addAll(get());
                result.addAll(getAllFuture());
                return result;
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * この S2 集合の時間変化のビューを返します。
         *
         * @return 変化が起きる時間とその時間における集合のマップ。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final SortedMap<Long, List<T>> getChanges() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                long backupedTargetTime = TransactionContext.getTargetTime();
                try {
                    SortedMap<Long, List<T>> result = new TreeMap<Long, List<T>>();
                    List<T> previousValues = new ArrayList<T>();
                    for (Long time : getChangeOccurrenceTimesSet()) {
                        TransactionContext.setTargetTime(time);
                        List<T> values = get();
                        if (!isConsistedFromSameMembers(previousValues, values)) {
                            result.put(time, values);
                        }
                        previousValues = values;
                    }
                    return result;
                } finally {
                    TransactionContext.setTargetTime(backupedTargetTime);
                }
            } finally {
                mutex.unlock();
            }
        }

        private boolean isConsistedFromSameMembers(List<T> values1, List<T> values2) {
            if (values1.size() != values2.size()) {
                return false;
            }
            List<T> list = new ArrayList<T>(values1);
            for (T value : values2) {
                list.remove(value);
            }
            return list.size() == 0;
        }

        @Override
        protected final List<Long> getChangeOccurrenceTimesSet() {
            mutexLock();
            try {
                return super.getChangeOccurrenceTimesSet();
            } finally {
                mutex.unlock();
            }
        }

        public final SortedMap<Long, Boolean> getFutureChanges(T value) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                long targetTime = TransactionContext.getTargetTime();
                SortedMap<Long, Boolean> changes = super.getChanges(value);
                SortedMap<Long, Boolean> result = new TreeMap<Long, Boolean>();
                if (changes != null) {
                    for (Long time : changes.keySet()) {
                        if (targetTime < time.longValue()) {
                            result.put(time, changes.get(time));
                        }
                    }
                }
                return result;
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * S2 集合の要素が変化する時間のリストを返します。
         */
        @TimeDimensioned(TimeDimension.ONE)
        public final List<Long> getChangeTimes() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return new ArrayList<Long>(getChanges().keySet());
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * S2 コンテキスト トランザクションの指定時刻より将来における集合の要素が変化する
         * 時間のリストを返します(コンテキスト トランザクションの指定時刻を含みません)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final List<Long> getFutureChangeTimes() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                List<Long> result = new ArrayList<Long>();
                long targetTime = TransactionContext.getTargetTime();
                for (Long time : getChangeTimes()) {
                    if (targetTime < time.longValue()) {
                        result.add(time);
                    }
                }
                return result;
            } finally {
                mutex.unlock();
            }
        }

        /**
         * <p>
         * S2 コンテキスト トランザクションの指定時刻および将来における集合の要素が変化する
         * 時間のリストを返します(コンテキスト トランザクションの指定時刻を含みます)。
         */
        @TimeDimensioned(TimeDimension.TWO)
        public final List<Long> getHereafterChangeTimes() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                List<Long> result = new ArrayList<Long>();
                result.add(TransactionContext.getTargetTime());
                result.addAll(getFutureChangeTimes());
                return result;
            } finally {
                mutex.unlock();
            }
        }
    }

    static interface M2HistoryElementProcessor
            extends BinaxesMapEngine.BinaxesMapHistoryElementProcessor {
        public void preprocessM2(M2 target);

        public void postprocessM2(M2 target);
    }

    protected final class M2<K, V>
            extends BinaxesMapEngine<K, V>
            implements MvoField, BinaxesMap<K, V> {
        public M2() {
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }

        private final void restoreElementsIfNeeded() {
            mutexLock();
            try {
                if (isEntriesInitialized()) {
                    return;
                }

                MvoBulkDump.restoreM2(this);
            } finally {
                mutex.unlock();
            }
        }

        public final MVO getParent() {
            return MVO.this;
        }

        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        public final List<K> getKeys() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getKeysList();
            } finally {
                mutex.unlock();
            }
        }

        public final V get(K key) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getValue(key);
            } finally {
                mutex.unlock();
            }
        }

        public final void put(K key, V value) {
            if (key != null && key.getClass().isArray()) {
                throw new IllegalArgumentException();
            }

            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.putEntry(key, value);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void remove(K key) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.removeEntry(key);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                super.commit(this, logger.getM2Logger());
            } finally {
                mutex.unlock();
            }
        }

        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.NONE)
        final void traverseHistory(M2HistoryElementProcessor processor) {
            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                processor.preprocessM2(this);
                super.traverseHistory(processor, TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocessM2(this);
            } finally {
                mutex.unlock();
            }
        }

        @TimeDimensioned(TimeDimension.ONE)
        public SortedMap<Long, V> getValueChanges(K key) {
            final SortedMap<Long, V> result = new TreeMap<Long, V>();
            final K targetKey = key;
            BinaxesMapHistoryElementProcessor traverser = new BinaxesMapHistoryElementProcessor() {

                private boolean isTarget_ = false;
                private BinaxesEngine.HistoryElementProcessor valueEntryProcessor
                        = new BinaxesEngine.HistoryElementProcessor() {
                    @Override
                    public void process(
                            Transaction historyElementTransaction,
                            long historyElementTargetTime,
                            Object historyElementValue) {
                        if (!isTarget_) {
                            return;
                        }
                        result.put(historyElementTargetTime, (V) historyElementValue);
                    }
                };

                @Override
                public void preprocessEntry(Object key) {
                    isTarget_ = key == targetKey;
                }

                @Override
                public void postprocessEntry(Object key) {
                    isTarget_ = false;
                }

                @Override
                public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor() {
                    return valueEntryProcessor;
                }
            };

            mutexLock();
            try {
                validateAccess();
                restoreElementsIfNeeded();

                super.traverseHistory(traverser, TransactionHistoryTraverseMode.AFFECTED_BY_TARGET_VERSION);
            } finally {
                mutex.unlock();
            }

            return result;
        }
    }

    static interface N2HistoryElementProcessor
            extends BinaxesNMapEngine.BinaxesNMapHistoryElementProcessor {
        public void preprocessN2(N2 target);

        public void postprocessN2(N2 target);
    }

    protected final class N2<K, V>
            extends BinaxesNMapEngine<K, V>
            implements MvoField {
        public N2() {
        }

        @Override
        public final MVO getParent() {
            return MVO.this;
        }

        @Override
        public final String getFieldName() {
            return MvoMeta.getFieldName(this);
        }

        public final List<K> getKeys() {
            mutexLock();
            try {
                validateAccess();

                return super.getKeyList();
            } finally {
                mutex.unlock();
            }
        }

        public final List<V> get(K key) {
            mutexLock();
            try {
                validateAccess();

                return super.getValueList(key);
            } finally {
                mutex.unlock();
            }
        }

        public final Set<V> getValues() {
            mutexLock();
            try {
                validateAccess();

                return super.getValuesImpl();
            } finally {
                mutex.unlock();
            }
        }

        public final Set<V> getHereafterValues() {
            mutexLock();
            try {
                validateAccess();

                return super.getHereafterValuesImpl();
            } finally {
                mutex.unlock();
            }
        }

        public final void add(K key, V value) {
            mutexLock();
            try {
                validateAccess();

                super.putEntry(key, value);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void remove(K key, V value) {
            mutexLock();
            try {
                validateAccess();

                super.removeValue(key, value);

                TransactionContext.getContextTransaction().addLog(this);
            } finally {
                mutex.unlock();
            }
        }

        public final void removeKey(K key) {
            throw new RuntimeException("not supported.");
        }

        final void commit(JournalWriter logger) {
            mutexLock();
            try {
                super.commit(this, logger.getN2Logger());
            } finally {
                mutex.unlock();
            }
        }

        @Override
        public final TransactionId.W getLatestVersion() {
            mutexLock();
            try {
                validateAccess();

                return super.getLatestVersion();
            } finally {
                mutex.unlock();
            }
        }

        final void traverseHistory(N2HistoryElementProcessor processor) {
            if (processor == null) {
                return;
            }

            mutexLock();
            try {
                validateAccess();

                processor.preprocessN2(this);
                super.traverseHistory(processor, TransactionHistoryTraverseMode.DO_NOT_AFFECTED_BY_TARGET_VERSION);
                processor.postprocessN2(this);
            } finally {
                mutex.unlock();
            }
        }

        private final void validateAccess() {
            getParent().validateAccess();
        }
    }

    static interface MvoFieldProcessor {

        public void preprocess(MVO mvo);

        public void postprocess(MVO mvo);

        public F1HistoryElementProcessor getF1Processor();

        public S1HistoryElementProcessor getS1Processor();

        public M1HistoryElementProcessor getM1Processor();

        public F2HistoryElementProcessor getF2Processor();

        public S2HistoryElementProcessor getS2Processor();

        public M2HistoryElementProcessor getM2Processor();

        public N2HistoryElementProcessor getN2Processor();
    }

    private static final Random random__ = new Random();

    private transient MvoId mvoId_;

    private transient Transaction writeLock_;

    private transient Transaction lastWriteLock_;

    private final transient ReentrantLock mutex = new ReentrantLock();

    /**
     * <p>
     * TEF サービス起動時にフレームワークがオブジェクトを復元するために使用する
     * コントラクタです。アプリケーションがこのコンストラクタを呼び出すことはできません。
     * <p>
     * サブタイプはこのコンストラクタと同じシグニチャを持つコンストラクタを定義し、
     * その中でスーパー タイプの同じシグニチャのコンストラクタを呼び出す必要があります。
     * そのコンストラクタ定義が無い場合、オブジェクト生成時に
     * {@link tef.MvoClassFormatException.ConstructorDefinition} が発生します。
     */
    protected MVO(MvoId mvoId) {
        Transaction transaction = TransactionContext.getContextTransaction();
        if (!transaction.isRestoredTransaction()) {
            throw new IllegalStateException("this constructor is for the framework.");
        }
        if (transaction.getId().serial != mvoId.transactionId.serial) {
            throw new IllegalStateException("wrong mvo-id.");
        }

        synchronized (mutex) {
            mvoId_ = mvoId;

            TefService.instance().getMvoRegistry().registForTransactionRestoration(MVO.this);
        }
    }

    /**
     * <p>
     * アプリケーションが使用する通常のオブジェクト生成のためのコンストラクタです。
     * サブタイプにはこのコンストラクタを呼び出すコンストラクタを最低一つ定義してください。
     */
    protected MVO() {
        TefService.instance().getMvoMeta().validateClassFormat(getClass());
        Transaction.validateWriteContext();

        Transaction transaction = TransactionContext.getContextTransaction();
        int serial = transaction.countNewObjects();

        synchronized (mutex) {
            mvoId_ = new MvoId((TransactionId.W) transaction.getId(), serial);
            writeLock_ = transaction;

            TefService.instance().getMvoRegistry().regist(MVO.this);
            TransactionContext.getContextTransaction().logNew(this);

            transaction.addLockingMvo(this);
        }
    }

    private void mutexLock() {
        while (!mutex.tryLock()) {
            try {
                Thread.sleep(random__.nextInt(100) + 100);
            } catch (InterruptedException ie) {
            }
        }
    }

    final void rollbackNew() {
        synchronized (mutex) {
            mvoId_ = null;
        }
    }

    private final void validateAccess() {
        Transaction transaction = TransactionContext.getContextTransaction();

        checkPhantomAccess();
        checkConcurrentWriteTransaction(transaction);

        if (transaction.isToKill()) {
            throw new TransactionKilledException();
        }
    }

    private final void checkPhantomAccess() {
        if (isPhantom()) {
            throw new PhantomAccessException();
        }
    }

    private final void checkConcurrentWriteTransaction(Transaction transaction) {
        if (transaction.isReadOnly()
                || TefService.instance().isInitializing()
                || TefService.instance().getRunningMode() != TefService.RunningMode.MASTER) {
            return;
        }

        mutexLock();
        try {
            if (writeLock_ == null) {
                if (lastWriteLock_ != null
                        && lastWriteLock_.getId().serial > transaction.getId().serial) {
                    throw new SuperSerializableException();
                }

                writeLock_ = transaction;
                transaction.addLockingMvo(this);
            } else if (writeLock_ != transaction) {
                throw new ConcurrentTransactionException(
                        getMvoId().getLocalStringExpression()
                                + ":" + writeLock_.getId().serial
                                + "-" + transaction.getId().serial);
            }
        } finally {
            mutex.unlock();
        }
    }

    /**
     * <p>
     * このMVOを識別する MvoId を返します。
     */
    public MvoId getMvoId() {
        checkPhantomAccess();

        synchronized (mutex) {
            return mvoId_;
        }
    }

    int getTransactionLocalSerial() {
        synchronized (mutex) {
            return mvoId_.transactionLocalSerial;
        }
    }

    final void commitLock() {
        if (TefService.instance().isInitializing()) {
            return;
        }

        mutexLock();
        try {
            Transaction transaction = TransactionContext.getContextTransaction();

            if (writeLock_ != transaction) {
                throw new IllegalStateException(
                        (writeLock_ == null ? "null" : writeLock_.getId().getIdString()) + "-" + transaction.getId());
            }

            if (lastWriteLock_ != null
                    && lastWriteLock_.getId().serial > writeLock_.getId().serial) {
                throw new IllegalStateException();
            }

            lastWriteLock_ = writeLock_;
            writeLock_ = null;
        } finally {
            mutex.unlock();
        }
    }

    final void rollbackLock() {
        if (TefService.instance().isInitializing()) {
            return;
        }

        mutexLock();
        try {
            if (writeLock_ == null) {
                return;
            }

            Transaction transaction = TransactionContext.getContextTransaction();
            if (writeLock_ != transaction) {
                throw new IllegalStateException();
            }

            writeLock_ = null;
        } finally {
            mutex.unlock();
        }
    }

    /**
     * <p>
     * この MVO が最後に更新されたバージョンを返します。
     */
    public TransactionId.W getLatestVersion() {
        validateAccess();

        mutexLock();
        try {
            TransactionId.W result = getInitialVersion();
            for (MvoField field : MvoMeta.getMvoFieldObjects(this)) {
                TransactionId.W fieldLatestTransactionId = field.getLatestVersion();
                if (fieldLatestTransactionId != null
                        && fieldLatestTransactionId.serial > result.serial) {
                    result = fieldLatestTransactionId;
                }
            }
            return result;
        } finally {
            mutex.unlock();
        }
    }

    final boolean isPhantom() {
        synchronized (mutex) {
            return mvoId_ == null;
        }
    }

    /**
     * <p>
     * この MVO が生成されたバージョンを返します。
     */
    public TransactionId.W getInitialVersion() {
        synchronized (mutex) {
            return (TransactionId.W) mvoId_.transactionId;
        }
    }

    final void traverseMvoFields(MvoFieldProcessor mvoFieldProcessor) {
        validateAccess();

        mutexLock();
        try {
            mvoFieldProcessor.preprocess(this);

            for (MvoField field : MvoMeta.getMvoFieldObjects(this)) {
                if (field instanceof F0 || field instanceof S0) {
                } else if (field instanceof F1) {
                    ((F1) field).traverseHistory(mvoFieldProcessor.getF1Processor());
                } else if (field instanceof S1) {
                    ((S1) field).traverseHistory(mvoFieldProcessor.getS1Processor());
                } else if (field instanceof M1) {
                    ((M1) field).traverseHistory(mvoFieldProcessor.getM1Processor());
                } else if (field instanceof F2) {
                    ((F2) field).traverseHistory(mvoFieldProcessor.getF2Processor());
                } else if (field instanceof S2) {
                    ((S2) field).traverseHistory(mvoFieldProcessor.getS2Processor());
                } else if (field instanceof M2) {
                    ((M2) field).traverseHistory(mvoFieldProcessor.getM2Processor());
                } else if (field instanceof N2) {
                    ((N2) field).traverseHistory(mvoFieldProcessor.getN2Processor());
                } else {
                    throw new RuntimeException(field.getClass().getName());
                }
            }

            mvoFieldProcessor.postprocess(this);
        } finally {
            mutex.unlock();
        }
    }

    final Transaction getLockingTransaction() {
        synchronized (mutex) {
            return writeLock_;
        }
    }

    static final void phantomCheck(Object o) throws PhantomAccessException {
        if ((o != null) && (o instanceof MVO) && ((MVO) o).isPhantom()) {
            throw new PhantomAccessException();
        }
    }

    protected <T extends tef.logic.BusinessLogic> T logic(Class<T> logicClass) {
        return TefService.instance().logic(logicClass);
    }
}
