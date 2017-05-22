package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.NaefTypeNameFactory;
import voss.model.Port;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AliasPortRenderer extends AbstractRenderer<Port> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public AliasPortRenderer(Port port, String parentID, int depth, Map<String, String> map) {
        super(port, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        String type = NaefTypeNameFactory.getNaefTypeName(getModel());
        return InventoryBuilder.appendContext(getParentAbsoluteName(), type, getModel().getIfName());
    }

    @Override
    protected String setInventoryID(String parentID) {
        return InventoryIdCalculator.getId(getModel());
    }

    public boolean isRoot() {
        return false;
    }

    public List<String> getAttributeNames() {
        List<String> result = new ArrayList<String>();
        for (Attr attr : Attr.values()) {
            result.add(attr.attributeName);
        }
        return result;
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
        Port port = getModel();
        Port source = port.getAliasSource();
        switch (attr) {
            case IFNAME:
                return port.getIfName();
            case CONFIGNAME:
                return port.getConfigName();
            case DESCRIPTION:
                return port.getUserDescription();
            case ALIAS_SOURCE_NODE:
                return source.getDevice().getDeviceName();
            case ALIAS_SOURCE_IFNAME:
                return source.getIfName();
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum Attr {
        ALIAS_SOURCE_NODE(ATTR.ATTR_ALIAS_SOURCE + ".node"),
        ALIAS_SOURCE_IFNAME(ATTR.ATTR_ALIAS_SOURCE + ".ifName"),
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),;

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

}