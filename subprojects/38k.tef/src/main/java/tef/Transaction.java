package tef;

import java.util.*;
import java.util.List;

final class Transaction {

    private static final class ActiveTransactions {

        private static final Object LOCK = new Object();

        private static final Map<Thread, Transaction> activeThreadToTransactionMapping__
                = new HashMap<Thread, Transaction>();
        private static final Map<Transaction, Thread> activeTransactionToThreadMapping__
                = new HashMap<Transaction, Thread>();
        private static final Map<Thread, String> originalThreadNames__
                = new HashMap<Thread, String>();
        private static final Set<Transaction> suspendedTransactions__
                = new HashSet<Transaction>();

        static void add(final Transaction transaction) {
            if (transaction == null) {
                throw new IllegalArgumentException();
            }

            if (getThread(transaction) != null) {
                throw new IllegalStateException();
            }

            if (getTransaction() != null) {
                throw new IllegalStateException();
            }

            final Thread transactionThread = Thread.currentThread();
            String originalThreadName = transactionThread.getName();

            synchronized (LOCK) {
                if (activeThreadToTransactionMapping__.get(transactionThread) != null) {
                    throw new IllegalStateException(
                            activeThreadToTransactionMapping__.get(transactionThread).getId().getIdString());
                }

                originalThreadNames__.put(transactionThread, originalThreadName);
                activeThreadToTransactionMapping__.put(transactionThread, transaction);
                activeTransactionToThreadMapping__.put(transaction, transactionThread);
            }

            String newThreadName
                    = "tef-transaction:" + transaction.getId()
                    + (originalThreadName.length() == 0
                    ? ""
                    : "(" + originalThreadName + ")");
            transactionThread.setName(newThreadName);
        }

        static void remove(Transaction transaction) {
            if (transaction == null) {
                throw new IllegalArgumentException();
            }

            Thread transactionThread = getThread(transaction);
            if (transactionThread == null) {
                throw new IllegalStateException();
            }

            String originalThreadName;
            synchronized (LOCK) {
                originalThreadName = originalThreadNames__.get(transactionThread);
                activeThreadToTransactionMapping__.remove(transactionThread);
                activeTransactionToThreadMapping__.remove(transaction);
                originalThreadNames__.remove(transactionThread);
            }

            if (originalThreadName == null) {
                throw new IllegalStateException();
            }
            transactionThread.setName(originalThreadName);
        }

        static void suspend(Transaction transaction) {
            if (transaction == null) {
                throw new IllegalArgumentException();
            }
            if (isSuspending(transaction)) {
                throw new IllegalStateException();
            }

            remove(transaction);
            synchronized (LOCK) {
                suspendedTransactions__.add(transaction);
            }
        }

        static void resume(Transaction transaction) {
            if (transaction == null) {
                throw new IllegalArgumentException();
            }
            if (!isSuspending(transaction)) {
                throw new IllegalStateException();
            }

            synchronized (LOCK) {
                suspendedTransactions__.remove(transaction);
            }
            add(transaction);
        }

        static Transaction getTransaction() {
            synchronized (LOCK) {
                return activeThreadToTransactionMapping__.get(Thread.currentThread());
            }
        }

        static Thread getThread(Transaction transaction) {
            synchronized (LOCK) {
                return activeTransactionToThreadMapping__.get(transaction);
            }
        }

        static Transaction[] getActiveTransactions() {
            synchronized (LOCK) {
                return activeTransactionToThreadMapping__.keySet().toArray(new Transaction[0]);
            }
        }

        static boolean isSuspending(Transaction transaction) {
            synchronized (LOCK) {
                return suspendedTransactions__.contains(transaction);
            }
        }
    }

    static Transaction getTransaction() {
        return ActiveTransactions.getTransaction();
    }

    static Transaction getContextTransaction() {
        return getTransaction();
    }

    static Thread getThread(Transaction transaction) {
        return ActiveTransactions.getThread(transaction);
    }

    static Transaction[] getActiveTransactions() {
        return ActiveTransactions.getActiveTransactions();
    }

