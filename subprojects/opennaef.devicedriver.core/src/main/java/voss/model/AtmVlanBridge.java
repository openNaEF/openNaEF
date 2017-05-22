package voss.model;

import java.util.*;

public class AtmVlanBridge extends AbstractLogicalPort {
    private static final long serialVersionUID = 1L;

    private AtmPvc pvc_;
    private Integer bridgePortNumber_;
    private VlanIf untaggedVlanIf_;
    private Set<VlanIf> taggedVlanIfs_ = new HashSet<VlanIf>();

    public AtmVlanBridge(AtmPvc pvc, String ifname) {
        initDevice(pvc.getDevice());
        initIfName(ifname);
        pvc_ = pvc;
    }

    public synchronized AtmPvc getPvc() {
        return pvc_;
    }

    public synchronized void setBridgePortNumber(Integer bridgePortNumber) {
        bridgePortNumber_ = bridgePortNumber;
    }

    public synchronized Integer getBridgePortNumber() {
        return bridgePortNumber_;
    }

    public synchronized void setUntaggedVlanIf(VlanIf vlanif) {
        untaggedVlanIf_ = vlanif;
    }

    public synchronized VlanIf getUntaggedVlanIf() {
        return untaggedVlanIf_;
    }

    public synchronized void addTaggedVlanIf(VlanIf vlanif) {
        taggedVlanIfs_.add(vlanif);
    }

    public synchronized VlanIf[] getTaggedVlanIfs() {
        return (VlanIf[]) taggedVlanIfs_.toArray(new VlanIf[0]);
    }

    public VlanIf[] getConnectedVlanIfs() {
        VlanIf[] taggedVlanIfs = getTaggedVlanIfs();
        VlanIf untaggedVlanif = getUntaggedVlanIf();
        if (untaggedVlanif == null) {
            return taggedVlanIfs;
        } else {
            List<VlanIf> result = new ArrayList<VlanIf>();
            result.add(untaggedVlanif);
            result.addAll(Arrays.asList(taggedVlanIfs));
            return (VlanIf[]) result.toArray(new VlanIf[0]);
        }
    }
}