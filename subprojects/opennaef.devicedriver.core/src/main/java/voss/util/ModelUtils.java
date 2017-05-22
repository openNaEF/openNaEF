package voss.util;


import voss.model.*;

import java.util.Collection;

public class ModelUtils {

    private ModelUtils() {
    }

    public static AtmPort getAtmPort(Port port) {
        if (port instanceof AtmPort) {
            return (AtmPort) port;
        } else if (port instanceof FeatureCapability) {
            LogicalPort feature = ((FeatureCapability) port).getLogicalFeature();
            if (feature != null && feature instanceof AtmPort) {
                return (AtmPort) feature;
            }
        }
        return null;
    }

    public static FrameRelayFeature getFrameRelayFeature(Port port) {
        if (port instanceof FrameRelayFeature) {
            return (FrameRelayFeature) port;
        } else if (port instanceof SerialPort) {
            SerialPort serial = (SerialPort) port;
            LogicalPort lf = serial.getLogicalFeature();
            if (lf instanceof FrameRelayFeature) {
                return (FrameRelayFeature) lf;
            }
        }
        return null;
    }

    public static <T extends Port> boolean containsPort(Collection<T> ports, T target) {
        if (target == null) {
            return false;
        }
        for (T port : ports) {
            if (!port.getClass().getName().equals(target.getClass().getName())) {
                continue;
            }
            try {
                if (!port.getDevice().getDeviceName().equals(target.getDevice().getDeviceName())) {
                    continue;
                }
                if (port.getIfName().equals(target.getIfName())) {
                    return true;
                }
            } catch (NotInitializedException e) {
            }
        }
        return false;
    }
}