package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.discovery.agent.juniper.PromiscuousModeRenderer;
import voss.model.AtmPort;
import voss.model.AtmPvc;
import voss.model.AtmVp;
import voss.model.Port;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtmVpRenderer extends AbstractRenderer<AtmVp> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public AtmVpRenderer(AtmVp pvc, String parentID, int depth, Map<String, String> map) {
        super(pvc, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getAtmVpAbsoluteName(getParentAbsoluteName(), getModel());
    }

    @Override
    protected String setInventoryID(String parentID) {
        AtmVp vp = getModel();
        AtmPort port = vp.getPhysicalPort();
        Port parent = port.getParentPort();
        if (vp.getIfName() == null) {
            return InventoryIdCalculator.getId(parent) + InventoryIdCalculator.DELIM2 + vp.getVpi();
        } else {
            return InventoryIdCalculator.getId(vp);
        }
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

    public String getValue(String naefAttributeName) {
        Attr attr = Attr.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    @Override
    public String getValue(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        AtmVp port = getModel();
        switch (attr) {
            case IFNAME:
                return port.getIfName();
            case CONFIGNAME:
                return port.getConfigName();
            case VPI:
                return String.valueOf(port.getVpi());
            case IP_ADDRESS:
                return ConfigModelUtil.getIpAddress(port, true);
            case SUBNETMASK:
                return ConfigModelUtil.getSubnetMask(port, true);
            case DESCRIPTION:
                return port.getUserDescription();
            case IMPLICIT:
                AtmPvc[] pvcs = port.getPvcs();
                if (pvcs != null && pvcs.length > 0) {
                    return Boolean.TRUE.toString();
                }
                PromiscuousModeRenderer promiscuous = new PromiscuousModeRenderer(port);
                if (!promiscuous.isDefined()) {
                    return Boolean.FALSE.toString();
                }
                return Boolean.valueOf(!promiscuous.get().booleanValue()).toString();
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum Attr {
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        VPI(ATTR.ATTR_ATM_PVP_VPI),
        IP_ADDRESS(MPLSNMS_ATTR.IP_ADDRESS),
        SUBNETMASK(MPLSNMS_ATTR.MASK_LENGTH),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),
        IMPLICIT(ATTR.IMPLICIT),;

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