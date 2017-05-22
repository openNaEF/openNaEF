package voss.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("serial")
public abstract class AbstractVlanModel implements VlanModel {
    private final Map<String, FunctionalComponent> functions = new HashMap<String, FunctionalComponent>();
    private String userDescription;
    private String systemDescription;
    private ConfigProperty[] configProperties_ = new ConfigProperty[0];
    private final List<String> remarks = new ArrayList<String>();

    protected synchronized void addProperty(ConfigProperty configProperty) {
        if (configProperty == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (selectConfigProperty(configProperty.getClass()) != null) {
            throw new IllegalStateException("duplicated config property: "
                    + getClass().getName() + ": "
                    + configProperty.getClass().getName());
        }

        ConfigProperty[] newConfigProperties = new ConfigProperty[configProperties_.length + 1];
        System.arraycopy(configProperties_, 0, newConfigProperties, 0,
                configProperties_.length);
        newConfigProperties[newConfigProperties.length - 1] = configProperty;
        configProperties_ = newConfigProperties;
    }

    public synchronized ConfigProperty selectConfigProperty(Class<?> targetClass) {
        for (int i = 0; i < configProperties_.length; i++) {
            if (targetClass.isInstance(configProperties_[i])) {
                return configProperties_[i];
            }
        }
        return null;
    }

    public synchronized ConfigProperty[] getConfigProperties() {
        return configProperties_;
    }

    private NonConfigurationExtInfo nonConfigurationExtInfo_;

    public synchronized NonConfigurationExtInfo getNonConfigurationExtInfo() {
        return nonConfigurationExtInfo_;
    }

    public synchronized NonConfigurationExtInfo gainNonConfigurationExtInfo() {
        if (nonConfigurationExtInfo_ == null) {
            nonConfigurationExtInfo_ = new NonConfigurationExtInfo();
        }

        return nonConfigurationExtInfo_;
    }

    public synchronized ConfigurationExtInfo getConfigurationExtInfo() {
        return (ConfigurationExtInfo) selectConfigProperty(ConfigurationExtInfo.class);
    }

    public synchronized ConfigurationExtInfo gainConfigurationExtInfo() {
        ConfigurationExtInfo result = getConfigurationExtInfo();
        if (result == null) {
            addProperty(new ConfigurationExtInfo());
            result = getConfigurationExtInfo();
            if (result == null) {
                throw new IllegalStateException();
            }
        }

        return result;
    }

    @Override
    public void addFunction(FunctionalComponent function) {
        if (this.functions.keySet().contains(function.getKey())) {
            return;
        }
        this.functions.put(function.getKey(), function);
    }

    @Override
    public List<FunctionalComponent> getFunctions() {
        List<FunctionalComponent> result = new ArrayList<FunctionalComponent>();
        for (FunctionalComponent function : this.functions.values()) {
            if (function == null) {
                continue;
            }
            result.add(function);
        }
        return result;
    }

    @Override
    public FunctionalComponent getFunction(String key) {
        return this.functions.get(key);
    }

    public synchronized String getSystemDescription() {
        return this.systemDescription;
    }

    public synchronized void setSystemDescription(String description) {
        this.systemDescription = description;
    }

    public synchronized String getUserDescription() {
        return this.userDescription;
    }

    public synchronized void setUserDescription(String description) {
        this.userDescription = description;
    }

    public synchronized List<String> getRemarks() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.remarks);
        return result;
    }

    public synchronized void addRemarks(String remark) {
        if (remark == null) {
            return;
        }
        this.remarks.add(remark);
    }

    public synchronized boolean removeRemarks(String remark) {
        if (remark == null) {
            return false;
        }
        return this.remarks.remove(remark);
    }

    public synchronized void clearRemarks() {
        this.remarks.clear();
    }
}