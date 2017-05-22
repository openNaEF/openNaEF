package voss.discovery.iolib;

public class UnknownTargetException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String sysObjectID;

    public UnknownTargetException(String message, String sysObjectID) {
        super(message);
        this.sysObjectID = sysObjectID;
    }

    public UnknownTargetException(Throwable cause, String sysObjectID) {
        super(cause);
        this.sysObjectID = sysObjectID;
    }

    public UnknownTargetException(String message, Throwable cause, String sysObjectID) {
        super(message, cause);
        this.sysObjectID = sysObjectID;
    }

    public String getSysObjectID() {
        return this.sysObjectID;
    }
}