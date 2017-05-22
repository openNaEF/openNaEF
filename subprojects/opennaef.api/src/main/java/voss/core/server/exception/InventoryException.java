package voss.core.server.exception;

public class InventoryException extends Exception {
    private static final long serialVersionUID = 1L;

    public InventoryException() {
        super();
    }

    public InventoryException(String msg) {
        super(msg);
    }

    public InventoryException(Throwable cause) {
        super(cause);
    }

    public InventoryException(String msg, Throwable cause) {
        super(msg, cause);
    }

}