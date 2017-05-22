package opennaef.rest.api.config;

/**
 * コンフィグに関係するException
 */
public class ConfigurationException extends Exception {

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
