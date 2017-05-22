package voss.discovery.agent.common;

import voss.model.VlanModel;

public class SimpleExtInfoRenderer<T> implements ExtInfoRenderer<T> {
    private final String key;
    private final VlanModel model;
    private Class<T> constraint = null;

    public SimpleExtInfoRenderer(String key, VlanModel model) {
        if (key == null || model == null) {
            throw new IllegalArgumentException();
        }
        this.key = key;
        this.model = model;
    }

    public void setContraint(Class<T> constraint) {
        this.constraint = constraint;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void set(T value) {
        this.model.gainConfigurationExtInfo().put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        Object o = this.model.gainConfigurationExtInfo().get(this.key);
        if (o == null) {
            return null;
        } else {
            if (this.constraint != null) {
                if (this.constraint.isInstance(o)) {
                    return this.constraint.cast(o);
                } else {
                    throw new IllegalStateException("invalid object class:" +
                            " expected=" + this.constraint.getName() +
                            ", actual=" + o.getClass().getName() + "(" + o.toString() + ")");
                }
            } else {
                return (T) o;
            }
        }
    }

    @Override
    public boolean isDefined() {
        Object o = this.model.gainConfigurationExtInfo().get(this.key);
        return o != null;
    }
}