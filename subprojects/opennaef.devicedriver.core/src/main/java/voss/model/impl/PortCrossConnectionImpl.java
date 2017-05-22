package voss.model.impl;


import voss.model.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PortCrossConnectionImpl implements PortCrossConnection {
    private static final long serialVersionUID = 1L;
    private final ConnectionMode mode;
    private final int maxPorts;
    private String name;
    private Device device;
    private Port primaryPort;
    private Port secondaryPort;
    private Set<Port> connectedPorts = new HashSet<Port>();

    public PortCrossConnectionImpl() {
        this.mode = ConnectionMode.PORT_TO_PORT;
        this.maxPorts = 2;
    }

    public PortCrossConnectionImpl(ConnectionMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException();
        }
        this.mode = mode;
        this.maxPorts = Integer.MAX_VALUE;
    }

    public synchronized void initDevice(Device device) {
        if (this.device != null) {
            throw new AlreadyInitializedException(this.device, device);
        }
        this.device = device;
    }

    public synchronized boolean isInitialized() {
        return this.device != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new NotInitializedException();
        }
    }

    public synchronized void initName(String name) {
        if (this.name != null) {
            throw new AlreadyInitializedException(this.name, name);
        }
        this.name = name;
    }

    public synchronized String getName() {
        checkInitialized();
        return this.name;
    }

    public synchronized void addPort(Port port) {
        checkInitialized();
        if (connectedPorts.contains(port)) {
            throw new IllegalArgumentException("already added.");
        }
        if (connectedPorts.size() >= this.maxPorts) {
            throw new IllegalStateException("max ports exceeded. max=" + this.maxPorts);
        }
        this.connectedPorts.add(port);
    }

    public synchronized void addPort(Port port, Priority priority) {
        checkInitialized();
        switch (priority) {
            case PRIMARY:
                if (primaryPort != null) {
                    throw new IllegalArgumentException();
                }
                break;
            case SECONDARY:
                if (this.secondaryPort != null) {
                    throw new IllegalArgumentException();
                }
                break;
            case NORNAL:
                break;
        }
        addPort(port);
        switch (priority) {
            case PRIMARY:
                this.primaryPort = port;
                break;
            case SECONDARY:
                this.secondaryPort = port;
                break;
            case NORNAL:
                break;
        }
    }

    public synchronized boolean contains(Port port) {
        checkInitialized();
        if (port == null) {
            throw new IllegalArgumentException();
        }
        return this.connectedPorts.contains(port);
    }

    public synchronized Port getConnectedPort(Port port) {
        checkInitialized();
        if (this.mode == ConnectionMode.MULTIPLE_PORT) {
            throw new IllegalStateException();
        }
        for (Port port_ : this.connectedPorts) {
            if (port_ != port) {
                return port_;
            }
        }
        throw new IllegalStateException();
    }

    public synchronized Set<Port> getConnectedPorts(Port port) {
        checkInitialized();
        Set<Port> result = new HashSet<Port>();
        result.addAll(this.connectedPorts);
        result.remove(port);
        return Collections.unmodifiableSet(result);
    }

    public synchronized Set<Port> getConnectedPorts() {
        checkInitialized();
        Set<Port> result = new HashSet<Port>();
        result.addAll(this.connectedPorts);
        return Collections.unmodifiableSet(result);
    }

    public synchronized ConnectionMode getConnectionMode() {
        checkInitialized();
        return this.mode;
    }

    public synchronized Port getPrioritizedPort(Priority priority) {
        checkInitialized();
        switch (priority) {
            case PRIMARY:
                return this.primaryPort;
            case SECONDARY:
                return this.secondaryPort;
        }
        throw new IllegalStateException("illeagal priority: " + priority.toString());
    }

    public synchronized Priority getPriority(Port port) {
        checkInitialized();
        if (!this.connectedPorts.contains(port)) {
            throw new IllegalArgumentException("unknown port: " + port.getFullyQualifiedName());
        }
        if (this.primaryPort == port) {
            return Priority.PRIMARY;
        } else if (this.secondaryPort == port) {
            return Priority.SECONDARY;
        }
        return Priority.NORNAL;
    }

}