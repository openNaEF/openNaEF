package voss.multilayernms.inventory.lock;

public class LockException extends Exception {
    private static final long serialVersionUID = 1L;

    public LockException(String msg) {
        super(msg);
    }

}