package voss.model.container;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class HistoryTag {
    private final long time;
    private final String description;
    private final Map<String, String> properties = Collections.synchronizedMap(new HashMap<String, String>());

    public HistoryTag() {
        this.time = System.currentTimeMillis();
        this.description = null;
    }

    public HistoryTag(long time) {
        this.time = time;
        this.description = null;
    }

    public HistoryTag(String description) {
        this.time = System.currentTimeMillis();
        this.description = description;
    }

    public HistoryTag(long time, String description) {
        this.time = time;
        this.description = description;
    }

    public long getTime() {
        return this.time;
    }

    public String getDescription() {
        return this.description;
    }

    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public String getProperty(String key) {
        return this.properties.get(key);
    }

    public Map<String, String> getProperties() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(this.properties);
        return result;
    }

    public void resetProperty() {
        this.properties.clear();
    }

    public static Comparator<HistoryTag> getComparator() {
        return new Comparator<HistoryTag>() {
            public int compare(HistoryTag o1, HistoryTag o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                if (o1 == o2) {
                    return 0;
                }
                if (o1.time > o2.time) {
                    return 1;
                } else if (o2.time > o1.time) {
                    return -1;
                }
                return 0;
            }
        };
    }
}