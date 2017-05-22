package voss.model;

@SuppressWarnings("serial")
public abstract class AbstractPort extends AbstractVlanModel implements Port {

    private Device device_;
    private Integer ifIndex_;
    private Integer alternativeIfIndex_;
    private String ifName_;
    private String configName_;
    private String ifDescr_;
    private String status_;
    private String adminStatus_;
    private String operationalStatus_;
    private Long bandwidth;
    private String ospfAreaID;
    private int igpCost;
    private Port associated = null;
    private Port associate = null;
    private Port aliasSource = null;

    public AbstractPort() {
    }

    public synchronized Device getDevice() {
        return device_;
    }

    public synchronized void initDevice(Device device) {
        if (device == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (device_ == device) {
            return;
        }

        device_ = device;

        device_.addPort(AbstractPort.this);
    }

    public synchronized int getIfIndex() {
        if (ifIndex_ == null) {
            throw new NotInitializedException();
        }
        return ifIndex_.intValue();
    }

    public synchronized Integer getRawIfIndex() {
        return this.ifIndex_;
    }

    public synchronized void initIfIndex(int ifIndex) {
        if (device_ == null) {
            throw new IllegalStateException("Device is not initialized.");
        }
        if (ifIndex_ != null && ifIndex_.intValue() == ifIndex) {
            return;
        }
        if (device_.getPortByIfIndex(ifIndex) != null) {
            throw new IllegalStateException("A port with duplicate ifindex has already been registered: "
                    + device_.getDeviceName() + " " + ifIndex);
        }
        if (this.ifIndex_ != null || this.alternativeIfIndex_ != null) {
            this.device_.updatePort(this);
        }
        this.ifIndex_ = Integer.valueOf(ifIndex);
    }

    public synchronized void initAlternativeIfIndex(int ifIndex) {
        if (device_ == null) {
            throw new IllegalStateException("Device is not initialized.");
        }
        if (this.alternativeIfIndex_ != null && this.alternativeIfIndex_.intValue() == ifIndex) {
            return;
        }
        if (device_.getPortByIfIndex(ifIndex) != null) {
            throw new IllegalStateException("A port with duplicate ifindex has already been registered: "
                    + device_.getDeviceName() + " " + ifIndex);
        }
        if (this.ifIndex_ != null || this.alternativeIfIndex_ != null) {
            this.device_.updatePort(this);
        }
        this.alternativeIfIndex_ = Integer.valueOf(ifIndex);
    }

    public synchronized Integer getAlternativeIfIndex() {
        return this.alternativeIfIndex_;
    }

    public synchronized String getIfName() {
        return ifName_;
    }

    public synchronized void initIfName(String ifName) {
        if (device_ == null) {
            throw new IllegalStateException("Device is not initialized.");
        }
        if (ifName == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (ifName.equals(ifName_)) {
            return;
        }
        if (device_.getPortByIfName(ifName) != null) {
            throw new IllegalStateException("A port with duplicate ifname has already been registered: " + device_.getDeviceName() + " [" + ifName + "]");
        }
        if (ifName_ != null) {
            this.device_.updatePort(this);
        }
        ifName_ = ifName;
    }

    public synchronized Port getAssociatedPort() {
        return this.associated;
    }

    public synchronized void setAssociatedPort(Port associated) {
        this.associated = associated;
    }

    public synchronized Port getAssociatePort() {
        return this.associate;
    }

    public synchronized void setAssociatePort(Port associate) {
        this.associate = associate;
    }

    public synchronized boolean isAssociatedPort() {
        return this.associate != null;
    }

    public synchronized Port getAliasSource() {
        return this.aliasSource;
    }

    public synchronized void setAliasSource(Port source) {
        this.aliasSource = source;
    }

    public synchronized boolean isAliasPort() {
        return this.aliasSource != null;
    }

    public synchronized String getConfigName() {
        return this.configName_;
    }

    public void setConfigName(String name) {
        this.configName_ = name;
    }

    public synchronized String getIfDescr() {
        return ifDescr_;
    }

    public synchronized void setIfDescr(String ifDescr) {
        ifDescr_ = ifDescr;
    }

    public synchronized String getStatus() {
        return status_;
    }

    public synchronized void setStatus(String status) {
        status_ = status;
    }

    public synchronized String getAdminStatus() {
        return adminStatus_;
    }

    public synchronized void setAdminStatus(String adminStatus) {
        adminStatus_ = adminStatus;
    }

    public synchronized String getOperationalStatus() {
        return operationalStatus_;
    }

    public synchronized void setOperationalStatus(String operationalStatus) {
        operationalStatus_ = operationalStatus;
    }

    public Long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getFullyQualifiedName() {
        return getDevice().getDeviceName() + ":" + getIfName();
    }

    @Override
    public String toString() {
        return device_.getDeviceName() + ":" + ifName_;
    }

    @Override
    public void setOspfAreaID(String id) {
        this.ospfAreaID = id;
    }

    @Override
    public String getOspfAreaID() {
        return this.ospfAreaID;
    }

    @Override
    public int getIgpCost() {
        return igpCost;
    }

    @Override
    public void setIgpCost(int igpCost) {
        this.igpCost = igpCost;
    }
}