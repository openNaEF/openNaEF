package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleException;


public class RemoteClosedException extends ConsoleException {
    private static final long serialVersionUID = 1L;

    public RemoteClosedException() {
        super("The session was closed from Remote");
    }

}