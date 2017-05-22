package voss.model;


import java.util.ArrayList;
import java.util.List;

public class ApresiaMmrpRing extends AbstractLogicalPort implements LogicalPort {
    private static final long serialVersionUID = 1L;
    private int mmrpRingId = -1;
    private int mmrpLockStatus = -1;
    private String ringName = null;

    private Port masterPort;
    private Port slavePort;
    private List<Port> awarePorts = new ArrayList<Port>();
    private final List<VlanIf> memberVlanIfs = new ArrayList<VlanIf>();

    public static final int MMRP_LOCK_ENABLE = 1;
    public static final int MMRP_LOCK_DISABLE = 2;

    public int getMmrpRingId() throws NotInitializedException {
        if (this.mmrpRingId == -1) {
            throw new NotInitializedException();
        }
        return this.mmrpRingId;
    }

    public void setMmrpRingId(int mmrpRingId) {
        this.mmrpRingId = mmrpRingId;
    }

    public String getMmrpRingName() {
        return getIfName();
    }

    public String getAdminStatus() {
        return super.getAdminStatus();
    }

    public int getMmrpLockStatus() {
        return this.mmrpLockStatus;
    }

    public void setMmrpLockStatus(int status) {
        this.mmrpLockStatus = status;
    }

    public Port getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(Port masterPort) {
        this.masterPort = masterPort;
    }

    public Port getSlavePort() {
        return slavePort;
    }

    public void setSlavePort(Port slavePort) {
        this.slavePort = slavePort;
    }

    public List<Port> getAwarePorts() {
        return new ArrayList<Port>(awarePorts);
    }

    public void setAwarePorts(List<Port> awarePorts) {
        this.awarePorts.clear();
        this.awarePorts.addAll(awarePorts);
    }

    public void addAwarePort(Port awarePort) {
        this.awarePorts.add(awarePort);
    }

    public String getRingName() {
        return ringName;
    }

    public void setRingName(String ringName) {
        this.ringName = ringName;
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