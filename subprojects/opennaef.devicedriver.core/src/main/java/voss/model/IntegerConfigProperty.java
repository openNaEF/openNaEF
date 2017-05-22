package voss.model;

public abstract class IntegerConfigProperty implements ConfigProperty {
    private static final long serialVersionUID = 1L;
    private Integer value_;

    protected IntegerConfigProperty() {
    }

    protected IntegerConfigProperty(Integer value) {
        value_ = value;
    }

    public synchronized Integer getValue() {
        return value_;
    }

    public int hashCode() {
        return getValue() == null ? 0 : getValue().intValue();
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegerConfigProperty another = (IntegerConfigProperty) o;
        return getValue() == null ? another.getValue() == null : getValue().equals(another.getValue());
    }
}