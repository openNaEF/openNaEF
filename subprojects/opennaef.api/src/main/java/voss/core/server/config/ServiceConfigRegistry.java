package voss.core.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceConfigRegistry {
    public static final String KEY_ROOT_DIR = "vossnms.root.dir";
    public static final String DEFAULT_ROOT_DIR_NAME = "./";
    public static final String CONFIG_DIR_NAME = "config";
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigRegistry.class);
    private final File rootDir;
    private final File configDir;
    private final ConcurrentHashMap<String, ServiceConfiguration> configs =
            new ConcurrentHashMap<String, ServiceConfiguration>();

    private static ServiceConfigRegistry registry = null;

    public synchronized static ServiceConfigRegistry getInstance() throws IOException {
        if (registry == null) {
            registry = new ServiceConfigRegistry();
        }
        return registry;
    }

    private ServiceConfigRegistry() throws IOException {
        String dirName = System.getProperty(KEY_ROOT_DIR, DEFAULT_ROOT_DIR_NAME);
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("dirName [" + dirName + "] is not exist or is not directory.");
        }
        this.rootDir = dir;
        log.info("rootDir = " + dir.getAbsolutePath());
        File configDir = new File(rootDir, CONFIG_DIR_NAME);
        if (!configDir.exists() || !configDir.isDirectory()) {
            throw new IOException("dirName [" + configDir.getCanonicalPath()
                    + "] is not exist or is not directory.");
        }
        this.configDir = configDir;
    }

    public File getRootDirectory() {
        return this.rootDir;
    }

    public File getConfigDirectory() {
        return this.configDir;
    }

    public synchronized void register(ServiceConfiguration config) {
        String name = config.getConfigName();
        ServiceConfiguration old = configs.putIfAbsent(name, config);
        if (old != null) {
            log.warn("duplicated config found: " + name);
            throw new IllegalArgumentException();
        } else {
            log.info("config registered: " + name);
        }
    }

    public synchronized void unregister(String name) {
        this.configs.remove(name);
    }

    public synchronized ServiceConfiguration getConfiguration(String name) {
        return this.configs.get(name);
    }

    public synchronized Set<String> getConfigNames() {
        Set<String> result = new HashSet<String>();
        result.addAll(this.configs.keySet());
        return result;
    }

    public synchronized void reloadAll() {
        for (Map.Entry<String, ServiceConfiguration> entry : configs.entrySet()) {
            try {
                entry.getValue().reloadConfiguration();
            } catch (IOException e) {
                log.warn("failed to reload config: [" + entry.getKey() + "]", e);
            }
        }
    }
}