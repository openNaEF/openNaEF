package naef.ui;

import tef.MVO;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.AuthenticationException;
import tef.skelton.Model;
import tef.skelton.SkeltonTefService;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.DtoOriginator;
import tef.skelton.dto.EntityDto;
import tef.ui.shell.InternalShellInvocation;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface NaefShellFacade extends Remote {

    public static class ShellException extends Exception {

        private final String command_;
        private final Integer batchLineCount_;

        public ShellException(String command, Integer batchLineCount, String message) {
            super(message);

            command_ = command;
            batchLineCount_ = batchLineCount;
        }

        public String getCommand() {
            return command_;
        }

        public Integer getBatchLineCount() {
            return batchLineCount_;
        }
    }

    public DtoChanges executeBatch(List<String> commands)
        throws AuthenticationException, ShellException, RemoteException;

    public class Impl extends NaefRmiFacade.Impl implements NaefShellFacade {

        private final DtoOriginator originator_;

        public Impl(DtoOriginator originator) throws RemoteException {
            originator_ = originator;
        }

        @Override public DtoChanges executeBatch(List<String> commands)
            throws AuthenticationException, ShellException
        {
            DtoChanges result;

            InternalShellInvocation shellInvocation = new InternalShellInvocation();
            try {
                beginWriteTransaction();
                try {
                    shellInvocation.processBatch(commands.toArray(new String[0]));

                    Set<MVO> newMvos = new HashSet<MVO>(TransactionContext.getNewObjects());
                    Set<MVO> changedMvos = new HashSet<MVO>(TransactionContext.getChangedObjects());
                    changedMvos.removeAll(newMvos);

                    Set<EntityDto> newDtos = buildDtos(newMvos);
                    Set<EntityDto> changedDtos = buildDtos(changedMvos);

                    TransactionId.W version = (TransactionId.W) TransactionContext.getTransactionId();
                    result = new DtoChanges(
                        originator_,
                        version,
                        version,
                        TransactionContext.getTargetTime(),
                        newDtos,
                        changedDtos);

                    commitTransaction();
                } finally {
                    closeTransaction();
                }
            } catch (InternalShellInvocation.InvocationException ie) {
                throw new ShellException(ie.getCommand(), ie.getBatchLineCount(), ie.getMessage());
            } finally {
                try {
                    shellInvocation.close();
                } catch (IOException ioe) {
                    tef.TefService.instance().logError("internal shell invocation", ioe);
                    throw new RuntimeException(ioe);
                }
            }

            return result;
        }

        private Set<EntityDto> buildDtos(Collection<MVO> mvos) {
            SkeltonTefService tefService = SkeltonTefService.instance();
            Set<EntityDto> result = new HashSet<EntityDto>();
            for (MVO mvo : mvos) {
                if ((mvo instanceof Model)
                    && tefService.getMvoDtoMapping().hasDtoMapping((Class<? extends Model>) mvo.getClass()))
                {
                    result.add(tefService.getMvoDtoFactory().build(originator_, mvo));
                }
            }
            return result;
        }
    }
}
