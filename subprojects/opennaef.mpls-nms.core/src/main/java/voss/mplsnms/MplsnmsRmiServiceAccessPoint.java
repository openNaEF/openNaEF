package voss.mplsnms;

import java.rmi.RemoteException;

import naef.ui.NaefRmiFacade;
import naef.ui.NaefRmiServiceAccessPoint;
import tef.skelton.AuthenticationException;

public interface MplsnmsRmiServiceAccessPoint extends NaefRmiServiceAccessPoint {

    @Override public MplsnmsRmiServiceFacade getServiceFacade() 
        throws AuthenticationException, RemoteException;

    public class Impl
        extends NaefRmiFacade.Impl
        implements MplsnmsRmiServiceAccessPoint
    {
        public static MplsnmsRmiServiceAccessPoint.Impl instance;

        Impl() throws RemoteException {
            synchronized(MplsnmsRmiServiceAccessPoint.Impl.class) {
                if (instance != null) {
                    throw new RuntimeException();
                }

                instance = this;
            }
        }

        @Override public MplsnmsRmiServiceFacade getServiceFacade() 
            throws AuthenticationException, RemoteException
        {
            authenticate();

            return new MplsnmsRmiServiceFacade.Impl();
        }
    }
}
