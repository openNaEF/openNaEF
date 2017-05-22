package voss.model;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class AtmVp extends AbstractLogicalPort {

    public class Pcr extends LongConfigProperty {

        Pcr() {
        }

        public Pcr(Long value) {
            super(value);
        }
    }

    private AtmPort physicalPort_;
    private int vpi_;

    private Map<Integer, AtmPvc> pvcs_ = new HashMap<Integer, AtmPvc>();

    public AtmVp(AtmPort physicalPort, String ifname, int vpi) {
        initDevice(physicalPort.getDevice());
        initIfName(ifname);

        this.physicalPort_ = physicalPort;
        this.vpi_ = vpi;
        this.physicalPort_.addVp(this);
    }

    public AtmVp(AtmPort physicalPort, int vpi) {
        initDevice(physicalPort.getDevice());

        this.physicalPort_ = physicalPort;
        this.vpi_ = vpi;
        this.physicalPort_.addVp(this);
    }

    public synchronized void addPvc(AtmPvc pvc) {
        if (pvcs_.get(pvc.getVci()) != null) {
            if (pvcs_.get(pvc.getVci()) == pvc) {
                return;
            } else {
                throw new IllegalArgumentException("Duplicate vci: "
                        + getDevice().getDeviceName() + ":"
                        + this.physicalPort_.getIfName() + "/" + this.vpi_
                        + "; vci=" + pvc.getVci());
            }
        } else {
            pvcs_.put(pvc.getVci(), pvc);
        }
    }

    public synchronized AtmPvc[] getPvcs() {
        return pvcs_.values().toArray(new AtmPvc[0]);
    }

    public synchronized AtmPvc getPvc(int vci) {
        return pvcs_.get(vci);
    }

    public synchronized AtmPort getPhysicalPort() {
        return physicalPort_;
    }

    public synchronized int getVpi() {
        return vpi_;
    }

    public void setPcr(Long value) {
        if (value == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        addProperty(new Pcr(value));
    }

    public Long getPcr() {
        Pcr pcr = (Pcr) selectConfigProperty(Pcr.class);
        return pcr == null ? null : pcr.getValue();
    }
}