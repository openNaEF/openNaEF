package naef.ui;

import naef.NaefTefService;
import tef.TransactionContext;
import tef.skelton.AuthenticationException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

public interface NaefRmiFacade extends Remote {

    public static class Impl extends UnicastRemoteObject implements NaefRmiFacade {

        public Impl() throws RemoteException {
        }

        protected String getClientHostStr() {
            try {
                return NaefTefService.instance().isRmiClientAuthenticationEnabled()
                    ? getClientHost()
                    : null;
            } catch (ServerNotActiveException snae) {
                throw new RuntimeException(snae);
            }
        }

        protected String authenticate() throws AuthenticationException {
            return NaefTefService.instance().authenticate(getClientHostStr());
        }

        protected String beginWriteTransaction() throws AuthenticationException {
            return NaefTefService.instance().beginWriteTransaction(getClientHostStr());
        }

        protected void commitTransaction() {
            TransactionContext.commit();
        }

        protected String setupReadTransaction() throws AuthenticationException {
            return NaefTefService.instance().setupReadTransaction(getClientHostStr());
        }

        protected void closeTransaction() {
            TransactionContext.close();
        }
    }
}
