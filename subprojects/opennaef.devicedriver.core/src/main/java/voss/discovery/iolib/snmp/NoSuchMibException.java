package voss.discovery.iolib.snmp;

public class NoSuchMibException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoSuchMibException() {
        super();
    }

    public NoSuchMibException(String arg) {
        super(arg);
    }
}