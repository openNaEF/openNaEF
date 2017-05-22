package tef;

public class JournalRestorationException extends TefInitializationFailedException {

    private String message_;

    private String journalName_;
    private int lineCount_;

    JournalRestorationException(Throwable cause) {
        super(cause);
    }

    JournalRestorationException(String message) {
        message_ = message;
    }

    JournalRestorationException(String message, Throwable cause) {
        super(cause);
        message_ = message;
    }

    @Override
    public String getMessage() {
        return journalName_ + "-" + lineCount_
                + (message_ == null
                ? ""
                : ": " + message_);
    }

    void setJournalName(String journalName) {
        journalName_ = journalName;
    }

    void setLineCount(int lineCount) {
        lineCount_ = lineCount;
    }
}
