package voss.core.common.diff;

public class DiffEntry<T> {
    private String key;
    private T base;
    private T target;
    private final ValueResolver<T> resolver;

    public DiffEntry() {
        this.resolver = null;
    }

    public DiffEntry(ValueResolver<T> resolver) {
        this.resolver = resolver;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setBase(T base) {
        if (this.base != null) {
            throw new IllegalStateException("cannot update base.");
        }
        this.base = base;
    }

    public void setTarget(T target) {
        if (this.target != null) {
            throw new IllegalStateException("cannot update target.");
        }
        this.target = target;
    }

    public T getBase() {
        return this.base;
    }

    public T getTarget() {
        return this.target;
    }

    public boolean isCreated() {
        return this.base == null && this.target != null;
    }

    public boolean isDeleted() {
        return this.base != null && this.target == null;
    }

    public boolean isUpdated() {
        if (this.base == null && this.target == null) {
            return false;
        } else if (this.base != null && this.target != null) {
            return !equals(this.base, this.target);
        }
        return false;
    }

    public boolean isSame() {
        if (this.base == null && this.target == null) {
            return true;
        } else if (this.base != null && this.target != null) {
            return equals(this.base, this.target);
        }
        return false;
    }

    public boolean isVacant() {
        return this.base == null && this.target == null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isCreated()) {
            sb.append("Create");
        } else if (isDeleted()) {
            sb.append("Delete");
        } else if (isSame()) {
            sb.append("Same");
        } else if (isUpdated()) {
            sb.append("Update");
        } else if (isVacant()) {
            sb.append("Vacant");
        } else {
            sb.append("UNKNOWN");
        }
        if (this.key != null) {
            sb.append(" ").append(key).append(": ");
        }
        if (this.resolver == null) {
            sb.append("[");
            if (this.base != null) {
                sb.append(this.base.toString());
            } else {
                sb.append("<null>");
            }
            sb.append("]->[");
            if (this.target != null) {
                sb.append(this.target.toString());
            } else {
                sb.append("<null>");
            }
            sb.append("]");
        } else {
            sb.append("[")
                    .append(this.resolver.getValue(this.base))
                    .append("]->[")
                    .append(this.resolver.getValue(this.target))
                    .append("]");
        }
        return sb.toString();
    }

    private boolean equals(T base, T target) {
        if (this.resolver == null) {
            return base.equals(target);
        } else {
            String baseValue = this.resolver.getValue(base);
            String targetValue = this.resolver.getValue(target);
            return baseValue.equals(targetValue);
        }
    }

}