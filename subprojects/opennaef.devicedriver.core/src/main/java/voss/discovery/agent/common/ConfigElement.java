package voss.discovery.agent.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigElement implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String PATH_DELIMITER = ">";
    public static final String ROOT_INDICATOR = PATH_DELIMITER + PATH_DELIMITER;
    private ConfigElement parent = null;
    private List<ConfigElement> children = new ArrayList<ConfigElement>();
    private List<String> attributes = new ArrayList<String>();
    private final String id;
    private boolean writable = true;

    public ConfigElement(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void addElement(ConfigElement child) {
        assertWritable();
        this.children.add(child);
        child.setParent(this);
    }

    public void setParent(ConfigElement parent) {
        assertWritable();
        this.parent = parent;
    }

    public ConfigElement getParent() {
        return this.parent;
    }

    public List<ConfigElement> getElements() {
        List<ConfigElement> result = new ArrayList<ConfigElement>();
        result.addAll(this.children);
        return result;
    }

    public ConfigElement getElementById(String id) {
        for (ConfigElement element : this.children) {
            if (element.getId().equals(id)) {
                return element;
            } else if (element.getId().matches(id)) {
                return element;
            }
        }
        return null;
    }

    public List<ConfigElement> getElementsById(String regex) {
        List<ConfigElement> result = new ArrayList<ConfigElement>();
        for (ConfigElement child : this.children) {
            if (child.getId().equals(regex)) {
                result.add(child);
            } else if (child.getId().matches(regex)) {
                result.add(child);
            }
        }
        return result;
    }

    public List<ConfigElement> getElementsByAttribute(String regex) {
        List<ConfigElement> result = new ArrayList<ConfigElement>();
        for (ConfigElement child : this.children) {
            if (child.getId().equals(regex)) {
                result.add(child);
            } else if (child.getId().matches(regex)) {
                result.add(child);
            }
        }
        return result;
    }

    public void addAttribute(String str) {
        assertWritable();
        this.attributes.add(str);
    }

    public void assertWritable() {
        if (!writable) {
            throw new IllegalStateException("ConfigElements[" + id + "] is read only.");
        }
    }

    public boolean hasAttribute(String regex) {
        assert regex != null;
        for (String attribute : attributes) {
            if (attribute.matches(regex)) {
                return true;
            }
        }
        for (String attribute : attributes) {
            for (String s : attribute.split("[ \t]+")) {
                s = s.trim();
                if (s.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getAttributes() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.attributes);
        return result;
    }

    public List<String> getAttributes(String name) {
        List<String> result = new ArrayList<String>();
        for (String attr : this.attributes) {
            if (attr.equals(name)) {
                result.add(attr);
            } else if (attr.matches(name)) {
                result.add(attr);
            }
        }
        return result;
    }

    public String getAttribute(String name) {
        for (String attr : this.attributes) {
            if (attr.equals(name)) {
                return attr;
            } else if (attr.matches(name)) {
                return attr;
            }
        }
        return null;
    }

    public String getAttributeValue(String pattern) {
        Pattern p = Pattern.compile(pattern);
        for (String attr : this.attributes) {
            Matcher matcher = p.matcher(attr);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public String getCurrentPath() {
        return getCurrentPath("");
    }

    public String getCurrentPath(String path) {
        if (this.parent == null) {
            return PATH_DELIMITER + path;
        }
        return this.parent.getCurrentPath(path) + PATH_DELIMITER + this.id;
    }

    public String toString() {
        String id = getCurrentPath();
        String result = "id:" + id + " [\r\n";
        for (ConfigElement elem : this.children) {
            String sub = elem.toString();
            for (String s : sub.split("\r\n")) {
                result += "\t" + s + "\r\n";
            }
        }
        for (String attr : this.attributes) {
            result += "\tattr: [" + attr + "]\r\n";
        }
        result = result + "]\r\n";
        return result;
    }

    public void setReadOnly() {
        this.writable = false;
    }

    public void setReadOnlyAll() {
        this.writable = false;
        for (ConfigElement child : this.children) {
            child.setReadOnlyAll();
        }
    }
}