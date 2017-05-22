package tef;

import java.util.*;
import java.util.List;

/**
 * <p>
 * MvoRegistry は TefService 内の全 MVO を保持します。
 * <p>
 * {@link tef.TefService#getMvoRegistry()} でインスタンスを得ることができます。
 * <p>
 * MvoRegistry の操作の隔離性はシリアライザブルです。
 * たとえば以下の順序でトランザクションを実行したとします:
 * <ul>
 * <li>読取トランザクション tx-r1 を開始
 * <li>書込トランザクション tx-w1 を開始
 * <li>tx-w1 内で MVO m1 を生成
 * <li>書込トランザクションをコミット
 * </ul>
 * ここで tx-r1 で {@link #get(MVO.MvoId)}, {@link #count()}, {@link #list()}
 * を実行すると、tx-w1 で生成した m1 は結果に含まれません。
 */
public final class MvoRegistry {

    private static final Comparator<Object> ID_COMPARATOR = new Comparator<Object>() {

        public int compare(final Object o1, final Object o2) {
            int transactionId1 = getTransactionId(o1);
            int transactionId2 = getTransactionId(o2);

            if (transactionId1 != transactionId2) {
                return transactionId1 - transactionId2;
            }

            int serial1 = getTransactionLocalSerial(o1);
            int serial2 = getTransactionLocalSerial(o2);
            return serial1 - serial2;
        }

        private int getTransactionId(Object o) {
            if (o instanceof MVO) {
                return ((MVO) o).getInitialVersion().serial;
            } else if (o instanceof MVO.MvoId) {
                return ((MVO.MvoId) o).transactionId.serial;
            } else {
                throw new RuntimeException();
            }
        }

        private int getTransactionLocalSerial(Object o) {
            if (o instanceof MVO) {
                return ((MVO) o).getTransactionLocalSerial();
            } else if (o instanceof MVO.MvoId) {
                return ((MVO.MvoId) o).transactionLocalSerial;
            } else {
                throw new RuntimeException();
            }
        }
    };

    private final TefService tefService_;

    private Transaction lock_;
    private Transaction lastLock_;

    private final List<MvoHome> homes_ = new ArrayList<MvoHome>();

    private final List<MVO> committedMvos_ = new ArrayList<MVO>();

    private final Map<Transaction, List<MVO>> uncommittedMvos_
            = new HashMap<Transaction, List<MVO>>();

    MvoRegistry(TefService tefService) {
        if (tefService.getMvoRegistry() != null) {
            throw new IllegalStateException();
        }

        tefService_ = tefService;
    }

    final synchronized <T extends MVO> void registHome(MvoHome<T> registee) {
        Class<T> homeType = registee.getType();
        if (homeType == null) {
            throw new RuntimeException();
        }

        for (MvoHome home : homes_) {
            if (home.getType() == homeType) {
                throw new IllegalStateException
                        ("home already exists: " + homeType.getClass().getName());
            }
        }

        homes_.add(registee);

        for (MVO mvo : committedMvos_) {
            if (homeType.isAssignableFrom(mvo.getClass())) {
                registee.regist(homeType.cast(mvo));
            }
        }
        for (Map.Entry<Transaction, List<MVO>> entry : uncommittedMvos_.entrySet()) {
            for (MVO mvo : entry.getValue()) {
                if (homeType.isAssignableFrom(mvo.getClass())) {
                    throw new RuntimeException();
                }
            }
        }
    }

    final void registForTransactionRestoration(MVO object) {
        tefService_.checkTransactionRestoringThread();

        addCommitted(object);

        for (MvoHome relatedHome : getRelatedHomes(object.getClass())) {
            relatedHome.registForTransactionRestoration(object);
        }
    }

    private synchronized void addCommitted(MVO object) {
        addMvoList(committedMvos_, object);
    }

    private synchronized void addUncommitted(MVO object) {
        addMvoList(getUncommittedMvos(), object);
    }

