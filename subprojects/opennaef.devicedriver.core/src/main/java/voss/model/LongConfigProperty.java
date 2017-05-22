package voss.model;

public abstract class LongConfigProperty implements ConfigProperty {
    private static final long serialVersionUID = 1L;
    private Long value_;

    protected LongConfigProperty() {
    }

    protected LongConfigProperty(Long value) {
        value_ = value;
    }

    public synchronized Long getValue() {
        return value_;
    }

    public int hashCode() {
        return getValue() == null ? 0 : getValue().intValue();
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LongConfigProperty another = (LongConfigProperty) o;
        return getValue() == null
                ? another.getValue() == null
                : getValue().equals(another.getValue());
    }
}