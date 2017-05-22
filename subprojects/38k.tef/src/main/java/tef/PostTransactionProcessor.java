package tef;

public abstract class PostTransactionProcessor {

    private final String name_;

    public PostTransactionProcessor(String name) {
        name_ = name;
    }

    public String getName() {
        return name_;
    }

    public abstract void process();
}
