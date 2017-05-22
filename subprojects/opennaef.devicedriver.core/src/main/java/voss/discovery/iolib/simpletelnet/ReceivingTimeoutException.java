package voss.discovery.iolib.simpletelnet;

import voss.discovery.iolib.console.ConsoleException;


public class ReceivingTimeoutException extends ConsoleException {
    private static final long serialVersionUID = 1L;

    public ReceivingTimeoutException() {
        super("Response timeout.");
    }


}