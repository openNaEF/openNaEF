package voss.model;


import java.util.ArrayList;
import java.util.List;

public class VplsInstanceImpl extends AbstractLogicalPort implements VplsInstance {
    private static final long serialVersionUID = 1L;
    private String vrfID;
    private final List<Port> attachmentPorts = new ArrayList<Port>();

    @Override
    public synchronized String getVplsID() {
        return this.vrfID;
    }

    @Override
    public synchronized void initVplsID(String id) {
        if (this.vrfID != null) {
            throw new IllegalArgumentException();
        }
        this.vrfID = id;
    }

    @Override
    public synchronized void addAttachmentPort(Port port) {
        if (!hasAttachmentPort(port)) {
            this.attachmentPorts.add(port);
        }
    }

    @Override
    public synchronized boolean hasAttachmentPort(Port port) {
        for (Port p : this.attachmentPorts) {
            if (p.equals(port)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void removeAttachmentPort(Port port) {
        List<Port> ports = new ArrayList<Port>();
        for (Port p : this.attachmentPorts) {
            if (!p.equals(port)) {
                ports.add(p);
            }
        }
        this.attachmentPorts.clear();
        this.attachmentPorts.addAll(ports);
    }

    @Override
    public synchronized List<Port> getAttachmentPorts() {
        List<Port> ports = new ArrayList<Port>();
        ports.addAll(this.attachmentPorts);
        return ports;
    }
}