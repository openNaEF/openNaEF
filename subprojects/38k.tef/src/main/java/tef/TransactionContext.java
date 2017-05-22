package tef;

import java.util.ArrayList;
import java.util.List;

public final class TransactionContext {

    private TransactionContext() {
    }

    static final Transaction getContextTransaction()
            throws NoTransactionContextFoundException {
        return TransactionManager.Facade.getContextTransaction();
    }

    /**
     * 書込トランザクションを開始します。
     */
    public static final TransactionId.W beginWriteTransaction() {
        return beginWriteTransaction(null);
    }

    public static final TransactionId.W beginWriteTransaction(String transactionDesc) {
        TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

        StackTraceCatalog.CatalogId stacktraceCatalogId
                = TefService.instance().beginWriteTransactionStacktraceCatalog
                .getCurrentThreadStackTraceCatalogId(0);
        Transaction transaction
                = TransactionManager.Facade
                .beginStandaloneTransaction(false, false, stacktraceCatalogId);
        transaction.setTransactionDescription(transactionDesc);
        return (TransactionId.W) transaction.getId();
    }

    public static final TransactionId.W beginConcurrentWriteTransaction() {
        return beginConcurrentWriteTransaction(null);
    }

    public static final TransactionId.W beginConcurrentWriteTransaction(String transactionDesc) {
        TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

        StackTraceCatalog.CatalogId stacktraceCatalogId
                = TefService.instance().beginWriteTransactionStacktraceCatalog
                .getCurrentThreadStackTraceCatalogId(0);
        Transaction transaction
                = TransactionManager.Facade
                .beginStandaloneTransaction(false, true, stacktraceCatalogId);
        transaction.setTransactionDescription(transactionDesc);
        return (TransactionId.W) transaction.getId();
    }

    /*
     * 読取トランザクションを開始します。
     */
    public static final TransactionId.R beginReadTransaction() {
        return beginReadTransaction(null);
    }

    public static final TransactionId.R beginReadTransaction(String transactionDesc) {
        TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

        StackTraceCatalog.CatalogId stacktraceCatalogId
                = TefService.instance().beginReadTransactionStacktraceCatalog
                .getCurrentThreadStackTraceCatalogId(0);
        Transaction transaction
                = TransactionManager.Facade
                .beginStandaloneTransaction(true, false, stacktraceCatalogId);
        transaction.setTransactionDescription(transactionDesc);
        return (TransactionId.R) transaction.getId();
    }

    static final Transaction restoreTransaction
            (int transactionId, long beginTime, long committedTime, String txDesc) {
        Transaction transaction
                = TransactionManager.Facade
                .restoreTransaction(transactionId, beginTime, committedTime);
        transaction.setTransactionDescription(txDesc);
        return transaction;
    }

    public static final TransactionId setupReadTransaction() {
        TransactionId currentTransactionId = getTransactionId();
        if (currentTransactionId instanceof TransactionId.W) {
            return currentTransactionId;
        }

        if ((currentTransactionId instanceof TransactionId.R)
                && !TefService.instance().getReadTransactionRecycler()
                .isRecycleTransaction(Transaction.getContextTransaction())) {
            return currentTransactionId;
        }

        return setupRecycleReadTransaction();
    }

    public static final TransactionId.R setupRecycleReadTransaction() {
        return TefService.instance().getReadTransactionRecycler().setupTransaction();
    }

    /*
     * 分散トランザクションを開始します。
     */
    public static final GlobalTransactionId beginDistributedTransaction() {
        TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

        StackTraceCatalog.CatalogId stacktraceCatalogId
                = TefService.instance().beginDistributedTransactionStacktraceCatalog
                .getCurrentThreadStackTraceCatalogId(0);
        return TransactionManager.Facade.beginDistributedTransaction(stacktraceCatalogId);
    }

    public static final GlobalTransactionId getGlobalTransactionId() {
        return TransactionManager.Facade.getGlobalTransactionId(getContextTransaction());
    }

