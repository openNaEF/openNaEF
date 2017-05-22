package voss.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VrfInstanceImpl extends AbstractLogicalPort implements VrfInstance {
    private static final long serialVersionUID = 1L;
    private String vrfID;
    private String routeDistinguisher;
    private String routeTarget;
    private final List<Port> attachmentPorts = new ArrayList<Port>();
    private final Map<Port, CidrAddress> vpnIpAddressMap = new HashMap<Port, CidrAddress>();

    @Override
    public synchronized String getVrfID() {
        return this.vrfID;
    }

    @Override
    public synchronized void initVrfID(String id) {
        if (this.vrfID != null) {
            throw new IllegalArgumentException();
        }
        this.vrfID = id;
    }

    @Override
    public synchronized String getRouteDistinguisher() {
        return this.routeDistinguisher;
    }

    @Override
    public synchronized void setRouteDistinguisher(String rd) {
        this.routeDistinguisher = rd;
    }

    @Override
    public synchronized String getRouteTarget() {
        return this.routeTarget;
    }

    @Override
    public synchronized void setRouteTarget(String rt) {
        this.routeTarget = rt;
    }

    @Override
    public synchronized void addAttachmentPort(Port port) {
        if (!hasAttachmentPort(port)) {
            this.attachmentPorts.add(port);
        }
    }

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

    @Override
    public synchronized void addVpnIpAddress(Port port, CidrAddress vpnIpAddress) {
        if (port == null) {
            return;
        } else if (!this.attachmentPorts.contains(port)) {
            throw new IllegalStateException("port is not an attachment-port: " + port.getFullyQualifiedName());
        }
        this.vpnIpAddressMap.put(port, vpnIpAddress);
    }

    @Override
    public synchronized void removeVpnIpAddress(Port port) {
        this.vpnIpAddressMap.remove(port);
    }

    @Override
    public synchronized CidrAddress getVpnIpAddress(Port port) {
        return this.vpnIpAddressMap.get(port);
    }

    @Override
    public synchronized Map<Port, CidrAddress> getVpnPortAddressMap() {
        return new HashMap<Port, CidrAddress>(this.vpnIpAddressMap);
    }

}