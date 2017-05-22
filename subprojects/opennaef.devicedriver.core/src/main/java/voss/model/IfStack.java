package voss.model;

import java.io.Serializable;

public class IfStack implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class ConfigType extends VlanModelConstants {
        private static final long serialVersionUID = 1L;

        public static final ConfigType CONNECT = new ConfigType("connect");
        public static final ConfigType DISCONNECT = new ConfigType("disconnect");
        public static final ConfigType NO_CHANGE = new ConfigType("no-change");

        private ConfigType() {
        }

        private ConfigType(String id) {
            super(id);
        }
    }

    private Port higherPort_;
    private Port lowerPort_;

    private ConfigType configType_;

    public IfStack() {
    }

    public IfStack(Port higherPort, Port lowerPort, ConfigType configType) {
        if (higherPort == null || lowerPort == null || configType == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (higherPort.getDevice() != lowerPort.getDevice()) {
            throw new IllegalArgumentException("Device does not match.");
        }

        higherPort_ = higherPort;
        lowerPort_ = lowerPort;
        configType_ = configType;

        getDevice().getConfigTargets().addIfStack(IfStack.this);
    }

    public synchronized Device getDevice() {
        return higherPort_.getDevice();
    }

    public synchronized Port getHigherPort() {
        return higherPort_;
    }

    public synchronized Port getLowerPort() {
        return lowerPort_;
    }

    public synchronized ConfigType getConfigType() {
        return configType_;
    }
}