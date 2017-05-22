package voss.model;


public class RouterVlanIfImpl extends VlanIfImpl implements RouterVlanIf {
    private static final long serialVersionUID = 1L;

    public LogicalEthernetPort routerPort = null;

    public synchronized void initDevice(Device device) {
        super.initDevice(device);

        if (!(device instanceof MplsVlanDevice)) {
            throw new IllegalArgumentException("It is not MplsVlanDevice: "
                    + device.getDeviceName());
        }
    }

    public synchronized void initRouterPort(LogicalEthernetPort port) {
        if (getDevice() == null) {
            throw new NotInitializedException();
        }
        if (vlanId_ == null) {
            throw new NotInitializedException();
        }

        MplsVlanDevice device = (MplsVlanDevice) getDevice();
        for (VlanIf vlan : device.getVlanIfs()) {
            if (vlan == this) {
                continue;
            }
            if (vlan.getVlanId() != this.getVlanId()) {
                continue;
            }
            if (!(vlan instanceof RouterVlanIf)) {
                continue;
            }
            LogicalEthernetPort ownerPort = ((RouterVlanIf) vlan).getRouterPort();
            if (ownerPort.equals(port)) {
                throw new IllegalArgumentException("It is already associated with another VLAN with the same ID: " +
                        "vlan=" + vlan.getFullyQualifiedName());
            }
        }
        this.routerPort = port;
    }

    @Override
    public synchronized LogicalEthernetPort getRouterPort() {
        return this.routerPort;
    }

    @Override
    public synchronized void addTaggedPort(LogicalEthernetPort taggedPort) {
        if (!this.routerPort.equals(taggedPort)) {
            throw new IllegalArgumentException("not bound router port: " +
                    "taggedPort=" + taggedPort.getFullyQualifiedName());
        }
        super.addTaggedPort(taggedPort);
    }

    @Override
    public synchronized void addUntaggedPort(LogicalEthernetPort untaggedPort) {
        throw new IllegalArgumentException("untagged bind is not supported: " +
                "untaggedPort=" + untaggedPort.getFullyQualifiedName());
    }

    @Override
    public synchronized void initVlanId(Integer eoeID, int vlanId) {
        if (getDevice() == null) {
            throw new NotInitializedException();
        }
        boolean vlanMatch = vlanId_ != null && vlanId_ == vlanId;
        boolean eoeMatch = (eoeID == getEoeId()) || (eoeID != null && eoeID.equals(getEoeId()));
        if (vlanMatch && eoeMatch) {
            return;
        }
        this.eoeId = eoeID;
        this.vlanId_ = new Integer(vlanId);
    }

    @Override
    public synchronized void setTaggedPorts(LogicalEthernetPort[] taggedPorts) {
        for (LogicalEthernetPort taggedPort : taggedPorts) {
            if (!this.routerPort.equals(taggedPort)) {
                throw new IllegalArgumentException("not bound router port: " +
                        "taggedPort=" + taggedPort.getFullyQualifiedName());
            }
        }
        super.setTaggedPorts(taggedPorts);
    }

    @Override
    public synchronized void setUntaggedPorts(
            LogicalEthernetPort[] untaggedPorts) {
        throw new IllegalArgumentException("untagged bind operation is not supported");
    }

    public synchronized String getRouterVlanIfName() {
        StringBuffer sb = new StringBuffer();
        sb.append(getDevice().getDeviceName());
        sb.append(":");
        sb.append(routerPort.getIfName());
        sb.append(":");
        sb.append(vlanId_);
        return sb.toString();
    }
}