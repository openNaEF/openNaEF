package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.model.Module;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleRenderer extends AbstractRenderer<Module> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public ModuleRenderer(Module module, String parentID, int depth, Map<String, String> map) {
        super(module, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getModelAbsoluteName(getParentAbsoluteName(), getModel());
    }

    @Override
    protected String setInventoryID(String parentID) {
        return parentID + ELEMENT_DELIMITER;
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

    public String getValue(String naefAttributeName) {
        TargetAttribute attr = TargetAttribute.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    @Override
    public String getValue(Enum<?> attr_) {
        TargetAttribute attr = (TargetAttribute) attr_;
        Module module = getModel();
        switch (attr) {
            case MODULE_TYPE:
                return module.getModelTypeName();
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum TargetAttribute {
        MODULE_TYPE(MPLSNMS_ATTR.MODULE_TYPE),;

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
            throw new IllegalArgumentException();
        }
    }
}