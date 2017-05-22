package voss.model;

import java.util.Arrays;

public class EthernetPortsAggregatorImpl extends AbstractLogicalEthernetPort implements EthernetPortsAggregator {
    private static final long serialVersionUID = 1L;
    private EthernetPort[] physicalPorts_ = new EthernetPort[0];
    private EthernetPort masterPort_;
    private Integer aggregationGroupId_;
    private String aggregationName_;

    public EthernetPortsAggregatorImpl() {
    }

    public boolean isAggregated() {
        return true;
    }

    public synchronized Integer getAggregationGroupId() {
        return aggregationGroupId_;
    }

    public synchronized void initAggregationGroupId(int aggregationGroupId) {
        if (aggregationGroupId_ != null && aggregationGroupId_.intValue() == aggregationGroupId) {
            return;
        }
        if (aggregationGroupId_ != null) {
            throw new AlreadyInitializedException(aggregationGroupId_, new Integer(aggregationGroupId));
        }
        if (((VlanDevice) getDevice()).getEthernetPortsAggregatorByAggregationGroupId(aggregationGroupId) != null) {
            throw new IllegalStateException("Duplicate was detected: " + getDevice().getDeviceName() + " " + aggregationGroupId);
        }

        aggregationGroupId_ = new Integer(aggregationGroupId);
    }

    public synchronized void addPhysicalPort(EthernetPort physicalPort) {
        if (Arrays.asList(physicalPorts_).contains(physicalPort)) {
            return;
        }
        if (physicalPort.getDevice() != getDevice()) {
            throw new IllegalArgumentException("Device does not match.");
        }
        if (AbstractLogicalEthernetPort.getLogicalEthernetPort(physicalPort) != null
                && AbstractLogicalEthernetPort.getLogicalEthernetPort(physicalPort) != this) {
            throw new IllegalStateException("It is associated with a different logical Ethernet port.");
        }
        physicalPorts_ = (EthernetPort[]) VlanModelUtils.arrayaddNoDuplicate(physicalPorts_, physicalPort);
    }

    public synchronized EthernetPort[] getPhysicalPorts() throws NotInitializedException {
        if (physicalPorts_ == null) {
            throw new NotInitializedException();
        }
        EthernetPort[] result = new EthernetPort[physicalPorts_.length];
        System.arraycopy(physicalPorts_, 0, result, 0, result.length);
        return result;
    }

    public boolean isAggregatorOf(EthernetPort physicalPort) {
        EthernetPort[] physicalPorts = getPhysicalPorts();
        for (int i = 0; i < physicalPorts.length; i++) {
            if (physicalPorts[i] == physicalPort) {
                return true;
            }
        }
        return false;
    }

    public synchronized EthernetPort getMasterPort() {
        return masterPort_;
    }

    public synchronized void initMasterPort(EthernetPort masterPort) {
        if (masterPort == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (masterPort_ != null && masterPort_ == masterPort) {
            return;
        }
        if (masterPort_ != null) {
            throw new AlreadyInitializedException(masterPort_, masterPort);
        }
        masterPort_ = masterPort;
    }

    public synchronized String getAggregationName() {
        return aggregationName_;
    }

    public synchronized void setAggregationName(String aggregationName) {
        aggregationName_ = aggregationName;
    }
}