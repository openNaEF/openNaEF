package voss.model;

@SuppressWarnings("serial")
public class AlreadyInitializedException extends RuntimeException {

    public final Object oldValue;
    public final Object newValue;

    public AlreadyInitializedException(Object oldValue, Object newValue) {
        super("An attempt was made to set " + newValue + " to the set value " + oldValue + ".");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}