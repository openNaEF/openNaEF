package voss.discovery.agent.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WarningMessenger implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> warnings = new ArrayList<String>();

    public WarningMessenger() {
    }

    public void addWarning(String warn) {
        if (warn == null) {
            return;
        }
        this.warnings.add(warn);
    }

    public boolean hasWarnings() {
        return this.warnings.size() > 0;
    }

    public List<String> getWarnings() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.warnings);
        return result;
    }

    public void clear() {
        this.warnings.clear();
    }
}