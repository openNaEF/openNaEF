package voss.core.server.util;

import naef.dto.NodeDto;
import naef.mvo.Node.VirtualizationHostedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;

import java.util.ArrayList;
import java.util.List;

public class VirtualNodeCoreUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VirtualNodeCoreUtil.class);

    public static boolean isVirtualizationHostingEnable(NodeDto node) {
        if (node == null) {
            return false;
        }
        return DtoUtil.getBoolean(node, ATTR.ATTR_VIRTUALIZATION_HOSTING_ENABLED);
    }

    public static VirtualizationHostedType getVirtualizationHostedType(NodeDto node) {
        if (node == null) {
            return null;
        }
        return node.getVirtualizationHostedType();
    }

    public static List<NodeDto> getVirtualizationGuestNodes(NodeDto node) {
        List<NodeDto> result = new ArrayList<NodeDto>();
        if (node == null) {
            return result;
        }
        result.addAll(node.getVirtualizationGuestNodes());
        return result;
    }

    public static List<NodeDto> getVirutalizationHostNodes(NodeDto node) {
        List<NodeDto> result = new ArrayList<NodeDto>();
        if (node == null) {
            return result;
        }
        result.addAll(node.getVirtualizationHostNodes());
        return result;
    }

    public static NodeDto getVirtualizationHostNode(NodeDto node) {
        if (node == null) {
            return null;
        }
        return node.getVirtualizationHostNode();
    }

    public static String getGuestNodeName(NodeDto node) {
        if (node == null) {
            return null;
        }
        return getGuestNodeName(node.getName());
    }

    public static String getGuestNodeName(String nodeName) {
        if (nodeName == null) {
            return null;
        }
        int idx = nodeName.indexOf('@');
        if (idx == -1) {
            return nodeName;
        } else {
            return nodeName.substring(0, idx);
        }
    }

    public static String getHostNodeName(NodeDto node) {
        if (node == null) {
            return null;
        }
        return getHostNodeName(node.getName());
    }

    public static String getHostNodeName(String nodeName) {
        if (nodeName == null) {
            return null;
        }
        String name = getNonGuestNodeName(nodeName);
        int idx = name.indexOf('@');
        if (idx == -1) {
            return name;
        } else {
            return name.substring(0, idx);
        }
    }

    public static String getNonGuestNodeName(NodeDto node) {
        if (node == null) {
            return null;
        }
        return getNonGuestNodeName(node.getName());
    }

    public static String getNonGuestNodeName(String nodeName) {
        if (nodeName == null) {
            return null;
        }
        int idx = nodeName.indexOf('@');
        if (idx == -1) {
            return null;
        } else {
            return nodeName.substring(idx + 1);
        }
    }
}