    public static final void suspendLocalTransaction() {
        getContextTransaction().suspend();
    }

    public static final void resumeLocalTransaction
            (GlobalTransactionId globalTransactionId) {
        Transaction localTransaction
                = TransactionManager.Facade.getLocalTransaction(globalTransactionId);
        if (localTransaction == null) {
            throw new NoTransactionContextFoundException();
        }

        localTransaction.resume();
    }

    private static StackTraceCatalog getCloseStackTraceCatalog() {
        return getCloseStackTraceCatalog(getContextTransaction());
    }

    private static StackTraceCatalog getCloseStackTraceCatalog(Transaction transaction) {
        if (transaction.isReadOnly()) {
            return TefService.instance().closeReadTransactionStacktraceCatalog;
        } else {
            return TefService.instance().closeWriteTransactionStacktraceCatalog;
        }
    }

    /**
     * 実行中のトランザクションをコミットして終了します。
     */
    public static final void commit() throws CommitFailedException {
        StackTraceCatalog.CatalogId stacktraceCatalogId
                = getCloseStackTraceCatalog().getCurrentThreadStackTraceCatalogId(0);

        TransactionManager.Facade.commit(stacktraceCatalogId);
    }

    public static final void killTransaction(TransactionId transactionId)
            throws NoTransactionContextFoundException {
        Transaction transaction = Transaction.getActiveTransaction(transactionId);
        if (transaction == null) {
            throw new NoTransactionContextFoundException();
        }

        transaction.kill();
    }

    /**
     * 実行中のトランザクションをロールバックして終了します。
     */
    public static final void rollback() {
        StackTraceCatalog.CatalogId stacktraceCatalogId
                = getCloseStackTraceCatalog().getCurrentThreadStackTraceCatalogId(0);

        TransactionManager.Facade.rollback(stacktraceCatalogId);
    }

    /**
     * トランザクションを終了します。
     * 書込トランザクションが実行中の場合はロールバックされます。
     */
    public static final void close() {
        close(Transaction.getContextTransaction());
    }

    static final void close(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        StackTraceCatalog.CatalogId stacktraceCatalogId
                = getCloseStackTraceCatalog(transaction)
                .getCurrentThreadStackTraceCatalogId(0);

        TransactionManager.Facade.close(transaction, stacktraceCatalogId);
    }

    /**
     * コンテキスト トランザクションのターゲット時刻をトランザクション開始時刻に変更します.
     * <p>
     * 現在時刻ではないことに注意が必要です.
     */
    public static final void resetTargetTime() {
        setTargetTime(getTransactionBeginTime());
    }

    /**
     * コンテキスト トランザクションのターゲット時刻を変更します.
     * <p>
     * null が指定された場合は resetTargetTime() の呼出と同じ意味になります.
     */
    public static final void setTargetTime(Long targetTime) {
        if (targetTime == null) {
            resetTargetTime();
        } else {
            getContextTransaction().setTargetTime(targetTime.longValue());
        }
    }

    /**
     * コンテキスト トランザクションのターゲット時刻を変更します。
     */
    public static final void setTargetTime(long targetTime) {
        getContextTransaction().setTargetTime(targetTime);
    }

    /**
     * コンテキスト トランザクションのターゲット時刻を変更します。
     */
    public static final void setTargetTime(DateTime time) {
        getContextTransaction().setTargetTime(time.getValue());
    }

    /**
     * コンテキスト トランザクションのターゲット時刻を返します。
     */
    public static final long getTargetTime() {
        return getContextTransaction().getTargetTime();
    }

    public static final void resetTargetVersion() {
        Transaction transaction = getContextTransaction();
        TransactionId.W targetVersion;
        if (transaction.isReadOnly()) {
            targetVersion = transaction.getBaseTransaction() == null
                    ? TransactionId.PREHISTORIC_TRANSACTION_ID
                    : (TransactionId.W) transaction.getBaseTransaction().getId();
        } else {
            targetVersion = (TransactionId.W) transaction.getId();
        }

        setTargetVersion(targetVersion);
    }

