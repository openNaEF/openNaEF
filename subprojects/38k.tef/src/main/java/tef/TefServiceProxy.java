package tef;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface TefServiceProxy extends Remote, DistributedTransactionComponent {

    public String getTefServiceId() throws RemoteException;

    public boolean isAlive() throws RemoteException;

    public LocalTransactionProxy createLocalTransactionProxy() throws RemoteException;

    static class Impl implements TefServiceProxy, RunsAtLocalSide {

        private static Impl instance__;

        Impl() throws RemoteException {
            synchronized (Impl.class) {
                if (instance__ != null) {
                    throw new IllegalStateException();
                } else {
                    instance__ = this;
                }
            }
        }

        public String getTefServiceId() {
            return TefService.instance().getServiceName();
        }

        public boolean isAlive() {
            return true;
        }

        public LocalTransactionProxy createLocalTransactionProxy()
                throws RemoteException {
            return new LocalTransactionProxy.Impl();
        }
    }
}