    private static synchronized void addMvoList(List<MVO> list, MVO object) {
        checkRegistrationListSequence(list, object);

        list.add(object);
    }

    private static void checkRegistrationListSequence(List<MVO> list, MVO object) {
        if (list.size() == 0) {
            return;
        }

        MVO last = list.get(list.size() - 1);
        int lastTransaction = last.getInitialVersion().serial;
        int objectTransaction = object.getInitialVersion().serial;

        if (lastTransaction > objectTransaction) {
            throw new IllegalStateException();
        }
        if (lastTransaction == objectTransaction) {
            int lastSerial = last.getTransactionLocalSerial();
            int objectSerial = object.getTransactionLocalSerial();
            if (lastSerial >= objectSerial) {
                throw new IllegalStateException();
            }
        }
    }

    final synchronized void regist(MVO object) {
        Transaction transaction = TransactionContext.getContextTransaction();
        if (lock_ != null && lock_ != transaction) {
            throw new ConcurrentTransactionException();
        }
        if (lastLock_ != null && lastLock_.getId().serial > transaction.getId().serial) {
            throw new SuperSerializableException();
        }

        addUncommitted(object);

        for (MvoHome relatedHome : getRelatedHomes(object.getClass())) {
            relatedHome.regist(object);
        }
    }

    final synchronized void transactionBegun(Transaction transaction) {
        if (transaction.isReadOnly()) {
            return;
        }

        uncommittedMvos_.put(transaction, new ArrayList<MVO>());
    }

    final synchronized void checkConstraints() throws MvoHome.HomeConstraintsException {
        Transaction transaction = TransactionContext.getContextTransaction();
        if (transaction.isReadOnly()) {
            throw new IllegalStateException();
        }

        for (MvoHome home : homes_) {
            if (home.getLock() == transaction) {
            }
        }
    }

    final synchronized void commitRegistration() {
        Transaction transaction = TransactionContext.getContextTransaction();
        if (transaction.isReadOnly()) {
            throw new IllegalStateException();
        }

        try {
            List<MVO> uncommittedMvos = getUncommittedMvos();
            for (MVO uncommittedMvo : uncommittedMvos) {
                addCommitted(uncommittedMvo);
            }

            for (MvoHome home : homes_) {
                if (home.getLock() == transaction) {
                    home.commitRegist();
                }
            }

            tefService_.getIndexes().commitTransaction(transaction);

            if (lock_ == transaction) {
                if (lastLock_ != null && lastLock_.getId().serial > lock_.getId().serial) {
                    throw new IllegalStateException();
                }

                lastLock_ = lock_;
            }
        } finally {
            cleanup(transaction);
        }
    }

    final synchronized void rollbackTransaction(Transaction transaction) {
        if (transaction != TransactionContext.getContextTransaction()) {
            throw new IllegalArgumentException();
        }

        if (transaction.isReadOnly()) {
            return;
        }

        try {
            for (MvoHome home : homes_) {
                if (home.getLock() == TransactionContext.getContextTransaction()) {
                    home.rollbackRegist();
                }
            }
        } finally {
            cleanup(transaction);
        }
    }

    private final void cleanup(Transaction transaction) {
        if (transaction.isReadOnly()) {
            return;
        }

        synchronized (this) {
            if (lock_ == transaction) {
                lock_ = null;
            }

            if (getUncommittedMvos(transaction) != null) {
                getUncommittedMvos(transaction).clear();
                uncommittedMvos_.remove(transaction);
            }
        }
    }

    private final Map<Class<?>, List<MvoHome>> relatedHomesMap_
            = new HashMap<Class<?>, List<MvoHome>>();

