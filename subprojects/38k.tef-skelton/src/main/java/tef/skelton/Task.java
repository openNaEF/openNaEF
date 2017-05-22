package tef.skelton;

import tef.MVO;

public abstract class Task extends MVO {

    public static class TaskException extends Exception {

        public TaskException(String message) {
            super(message);
        }
    }

    private final F0<String> name_ = new F0<String>();

    public Task(MvoId id) {
        super(id);
    }

    public Task(String name) throws TaskException {
        name_.initialize(name);
    }

    public String getName() {
        return name_.get();
    }

    abstract public void cancel() throws TaskException;
    abstract public void activate() throws TaskException;
}
