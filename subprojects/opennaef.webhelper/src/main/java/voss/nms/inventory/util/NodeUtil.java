package voss.nms.inventory.util;

import naef.dto.*;
import org.apache.wicket.PageParameters;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.Util;
import voss.nms.inventory.database.InventoryConnector;

import java.util.Set;

public class NodeUtil extends voss.core.server.util.NodeUtil {
    public static final String KEY_NODE = "node";
    public static final String KEY_SLOT_FQN = "slot";
    public static final String KEY_PORT_FQN = "ifName";

    public static NodeDto getNode(PageParameters param) {
        try {
            String nodeName = Util.decodeUTF8(param.getString(KEY_NODE));
            if (nodeName == null) {
                return null;
            }
            return getNode(nodeName);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static String getNodeName(PageParameters param) {
        String nodeName = param.getString(KEY_NODE);
        return Util.decodeUTF8(nodeName);
    }

    public static SlotDto getSlot(PageParameters param) throws InventoryException, ExternalServiceException {
        try {
            String nodeName = Util.decodeUTF8(param.getString(KEY_NODE));
            String slotFqn = Util.decodeUTF8(param.getString(KEY_SLOT_FQN));
            InventoryConnector conn = InventoryConnector.getInstance();
            NodeDto node = conn.getNodeDto(nodeName);
            Set<ChassisDto> chassises = node.getChassises();
            for (ChassisDto chassis : chassises) {
                for (SlotDto slot : chassis.getSlots()) {
                    if (slot.getAbsoluteName().equals(slotFqn)) {
                        return slot;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static String getFqnFromParameter(PageParameters param) {
        if (param == null) {
            return null;
        }
        String fqn = Util.decodeUTF8(param.getString(KEY_PORT_FQN));
        return fqn;
    }

    public static PageParameters getParameters(NodeDto node) {
        PageParameters param = new PageParameters();
        if (node != null) {
            param.add(KEY_NODE, Util.encodeUTF8(node.getName()));
        }
        return param;
    }

    public static PageParameters getParameters(SlotDto slot) {
        PageParameters param = new PageParameters();
        param.add(KEY_NODE, Util.encodeUTF8(slot.getNode().getName()));
        param.add(KEY_SLOT_FQN, Util.encodeUTF8(slot.getAbsoluteName()));
        return param;
    }

    public static PageParameters getNodeParameters(NodeElementDto port) {
        PageParameters param = new PageParameters();
        if (port != null) {
            param.add(KEY_NODE, Util.encodeUTF8(port.getNode().getName()));
        }
        return param;
    }

    public static PageParameters getNodeParameters(String nodeName) {
        PageParameters param = new PageParameters();
        if (nodeName != null) {
            param.add(KEY_NODE, Util.encodeUTF8(nodeName));
        }
        return param;
    }

    public static PageParameters getPortParameters(PortDto port) {
        PageParameters param = new PageParameters();
        param.add(KEY_NODE, Util.encodeUTF8(port.getNode().getName()));
        param.add(KEY_PORT_FQN, Util.encodeUTF8(port.getAbsoluteName()));
        return param;
    }

    public static PageParameters getPortParameters(JackDto jack) {
        PageParameters param = new PageParameters();
        param.add(KEY_NODE, Util.encodeUTF8(jack.getNode().getName()));
        param.add(KEY_PORT_FQN, Util.encodeUTF8(jack.getAbsoluteName()));
        return param;
    }
}