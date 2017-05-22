package tef.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigUpdateNotifier {

    private static Map<String, List<ConfigUpdateListener>> listenersMap__
            = new HashMap<String, List<ConfigUpdateListener>>();

    private ConfigUpdateNotifier() {
    }

    public static synchronized void addListener
            (String configName, ConfigUpdateListener listener) {
        List<ConfigUpdateListener> listeners = listenersMap__.get(configName);
        if (listeners == null) {
            listeners = new ArrayList<ConfigUpdateListener>();
            listenersMap__.put(configName, listeners);
        }

        listeners.add(listener);
    }

    public static synchronized void removeListener(ConfigUpdateListener listener) {
        for (String configName : listenersMap__.keySet()) {
            List<ConfigUpdateListener> listeners = listenersMap__.get(configName);
            listeners.remove(listener);
        }
    }

    public static synchronized int notifyUpdated(String updatedConfigName)
            throws ConfigUpdateException {
        List<ConfigUpdateListener> listeners = listenersMap__.get(updatedConfigName);

        if (listeners == null) {
            throw new ConfigUpdateException("no such config.");
        }

        for (ConfigUpdateListener listener : listeners) {
            listener.update();
        }

        return listeners.size();
    }

    public static String[] getConfigNames() {
        return listenersMap__.keySet().toArray(new String[0]);
    }
}
