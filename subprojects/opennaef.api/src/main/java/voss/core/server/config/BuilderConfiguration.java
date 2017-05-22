package voss.core.server.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BuilderConfiguration extends ServiceConfiguration {
    public static final String NAME = "BuilderConfig";
    public static final String FILE_NAME = "BuilderConfiguration.xml";
    public static final String DESCRIPTION = "command builder configuration.";

    public static final String KEY_ENABLE_SYSTEM_USER_UPDATE = "enable-system-user-update";

    private static BuilderConfiguration instance = null;

    public synchronized static BuilderConfiguration getInstance() {
        if (instance == null) {
            try {
                instance = new BuilderConfiguration();
                instance.reloadConfiguration();
            } catch (IOException e) {
                throw new IllegalStateException("failed to load config.", e);
            }
        }
        return instance;
    }

    private boolean isSystemUserUpdateEnabled = false;

    private BuilderConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected synchronized void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            String __isSystemUserUpdateEnabled = config.getString(KEY_ENABLE_SYSTEM_USER_UPDATE);
            boolean _isSystemUserUpdateEnabled = (__isSystemUserUpdateEnabled == null ? false :
                    Boolean.parseBoolean(__isSystemUserUpdateEnabled));

            this.isSystemUserUpdateEnabled = _isSystemUserUpdateEnabled;

            log().info("isSystemUserUpdateEnabled=" + this.isSystemUserUpdateEnabled);
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    protected void publishServices() {
    }

    public synchronized boolean isSystemUserUpdateEnabled() {
        return this.isSystemUserUpdateEnabled;
    }

    private synchronized static Logger log() {
        return LoggerFactory.getLogger(BuilderConfiguration.class);
    }
}