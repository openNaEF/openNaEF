package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.server.util.DtoUtil;

import java.io.Serializable;
import java.util.*;

public class CollectionChangeUnit implements Serializable, ChangeUnit {
    private static final long serialVersionUID = 1L;
    private transient NaefDto target;
    private final String absoluteName;
    private final String key;
    private final List<String> preChangedValues = new ArrayList<String>();
    private final Set<String> added = new HashSet<String>();
    private final Map<String, String> addedCaption = new HashMap<String, String>();
    private final Set<String> removed = new HashSet<String>();
    private final Map<String, String> removedCaption = new HashMap<String, String>();
    private boolean isPublic = true;

    public CollectionChangeUnit(NaefDto target, String key, List<String> preChanged) {
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
        this.preChangedValues.addAll(preChanged);
    }

    public Set<String> getRemovedValues() {
        return removed;
    }

    public Set<String> getAddedValues() {
        return added;
    }

    public void addValue(String value) {
        this.added.add(value);
    }

    public void addValues(List<String> values) {
        this.added.addAll(values);
    }

    public void addValue(String value, String caption) {
        this.added.add(value);
        this.addedCaption.put(value, caption);
    }

    public void removeValue(String value) {
        this.removed.add(value);
    }

    public void removeValues(List<String> values) {
        this.removed.addAll(values);
    }

    public void removeValue(String value, String caption) {
        this.removed.add(value);
        this.removedCaption.put(value, caption);
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

    public List<String> getPreChangedValues() {
        List<String> preChanged = new ArrayList<String>();

        return preChanged;
    }

    @Override
    public String getPreChangedValue() {
        StringBuilder sb = new StringBuilder("[");
        for (String remove : removed) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append("-");
            String caption = this.removedCaption.get(remove);
            if (caption != null) {
                sb.append(caption);
            } else {
                sb.append(remove);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String getChangedValue() {
        StringBuilder sb = new StringBuilder("[");
        for (String add : added) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append("+");
            String caption = this.addedCaption.get(add);
            if (caption != null) {
                sb.append(caption);
            } else {
                sb.append(add);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean isChanged() {
        return added.size() > 0 || removed.size() > 0;
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
        } else if (!(o instanceof CollectionChangeUnit)) {
            return false;
        }
        CollectionChangeUnit other = (CollectionChangeUnit) o;
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
                .append(simpleToString());
        return sb.toString();
    }

    public String simpleToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key)
                .append(" [");
        for (String add : added) {
            sb.append("+").append(add);
            String caption = this.addedCaption.get(add);
            if (caption != null) {
                sb.append("(").append(caption).append(")");
            }
            sb.append(",");
        }
        for (String remove : removed) {
            sb.append("-").append(remove);
            String caption = this.removedCaption.get(remove);
            if (caption != null) {
                sb.append("(").append(caption).append(")");
            }
            sb.append(", ");
        }
        sb.append("]");
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
        return ChangeUnitType.COLLECTION;
    }
}