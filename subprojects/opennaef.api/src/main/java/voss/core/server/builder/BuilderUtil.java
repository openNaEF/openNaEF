package voss.core.server.builder;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.database.ATTR;

public class BuilderUtil {

    public static String getIpIfName(String ipAddress) {
        if (ipAddress != null && !ipAddress.isEmpty()) {
            return ipAddress;
        }
        return null;
    }

    public static String getIpAbsoluteName(String ipAddress, NodeElementDto owner) {
        String ipIfName = getIpIfName(ipAddress);
        if (ipIfName == null) {
            return null;
        }
        NodeDto node = owner.getNode();
        return node.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY + ipIfName;
    }

    public static String getNodeName(String absoluteName) {
        if (absoluteName == null) {
            return null;
        }
        int idx = absoluteName.indexOf(',');
        if (idx > -1) {
            absoluteName = absoluteName.substring(0, idx);
        }
        if (absoluteName.startsWith("node;")) {
            absoluteName = absoluteName.substring(5);
        }
        return absoluteName;
    }

    public static boolean isValidVlanIfOwner(NodeElementDto owner) {
        if (NodeDto.class.isInstance(owner)) {
            return true;
        } else if (EthPortDto.class.isInstance(owner)) {
            return true;
        } else if (EthLagIfDto.class.isInstance(owner)) {
            return true;
        } else if (VlanIfDto.class.isInstance(owner)) {
            return true;
        }
        return false;
    }
}