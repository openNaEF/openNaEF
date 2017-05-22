package voss.nms.inventory.diff.network;

import voss.model.AbstractVlanModel;
import voss.model.Port;

import java.util.HashSet;
import java.util.Set;

public class Subnet extends AbstractVlanModel {
    private static final long serialVersionUID = 1L;

    private final Set<Port> ports = new HashSet<Port>();
    private boolean valid = true;

    public Subnet(Set<Port> ports) {
        this.ports.addAll(ports);
    }

    public Set<Port> getPorts() {
        return this.ports;
    }

    public synchronized boolean isValid() {
        return valid;
    }

    public synchronized void setValid(boolean valid) {
        this.valid = valid;
    }
}