    private synchronized final List<MvoHome> getRelatedHomes(Class<?> clazz) {
        List<MvoHome> result = relatedHomesMap_.get(clazz);
        if (result == null) {
            result = new ArrayList<MvoHome>();
            for (MvoHome<?> home : homes_) {
                if (home.getType().isAssignableFrom(clazz)) {
                    result.add(home);
                }
            }
            relatedHomesMap_.put(clazz, result);
        }
        return result;
    }

    /**
     * <p>
     * 指定された MvoId を持つ MVO を返します。
     */
    public synchronized final MVO get(MVO.MvoId mvoId) {
        if (mvoId == null) {
            throw new IllegalArgumentException("null");
        }

        if (!mvoId.isLocalObjectId()) {
            throw new IllegalArgumentException("non local mvo id: " + mvoId);
        }

        int index = Collections.binarySearch(committedMvos_, mvoId, ID_COMPARATOR);
        if (index >= 0) {
            MVO result = committedMvos_.get(index);
            if (Thread.currentThread()
                    == TefService.instance().getTransactionRestoringThread()) {
                return result;
            }

            if (TransactionContext.getContextTransaction().isReadOnly()
                    && (!MvoUtils
                    .isExistingAt(result, TransactionContext.getBaseTransactionId()))) {
                return null;
            } else {
                return result;
            }
        }

        Transaction transaction = TransactionContext.getContextTransaction();
        if (transaction.isRestoredTransaction()) {
            throw new TefInitializationFailedException
                    ("no such object: " + mvoId.getLocalStringExpression());
        }

        if (transaction.isReadOnly()) {
            return null;
        }

        int uncommittedsIndex
                = Collections.binarySearch(getUncommittedMvos(), mvoId, ID_COMPARATOR);
        return uncommittedsIndex >= 0
                ? getUncommittedMvos().get(uncommittedsIndex)
                : null;
    }

    /**
     * <p>
     * 登録されている MVO の数を返します。
     */
    public synchronized final int count() {
        checkConcurrentWriteTransaction();

        return getCommittedMvos().size()
                + (getUncommittedMvos() == null ? 0 : getUncommittedMvos().size());
    }

    /**
     * <p>
     * 登録されている全 MVO を返します。
     */
    public synchronized final List<MVO> list() {
        checkConcurrentWriteTransaction();

        List<MVO> result = new ArrayList<MVO>();
        result.addAll(getCommittedMvos());

        List<MVO> uncommitteds = getUncommittedMvos();
        if (uncommitteds != null) {
            result.addAll(uncommitteds);
        }

        return result;
    }

    public final <T extends MVO> List<T> select(Class<T> type) {
        return TefUtils.select(list(), type);
    }

    private synchronized List<MVO> getCommittedMvos() {
        if (TransactionContext.getContextTransaction().isReadOnly()) {
            TransactionId.W lastWriteTxId = TransactionContext.getBaseTransactionId();
            int committedsCount = committedMvos_.size();
            int count = 0;
            while (count < committedsCount) {
                if (committedMvos_.get(committedsCount - count - 1).getInitialVersion().serial
                        <= lastWriteTxId.serial) {
                    break;
                } else {
                    count++;
                }
            }

            if (count == 0) {
                return committedMvos_;
            } else {
                ArrayList<MVO> result = new ArrayList<MVO>(committedMvos_);
                for (int i = 0; i < count; i++) {
                    result.remove(committedsCount - i - 1);
                }
                return result;
            }
        } else {
            return committedMvos_;
        }
    }

    private synchronized List<MVO> getUncommittedMvos(Transaction transaction) {
        return uncommittedMvos_.get(transaction);
    }

    private synchronized List<MVO> getUncommittedMvos() {
        return getUncommittedMvos(TransactionContext.getContextTransaction());
    }

    private final void checkConcurrentWriteTransaction() {
        Transaction transaction = TransactionContext.getContextTransaction();

        if (transaction.isReadOnly()
                || tefService_.isInitializing()
                || tefService_.getRunningMode() != TefService.RunningMode.MASTER) {
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
}
