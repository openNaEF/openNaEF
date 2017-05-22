package voss.nms.inventory.diff;

public class TaskException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Code causeCode;

    public TaskException(Code code) {
        super();
        this.causeCode = code;
    }

    public TaskException(String msg, Code code) {
        super(msg);
        this.causeCode = code;
    }

    public TaskException(Throwable t, Code code) {
        super(t);
        this.causeCode = code;
    }

    public TaskException(String msg, Throwable t, Code code) {
        super(msg, t);
        this.causeCode = code;
    }

    public Code getCauseCode() {
        return causeCode;
    }

    public String getCauseName() {
        if (causeCode != null) {
            return causeCode.name();
        }
        return null;
    }

    public static enum Code {
        ALREADY_RUNNING,
        ALREADY_LOCKED,;
    }
}