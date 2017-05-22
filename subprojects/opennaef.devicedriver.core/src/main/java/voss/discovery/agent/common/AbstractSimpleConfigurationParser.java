package voss.discovery.agent.common;

import java.io.IOException;


public abstract class AbstractSimpleConfigurationParser implements ConfigurationParser {

    protected String textConfiguration;
    protected ConfigElement config = null;

    public AbstractSimpleConfigurationParser(String textConfig) {
        this.textConfiguration = textConfig;
    }

    public void setConfiguration(String config) {
        this.textConfiguration = config;
    }

    public String getConfiguration() {
        return this.textConfiguration;
    }

    public ConfigElement getResult() {
        return this.config;
    }

    public ConfigurationStructure getConfigurationStructure() {
        return new ConfigurationStructure(this.config);
    }

    public abstract void parse() throws IOException;
}