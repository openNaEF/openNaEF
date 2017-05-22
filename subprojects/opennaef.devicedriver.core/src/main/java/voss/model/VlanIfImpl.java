package voss.model;

import java.util.*;

@SuppressWarnings("serial")
public class VlanIfImpl extends AbstractLogicalPort implements VlanIf {

    private String vlanKey;

    private Integer vlanIfIndex_;

    protected Integer eoeId;

    protected Integer vlanId_;

    private boolean isBridge;

    private String vlanName_;
    private LogicalEthernetPort[] taggedPorts_ = new LogicalEthernetPort[0];
    private LogicalEthernetPort[] untaggedPorts_ = new LogicalEthernetPort[0];
    private String[] ipAddresses_ = new String[0];
    private VlanStpElement vlanStpElement_;

    public VlanIfImpl() {
    }

    public synchronized void initDevice(Device device) {
        if (!(device instanceof VlanDevice)) {
            throw new IllegalArgumentException("It is not vlan-device: "
                    + device.getDeviceName());
        }
        super.initDevice(device);
    }

    public synchronized void initVlanKey(String key) {
        if (getDevice() == null) {
            throw new NotInitializedException();
        }
        if (key == null) {
            throw new IllegalArgumentException("key is null.");
        }
        if (this.vlanKey != null) {
            throw new AlreadyInitializedException(this.vlanKey, key);
        }
        this.vlanKey = key;
    }

    public synchronized String getVlanKey() {
        getDevice();
        return this.vlanKey;
    }

    public synchronized int getVlanIfIndex() throws NotInitializedException {
        if (vlanIfIndex_ == null) {
            throw new NotInitializedException();
        }

        return vlanIfIndex_.intValue();
    }

    public synchronized void initVlanIfIndex(int vlanIfIndex) {
        if (vlanIfIndex_ != null && vlanIfIndex_.intValue() == vlanIfIndex) {
            return;
        }
        if (vlanIfIndex_ != null) {
            throw new AlreadyInitializedException(vlanIfIndex_, new Integer(vlanIfIndex));
        }

        vlanIfIndex_ = new Integer(vlanIfIndex);
    }

    public synchronized int getVlanId() {
        if (vlanId_ == null) {
            throw new NotInitializedException();
        }

        return vlanId_.intValue();
    }

    private VlanDevice getVlanDevice() {
        return (VlanDevice) getDevice();
    }

    public synchronized void initVlanId(Integer eoeId, int vlanId) {
        if (vlanId_ != null && vlanId_.intValue() == vlanId
                && isSameEoeId(eoeId)) {
            return;
        }
        if (vlanId_ != null) {
            throw new AlreadyInitializedException(vlanId_, new Integer(vlanId));
        }
        if (eoeId != null && !getVlanDevice().isEoeEnable()) {
            throw new IllegalStateException("device's EoE feature disabled.");
        }

        if (vlanId != NULL_VLAN_ID) {
            VlanIf vif = getVlanDevice().getVlanIfBy(eoeId, vlanId);
            if (vif != null) {
                throw new IllegalStateException("duplicated vlan-id: "
                        + getDevice().getDeviceName() + " " + vlanId);
            }
        }

        this.eoeId = eoeId;
        this.vlanId_ = new Integer(vlanId);
    }

    public synchronized void initVlanId(int vlanId) {
        initVlanId(null, vlanId);
    }

    public synchronized Integer getEoeId() {
        return this.eoeId;
    }

    public synchronized boolean isSameVlan(Integer eoeID, int vlanID) {
        if (this.eoeId == null && eoeID != null) {
            return false;
        } else if (this.eoeId != null && eoeID == null) {
            return false;
        } else if (this.eoeId != null && eoeID != null) {
            return this.eoeId.intValue() == eoeID.intValue() && this.vlanId_.intValue() == vlanID;
        } else {
            return this.vlanId_.intValue() == vlanID;
        }
    }

    private boolean isSameEoeId(Integer eoeId) {
        if (this.eoeId == null && eoeId == null) {
            return true;
        } else if (this.eoeId != null && eoeId != null
                && this.eoeId.intValue() == eoeId.intValue()) {
            return true;
        }
        return false;
    }