    static Transaction getActiveTransaction(TransactionId transactionId) {
        Transaction[] transactions = getActiveTransactions();
        for (int i = 0; i < transactions.length; i++) {
            if (transactions[i].getId().equals(transactionId)) {
                return transactions[i];
            }
        }
        return null;
    }

    private static class TransactionIdCounter {

        private int maxId_ = -1;

        synchronized int getNextId() {
            return ++maxId_;
        }

        synchronized int getCurrentId() {
            return maxId_;
        }

        synchronized void setCurrentId(int id) {
            maxId_ = id;
        }
    }

    private static TransactionIdCounter writeTransactionIdCounter__ = new TransactionIdCounter();

    static void setMaxWriteTransactionId(int id) {
        if (id <= writeTransactionIdCounter__.getCurrentId()) {
            throw new IllegalStateException();
        }

        writeTransactionIdCounter__.setCurrentId(id);
    }

    private static int maxReadTransactionId__ = -1;

    private static final List<Transaction> committedTransactions__ = new ArrayList<Transaction>();

    static Transaction[] getCommittedTransactions() {
        synchronized (committedTransactions__) {
            return committedTransactions__.toArray(new Transaction[0]);
        }
    }

    static Transaction getTransaction(TransactionId.W transactionId) {
        synchronized (committedTransactions__) {
            int index = Collections.binarySearch(
                    committedTransactions__,
                    transactionId,
                    new Comparator<Object>() {

                        @Override
                        public int compare(Object o1, Object o2) {
                            int serial1 = getTransactionSerial(o1);
                            int serial2 = getTransactionSerial(o2);

                            return serial1 - serial2;
                        }

                        private int getTransactionSerial(Object o) {
                            if (o instanceof TransactionId.W) {
                                return ((TransactionId.W) o).serial;
                            } else if (o instanceof Transaction) {
                                return ((Transaction) o).getId().serial;
                            } else {
                                throw new IllegalArgumentException();
                            }
                        }
                    });
            if (index < 0) {
                return null;
            } else {
                return committedTransactions__.get(index);
            }
        }
    }

    static Transaction getTransactionAt(long time) {
        synchronized (committedTransactions__) {
            if (committedTransactions__.size() == 0) {
                return null;
            }

            Transaction firstTransaction = committedTransactions__.get(0);
            if (time < firstTransaction.getCommittedTime()) {
                return null;
            }

            for (int i = committedTransactions__.size() - 1; i >= 0; i--) {
                Transaction transaction = committedTransactions__.get(i);
                if (transaction.getCommittedTime() <= time) {
                    return transaction;
                }
            }

            return null;
        }
    }

    static Transaction getLastCommittedTransaction() {
        synchronized (committedTransactions__) {
            if (committedTransactions__.size() == 0) {
                return null;
            } else {
                return committedTransactions__.get(committedTransactions__.size() - 1);
            }
        }
    }

    static interface TransactionCloseListener {

        public void notifyClosed(Transaction transaction);
    }

    private static final long VOID_COMMITTED_TIME = 0;

    private final TransactionId id_;
    private final boolean isConcurrentWrite_;
    private long beginTime_;
    private long committedTime_ = VOID_COMMITTED_TIME;
    private final boolean isRestoredTransaction_;

    private String transactionDescription_;

    static enum TransactionStatus {

        ROLLBACKED(new TransactionStatus[0]),
        ROLLING_BACK(new TransactionStatus[]{ROLLBACKED}),
        COMMITTED(new TransactionStatus[0]),
        PHASE1_COMMITTED(new TransactionStatus[]{COMMITTED, ROLLING_BACK}),
        RUNNING(new TransactionStatus[]{PHASE1_COMMITTED, ROLLING_BACK}),
        BEFORE_RUNNING(new TransactionStatus[]{RUNNING});

        private TransactionStatus[] acceptableSuccessors_;

        private TransactionStatus(TransactionStatus[] acceptableSuccessors) {
            acceptableSuccessors_ = acceptableSuccessors;
        }

