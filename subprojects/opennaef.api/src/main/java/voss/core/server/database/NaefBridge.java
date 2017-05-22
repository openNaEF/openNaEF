package voss.core.server.database;

import naef.ui.NaefDtoFacade;
import naef.ui.NaefShellFacade;
import voss.core.server.exception.ExternalServiceException;

import java.io.IOException;
import java.rmi.RemoteException;

public interface NaefBridge {
    NaefDtoFacade getDtoFacade() throws IOException, RemoteException, ExternalServiceException;

    NaefShellFacade getShellFacade() throws IOException, RemoteException, ExternalServiceException;

    void close() throws IOException, RemoteException, ExternalServiceException;
}