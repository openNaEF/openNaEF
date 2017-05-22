package voss.discovery.agent.common;

import voss.model.VlanModel;

public class SimpleBooleanExtInfoRenderer implements ExtInfoRenderer<Boolean> {
    private final String key;
    private final VlanModel model;

    public SimpleBooleanExtInfoRenderer(String key, VlanModel model) {
        if (key == null || model == null) {
            throw new IllegalArgumentException();
        }
        this.key = key;
        this.model = model;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void set(Boolean value) {
        if (value == null) {
            this.model.gainConfigurationExtInfo().put(key, null);
        } else if (value.booleanValue()) {
            this.model.gainConfigurationExtInfo().put(key, Boolean.TRUE);
        } else {
            this.model.gainConfigurationExtInfo().put(key, null);
        }
    }

    @Override
    public Boolean get() {
        Object o = this.model.gainConfigurationExtInfo().get(this.key);
        if (o == null) {
            return Boolean.FALSE;
        } else {
            if (!Boolean.class.isInstance(o)) {
                throw new IllegalStateException();
            }
            return Boolean.class.cast(o);
        }
    }

    @Override
    public boolean isDefined() {
        return true;
    }
}