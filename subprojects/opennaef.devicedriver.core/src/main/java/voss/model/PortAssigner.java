package voss.model;

import voss.model.container.ConfigModelSet;

@SuppressWarnings("serial")
public class PortAssigner extends AbstractVlanModel {

    public static class ConfigType extends VlanModelConstants {

        public static final ConfigType ASSIGN = new ConfigType("assign");
        public static final ConfigType UNASSIGN = new ConfigType("unassign");
        public static final ConfigType MODIFY = new ConfigType("modify");
        public static final ConfigType NA = new ConfigType("na");

        private ConfigType(String id) {
            super(id);
        }
    }

    private final ConfigModelSet models_;
    private final String targetDeviceName_;
    private final String targetPortName_;
    private final String userlineId_;
    private final String orderId_;
    private final ConfigType configType_;
    private final String extInfo_;
    private final PortAssigner previousAssigner_;

    public PortAssigner
            (ConfigModelSet models, String targetDeviceName, String targetPortName,
             String userlineId, String orderId, ConfigType configType,
             String extInfo, PortAssigner previousAssigner) {
        if (models == null || targetDeviceName == null || targetPortName == null
                || configType == null) {
            throw new IllegalArgumentException(userlineId + " " + orderId);
        }

        models_ = models;
        targetDeviceName_ = targetDeviceName;
        targetPortName_ = targetPortName;
        userlineId_ = userlineId;
        orderId_ = orderId;
        configType_ = configType;
        extInfo_ = extInfo;
        previousAssigner_ = previousAssigner;

        models_.addPortAssigner(this);
    }

    public Port getTargetPort() {
        Device targetDevice = models_.getDeviceByName(targetDeviceName_);
        if (targetDevice == null) {
            throw new IllegalStateException
                    ("Target device not found: " + userlineId_ + " " + orderId_
                            + " " + targetDeviceName_);
        }
        Port targetPort = targetDevice.getPortByIfName(targetPortName_);
        if (targetPort == null) {
            throw new IllegalStateException
                    ("Target port not found: " + userlineId_ + " " + orderId_
                            + " " + targetDeviceName_ + " " + targetPortName_);
        }
        return targetPort;
    }

    public String getUserlineId() {
        return userlineId_;
    }

    public String getOrderId() {
        return orderId_;
    }

    public ConfigType getConfigType() {
        return configType_;
    }

    public String getExtInfo() {
        return extInfo_;
    }

    public PortAssigner getPreviousAssigner() {
        return previousAssigner_;
    }
}