        boolean isAcceptableSuccessor(TransactionStatus successor) {
            for (int i = 0; i < acceptableSuccessors_.length; i++) {
                if (acceptableSuccessors_[i] == successor) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class RunningContext {

        private final Transaction transaction;
        private Long currentTargetTime;
        private TransactionId.W currentTargetVersion;
        private Map<Long, BinaxesEngine.BinaxesArg> cachedBinaxesArgs = new HashMap<Long, BinaxesEngine.BinaxesArg>();
        private Map<Object, Object> contextInfo;

        private volatile TransactionStatus status_;
        private volatile boolean isToKill = false;
        private volatile boolean isClosing = false;

        private TransactionChangeLogger changeLogger;

        private Transaction baseTransaction;

        private Set<MVO> lockingMvos = new HashSet<MVO>();

        private Set<MVO.F0> initializedF0s = new HashSet<MVO.F0>();
        private Set<MVO.S0> initializedS0s = new HashSet<MVO.S0>();

        private List<PostTransactionProcessor> postTransactionProcessors = new ArrayList<PostTransactionProcessor>();

        private final long baseTimeNano;
        private final long baseCpuTimeNano;
        private final long baseUserTimeNano;

        private final Map<Object, Object> postProcessorExtInfos = new HashMap<Object, Object>();

        private Map<Object, MonaxisEngine.HistoryElement<?>> monaxisHistoryElements
                = new HashMap<Object, MonaxisEngine.HistoryElement<?>>();
        private Map<Object, Map<Long, BinaxesEngine.HistoryElement<?>>> binaxesHistoryElements
                = new HashMap<Object, Map<Long, BinaxesEngine.HistoryElement<?>>>();

        RunningContext(
                Transaction transaction,
                long baseTimeNano,
                long baseCpuTimeNano,
                long baseUserTimeNano) {
            this.transaction = transaction;
            status_ = TransactionStatus.BEFORE_RUNNING;
            this.baseTimeNano = baseTimeNano;
            this.baseCpuTimeNano = baseCpuTimeNano;
            this.baseUserTimeNano = baseUserTimeNano;
        }

        synchronized void initialize() {
            if (status_ != TransactionStatus.BEFORE_RUNNING) {
                throw new IllegalStateException();
            }

            baseTransaction = getLastCommittedTransaction();

            if (!transaction.isRestoredTransaction()) {
                if (!transaction.isReadOnly()) {
                    changeLogger = new TransactionChangeLogger(transaction);
                }
                contextInfo = new HashMap<Object, Object>();
            }

            currentTargetTime = new Long(transaction.getBeginTime());

            if (transaction.isReadOnly()) {
                currentTargetVersion = baseTransaction == null
                        ? TransactionId.PREHISTORIC_TRANSACTION_ID
                        : ((TransactionId.W) baseTransaction.getId());
            } else {
                currentTargetVersion = (TransactionId.W) transaction.getId();
            }

            setStatus(TransactionStatus.RUNNING);
        }

        synchronized void setStatus(TransactionStatus status) {
            if (!status_.isAcceptableSuccessor(status)) {
                throw new RuntimeException(status_.name() + " -> " + status.name());
            }
            status_ = status;
        }

        synchronized TransactionStatus getStatus() {
            return status_;
        }

        synchronized void close() {
            if (changeLogger != null) {
                changeLogger.close();
                changeLogger = null;
            }

            lockingMvos = null;

            cachedBinaxesArgs.clear();
            cachedBinaxesArgs = null;
            monaxisHistoryElements.clear();
            monaxisHistoryElements = null;
            binaxesHistoryElements.clear();
            binaxesHistoryElements = null;

            if (contextInfo != null) {
                contextInfo.clear();
                contextInfo = null;
            }

            initializedF0s = null;
            initializedS0s = null;
            postTransactionProcessors = null;
        }
    }

    private RunningContext runningContext_;

    static final boolean TRANSACTION_TYPE_STANDALONE = false;
    static final boolean TRANSACTION_TYPE_DISTRIBUTED = true;

    Transaction(
            boolean transactionType,
            boolean isReadOnly,
            boolean isConcurrentWrite,
            StackTraceCatalog.CatalogId stacktraceCatalogId) {
        final long baseTimeNano = System.nanoTime();
        final long baseCpuTimeNano = TefUtils.getCurrentThreadCpuTime();
        final long baseUserTimeNano = TefUtils.getCurrentThreadUserTime();

        if (!TefService.instance().isTefServiceAvailable()) {
            throw new IllegalStateException("tef service is not available.");
        }
        if (getContextTransaction() != null) {
            throw new IllegalStateException(
                    "nested transaction is not supported: running is " + getContextTransaction().getId());
        }
        if (!isReadOnly) {
            if (TefService.instance().getRunningMode() != TefService.RunningMode.MASTER) {
                throw new IllegalStateException("running-mode is not master.");
            }
            if (TefService.instance().getTransactionRestoringThread() != null) {
                throw new IllegalStateException();
            }
        }

        isRestoredTransaction_ = false;
        isConcurrentWrite_ = isConcurrentWrite;

        synchronized (this) {
            runningContext_ = new RunningContext(Transaction.this, baseTimeNano, baseCpuTimeNano, baseUserTimeNano);
        }

        if (!isReadOnly) {
            synchronized (writeTransactionIdCounter__) {
                id_ = new TransactionId.W(writeTransactionIdCounter__.getNextId());

                TefService.instance().getTransactionExecLogger()
                        .writeReceptionLog(Transaction.this, transactionType, stacktraceCatalogId);

                WriteTransactionExecutionController.getInstance().enqueue(Transaction.this);
            }
        } else {
            synchronized (Transaction.class) {
                id_ = new TransactionId.R(++maxReadTransactionId__);
            }

            TefService.instance().getTransactionExecLogger()
                    .writeReceptionLog(Transaction.this, transactionType, stacktraceCatalogId);
        }
    }

    Transaction(int id, long beginTime, long committedTime) {
        if (getContextTransaction() != null) {
            throw new IllegalStateException(
                    "nested transaction is not supported: running is " + getContextTransaction().getId());
        }

        TefService.instance().checkTransactionRestoringThread();

        isRestoredTransaction_ = true;
        isConcurrentWrite_ = false;

        if (id <= writeTransactionIdCounter__.getCurrentId()) {
            throw new Error("max=" + writeTransactionIdCounter__.getCurrentId() + ", new=" + id);
        }
        if (committedTime > System.currentTimeMillis()) {
            if (TefService.instance().isInitializing()) {
                throw new Error("future transaction: " + Integer.toString(id, 16));
            }
        }

        id_ = new TransactionId.W(id);

        writeTransactionIdCounter__.setCurrentId(id_.serial);

        beginTime_ = beginTime;
        committedTime_ = committedTime;

        synchronized (this) {
            runningContext_ = new RunningContext(Transaction.this, 0L, 0L, 0L);
        }
    }

    private static final List<TransactionCloseListener> transactionCloseListeners__
            = new ArrayList<TransactionCloseListener>();

    static void addTransactionCloseListener(TransactionCloseListener listener) {
        synchronized (transactionCloseListeners__) {
            transactionCloseListeners__.add(listener);
        }
    }

    static void removeTransactionCloseListener(TransactionCloseListener listener) {
        synchronized (transactionCloseListeners__) {
            transactionCloseListeners__.remove(listener);
        }
    }

    synchronized void begin() {
        if (!isActive()) {
            throw new IllegalStateException();
        }
        if (runningContext_.getStatus() != TransactionStatus.BEFORE_RUNNING) {
            throw new IllegalStateException();
        }

        if (isRestoredTransaction_) {
        } else {
            if (!isReadOnly()) {
                WriteTransactionExecutionController.getInstance().getExecutionMutex(Transaction.this).lock();

                LongTermWriteTransactionExecutionMonitor.getInstance().begin(Transaction.this);
            }

            Transaction lastCommittedTransaction = getLastCommittedTransaction();
            if (lastCommittedTransaction != null) {
                adjustTime(lastCommittedTransaction.getCommittedTime(), "transaction begin");
            }

            beginTime_ = System.currentTimeMillis();
            TefService.instance().getMvoRegistry().transactionBegun(Transaction.this);
            TefService.instance().getTransactionExecLogger().writeBeginLog(Transaction.this);
        }

        runningContext_.initialize();

        ActiveTransactions.add(Transaction.this);
    }

    synchronized final void phase1Commit() {
        if (isReadOnly()) {
            throw new CommitFailedException("transaction is read only.");
        }

        if (!isRestoredTransaction_) {
            if (!isActive()) {
                throw new RuntimeException("not active.");
            }

            adjustTime(beginTime_, "transaction phase1 commit");

            validateContextTransaction();

            try {
                TefService.instance().getMvoRegistry().checkConstraints();
            } catch (MvoHome.HomeConstraintsException hce) {
                throw new CommitFailedException("home constraints violation.", hce);
            }

            if (committedTime_ != VOID_COMMITTED_TIME) {
                throw new IllegalStateException();
            }
            committedTime_ = System.currentTimeMillis();

            getChangeLogger().writePhase1Commit();

            runningContext_.setStatus(TransactionStatus.PHASE1_COMMITTED);

            if (!TefService.instance().isTefServiceAvailable()) {
                throw new Error("tef service is not available.");
            }
        }
    }

    final void phase2Commit(StackTraceCatalog.CatalogId stacktraceCatalogId) {
        final TefService tefService = TefService.instance();
        try {
            if (isRestoredTransaction_) {
                synchronized (committedTransactions__) {
                    committedTransactions__.add(Transaction.this);
                }

                close();
            } else {
                PostCommitProcessService.CommittedTransactionContainer committedTransactionContainer;
                synchronized (this) {
                    if (runningContext_.getStatus() != TransactionStatus.PHASE1_COMMITTED) {
                        throw new IllegalStateException();
                    }

                    tefService.getMvoRegistry().commitRegistration();

                    for (MVO lockingMvo : runningContext_.lockingMvos) {
                        lockingMvo.commitLock();
                    }

                    TransactionId.W transactionid = (TransactionId.W) getId();

                    getChangeLogger().writePhase2Commit();
                    byte[] journalContents = getChangeLogger().getContents();
                    CommittedTransaction committedTransaction = new CommittedTransaction(
                            transactionid,
                            getNewObjects(),
                            getChangedObjects(),
                            getChangedFields(),
                            runningContext_.postProcessorExtInfos);

                    tefService.getTransactionExecLogger().writeCommitLog(Transaction.this, stacktraceCatalogId);

                    runningContext_.setStatus(TransactionStatus.COMMITTED);

                    synchronized (committedTransactions__) {
                        committedTransactions__.add(Transaction.this);
                    }

                    TransactionDigestComputer transactionDigestComputer = tefService.getTransactionDigestComputer();
                    transactionDigestComputer.update(transactionid, journalContents);

                    committedTransactionContainer = tefService.getPostCommitProcessService().registCommittedTransaction(
                            new JournalMirroringServer.TransferableJournalEntry(
                                    transactionDigestComputer.getDigest(),
                                    journalContents),
                            committedTransaction,
                            getPostTransactionProcessors());

                    close();
                }

                committedTransactionContainer.transactionClosed();
            }
        } catch (RuntimeException re) {
            tefService.disableTefService();
            tefService.logError("", re);
            throw re;
        } catch (Error e) {
            tefService.disableTefService();
            tefService.logError("", e);
            throw e;
        }
    }

    final void kill() {
        if (isRestoredTransaction_) {
            throw new IllegalStateException();
        }

        TefService.instance().getTransactionExecLogger().writeKillLog(Transaction.this);

        synchronized (this) {
            runningContext_.isToKill = true;

            Thread transactionThread = ActiveTransactions.getThread(this);
            if (transactionThread != null) {
                transactionThread.interrupt();
            }
        }
    }

    synchronized final void rollback(StackTraceCatalog.CatalogId stacktraceCatalogId) {
        try {
            if (isRestoredTransaction_) {
                throw new IllegalStateException();
            }

            if (runningContext_.getStatus() == TransactionStatus.COMMITTED) {
                throw new RuntimeException("already committed.");
            }
            if (runningContext_.getStatus() == TransactionStatus.ROLLBACKED
                    || runningContext_.getStatus() == TransactionStatus.ROLLING_BACK) {
                return;
            }

            runningContext_.setStatus(TransactionStatus.ROLLING_BACK);

            validateContextTransaction();

            TefService.instance().getIndexes().rollbackTransaction(this);
            TefService.instance().getMvoRegistry().rollbackTransaction(this);

            for (Iterator<MVO> i = runningContext_.lockingMvos.iterator(); i.hasNext(); ) {
                i.next().rollbackLock();
            }

            if (!isReadOnly()) {
                getChangeLogger().rollback();
            }

            runningContext_.setStatus(TransactionStatus.ROLLBACKED);

            TefService.instance().getTransactionExecLogger().writeRollbackLog(Transaction.this, stacktraceCatalogId);
        } catch (RuntimeException re) {
            TefService.instance().logError("", re);
            throw re;
        } catch (Error e) {
            TefService.instance().logError("", e);
            throw e;
        }

        close();
    }

    final long getBeginTime() {
        return beginTime_;
    }

    synchronized final long getCommittedTime() {
        if (committedTime_ == VOID_COMMITTED_TIME) {
            throw new IllegalStateException("not yet committed.");
        }
        return committedTime_;
    }

    synchronized final boolean hasCommitted() {
        if (isActive()) {
            return runningContext_.getStatus() == TransactionStatus.COMMITTED;
        } else {
            return committedTime_ != VOID_COMMITTED_TIME;
        }
    }

    synchronized final boolean isActive() {
        return runningContext_ != null;
    }

    private synchronized void close() {
        try {
            TefService.instance().getIndexes().processInitializedMvoFields();

            if (isRestoredTransaction_) {
                runningContext_.close();
                runningContext_ = null;

                ActiveTransactions.remove(this);
                notifyTransactionClose();
                return;
            }

            if (!isActive()) {
                return;
            }

            if (runningContext_.getStatus() != TransactionStatus.COMMITTED
                    && runningContext_.getStatus() != TransactionStatus.ROLLBACKED) {
                throw new IllegalStateException(runningContext_.getStatus().name());
            }

            if (runningContext_.isClosing) {
                return;
            }

            if ((!isToKill())
                    && (Thread.currentThread() != ActiveTransactions.getThread(this))) {
                throw new IllegalStateException();
            }

            runningContext_.isClosing = true;

            if (!isReadOnly()) {
                releaseLock();

                LongTermWriteTransactionExecutionMonitor.getInstance().end(Transaction.this);
            }

            runningContext_.close();

            TefService.instance().getTransactionExecLogger().writeCloseLog(Transaction.this);

            runningContext_ = null;

            ActiveTransactions.remove(this);

            notifyTransactionClose();
        } catch (RuntimeException re) {
            TefService.instance().logError("", re);
            throw re;
        } catch (Error e) {
            TefService.instance().logError("", e);
            throw e;
        }
    }

    private void releaseLock() {
        WriteTransactionExecutionController.getInstance().transactionFinished(Transaction.this);
    }

    private void notifyTransactionClose() {
        synchronized (transactionCloseListeners__) {
            for (TransactionCloseListener listener : transactionCloseListeners__) {
                listener.notifyClosed(Transaction.this);
            }
        }
    }

    private void checkLoggingAccess() {
        if (isReadOnly()) {
            throw new IllegalStateException("transaction is read only.");
        }
        if (hasCommitted()) {
            throw new IllegalStateException("already committed.");
        }
        if (!isActive()) {
            throw new RuntimeException("not alive.");
        }

        validateContextTransaction();
    }

    final void logNew(MVO mvo) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addNewObject(mvo);
    }

    final void addLog(MVO.F2 f2) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addF2(f2);
    }

