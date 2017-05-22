package voss.nms.inventory.diff;

public class DuplicationException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicationException() {
        super();
    }

    public DuplicationException(String msg) {
        super(msg);
    }
}