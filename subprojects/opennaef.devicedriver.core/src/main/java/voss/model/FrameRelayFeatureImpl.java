package voss.model;


import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class FrameRelayFeatureImpl extends AbstractLogicalPort implements FrameRelayFeature {
    private final List<FrameRelayDLCIEndPoint> endpoints = new ArrayList<FrameRelayDLCIEndPoint>();
    private Port parent = null;
    private FrameRelayEncapsulationType type = null;

    public void initPhysicalPort(SerialPort parent) {
        if (parent == null) {
            throw new IllegalArgumentException();
        }
        if (this.parent != null) {
            throw new AlreadyInitializedException(this.parent, parent);
        }
        this.parent = parent;
    }

    public boolean hasEndPoint(FrameRelayDLCIEndPoint ep) {
        for (FrameRelayDLCIEndPoint endpoint : endpoints) {
            if (ep.getDLCI() == endpoint.getDLCI()) {
                return true;
            }
        }
        return false;
    }

    public void addEndPoint(FrameRelayDLCIEndPoint ep) {
        if (hasEndPoint(ep)) {
            throw new IllegalArgumentException("already have DLCI: " + ep.getDLCI());
        }
        this.endpoints.add(ep);
    }

    public FrameRelayEncapsulationType getEncapsulationType() {
        return type;
    }

    public FrameRelayDLCIEndPoint getEndPoint(int dlci) {
        for (FrameRelayDLCIEndPoint endpoint : endpoints) {
            if (dlci == endpoint.getDLCI()) {
                return endpoint;
            }
        }
        return null;
    }

    public List<FrameRelayDLCIEndPoint> getEndPoints() {
        ArrayList<FrameRelayDLCIEndPoint> result = new ArrayList<FrameRelayDLCIEndPoint>();
        result.addAll(this.endpoints);
        return result;
    }

    public Port getParentPort() {
        return this.parent;
    }

    public boolean removeEndPoint(FrameRelayDLCIEndPoint ep) {
        FrameRelayDLCIEndPoint del = getEndPoint(ep.getDLCI());
        if (del == null) {
            return false;
        }
        return this.endpoints.remove(del);
    }

    public void setEncapsulationType(FrameRelayEncapsulationType type) {
        this.type = type;
    }

    @Override
    public void setParentPort(Port port) {
        this.parent = port;
    }

}