    final void addLog(MVO.F1 f1) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addF1(f1);
    }

    final void addLog(MVO.S2 s2) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addS2(s2);
    }

    final void addLog(MVO.S1 s1) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addS1(s1);
    }

    final void addLog(MVO.M1 m1) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addM1(m1);
    }

    final void addLog(MVO.M2 m2) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addM2(m2);
    }

    final void addLog(MVO.N2 n2) {
        if (isRestoredTransaction_) {
            return;
        }

        checkLoggingAccess();

        getChangeLogger().addN2(n2);
    }

    final TransactionId getId() {
        return id_;
    }

    final String getTransactionDescription() {
        return transactionDescription_;
    }

    final void setTransactionDescription(String desc) {
        transactionDescription_ = desc == null ? null : desc.intern();
    }

    final boolean isReadOnly() {
        return id_ instanceof TransactionId.R;
    }

    final boolean isExclusiveWrite() {
        return !isConcurrentWrite_;
    }

    final boolean isConcurrentWrite() {
        return isConcurrentWrite_;
    }

    boolean isRestoredTransaction() {
        return isRestoredTransaction_;
    }

    int countNewObjects() {
        return getChangeLogger().countNewObjects();
    }

    int countChangedObjects() {
        return getChangeLogger().getChangedObjects().size();
    }

    int countChangedFields() {
        return getChangeLogger().getChangedFields().size();
    }

    List<MVO> getNewObjects() {
        return getChangeLogger().getNewObjects();
    }

    List<MVO> getChangedObjects() {
        return getChangeLogger().getChangedObjects();
    }

    List<MVO.MvoField> getChangedFields() {
        return getChangeLogger().getChangedFields();
    }

    private synchronized TransactionChangeLogger getChangeLogger() {
        return runningContext_ == null
                ? null
                : runningContext_.changeLogger;
    }

    final synchronized Transaction getBaseTransaction() {
        return runningContext_.baseTransaction;
    }

    final synchronized void addLockingMvo(MVO mvo) {
        runningContext_.lockingMvos.add(mvo);
    }

    final synchronized MVO[] getLockingMvos() {
        return (MVO[]) runningContext_.lockingMvos.toArray(new MVO[0]);
    }

    final synchronized boolean isToKill() {
        return runningContext_ != null
                && runningContext_.isToKill;
    }

    final synchronized void setTargetTime(long targetTime) {
        if (runningContext_.currentTargetTime.longValue() == targetTime) {
            return;
        } else {
            runningContext_.currentTargetTime = new Long(targetTime);
        }
    }

    final synchronized long getTargetTime() {
        return runningContext_.currentTargetTime.longValue();
    }

    final synchronized void setTargetVersion(TransactionId.W targetVersion) {
        runningContext_.currentTargetVersion = targetVersion;
    }

    final synchronized TransactionId.W getTargetVersion() {
        return runningContext_.currentTargetVersion;
    }

    final synchronized BinaxesEngine.BinaxesArg getBinaxesArg() {
        Long targetTime = runningContext_.currentTargetTime;
        BinaxesEngine.BinaxesArg binaxesArg = runningContext_.cachedBinaxesArgs.get(targetTime);
        if (binaxesArg == null) {
            binaxesArg = new BinaxesEngine.BinaxesArg(targetTime.longValue(), this);
            runningContext_.cachedBinaxesArgs.put(targetTime, binaxesArg);
        }
        return binaxesArg;
    }

    final synchronized MonaxisEngine.HistoryElement<?> getMonaxisHistoryElement(Object value) {
        MonaxisEngine.HistoryElement<?> result = runningContext_.monaxisHistoryElements.get(value);
        if (result == null) {
            result = new MonaxisEngine.HistoryElement<Object>(this, value);
            runningContext_.monaxisHistoryElements.put(value, result);
        }
        return result;
    }

    final synchronized BinaxesEngine.HistoryElement<?> getBinaxesHistoryElement(
            BinaxesEngine.BinaxesArg binarg, Object value) {
        Map<Long, BinaxesEngine.HistoryElement<?>> map = runningContext_.binaxesHistoryElements.get(value);
        if (map == null) {
            map = new HashMap<Long, BinaxesEngine.HistoryElement<?>>();
            runningContext_.binaxesHistoryElements.put(value, map);
        }

        BinaxesEngine.HistoryElement<?> result = map.get(binarg.time);
        if (result == null) {
            result = new BinaxesEngine.HistoryElement<Object>(binarg, value);
            map.put(binarg.time, result);
        }
        return result;
    }

    final synchronized void putContextInfo(Object key, Object value) {
        runningContext_.contextInfo.put(key, value);
    }

    final synchronized Object getContextInfo(Object key) {
        return runningContext_.contextInfo.get(key);
    }

    final synchronized void putPostProcessorExtInfo(Object key, Object value) {
        runningContext_.postProcessorExtInfos.put(key, value);
    }

    final synchronized Object getPostProcessorExtInfo(Object key) {
        return runningContext_.postProcessorExtInfos.get(key);
    }

    private void validateContextTransaction() {
        Transaction contextTransaction = getContextTransaction();
        if (contextTransaction == null
                || contextTransaction != Transaction.this) {
            throw new IllegalStateException();
        }
    }

    static void validateWriteContext() {
        Transaction transaction = getContextTransaction();
        if (transaction == null) {
            throw new NoTransactionContextFoundException();
        }

        if (transaction.isRestoredTransaction()) {
            return;
        }

        if (transaction.isReadOnly()) {
            throw new IllegalStateException("transaction is read only.");
        }
        if (transaction.getTargetVersion().serial != transaction.getId().serial) {
            throw new IllegalStateException("illegal transaction target version.");
        }
    }

    synchronized void addInitializedF0(MVO.F0 f0Field) {
        if (runningContext_.initializedF0s.contains(f0Field)) {
            throw new IllegalStateException(
                    "already initialized: " + f0Field.getParent().getClass().getName() + "." + f0Field.getFieldName());
        }
        runningContext_.initializedF0s.add(f0Field);
    }

    synchronized boolean hasInitialized(MVO.F0 f0Field) {
        return runningContext_.initializedF0s.contains(f0Field);
    }

    synchronized void addInitializedS0(MVO.S0 s0Field) {
        if (runningContext_.initializedS0s.contains(s0Field)) {
            throw new IllegalStateException(
                    "already initialized: " + s0Field.getParent().getClass().getName() + "." + s0Field.getFieldName());
        }
        runningContext_.initializedS0s.add(s0Field);
    }

    synchronized boolean hasInitialized(MVO.S0 s0Field) {
        return runningContext_.initializedS0s.contains(s0Field);
    }

    synchronized void addPostTransactionProcessor(PostTransactionProcessor postTransactionProcessor) {
        runningContext_.postTransactionProcessors.add(postTransactionProcessor);
    }

    synchronized List<PostTransactionProcessor> getPostTransactionProcessors() {
        return new ArrayList<PostTransactionProcessor>(runningContext_.postTransactionProcessors);
    }

    void suspend() {
        ActiveTransactions.suspend(this);
    }

    void resume() {
        TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

        ActiveTransactions.resume(this);
    }

    static void adjustTime(long timeToWait, String info) {
        long offset = timeToWait - System.currentTimeMillis();
        if (offset <= 0) {
            return;
        }

        long adjustingStart = System.nanoTime();
        TefService.instance().logMessage(
                "adjusting time" + (info == null ? "" : "(" + info + ")") + ", offset:" + Long.toString(offset, 16));

        while (true) {
            offset = timeToWait - System.currentTimeMillis();
            if (offset <= 0) {
                break;
            }

            try {
                Thread.sleep(Math.min(1000, offset));
            } catch (InterruptedException ie) {
            }
        }

        TefService.instance().logMessage("time adjusted: " + Long.toString(System.nanoTime() - adjustingStart, 16));
    }

    long getTime() {
        return System.nanoTime() - runningContext_.baseTimeNano;
    }

    long getCpuTime() {
        return TefUtils.getCurrentThreadCpuTime() - runningContext_.baseCpuTimeNano;
    }

    long getUserTime() {
        return TefUtils.getCurrentThreadUserTime() - runningContext_.baseUserTimeNano;
    }
}
