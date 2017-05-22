package voss.nms.inventory.diff.network;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.model.NodeInfo;
import voss.model.ProtocolPort;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DefaultNodeInfoFactory implements NodeInfoFactory {

    @Override
    public NodeInfo createNodeInfo(NodeDto node) throws IOException {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        Logger log = LoggerFactory.getLogger(getClass());
        try {
            String ipAddress = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.MANAGEMENT_IP);
            String snmpRO = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.SNMP_COMMUNITY);
            String userName = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.TELNET_ACCOUNT);
            String userPass = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.TELNET_PASSWORD);
            String adminName = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.ADMIN_ACCOUNT);
            String adminPass = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.ADMIN_PASSWORD);

            if (ipAddress == null) {
                throw new IllegalStateException("magament ip address is mandatory.");
            }
            if (snmpRO == null) {
                throw new IllegalStateException("SNMP community string (RO) is mandatory.");
            }

            NodeInfo nodeinfo = new NodeInfo(node.getName());
            InetAddress inetAddress = InetAddress.getByAddress(
                    VossMiscUtility.getByteFormIpAddress(ipAddress));
            nodeinfo.addIpAddress(inetAddress);
            nodeinfo.setCommunityStringRO(snmpRO);
            nodeinfo.setUserAccount(userName);
            nodeinfo.setUserPassword(userPass);
            nodeinfo.setAdminAccount(adminName);
            nodeinfo.setAdminPassword(adminPass);
            nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETBULK);
            nodeinfo.addSupportedProtocol(ProtocolPort.TELNET);
            return nodeinfo;
        } catch (UnknownHostException e) {
            log.warn("failed to create node-info: " + node.getName(), e);
            return null;
        }
    }

}