package tef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommittedTransaction {

    private final TransactionId.W transactionId_;
    private final List<MVO> newObjects_;
    private final List<MVO> changedObjects_;
    private final List<MVO.MvoField> changedFields_;
    private final Map<Object, Object> extraInfos_;

    CommittedTransaction
            (TransactionId.W transactionId,
             List<MVO> newObjects, List<MVO> changedObjects, List<MVO.MvoField> changedFields,
             Map<Object, Object> extraInfos) {
        transactionId_ = transactionId;
        newObjects_ = new ArrayList<MVO>(newObjects);
        changedObjects_ = new ArrayList<MVO>(changedObjects);
        changedFields_ = new ArrayList<MVO.MvoField>(changedFields);
        extraInfos_ = new HashMap<Object, Object>(extraInfos);
    }

    public TransactionId.W getTransactionId() {
        return transactionId_;
    }

    public List<MVO> getNewObjects() {
        return new ArrayList<MVO>(newObjects_);
    }

    public <T extends MVO> List<T> selectNewObjects(Class<T> type) {
        return TefUtils.select(getNewObjects(), type);
    }

    public List<MVO> getChangedObjects() {
        return new ArrayList<MVO>(changedObjects_);
    }

    public <T extends MVO> List<T> selectChangedObjects(Class<T> type) {
        return TefUtils.select(getChangedObjects(), type);
    }

    public List<MVO.MvoField> getChangedFields() {
        return new ArrayList<MVO.MvoField>(changedFields_);
    }

    public Map<Object, Object> getExtraInfos() {
        return extraInfos_;
    }
}
