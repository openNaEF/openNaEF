package pasaran.api;

import naef.mvo.CustomerInfo;
import pasaran.naef.mvo.CustomerInfo2d;
import pasaran.pojo.CustomerInfoPOJO;
import pasaran.pojo.PasaranPOJO;
import pasaran.util.MvoUtil;
import pasaran.util.TxUtil;
import tef.MVO;
import tef.TransactionContext;
import tef.skelton.Model;
import tef.skelton.MvoSet;

import java.rmi.RemoteException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomerInfoPOJOUtil {
    /**
     * 時間を指定してcustomer-infoを取得
     * @param timeMillis 時間
     * @return CustomerInfoのリスト
     */
    public static List<PasaranPOJO> getCustomerInfos(String timeMillis) throws RemoteException {
        long start = System.currentTimeMillis();

        boolean needRestoreTime = false;
        Long restoreTime = null;
        if (TransactionContext.hasTransactionStarted()) {
            needRestoreTime = true;
            restoreTime = TransactionContext.getTargetTime();
        }

        try {
            TxUtil.beginReadTx(timeMillis, null);
            List<PasaranPOJO> pojos = CustomerInfo.home.list().stream()
                    .map(customerInfo -> {
                        CustomerInfoPOJO pojo = new CustomerInfoPOJO();
                        setCommonPOJOAttr(pojo, customerInfo);
                        pojo.name = customerInfo.getName();
                        pojo.references = getReferenceIds(customerInfo);
                        return pojo;
                    })
                    .collect(Collectors.toList());

            long end = System.currentTimeMillis();
            System.out.printf("node end. %dms\n", (end - start));

            TransactionContext.setTargetTime(restoreTime);
            return pojos;
        } finally {
            if (needRestoreTime) {
                TransactionContext.setTargetTime(restoreTime);
            }
            TxUtil.closeTx();
        }
    }

    private static List<String> getReferenceIds(CustomerInfo customerInfo) {
        Set<Model> references = new LinkedHashSet<>();

        CustomerInfo2d.CustomerInfoAttr.Holder references2d
                = CustomerInfo2d.CustomerInfoAttr.Mvo.REFERENCES_2D.get(customerInfo);
        if (references2d != null) {
            references.addAll(references2d.getReferences());
        }


        // CustomerInfo.Attr.REFERENCES が存在する場合は合成する
        MvoSet<Model> refs = CustomerInfo.Attr.REFERENCES.get(customerInfo);
        if (refs != null) {
            references.addAll(refs.get());
        }

        return references.stream()
                .map(model -> {
                    if (!(model instanceof MVO)) {
                        System.err.println("customer-info に不正な値が入っている" + model);
                    }
                    return MvoUtil.toMvoId((MVO) model);
                })
                .collect(Collectors.toList());
    }

    // TODO すべてのPasaranPOJOで使えるはず
    private static void setCommonPOJOAttr(PasaranPOJO pojo, MVO mvo) {
        pojo.mvoId = MvoUtil.toMvoId(mvo);
        pojo.readTxTime = TransactionContext.getTargetTime();
        pojo.readTxVersion = TransactionContext.getTargetVersion().getIdString();
    }
}