    /**
     * コンテキスト トランザクションのターゲット バージョンを変更します。
     */
    public static final void setTargetVersion(TransactionId.W targetVersion) {
        if (targetVersion == null) {
            resetTargetVersion();
        } else {
            getContextTransaction().setTargetVersion(targetVersion);
        }
    }

    /**
     * コンテキスト トランザクションのターゲット バージョンを返します。
     */
    public static final TransactionId.W getTargetVersion() {
        return getContextTransaction().getTargetVersion();
    }

    public static final long getTransactionBeginTime() {
        return getContextTransaction().getBeginTime();
    }

    /**
     * コンテキスト トランザクションのバージョンを返します.
     * <p>
     * トランザクションが実行されていない場合は null を返します.
     */
    public static final TransactionId getTransactionId() {
        return isTransactionRunning()
                ? getContextTransaction().getId()
                : null;
    }

    /**
     * コンテキスト トランザクションが recycled read であるかどうかを返します.
     * <p>
     * write transaction の場合は false を返します.
     */
    public static final boolean isRecycledReadTransaction() {
        return isTransactionRunning()
                && TefService.instance().getReadTransactionRecycler()
                .isRecycleTransaction(Transaction.getContextTransaction());
    }

    public static List<MVO> getNewObjects() {
        return getContextTransaction().getNewObjects();
    }

    public static List<MVO> getChangedObjects() {
        return getContextTransaction().getChangedObjects();
    }

    public static List<MVO.MvoField> getChangedFields() {
        return getContextTransaction().getChangedFields();
    }

    static final String getTransactionDescription() {
        return getContextTransaction().getTransactionDescription();
    }

    static final BinaxesEngine.BinaxesArg getBinaxesArg() {
        return getContextTransaction().getBinaxesArg();
    }

    /**
     * @deprecated isTransactionRunning() を使用してください.
     */
    @Deprecated
    public static boolean hasTransactionStarted() {
        return isTransactionRunning();
    }

    public static boolean isTransactionRunning() {
        return Transaction.getContextTransaction() != null;
    }

    public static TransactionId[] getActiveTransactionIds() {
        Transaction[] activeTransactions = Transaction.getActiveTransactions();

        List<TransactionId> result = new ArrayList<TransactionId>();
        for (int i = 0; i < activeTransactions.length; i++) {
            result.add(activeTransactions[i].getId());
        }

        return result.toArray(new TransactionId[0]);
    }

    public static long getActiveTransactionBeginTime(TransactionId transactionId)
            throws NoTransactionContextFoundException {
        Transaction transaction = Transaction.getActiveTransaction(transactionId);
        if (transaction == null) {
            throw new NoTransactionContextFoundException();
        }
        return transaction.getBeginTime();
    }

    public static Long getTransactionCommittedTime(TransactionId.W transactionId) {
        Transaction transaction = Transaction.getTransaction(transactionId);
        return transaction == null
                ? null
                : new Long(transaction.getCommittedTime());
    }

    public static String getTransactionDescription(TransactionId transactionId)
            throws NoTransactionContextFoundException {
        Transaction transaction = Transaction.getActiveTransaction(transactionId);
        if (transaction == null
                && transactionId instanceof TransactionId.W) {
            transaction = Transaction.getTransaction((TransactionId.W) transactionId);
        }

        if (transaction != null) {
            return transaction.getTransactionDescription();
        } else {
            throw new NoTransactionContextFoundException();
        }
    }

    /**
     * <p>
     * 現在実行中のトランザクションのベース トランザクションのIDを返します。ベース トランザクション
     * とはトランザクション開始時点でコミット済の最後の書込トランザクションです。つまり、ベース
     * トランザクションはトランザクション開始時に決定され、その後変わることがありません。
     * <p>
     * {@link getLastCommittedTransactionId() 最新トランザクションID} との違いに注意してください。
     */
    public static TransactionId.W getBaseTransactionId() {
        Transaction baseTransaction = getContextTransaction().getBaseTransaction();
        return baseTransaction == null
                ? null
                : (TransactionId.W) baseTransaction.getId();
    }

