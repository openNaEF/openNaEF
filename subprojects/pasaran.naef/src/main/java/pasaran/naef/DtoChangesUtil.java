package pasaran.naef;

import naef.NaefTefService;
import naef.ui.NaefDtoFacade;
import tef.MVO;
import tef.TransactionContext;
import tef.TransactionId;
import tef.TransactionIdAggregator;
import tef.skelton.Model;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoOriginator;
import tef.skelton.dto.MvoOid;

import java.rmi.RemoteException;
import java.util.*;

/**
 * DtoChanges再生成用Utilityクラス
 */
public class DtoChangesUtil {
    /**
     * 指定された時間・バージョンからDtoChangesを再生成する。
     * 時間はエポックミリ秒(Long)で指定する。
     * <p>
     * 時間・バージョンが指定されない場合、以下のデフォルト値が使用される。
     * 時間: 現在の時間
     * バージョン: 最新のバージョン
     *
     * @param targetTime
     * @param targetTx
     * @return DtoChanges
     * @throws RemoteException
     */
    public static DtoChanges getDtoChanges(Long targetTime, TransactionId.W targetTx) throws RemoteException {
        try {
            TransactionContext.beginReadTransaction();
            NaefDtoFacade dtoFacade = PasaranNaefService.getRmiServiceAccessPoint().getServiceFacade().getDtoFacade();
            MvoDtoOriginator originator = new MvoDtoOriginator(dtoFacade);

            // 時間とバージョンを指定
            TransactionContext.setTargetTime(targetTime);
            TransactionContext.setTargetVersion(targetTx);

            // バージョンからnewObjectsとchangedObjectsを取得
            Set<MVO>[] mvos = findMVO();
            Set<EntityDto> newObjects = mvo2Dto(originator, mvos[0]);
            Set<EntityDto> changedObjects = mvo2Dto(originator, mvos[1]);

            long time = TransactionContext.getTargetTime();
            TransactionId.W tx = TransactionContext.getTargetVersion();

            // DtoChangesの生成
            DtoChanges dtoChanges = new DtoChanges(originator, tx, tx, time, newObjects, changedObjects);
            return dtoChanges;
        } finally {
            TransactionContext.close();
        }
    }

    public static TransactionId.W getTx(String txString) {
        TransactionId tx = TransactionId.W.getInstance(txString);
        return tx instanceof TransactionId.W ? (TransactionId.W) tx : null;
    }

    /**
     * 指定したトランザクションで新規作成、変更があったオブジェクトを見つける
     *
     * @return [新規作成されたオブジェクトのリスト, 変更されたオブジェクトのリスト]
     */
    private static Set<MVO>[] findMVO() {
        TransactionId.W targetTx = TransactionContext.getTargetVersion();
        Set<MVO> newMvos = new HashSet<>();
        Set<MVO> updateMvos = new HashSet<>();
        for (MVO mvo : NaefTefService.instance().getMvoRegistry().list()) {
            if (mvo.getInitialVersion().serial == targetTx.serial) {
                newMvos.add(mvo);
            } else {
                if (Arrays.binarySearch(TransactionIdAggregator.getTransactionIds(mvo), targetTx) >= 0) {
                    updateMvos.add(mvo);
                }
            }
        }
        return new Set[]{newMvos, updateMvos};
    }

    /**
     * MVOからDTOを生成する
     *
     * @param originator DTOを生成するのに必要なオブジェクト
     * @param mvos       MVOのリスト
     * @return DTOのリスト
     */
    private static Set<EntityDto> mvo2Dto(MvoDtoOriginator originator, Collection<MVO> mvos) {
        Set<EntityDto> dtos = new LinkedHashSet<>();
        for (MVO mvo : mvos) {
            try {
                if ((mvo instanceof Model)
                        && NaefTefService.instance().getMvoDtoMapping()
                        .hasDtoMapping((Class<? extends Model>) mvo.getClass())) {
                    dtos.add(originator.getDto(new MvoOid(mvo.getMvoId())));
                }
            } catch (Exception e) {
                // Dto化に失敗した場合は無視する
                System.err.printf("dto化失敗. %s %s\n", mvo.getMvoId(), mvo.getClass());
            }
        }
        return dtos;
    }
}
