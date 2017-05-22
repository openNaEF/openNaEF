package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ExceptionUtils;
import voss.discovery.agent.netscreen.NetScreenZoneRenderer;
import voss.model.Device;
import voss.model.NetScreenZone;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.DiffPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceRenderer extends AbstractRenderer<Device> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private final DiffPolicy policy;

    public DeviceRenderer(Device device, int depth, Map<String, String> map) {
        super(device, null, depth, map);
        try {
            this.policy = DiffConfiguration.getInstance().getDiffPolicy();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getNodeAbsoluteName2(getModel());
    }

    @Override
    protected String setInventoryID(String parentID) {
        return InventoryIdCalculator.getId(getModel());
    }

    public boolean isRoot() {
        return true;
    }

    public List<String> getAttributeNames() {
        List<String> result = new ArrayList<String>();
        for (Attr attr : Attr.values()) {
            result.add(attr.attributeName);
        }
        return result;
    }

    public String getDescription() {
        return getID() + " diff";
    }

    @Override
    public String getValue(String naefAttributeName) {
        Attr attr = Attr.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    @Override
    public String getValue(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        Device device = getModel();
        switch (attr) {
            case VENDOR_NAME:
                String vendorName = device.getVendorName();
                return this.policy.getRegularVendorName(vendorName);
            case GUEST_DEVICE_NAME:
                return device.getDeviceName();
            case DEVICE_TYPE:
                String typeName = device.getModelTypeName();
                return this.policy.getRegularModelName(typeName);
            case OS_TYPE:
                return device.getOsTypeName();
            case OS_VERSION:
                return device.getOsVersion();
            case DIFF_TARGET:
                if (device.isVirtualDevice()) {
                    return Boolean.TRUE.toString();
                } else {
                    return null;
                }
            case VIRTUAL:
                if (device.isVirtualDevice()) {
                    return Boolean.TRUE.toString();
                } else {
                    return null;
                }
            case LOCATION:
                return this.policy.getDefaultLocationName(device);
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public boolean isVirtual() {
        if (getModel() == null) {
            throw new IllegalStateException();
        }
        return getModel().isVirtualDevice();
    }

    public String getHostNodeName() {
        Device d = getModel();
        if (d == null) {
            throw new IllegalStateException("model is null.");
        } else if (!d.isVirtualDevice()) {
            return null;
        }
        Device host = d.getPhysicalDevice();
        if (host == null) {
            return null;
        }
        return host.getDeviceName();
    }

    public static enum Attr {
        VENDOR_NAME(MPLSNMS_ATTR.VENDOR_NAME),
        GUEST_DEVICE_NAME(MPLSNMS_ATTR.NAME),
        OS_TYPE(MPLSNMS_ATTR.OS_TYPE),
        OS_VERSION(MPLSNMS_ATTR.OS_VERSION),
        DEVICE_TYPE(MPLSNMS_ATTR.NODE_TYPE),
        DIFF_TARGET(ATTR.DIFF_TARGET),
        VIRTUAL(MPLSNMS_ATTR.VIRTUAL_NODE),
        LOCATION(MPLSNMS_ATTR.NODE_LOCATION),
        ZONES(MPLSNMS_ATTR.ZONE_LIST),;

        private final String attributeName;

        private Attr(String name) {
            this.attributeName = name;
        }

        public String getAttributeName() {
            return this.attributeName;
        }

        public static Attr getByAttributeName(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }
            for (Attr attr : Attr.values()) {
                if (attr.attributeName.equals(name)) {
                    return attr;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public List<String> getZones() {
        List<String> result = new ArrayList<String>();
        NetScreenZoneRenderer renderer = new NetScreenZoneRenderer(getModel());
        for (NetScreenZone zone : renderer.get()) {
            result.add(zone.getZoneName());
        }
        return result;
    }
}