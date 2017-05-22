package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.model.LogicalEthernetPort;
import voss.model.Port;
import voss.model.VlanIf;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VlanIfBindingRenderer extends AbstractRenderer<VlanIf> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    public static final String SUFFIX = "-binding";

    public VlanIfBindingRenderer(VlanIf port, String parentID, int depth, Map<String, String> map) {
        super(port, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getVlanIfAbsoluteName(getParentAbsoluteName(), getModel());
    }

    @Override
    protected String setInventoryID(String parentID) {
        return InventoryIdCalculator.getId(getModel());
    }

    @Override
    public String getID() {
        return super.getID() + SUFFIX;
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

    public List<String> getTaggedPorts() {
        List<String> result = new ArrayList<String>();
        for (LogicalEthernetPort port : getModel().getTaggedPorts()) {
            log.debug("getTaggedPorts: " + port.getFullyQualifiedName());
            String portAbsoluteName = getPortAbsoluteName(port);
            if (portAbsoluteName == null) {
                log.warn("getTaggedPorts: no absolute-name: " + port.getFullyQualifiedName());
            } else {
                log.debug("- absolute-name: " + portAbsoluteName);
                result.add(portAbsoluteName);
            }
        }
        return result;
    }

    public List<String> getUntaggedPorts() {
        List<String> result = new ArrayList<String>();
        for (LogicalEthernetPort port : getModel().getUntaggedPorts()) {
            log.debug("getUntaggedPorts: " + port.getFullyQualifiedName());
            String portAbsoluteName = getPortAbsoluteName(port);
            if (portAbsoluteName == null) {
                log.warn("getUntaggedPorts: no absolute-name: " + port.getFullyQualifiedName());
            } else {
                log.debug("- absolute-name: " + portAbsoluteName);
                result.add(portAbsoluteName);
            }
        }
        return result;
    }

    public List<String> getTaggedPortIfNames() {
        List<String> result = new ArrayList<String>();
        for (LogicalEthernetPort port : getModel().getTaggedPorts()) {
            log.debug("getTaggedPortIfNames: " + port.getFullyQualifiedName());
            String ifName = getIfName(port);
            if (ifName == null) {
                log.warn("getTaggedPortIfNames: no ifName: " + port.getFullyQualifiedName());
            } else {
                log.debug("- ifName: " + ifName);
                result.add(ifName);
            }
        }
        return result;
    }

    public List<String> getUntaggedPortIfNames() {
        List<String> result = new ArrayList<String>();
        for (LogicalEthernetPort port : getModel().getUntaggedPorts()) {
            log.debug("getUntaggedPortIfNames: " + port.getFullyQualifiedName());
            String ifName = getIfName(port);
            if (ifName == null) {
                log.warn("getUntaggedPortIfNames: no ifName: " + port.getFullyQualifiedName());
            } else {
                log.debug("- ifName: " + ifName);
                result.add(ifName);
            }
        }
        return result;
    }

    private String getPortAbsoluteName(LogicalEthernetPort port) {
        String nodeName = AbsoluteNameFactory.getNodeName(port.getDevice());
        String ifName;
        if (port.isAggregated()) {
            ifName = port.getIfName();
        } else {
            Port phy = ConfigModelUtil.getPhysicalPort(port);
            if (phy != null) {
                ifName = phy.getIfName();
            } else {
                log.debug("- no physical-port: " + port.getFullyQualifiedName());
                return null;
            }
        }
        log.debug("- -> " + nodeName + ":" + ifName);
        String portID = InventoryIdBuilder.getPortID(nodeName, ifName);
        log.debug("- -> " + portID);
        String portAbsoluteName = getAbsoluteNameByInventoryID(portID);
        return portAbsoluteName;
    }

    private String getIfName(LogicalEthernetPort port) {
        if (port.isAggregated()) {
            return port.getIfName();
        } else {
            Port phy = ConfigModelUtil.getPhysicalPort(port);
            if (phy != null) {
                return phy.getIfName();
            } else {
                return null;
            }
        }
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
        VlanIf port = getModel();
        switch (attr) {
            case PORT_ID:
                return AbsoluteNameFactory.getVlanIfName(port);
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum Attr {
        PORT_ID(MPLSNMS_ATTR.NAME),;

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