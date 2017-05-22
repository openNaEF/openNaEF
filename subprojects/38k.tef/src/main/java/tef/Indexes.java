package tef;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

final class Indexes {

    private final ReentrantLock lock_ = new ReentrantLock();

    private final
    Map<MVO.F1, MvoHome.F1UniqueIndex> initializedF1Fields_
            = new HashMap<MVO.F1, MvoHome.F1UniqueIndex>();
    private final
    Map<Field, MvoHome.F1UniqueIndex> f1UniqueIndexes_
            = new HashMap<Field, MvoHome.F1UniqueIndex>();

    private final
    Map<MVO.F0, MvoHome.F0UniqueIndex> initializedF0Fields_
            = new HashMap<MVO.F0, MvoHome.F0UniqueIndex>();
    private final
    Map<Field, MvoHome.F0UniqueIndex> f0UniqueIndexes_
            = new HashMap<Field, MvoHome.F0UniqueIndex>();

    private final List<IndexEngine> indexes_ = new ArrayList<IndexEngine>();

    void processInitializedMvoFields() {
        lock_.lock();
        try {
            for (MVO.F0 f0 : initializedF0Fields_.keySet()) {
                MvoHome.F0UniqueIndex index = initializedF0Fields_.get(f0);
                Field field = MvoMeta.getReflectionField(f0);
                if (f0UniqueIndexes_.get(field) == index) {
                    continue;
                }

                if (f0UniqueIndexes_.get(field) == null) {
                    f0UniqueIndexes_.put(field, index);
                    index.initialize(MvoMeta.getReflectionField(f0));
                } else {
                    throw new IllegalStateException();
                }
            }
            initializedF0Fields_.clear();

            for (MVO.F1 f1 : initializedF1Fields_.keySet()) {
                MvoHome.F1UniqueIndex index = initializedF1Fields_.get(f1);
                Field field = MvoMeta.getReflectionField(f1);
                if (f1UniqueIndexes_.get(field) == index) {
                    continue;
                }

                if (f1UniqueIndexes_.get(field) == null) {
                    f1UniqueIndexes_.put(field, index);
                    index.initialize(MvoMeta.getReflectionField(f1));
                } else {
                    throw new IllegalStateException();
                }
            }
            initializedF1Fields_.clear();
        } finally {
            lock_.unlock();
        }
    }

    void addInitializedF0(MVO.F0 f0, MvoHome.F0UniqueIndex index) {
        if (f0 == null) {
            return;
        }

        lock_.lock();
        try {
            initializedF0Fields_.put(f0, index);
        } finally {
            lock_.unlock();
        }
    }

    MvoHome.F0UniqueIndex getF0UniqueIndex(MVO.F0 f0) {
        lock_.lock();
        try {
            if (initializedF0Fields_.size() > 0) {
                processInitializedMvoFields();
            }

            return f0UniqueIndexes_.get(MvoMeta.getReflectionField(f0));
        } finally {
            lock_.unlock();
        }
    }

    void addInitializedF1(MVO.F1 f1, MvoHome.F1UniqueIndex index) {
        if (f1 == null) {
            return;
        }

        lock_.lock();
        try {
            initializedF1Fields_.put(f1, index);
        } finally {
            lock_.unlock();
        }
    }

    MvoHome.F1UniqueIndex getF1UniqueIndex(MVO.F1 f1) {
        lock_.lock();
        try {
            if (initializedF1Fields_.size() > 0) {
                processInitializedMvoFields();
            }

            return f1UniqueIndexes_.get(MvoMeta.getReflectionField(f1));
        } finally {
            lock_.unlock();
        }
    }

    void addIndex(IndexEngine index) {
        lock_.lock();
        try {
            indexes_.add(index);
        } finally {
            lock_.unlock();
        }
    }

    void commitTransaction(Transaction transaction) {
        lock_.lock();
        try {
            if (transaction != TransactionContext.getContextTransaction()) {
                throw new IllegalArgumentException();
            }
            if (transaction.isReadOnly()) {
                throw new IllegalArgumentException();
            }

            for (IndexEngine index : indexes_) {
                index.commitTransaction();
            }
        } finally {
            lock_.unlock();
        }
    }

    void rollbackTransaction(Transaction transaction) {
        lock_.lock();
        try {
            if (transaction != TransactionContext.getContextTransaction()) {
                throw new IllegalArgumentException();
            }

            if (transaction.isReadOnly()) {
                return;
            }

            for (IndexEngine index : indexes_) {
                index.rollbackTransaction();
            }
        } finally {
            lock_.unlock();
        }
    }
}
