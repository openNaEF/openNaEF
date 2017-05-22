package voss.nms.inventory.diff.network;

import voss.model.AbstractVlanModel;
import voss.model.PseudoWirePort;

public class NetworkPseudoWire extends AbstractVlanModel {
    private static final long serialVersionUID = 1L;
    private PseudoWirePort pw1 = null;
    private PseudoWirePort pw2 = null;
    private final String pseudoWireId;
    private long vcID = 0;

    public NetworkPseudoWire(String pseudoWireId) {
        this.pseudoWireId = pseudoWireId;
    }

    public void setAc(PseudoWirePort pw) {
        if (this.pw1 == null) {
            this.pw1 = pw;
        } else if (this.pw2 == null) {
            this.pw2 = pw;
        } else {
            throw new IllegalArgumentException("too many ac.");
        }
    }

    public void setAc1(PseudoWirePort pw) {
        this.pw1 = pw;
    }

    public void setAc2(PseudoWirePort pw) {
        this.pw2 = pw;
    }

    public String getID() {
        return this.pseudoWireId;
    }

    public PseudoWirePort getAc1() {
        return this.pw1;
    }

    public PseudoWirePort getAc2() {
        return this.pw2;
    }

    public long getVcID() {
        return vcID;
    }

    public void setVcID(long vcID) {
        this.vcID = vcID;
    }

    public boolean isOk() {
        return this.pw1 != null && this.pw2 != null;
    }

    @Override
    public int hashCode() {
        return this.pseudoWireId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        }
        if (!(o instanceof NetworkPseudoWire)) {
            return false;
        }
        return !this.pseudoWireId.equals("-1") &&
                this.pseudoWireId == ((NetworkPseudoWire) o).pseudoWireId;
    }
}