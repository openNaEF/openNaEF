package voss.model;

import java.util.ArrayList;
import java.util.List;


public class ApresiaVdr extends AbstractLogicalPort implements LogicalPort {
    private static final long serialVersionUID = 1L;
    private int vdrId = -1;
    private ApresiaVdrUplinkPort uplink1;
    private ApresiaVdrUplinkPort uplink2;
    private final List<VlanIf> memberVlanIfs = new ArrayList<VlanIf>();

    public void initVdrId(int vdrId) throws NotInitializedException {
        if (this.vdrId != -1) {
            throw new NotInitializedException();
        }
        this.vdrId = vdrId;
    }

    public void setUplink1(ApresiaVdrUplinkPort uplink) {
        if (uplink == null) {
            throw new IllegalArgumentException("uplink is null");
        }
        this.uplink1 = uplink;
    }

    public void setUplink2(ApresiaVdrUplinkPort uplink) {
        if (uplink == null) {
            throw new IllegalArgumentException("uplink is null");
        }
        this.uplink2 = uplink;
    }

    public ApresiaVdrUplinkPort getUplink1() {
        return this.uplink1;
    }

    public ApresiaVdrUplinkPort getUplink2() {
        return this.uplink2;
    }

    public int getVdrId() throws NotInitializedException {
        if (this.vdrId == -1) {
            throw new NotInitializedException();
        }
        return this.vdrId;
    }

    public void addMemberVlanIf(VlanIf vlanIf) {
        if (vlanIf == null) {
            return;
        }
        this.memberVlanIfs.add(vlanIf);
    }

    public List<VlanIf> getMemberVlanIfs() {
        return new ArrayList<VlanIf>(this.memberVlanIfs);
    }
}