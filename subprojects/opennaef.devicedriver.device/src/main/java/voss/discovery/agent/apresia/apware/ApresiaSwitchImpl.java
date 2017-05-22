package voss.discovery.agent.apresia.apware;

import voss.model.ApresiaMmrpRing;
import voss.model.ApresiaVdr;
import voss.model.GenericEthernetSwitch;

import java.util.HashSet;
import java.util.Set;


public class ApresiaSwitchImpl extends GenericEthernetSwitch {
    private static final long serialVersionUID = 1L;
    private final Set<ApresiaMmrpRing> mmrps = new HashSet<ApresiaMmrpRing>();
    private final Set<ApresiaVdr> vdrs = new HashSet<ApresiaVdr>();

    public Set<ApresiaMmrpRing> getApresiaMmrpRings() {
        synchronized (mmrps) {
            Set<ApresiaMmrpRing> result = new HashSet<ApresiaMmrpRing>(this.mmrps);
            return result;
        }
    }

    public Set<ApresiaVdr> getApresiaVdrs() {
        synchronized (vdrs) {
            Set<ApresiaVdr> result = new HashSet<ApresiaVdr>(this.vdrs);
            return result;
        }
    }

    public ApresiaMmrpRing getRing(int ringId) {
        synchronized (mmrps) {
            for (ApresiaMmrpRing ring : mmrps) {
                if (ring.getMmrpRingId() == ringId) {
                    return ring;
                }
            }
            return null;
        }
    }

    public ApresiaVdr getVdr(int vdrId) {
        synchronized (vdrs) {
            for (ApresiaVdr vdr : vdrs) {
                if (vdr.getVdrId() == vdrId) {
                    return vdr;
                }
            }
            return null;
        }
    }

    public void addApresiaMmrpRing(ApresiaMmrpRing ring) {
        synchronized (mmrps) {
            if (getRing(ring.getMmrpRingId()) != null) {
                mmrps.add(ring);
            } else {
                throw new IllegalArgumentException("already added: ring-id: " + ring.getMmrpRingId());
            }
        }
    }

    public void addAprsiaVdr(ApresiaVdr vdr) {
        synchronized (vdrs) {
            if (getVdr(vdr.getVdrId()) != null) {
                vdrs.add(vdr);
            } else {
                throw new IllegalArgumentException("already added: vdr-id: " + vdr.getVdrId());
            }
        }
    }

    public void clearMmrpRing() {
        synchronized (mmrps) {
            mmrps.clear();
        }
    }

    public void clearVdr() {
        synchronized (vdrs) {
            vdrs.clear();
        }
    }

}