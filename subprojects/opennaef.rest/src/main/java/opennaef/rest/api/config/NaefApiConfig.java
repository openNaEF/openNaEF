package opennaef.rest.api.config;

import java.util.Map;
import java.util.Objects;

/**
 * naef restful api のコンフィグ
 * config/naef.yaml を読み込む
 */
public class NaefApiConfig extends Config<Map<String, Object>> {
    private static NaefApiConfig _instance = new NaefApiConfig();
    public static final String CONFIG_FILE = "naef.yaml";

    @SuppressWarnings("unchecked")
    private NaefApiConfig() {
        super(CONFIG_FILE, (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    public static NaefApiConfig instance() {
        return NaefApiConfig._instance;
    }

    private String getValue(Key key) {
        try {
            load();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return Objects.toString(config().get(key.key()), key.defaultValue());
    }

    public String apiIpAddr() {
        return getValue(Key.API_IP_ADDR);
    }

    public int apiPort() {
        return Integer.parseInt(getValue(Key.API_PORT));
    }

    public String naefAddr() {
        return getValue(Key.NAEF_ADDRESS);
    }

    public String naefPort() {
        return getValue(Key.NAEF_PORT);
    }

    public String naefServiceName() {
        return getValue(Key.NAEF_SERVICE_NAME);
    }

    public String notifierDb() {
        return getValue(Key.NOTIFIER_DB);
    }

    private enum Key {
        API_IP_ADDR("api-ip-addr", "localhost"),
        API_PORT("api-port", "2510"),
        NAEF_ADDRESS("naef-address", "127.0.0.1"),
        NAEF_PORT("naef-port", "38100"),
        NAEF_SERVICE_NAME("naef-service-name", "mplsnms"),
        NOTIFIER_DB("notifier-db", "./notifier-db"),;

        private final String _key;
        private final String _default;

        Key(String key, String defaultValue) {
            _key = key;
            _default = defaultValue;
        }

        public String key() {
            return _key;
        }

        public String defaultValue() {
            return _default;
        }
    }
}
