package tef.skelton.dto;

import tef.MVO;
import tef.TefService;
import tef.TransactionContext;
import tef.TransactionId;
import tef.TransactionIdAggregator;
import tef.skelton.AbstractModel;
import tef.skelton.Attribute;
import tef.skelton.MvoList;
import tef.skelton.MvoMap;
import tef.skelton.MvoSet;
import tef.skelton.SkeltonTefService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class DtoChangesBuilder {

    private final SkeltonTefService tefservice_;
    private final DtoOriginator originator_;

    public DtoChangesBuilder(DtoOriginator originator) {
        tefservice_ = SkeltonTefService.instance();
        originator_ = originator;
    }

    public DtoChanges buildDtoChanges(
        final TransactionId.W lowerTxid,
        final boolean lowerInclusive,
        final TransactionId.W upperTxid,
        final boolean upperInclusive)
    {
        final Set<AbstractModel> newMvos = new HashSet<AbstractModel>();
        final Set<AbstractModel> changedMvos = new HashSet<AbstractModel>();
        for (AbstractModel obj : getAbstractModels()) {
            final TransactionId.W initialTxid = obj.getInitialVersion();
            if (lowerTxid.serial <= initialTxid.serial && initialTxid.serial <= upperTxid.serial) {
                newMvos.add(obj);
            }
            NavigableSet<TransactionId.W> txids = new TreeSet<TransactionId.W>();
            txids.addAll(getTransactionIds(obj));

            for (String attrname : obj.getAttributeNames()) {
                final Object value = obj.getValue(attrname);
                if (value instanceof Attribute.SetAttr<?, ?>) {
                    final MvoSet<?> mvoset = ((Attribute.SetAttr<?, AbstractModel>) value).get(obj);
                    txids.addAll(getTransactionIds(mvoset));
                }
                if (value instanceof Attribute.ListAttr<?, ?>) {
                    final MvoList<?> mvolist = ((Attribute.ListAttr<?, AbstractModel>) value).get(obj);
                    txids.addAll(getTransactionIds(mvolist));
                }
                if (value instanceof Attribute.MapAttr<?, ?, ?>) {
                    final MvoMap<?, ?> mvomap = ((Attribute.MapAttr<?, ?, AbstractModel>) value).get(obj);
                    txids.addAll(getTransactionIds(mvomap));
                }
            }

            txids = txids.subSet(lowerTxid, lowerInclusive, upperTxid, upperInclusive);

            if (0 < txids.size()) {
                changedMvos.add(obj);
            }
        }
        changedMvos.removeAll(newMvos);

        return new DtoChanges(
            originator_, lowerTxid, upperTxid, TransactionContext.getTargetTime(),
            buildDtos(newMvos), buildDtos(changedMvos));
    }

    private List<TransactionId.W> getTransactionIds(MVO obj) {
        return array2list(TransactionIdAggregator.getTransactionIds(obj));
    }

    private <T> List<T> array2list(T[] array) {
        return Arrays.<T>asList(array);
    }

    private List<AbstractModel> getAbstractModels() {
        final List<AbstractModel> result = new ArrayList<AbstractModel>();
        for (MVO mvo : TefService.instance().getMvoRegistry().list()) {
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
