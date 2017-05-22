package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;

import java.io.Serializable;

public class SimpleChangeUnit implements Serializable, ChangeUnit {
    private static final long serialVersionUID = 1L;
    private transient NaefDto target;
    private final String absoluteName;
    private final String key;
    private String previous;
    private String current;
    private boolean isPublic = true;

    public SimpleChangeUnit(NaefDto target, String key) {
        if (key == null) {
            throw new IllegalArgumentException("no key.");
        }
        this.target = target;
        if (target == null) {
            this.absoluteName = null;
        } else {
            this.absoluteName = target.getAbsoluteName();
        }
        this.key = key;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    @Override
    public void setPublic(boolean value) {
        this.isPublic = value;
    }

    @Override
    public boolean isPublic() {
        return this.isPublic;
    }

    @Override
    public NaefDto getTarget() {
        return this.target;
    }

    @Override
    public String getTargetAbsoluteName() {
        return this.absoluteName;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getPreChangedValue() {
        return this.previous;
    }

    @Override
    public String getChangedValue() {
        return this.current;
    }

    @Override
    public boolean isChanged() {
        return Util.hasDiff(this.previous, this.current);
    }

    @Override
    public int hashCode() {
        return (this.absoluteName + ":" + this.key).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (!(o instanceof SimpleChangeUnit)) {
            return false;
        }
        SimpleChangeUnit other = (SimpleChangeUnit) o;
        if (this.absoluteName == null) {
            return other.absoluteName == null && this.key.equals(other.key);
        } else {
            return this.absoluteName.equals(other.absoluteName) && this.key.equals(other.key);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.absoluteName)
                .append("#")
                .append(key)
                .append(" [")
                .append(previous)
                .append("]=>[")
                .append(current)
                .append("]");
        return sb.toString();
    }

    public String simpleToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key)
                .append(" [")
                .append(previous)
                .append("]=>[")
                .append(current)
                .append("]");
        return sb.toString();
    }

    @Override
    public boolean isFor(NaefDto dto) {
        if (dto == null) {
            return false;
        } else if (this.target != null && DtoUtil.mvoEquals(dto, this.target)) {
            return true;
        }
        return false;
    }

    @Override
    public ChangeUnitType getChangeUnitType() {
        return ChangeUnitType.SIMPLE;
    }
}