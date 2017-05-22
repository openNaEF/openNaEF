package naef.ui;

import tef.skelton.AuthenticationException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NaefRmiServiceAccessPoint extends Remote {

    public NaefRmiServiceFacade getServiceFacade() throws AuthenticationException, RemoteException;
}
