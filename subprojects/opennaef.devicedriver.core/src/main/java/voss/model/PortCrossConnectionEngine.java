package voss.model;

import java.io.Serializable;
import java.util.Set;


public interface PortCrossConnectionEngine extends Serializable {
    void addPortCrossConnection(PortCrossConnection connection);

    <T extends Port> PortCrossConnection getPortCrossConnection(T port);

    Set<PortCrossConnection> getPortCrossConnections();
}