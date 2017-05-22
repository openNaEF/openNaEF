package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.model.Device;
import voss.nms.inventory.constants.LogConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChassisRenderer extends AbstractRenderer<Device> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public ChassisRenderer(Device device, String parentID, int depth, Map<String, String> map) {
        super(device, parentID, depth, map);
        if (device == null || parentID == null) {
            throw new IllegalArgumentException();
        }
    }

    public boolean isRoot() {
        return false;
    }

    public List<String> getAttributeNames() {
        List<String> result = new ArrayList<String>();
        for (TargetAttribute attr : TargetAttribute.values()) {
            result.add(attr.attributeName);
        }
        return result;
    }

    public String getDescription() {
        return getID() + " diff";
    }

    public String getValue(String naefAttributeName) {
        TargetAttribute attr = TargetAttribute.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    @Override
    public String getValue(Enum<?> attr_) {
        TargetAttribute attr = (TargetAttribute) attr_;
        Device device = getModel();
        switch (attr) {
            case NAME:
                return null;
            case SERIAL_NUMBER:
                return device.getSerialNumber();
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum TargetAttribute {
        NAME("name"),
        SERIAL_NUMBER("serial"),;

        private final String attributeName;

        private TargetAttribute(String name) {
            this.attributeName = name;
        }

        public String getAttributeName() {
            return this.attributeName;
        }

        public static TargetAttribute getByAttributeName(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }
            for (TargetAttribute attr : TargetAttribute.values()) {
                if (attr.attributeName.equals(name)) {
                    return attr;
                }
            }
            throw new IllegalArgumentException("unknown: " + name);
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getChassisAbsoluteName(getParentAbsoluteName(), "");
    }

    @Override
    protected String setInventoryID(String parentID) {
        String nodeID = AbsoluteNameFactory.getNodeName(getModel());
        return InventoryIdBuilder.getNodeElementID(nodeID, "/");
    }
}