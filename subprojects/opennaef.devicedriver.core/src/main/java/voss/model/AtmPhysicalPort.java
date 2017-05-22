package voss.model;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class AtmPhysicalPort extends AbstractPhysicalPort implements AtmPort {

    private Map<Integer, AtmVp> vps_ = new HashMap<Integer, AtmVp>();

    public AtmPhysicalPort
            (EAConverter eaConverter, String ifName, String portTypeName) {
        initDevice(eaConverter);
        initIfName(ifName);
        setPortTypeName(portTypeName);
    }

    public synchronized void addVp(AtmVp vp) {
        if (vps_.get(vp.getVpi()) != null) {
            if (vps_.get(vp.getVpi()) == vp) {
                return;
            } else {
                throw new IllegalArgumentException
                        ("Duplicate vpi: " + getDevice().getDeviceName() + " " + vp.getVpi());
            }
        } else {
            vps_.put(vp.getVpi(), vp);
        }
    }

    public synchronized AtmVp[] getVps() {
        return vps_.values().toArray(new AtmVp[0]);
    }

    public synchronized AtmVp getVp(int vpi) {
        return vps_.get(new Integer(vpi));
    }

    @Override
    public Port getParentPort() {
        return this;
    }

    @Override
    public void setParentPort(Port port) {
    }
}