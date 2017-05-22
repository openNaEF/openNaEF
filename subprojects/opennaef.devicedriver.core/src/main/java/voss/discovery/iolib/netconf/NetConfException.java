package voss.discovery.iolib.netconf;

@SuppressWarnings("serial")
public class NetConfException extends Exception {
    private final String operation;

    public NetConfException() {
        super();
        this.operation = null;
    }

    public NetConfException(String msg) {
        super(msg);
        this.operation = null;
    }

    public NetConfException(Throwable cause) {
        super(cause);
        this.operation = null;
    }

    public NetConfException(String msg, String op) {
        super(msg);
        this.operation = op;
    }

    public NetConfException(Throwable cause, String op) {
        super(cause);
        this.operation = op;
    }

    public NetConfException(String msg, Throwable cause, String op) {
        super(msg, cause);
        this.operation = op;
    }

    public String getOperation() {
        return operation;
    }
}