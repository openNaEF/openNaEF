package voss.nms.inventory.diff;

import voss.core.server.builder.ChangeUnit;

import java.io.Serializable;

public class Diff implements Serializable {
    private static final long serialVersionUID = 1L;
    private DiffUnit parentUnit;
    private final String attributeName;
    private final String currentValue;
    private final String changedValue;

    public Diff(String attributeName, String currentValue, String changedValue) {
        this.attributeName = attributeName;
        this.currentValue = currentValue;
        this.changedValue = changedValue;
    }

    public Diff(ChangeUnit unit) {
        this.attributeName = unit.getKey();
        this.currentValue = unit.getPreChangedValue();
        this.changedValue = unit.getChangedValue();
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public String getCurrentValue() {
        return this.currentValue;
    }

    public String getChangedValue() {
        return this.changedValue;
    }

    public DiffUnit getParentUnit() {
        return parentUnit;
    }

    public void setParentUnit(DiffUnit parentUnit) {
        this.parentUnit = parentUnit;
    }

    @Override
    public String toString() {
        return attributeName + ": [" + currentValue + "] => [" + changedValue + "]";
    }

    public int hashCode() {
        return ("Diff:" + attributeName).hashCode();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        }
        return this.attributeName.equals(((Diff) o).getAttributeName());
    }
}