    public synchronized String getExtendedVlanId() {
        return getExtendedVlanId(this.eoeId, this.vlanId_);
    }


    public static String getExtendedVlanId(Integer eoeID, Integer vlanID) {
        if (vlanID == null) {
            throw new NotInitializedException();
        } else if (eoeID == null) {
            return vlanID.toString();
        } else {
            return eoeID.toString() + "." + vlanID.toString();
        }
    }


    public synchronized String getVlanName() {
        return vlanName_;
    }

    public synchronized void setVlanName(String vlanName) {
        vlanName_ = vlanName;
    }

    public synchronized void setTaggedPorts(LogicalEthernetPort[] taggedPorts) {
        taggedPorts_ = taggedPorts;
    }

    public synchronized void addTaggedPort(LogicalEthernetPort taggedPort) {
        if (taggedPort == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        taggedPorts_ = (LogicalEthernetPort[]) VlanModelUtils
                .arrayaddNoDuplicate(taggedPorts_, taggedPort);
    }

    public synchronized void removeTaggedPort(LogicalEthernetPort taggedPort) {
        List<LogicalEthernetPort> taggedPorts = new ArrayList<LogicalEthernetPort>(
                Arrays.asList(taggedPorts_));
        taggedPorts.remove(taggedPort);
        taggedPorts_ = taggedPorts.toArray(new LogicalEthernetPort[0]);
    }

    public synchronized LogicalEthernetPort[] getTaggedPorts() {
        LogicalEthernetPort[] result = new LogicalEthernetPort[taggedPorts_.length];
        System.arraycopy(taggedPorts_, 0, result, 0, result.length);
        return result;
    }

    public synchronized void setUntaggedPorts(
            LogicalEthernetPort[] untaggedPorts) {
        untaggedPorts_ = untaggedPorts;
    }

    public synchronized void addUntaggedPort(LogicalEthernetPort untaggedPort) {
        if (untaggedPort == null) {
            throw new NullArgumentIsNotAllowedException();
        }

        untaggedPorts_ = (LogicalEthernetPort[]) VlanModelUtils
                .arrayaddNoDuplicate(untaggedPorts_, untaggedPort);
    }

    public synchronized void removeUntaggedPort(LogicalEthernetPort untaggedPort) {
        List<LogicalEthernetPort> untaggedPorts = new ArrayList<LogicalEthernetPort>(
                Arrays.asList(untaggedPorts_));
        untaggedPorts.remove(untaggedPort);
        untaggedPorts_ = untaggedPorts.toArray(new LogicalEthernetPort[0]);
    }

    public synchronized LogicalEthernetPort[] getUntaggedPorts() {
        LogicalEthernetPort[] result = new LogicalEthernetPort[untaggedPorts_.length];
        System.arraycopy(untaggedPorts_, 0, result, 0, result.length);
        return result;
    }

    public synchronized LogicalEthernetPort[] getBindedPorts() {
        Set<LogicalEthernetPort> result = new HashSet<LogicalEthernetPort>();
        result.addAll(Arrays.asList(taggedPorts_));
        result.addAll(Arrays.asList(untaggedPorts_));
        return (LogicalEthernetPort[]) result
                .toArray(new LogicalEthernetPort[0]);
    }

    public synchronized boolean isBindedAsTagged(LogicalEthernetPort logicalEth) {
        for (int i = 0; i < taggedPorts_.length; i++) {
            if (taggedPorts_[i] == logicalEth) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isBindedAsUntagged(
            LogicalEthernetPort logicalEth) {
        for (int i = 0; i < untaggedPorts_.length; i++) {
            if (untaggedPorts_[i] == logicalEth) {
                return true;
            }
        }
        return false;
    }

    public synchronized String[] getIpAddresses() {
        return ipAddresses_;
    }

    public synchronized void setIpAddresses(String[] ipAddresses) {
        ipAddresses_ = ipAddresses;
    }

    public synchronized VlanStpElement getVlanStpElement() {
        return vlanStpElement_;
    }

    public synchronized void setVlanStpElement(VlanStpElement vlanStpElement) {
        vlanStpElement_ = vlanStpElement;
    }

    public synchronized boolean isBridge() {
        return this.isBridge;
    }

    public synchronized void setBridge(boolean isBridge) {
        this.isBridge = isBridge;
    }
}