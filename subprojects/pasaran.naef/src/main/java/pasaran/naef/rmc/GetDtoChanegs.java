package pasaran.naef.rmc;

import lib38k.rmc.MethodCall2;
import lib38k.rmc.MethodExec2;
import pasaran.naef.DtoChangesUtil;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import java.rmi.RemoteException;

/**
 * DtoChangesを生成するrmc
 *
 * TefServiceConfig.xml <rmc-server> セクションへ以下を追加する必要がある
 * <rmc-service call="pasaran.naef.rmc.GetDtoChanegs$Call" exec="pasaran.naef.rmc.GetDtoChanegs$Exec"/>
 */
public class GetDtoChanegs {
    public static class Exec extends MethodExec2<GetDtoChanegs.Call, DtoChanges, Long, TransactionId.W> {

        @Override
        public DtoChanges execute(Long time, TransactionId.W version) {
            try {
                return DtoChangesUtil.getDtoChanges(time, version);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            throw new IllegalStateException();
        }
    }

    public static class Call extends MethodCall2<DtoChanges, Long, TransactionId.W> {
        public Call(Long arg1, TransactionId.W arg2) {
            super(arg1, arg2);
        }
    }
}
