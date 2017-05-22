package voss.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class VlanModelConstants implements Serializable {
    private static final long serialVersionUID = 1L;

    private static transient final Map<String, VlanModelConstants> instances__ = new HashMap<String, VlanModelConstants>();

    public static VlanModelConstants getInstance(String instanceId) {
        synchronized (instances__) {
            return (VlanModelConstants) instances__.get(instanceId);
        }
    }

    private String id_;

    protected VlanModelConstants() {
    }

    protected VlanModelConstants(String id) {
        synchronized (instances__) {
            String instanceId = getInstanceId(getClass(), id);

            if (getInstance(instanceId) != null) {
                throw new IllegalStateException("duplicated id found: " + instanceId);
            }

            if (id == null) {
                throw new IllegalArgumentException();
            }

            id_ = id;

            instances__.put(instanceId, VlanModelConstants.this);
        }
    }

    protected static String getInstanceId(Class<?> clazz, String id) {
        return clazz.getName() + ":" + id;
    }

    public synchronized String getId() {
        return id_;
    }

    public int hashCode() {
        return getId().hashCode();
    }

    public boolean equals(Object o) {
        return o != null
                && o.getClass() == getClass()
                && ((VlanModelConstants) o).getId().equals(getId());
    }
}