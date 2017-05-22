package voss.core.server.aaa;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AAAUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final Set<String> organizationalUnits = new HashSet<String>();
    private final String displayName;
    private final String ipAddress;

    public AAAUser(String name, Set<String> ous, String displayName, String ipAddress) {
        this.name = name;
        this.organizationalUnits.addAll(ous);
        this.displayName = displayName;
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return name;
    }

    public Set<String> getOrganizationalUnits() {
        HashSet<String> result = new HashSet<String>();
        result.addAll(organizationalUnits);
        return result;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }
}