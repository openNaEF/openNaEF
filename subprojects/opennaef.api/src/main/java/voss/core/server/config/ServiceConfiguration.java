package voss.core.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public abstract class ServiceConfiguration {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ServiceConfiguration.class);

    private final File configFile;
    private final String configName;
    private final String description;
    private final List<ConfigListener> listeners = new ArrayList<ConfigListener>();

    public ServiceConfiguration(String fileName, String name, String description) throws IOException {
        assert fileName != null : "fileName is null.";
        assert name != null : "name is null.";
        assert description != null : "description is null.";

        ServiceConfigRegistry registry = ServiceConfigRegistry.getInstance();
        File config = new File(registry.getConfigDirectory(), fileName);
        if (!config.exists() || !config.isFile()) {
            throw new IOException("configName [" + fileName + "] is not exist or is not file.");
        }
        this.configFile = config;
        this.configName = name;
        this.description = description;
        registry.register(this);
    }

    public File getRootDir() {
        try {
            return ServiceConfigRegistry.getInstance().getRootDirectory();
        } catch (IOException e) {
            throw new IllegalStateException("no root dir.", e);
        }
    }

    public String getConfigName() {
        return this.configName;
    }

    public File getConfigFile() {
        return this.configFile;
    }

    public String getDescription() {
        return this.description;
    }

    protected abstract void reloadConfigurationInner() throws IOException;

    public final void reloadConfiguration() throws IOException {
        reloadConfigurationInner();
        try {
            publishServices();
        } catch (VossConfigException e) {
            throw new IOException(e);
        } catch (ExternalServiceException e) {
            throw new IOException(e);
        } catch (InventoryException e) {
            throw new IOException(e);
        }
        for (ConfigListener listener : listeners) {
            ConfigUpdateThread th = new ConfigUpdateThread(listener);
            th.start();
        }
    }

    abstract protected void publishServices() throws RemoteException, IOException, VossConfigException,
            InventoryException, ExternalServiceException;

    public synchronized void addListener(ConfigListener listener) {
        this.listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    protected <T> Class<T> loadClass(String className, Class<T> target) {
        if (className == null) {
            throw new IllegalArgumentException("null className.");
        }
        try {
            Class<?> loaded = Class.forName(className);
            if (target.isAssignableFrom(loaded)) {
                return (Class<T>) loaded;
            } else {
                throw new IllegalArgumentException("invalid config: [" + className
                        + "] isn't applicable with [" + target.getName() + "]");
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to load class.", e);
        }
    }

    private static final class ConfigUpdateThread extends Thread {
        private ConfigListener listener = null;

        public ConfigUpdateThread(ConfigListener listener) {
            this.listener = listener;
        }

        public void run() {
            this.listener.configUpdated();
        }
    }

}