    /**
     * <p>
     * 最新トランザクション(最後にコミットされたトランザクション)のIDを返します。
     * <p>
     * 読取トランザクションの場合、トランザクション実行中に書込トランザクションがコミットされる
     * 場合があり、その場合は{@link getBaseTransactionId() ベース トランザクションID}と最新
     * トランザクションIDは異なる値となります。
     * <p>
     * 書込トランザクションではベース トランザクションと最新トランザクションは常に一致し、
     * トランザクション実行中に変化することはありません。
     * <p>
     * 別の言い方をすると{@link getBaseTransactionId() ベース トランザクション}はメソッドの呼出
     * スレッドに結び付けられた実行中のトランザクションに対して決定される「コンテキストフル」
     * なものであるのに対して、最新トランザクションは「コンテキストレス」であると言えます。
     */
    public static TransactionId.W getLastCommittedTransactionId() {
        Transaction lastCommittedTransaction = Transaction.getLastCommittedTransaction();
        return lastCommittedTransaction == null
                ? null
                : (TransactionId.W) lastCommittedTransaction.getId();
    }

    public static TransactionId.W getTransactionIdAt(long time) {
        Transaction transaction = Transaction.getTransactionAt(time);
        if (transaction == null) {
            return TransactionId.PREHISTORIC_TRANSACTION_ID;
        } else {
            return (TransactionId.W) transaction.getId();
        }
    }

    public static void putContextInfo(Object key, Object value) {
        getContextTransaction().putContextInfo(key, value);
    }

    public static Object getContextInfo(Object key) {
        return getContextTransaction().getContextInfo(key);
    }

    public static void putPostProcessorExtInfo(Object key, Object value) {
        getContextTransaction().putPostProcessorExtInfo(key, value);
    }

    public static Object getPostProcessorExtInfo(Object key) {
        return getContextTransaction().getPostProcessorExtInfo(key);
    }

    public static void addPostTransactionProcessor
            (PostTransactionProcessor postTransactionProcessor) {
        if (postTransactionProcessor == null) {
            throw new IllegalArgumentException("null argument.");
        }

        getContextTransaction().addPostTransactionProcessor(postTransactionProcessor);
    }

    public static List<PostTransactionProcessor> getPostTransactionProcessors() {
        return getContextTransaction().getPostTransactionProcessors();
    }

    public static <T> T execute(WriteTransaction<T> transaction)
            throws TransactionAbortedException {
        beginWriteTransaction(null);
        try {
            T result = transaction.execute();

            if (isTransactionRunning()) {
                commit();
            }

            return result;
        } finally {
            close();
        }
    }

    public static void execute(WriteTransaction.Resultless transaction)
            throws TransactionAbortedException {
        beginWriteTransaction(null);
        try {
            transaction.execute();

            if (isTransactionRunning()) {
                commit();
            }
        } finally {
            close();
        }
    }

    public static <T> T execute(ReadTransaction<T> transaction)
            throws TransactionAbortedException {
        beginReadTransaction(null);
        try {
            return transaction.execute();
        } finally {
            close();
        }
    }

    public static void execute(ReadTransaction.Resultless transaction)
            throws TransactionAbortedException {
        beginReadTransaction(null);
        try {
            transaction.execute();
        } finally {
            close();
        }
    }

    public static boolean isDistributedTransactionServiceAvailable() {
        return TransactionManager.Facade.isDistributedTransactionServiceAvailable();
    }

    public static void setTransactionDescription(String desc) {
        if (getContextTransaction().getTransactionDescription() != null) {
            throw new IllegalStateException();
        }
        getContextTransaction().setTransactionDescription(desc);
    }
}
