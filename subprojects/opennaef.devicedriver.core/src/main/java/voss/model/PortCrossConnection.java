package voss.model;

import java.io.Serializable;
import java.util.Set;


public interface PortCrossConnection extends Serializable {
    void initDevice(Device device);

    String getName();

    void initName(String name);

    void addPort(Port port);

    void addPort(Port port, Priority priority);

    boolean contains(Port port);

    Port getConnectedPort(Port port);

    Set<Port> getConnectedPorts(Port port);

    Set<Port> getConnectedPorts();

    Priority getPriority(Port port);

    Port getPrioritizedPort(Priority priority);

    ConnectionMode getConnectionMode();

    public static enum ConnectionMode {
        PORT_TO_PORT,
        MULTIPLE_PORT,;
    }

    public static enum Priority {
        PRIMARY,
        SECONDARY,
        NORNAL,;
    }
}