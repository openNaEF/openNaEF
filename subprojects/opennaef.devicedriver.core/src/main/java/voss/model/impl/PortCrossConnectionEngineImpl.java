package voss.model.impl;


import voss.model.Port;
import voss.model.PortCrossConnection;
import voss.model.PortCrossConnectionEngine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PortCrossConnectionEngineImpl implements PortCrossConnectionEngine {
    private static final long serialVersionUID = 1L;
    private Set<PortCrossConnection> connections = new HashSet<PortCrossConnection>();

    public void addPortCrossConnection(PortCrossConnection connection) {
        if (this.connections.contains(connection)) {
            throw new IllegalArgumentException();
        }
        this.connections.add(connection);
    }

    public PortCrossConnection getPortCrossConnection(Port port) {
        for (PortCrossConnection connection : this.connections) {
            if (connection.contains(port)) {
                return connection;
            }
        }
        return null;
    }

    public Set<PortCrossConnection> getPortCrossConnections() {
        Set<PortCrossConnection> result = new HashSet<PortCrossConnection>();
        result.addAll(this.connections);
        return Collections.unmodifiableSet(result);
    }

}