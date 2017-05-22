package tef;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

final class TransactionChangeLogger {

    private Transaction transaction_;
    private JournalWriter journalWriter_;

    private LinkedHashSet<MVO> newObjects_ = new LinkedHashSet<MVO>();
    private LinkedHashSet<MVO.F1> f1s_ = new LinkedHashSet<MVO.F1>();
    private LinkedHashSet<MVO.S1> s1s_ = new LinkedHashSet<MVO.S1>();
    private LinkedHashSet<MVO.M1> m1s_ = new LinkedHashSet<MVO.M1>();
    private LinkedHashSet<MVO.F2> f2s_ = new LinkedHashSet<MVO.F2>();
    private LinkedHashSet<MVO.S2> s2s_ = new LinkedHashSet<MVO.S2>();
    private LinkedHashSet<MVO.M2> m2s_ = new LinkedHashSet<MVO.M2>();
    private LinkedHashSet<MVO.N2> n2s_ = new LinkedHashSet<MVO.N2>();

    TransactionChangeLogger(Transaction transaction) {
        transaction_ = transaction;

        try {
            journalWriter_ = new JournalWriter(transaction_);
        } catch (IOException ioe) {
            throw new Error(ioe);
        }
    }

    final synchronized void addNewObject(MVO mvo) {
        if (newObjects_.contains(mvo)) {
            throw new IllegalStateException(mvo.getMvoId().getLocalStringExpression());
        }
        if (transaction_.getId().serial != mvo.getInitialVersion().serial) {
            throw new IllegalStateException(mvo.getMvoId().getLocalStringExpression());
        }

        newObjects_.add(mvo);
    }

    final synchronized void addF1(MVO.F1 f1) {
        if (!f1s_.contains(f1)) {
            f1s_.add(f1);
        }
    }

    final synchronized void addS1(MVO.S1 s1) {
        if (!s1s_.contains(s1)) {
            s1s_.add(s1);
        }
    }

    final synchronized void addM1(MVO.M1 m1) {
        if (!m1s_.contains(m1)) {
            m1s_.add(m1);
        }
    }

    final synchronized void addF2(MVO.F2 f2) {
        if (!f2s_.contains(f2)) {
            f2s_.add(f2);
        }
    }

    final synchronized void addS2(MVO.S2 s2) {
        if (!s2s_.contains(s2)) {
            s2s_.add(s2);
        }
    }

    final synchronized void addM2(MVO.M2 m2) {
        if (!m2s_.contains(m2)) {
            m2s_.add(m2);
        }
    }

    final synchronized void addN2(MVO.N2 n2) {
        if (!n2s_.contains(n2)) {
            n2s_.add(n2);
        }
    }

    final synchronized int countNewObjects() {
        return newObjects_.size();
    }

    final synchronized List<MVO> getNewObjects() {
        return new ArrayList<MVO>(newObjects_);
    }

    final synchronized List<MVO> getChangedObjects() {
        List<MVO> result = new ArrayList<MVO>();
        for (MVO.MvoField field : getChangedFields()) {
            result.add(field.getParent());
        }
        Collections.sort(result, new ObjectComparator());
        return result;
    }

    final synchronized List<MVO.MvoField> getChangedFields() {
        List<MVO.MvoField> result = new ArrayList<MVO.MvoField>();
        result.addAll(f1s_);
        result.addAll(s1s_);
        result.addAll(m1s_);
        result.addAll(f2s_);
        result.addAll(s2s_);
        result.addAll(m2s_);
        result.addAll(n2s_);
        return result;
    }

    final synchronized void writePhase1Commit() {
        journalWriter_.printTransactionInfo();

        MvoMeta mvoMeta = TefService.instance().getMvoMeta();

        MVO[] newMvos = newObjects_.toArray(new MVO[0]);
        for (MVO newMvo : newMvos) {
            Class<? extends MVO> clazz = newMvo.getClass();
            if (mvoMeta.isNewClass(clazz)) {
                journalWriter_.printNewClass(clazz);
            }
        }
        for (MVO newMvo : newMvos) {
            for (MVO.F0 f0Obj : MvoMeta.selectMvoFieldObjects(newMvo, MVO.F0.class)) {
                Field f0Field = MvoMeta.getReflectionField(f0Obj);
                if (mvoMeta.isNewField(f0Field)) {
                    journalWriter_.printNewField(f0Field);
                }
            }
            for (MVO.S0 s0Obj : MvoMeta.selectMvoFieldObjects(newMvo, MVO.S0.class)) {
                Field s0Field = MvoMeta.getReflectionField(s0Obj);
                if (mvoMeta.isNewField(s0Field)) {
                    journalWriter_.printNewField(s0Field);
                }
            }
        }
        for (MVO.MvoField changedField : getChangedFields()) {
            Field field = MvoMeta.getReflectionField(changedField);
            if (mvoMeta.isNewField(field)) {
                journalWriter_.printNewField(field);
            }
        }
        for (MVO newMvo : newMvos) {
            journalWriter_.printNewObject(newMvo);
        }
        for (MVO newMvo : newMvos) {
            journalWriter_.printF0s(newMvo);
        }
        for (MVO newMvo : newMvos) {
            journalWriter_.printS0s(newMvo);
        }

        for (MVO.F1 f1 : f1s_) {
            f1.commit(journalWriter_);
        }
        for (MVO.S1 s1 : s1s_) {
            s1.commit(journalWriter_);
        }
        for (MVO.M1 m1 : m1s_) {
            m1.commit(journalWriter_);
        }
        for (MVO.F2 f2 : f2s_) {
            f2.commit(journalWriter_);
        }
        for (MVO.S2 s2 : s2s_) {
            s2.commit(journalWriter_);
        }
        for (MVO.M2 m2 : m2s_) {
            m2.commit(journalWriter_);
        }
        for (MVO.N2 n2 : n2s_) {
            n2.commit(journalWriter_);
        }

        journalWriter_.phase1Commit();
    }

    final synchronized void writePhase2Commit() {
        journalWriter_.phase2Commit();
    }

    final synchronized void rollback() {
        for (MVO.F1 f1 : f1s_) {
            f1.rollback();
        }
        for (MVO.S1 s1 : s1s_) {
            s1.rollback();
        }
        for (MVO.M1 m1 : m1s_) {
            m1.rollback();
        }
        for (MVO.F2 f2 : f2s_) {
            f2.rollback();
        }
        for (MVO.S2 s2 : s2s_) {
            s2.rollback();
        }
        for (MVO.M2 m2 : m2s_) {
            m2.rollback();
        }
        for (MVO.N2 n2 : n2s_) {
            n2.rollback();
        }

        for (MVO newObject : newObjects_) {
            newObject.rollbackNew();
        }

        journalWriter_.rollback();
    }

    final byte[] getContents() {
        return journalWriter_.getContents();
    }

    final synchronized void close() {
        if (journalWriter_ != null) {
            journalWriter_.close();
        }

        transaction_ = null;
        journalWriter_ = null;

        newObjects_ = null;
        f1s_ = null;
        s1s_ = null;
        m1s_ = null;
        f2s_ = null;
        s2s_ = null;
        m2s_ = null;
        n2s_ = null;
    }
}
