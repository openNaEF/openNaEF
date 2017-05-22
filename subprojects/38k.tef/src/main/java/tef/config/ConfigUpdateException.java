package tef.config;

public class ConfigUpdateException extends Exception {

    public ConfigUpdateException() {
    }

    public ConfigUpdateException(String message) {
        super(message);
    }

    public ConfigUpdateException(Throwable cause) {
        super(cause);
    }

    public ConfigUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
