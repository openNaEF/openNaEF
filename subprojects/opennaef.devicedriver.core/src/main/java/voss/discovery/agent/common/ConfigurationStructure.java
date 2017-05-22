package voss.discovery.agent.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationStructure implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ConfigElement root;

    public ConfigurationStructure(ConfigElement root) {
        this.root = root;
    }

    public ConfigElement getRootElement() {
        return this.root;
    }

    public ConfigElement getByPath(String path) {
        assert path != null;
        assert !path.equals("");
        assert path.startsWith(ConfigElement.ROOT_INDICATOR);

        path = path.substring(2);
        String[] pathElements = path.split(ConfigElement.PATH_DELIMITER);
        ConfigElement current = this.root;
        for (String pathElement : pathElements) {
            ConfigElement sub = current.getElementById(pathElement);
            if (sub == null) {
                return null;
            }
            current = sub;
        }
        return current;
    }

    public List<ConfigElement> getsByPath(String path) {
        assert path != null;
        assert !path.equals("");
        assert path.startsWith(ConfigElement.ROOT_INDICATOR);

        path = path.substring(2);
        List<ConfigElement> result = new ArrayList<ConfigElement>();
        String[] pathElements = path.split(ConfigElement.PATH_DELIMITER);
        List<ConfigElement> current = new ArrayList<ConfigElement>();
        current.add(this.root);
        for (String pathElement : pathElements) {
            List<ConfigElement> sub = new ArrayList<ConfigElement>();
            for (ConfigElement c : current) {
                sub.addAll(c.getElementsById(pathElement));
            }
            if (sub.size() == 0) {
                return new ArrayList<ConfigElement>();
            }
            current = sub;
        }
        result.addAll(current);
        return result;
    }

    public List<ConfigElement> getsByAttribute(String path) {
        assert path != null;
        assert path.length() > 3;
        assert path.startsWith(ConfigElement.ROOT_INDICATOR);

        path = path.substring(2);
        String[] pathAndAttribute = path.split(ConfigElement.PATH_DELIMITER);
        List<String> pathElements = new ArrayList<String>();
        for (int i = 0; i < (pathAndAttribute.length - 1); i++) {
            pathElements.add(pathAndAttribute[i]);
        }
        String attr = pathAndAttribute[pathAndAttribute.length - 1];
        List<ConfigElement> result = new ArrayList<ConfigElement>();

        List<ConfigElement> current = new ArrayList<ConfigElement>();
        current.add(this.root);
        for (String pathElement : pathElements) {
            List<ConfigElement> sub = new ArrayList<ConfigElement>();
            for (ConfigElement c : current) {
                sub.addAll(c.getElementsById(pathElement));
            }
            if (sub.size() == 0) {
                return new ArrayList<ConfigElement>();
            }
            current = sub;
        }

        for (ConfigElement element : current) {
            if (element.hasAttribute(attr)) {
                result.add(element);
            }
        }

        return result;
    }

}