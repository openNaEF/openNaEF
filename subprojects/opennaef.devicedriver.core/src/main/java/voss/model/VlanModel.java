package voss.model;

import java.util.List;


public interface VlanModel extends java.io.Serializable {

    public String getSystemDescription();

    public void setSystemDescription(String description);

    public String getUserDescription();

    public void setUserDescription(String description);

    public List<String> getRemarks();

    public void addRemarks(String remarks);

    public boolean removeRemarks(String remarks);

    public void clearRemarks();

    public void addFunction(FunctionalComponent function);

    public List<FunctionalComponent> getFunctions();

    public FunctionalComponent getFunction(String key);

    public NonConfigurationExtInfo getNonConfigurationExtInfo();

    public NonConfigurationExtInfo gainNonConfigurationExtInfo();

    public ConfigurationExtInfo getConfigurationExtInfo();

    public ConfigurationExtInfo gainConfigurationExtInfo();
}