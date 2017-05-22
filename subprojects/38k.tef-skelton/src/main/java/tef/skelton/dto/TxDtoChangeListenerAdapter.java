package tef.skelton.dto;

import tef.CommittedTransaction;
import tef.MVO;
import tef.TefService;
import tef.TransactionCommitListener;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.SkeltonTefService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TxDtoChangeListenerAdapter implements TransactionCommitListener {

    private final SkeltonTefService tefservice_;
    private final DtoOriginator originator_;
    private final String listenerName_;
    private final DtoChangeListener listener_;

    public TxDtoChangeListenerAdapter(DtoOriginator originator, String listenerName, DtoChangeListener listener) {
        tefservice_ = SkeltonTefService.instance();
        originator_ = originator;
        listenerName_ = listenerName;
        listener_ = listener;

        TefService.instance().removeTransactionCommitListener(listenerName_);
        TefService.instance().addTransactionCommitListener(listenerName_, this);

        tefservice_.logMessage("added: dto change listener, " + System.identityHashCode(listener));
    }

    @Override public void notifyTransactionCommitted(CommittedTransaction committedTx) {
        TransactionContext.beginReadTransaction();

        try {
            TransactionId.W targetVersion = committedTx.getTransactionId();
            TransactionContext.setTargetVersion(targetVersion);

            long targetTime = TransactionContext.getTargetTime();

            Set<AbstractModel> newMvos = selectModels(committedTx.getNewObjects());
            Set<AbstractModel> changedMvos = selectModels(committedTx.getChangedObjects());

            Map<MVO, MVO> mvolistsetmap2model
                = (Map<MVO, MVO>) committedTx.getExtraInfos().get(Attribute.MVOLISTSETMAP2MODEL_KEY);
            if (mvolistsetmap2model != null) {
                for (MVO mvo : mvolistsetmap2model.values()) {
                    if (mvo instanceof AbstractModel) {
                        changedMvos.add((AbstractModel) mvo);
                    }
                }
            }

            changedMvos.removeAll(newMvos); 

            Set<EntityDto> newDtos = buildDtos(newMvos);
            Set<EntityDto> changedDtos = buildDtos(changedMvos);

            listener_.transactionCommitted(
                targetVersion,
                newDtos,
                changedDtos,
                new DtoChanges(originator_, targetVersion, targetVersion, targetTime, newDtos, changedDtos));
        } catch (Throwable t) {
            tefservice_.logError("removed: dto change listener, " + System.identityHashCode(listener_), t);
            TefService.instance().removeTransactionCommitListener(listenerName_);
        } finally {
            TransactionContext.close();
        }
    }

    private Set<AbstractModel> selectModels(Collection<MVO> mvos) {
        final Set<AbstractModel> result = new HashSet<AbstractModel>();
        for (MVO mvo : mvos) {
            if (mvo instanceof AbstractModel) {
                result.add((AbstractModel) mvo);
            }
        }
        return result;
    }

    private Set<EntityDto> buildDtos(Collection<AbstractModel> mvos) {
        Set<EntityDto> result = new HashSet<EntityDto>();
        for (AbstractModel mvo : mvos) {
            if (tefservice_.getMvoDtoMapping().hasDtoMapping(mvo.getClass())) {
                result.add(tefservice_.getMvoDtoFactory().build(originator_, mvo));
            }
        }
        return result;
    }
}
