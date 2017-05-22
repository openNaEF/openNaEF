package voss.discovery.agent.common;

import java.io.IOException;

public interface ConfigurationParser {

    public abstract void setConfiguration(String config);

    public abstract String getConfiguration();

    public abstract ConfigElement getResult();

    public abstract ConfigurationStructure getConfigurationStructure();

    public abstract void parse() throws IOException;

}