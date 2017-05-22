package voss.mplsnms;

import java.rmi.RemoteException;

import naef.ui.NaefDtoFacade;
import naef.ui.NaefRmiFacade;
import naef.ui.NaefRmiServiceFacade;
import naef.ui.NaefShellFacade;
import tef.skelton.AuthenticationException;
import tef.skelton.dto.MvoDtoOriginator;

public interface MplsnmsRmiServiceFacade extends NaefRmiServiceFacade {

    public class Impl extends NaefRmiFacade.Impl implements MplsnmsRmiServiceFacade {

        public Impl() throws RemoteException {
        }

        @Override public NaefDtoFacade getDtoFacade() 
            throws AuthenticationException, RemoteException
        {
            authenticate();
            return new NaefDtoFacade.Impl();
        }

        @Override public NaefShellFacade getShellFacade() 
            throws AuthenticationException, RemoteException
        {
            authenticate();
            return new NaefShellFacade.Impl(new MvoDtoOriginator(new NaefDtoFacade.Impl()));
        }
    }
}
