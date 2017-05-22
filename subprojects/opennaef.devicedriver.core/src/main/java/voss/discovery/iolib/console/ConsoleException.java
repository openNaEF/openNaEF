package voss.discovery.iolib.console;


public class ConsoleException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConsoleException() {
    }

    public ConsoleException(String message) {
        super(message);
    }

    public ConsoleException(String message, Throwable cause) {
        super(message, cause);
    }

}