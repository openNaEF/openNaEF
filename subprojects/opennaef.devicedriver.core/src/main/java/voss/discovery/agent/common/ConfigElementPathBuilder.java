package voss.discovery.agent.common;

public class ConfigElementPathBuilder {
    private String path = null;

    public ConfigElementPathBuilder() {
    }

    public ConfigElementPathBuilder append(String element) {
        if (path == null) {
            path = ConfigElement.ROOT_INDICATOR + element;
        } else {
            path = path + ConfigElement.PATH_DELIMITER + element;
        }
        return this;
    }

    public String toString() {
        return this.path;
    }
}