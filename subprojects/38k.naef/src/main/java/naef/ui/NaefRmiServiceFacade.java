package naef.ui;

import tef.skelton.AuthenticationException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NaefRmiServiceFacade extends Remote {

    public NaefDtoFacade getDtoFacade()
        throws AuthenticationException, RemoteException;
    public NaefShellFacade getShellFacade()
        throws AuthenticationException, RemoteException;
}
