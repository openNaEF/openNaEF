package voss.multilayernms.inventory.diff;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.NodeInfo;
import voss.model.Protocol;
import voss.model.ProtocolPort;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.diff.network.NodeInfoFactory;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MplsNmsNodeInfoFactory implements NodeInfoFactory {

    @Override
    public NodeInfo createNodeInfo(NodeDto node) throws IOException {
        Logger log = LoggerFactory.getLogger(MplsNmsNodeInfoFactory.class);
        if (node == null) {
            throw new IllegalArgumentException();
        }

        try {
            String ipAddress = NodeRenderer.getManagementIpAddress(node);
            String snmpRO = NodeRenderer.getSnmpCommunity(node);
            String snmpMethod = NodeRenderer.getSnmpMode(node);
            String userName = NodeRenderer.getConsoleLoginUserName(node);
            String userPass = NodeRenderer.getConsoleLoginPassword(node);
            String adminName = NodeRenderer.getPrivilegedUserName(node);
            String adminPass = NodeRenderer.getPrivilegedLoginPassword(node);

            if (snmpRO == null) {
                snmpRO = InventoryConfiguration.getInstance().getDefaultSnmpCommunityString();
                log.info("using default snmp-community: " + snmpRO + " (" + node.getName() + ")");
            } else {
                log.info("using snmp-community: " + snmpRO + " (" + node.getName() + ")");
            }
            if (snmpMethod == null) {
                snmpMethod = InventoryConfiguration.getInstance().getDefaultSnmpMethod();
                log.info("using default snmp-method: " + snmpMethod + " (" + node.getName() + ")");
            } else {
                log.info("using snmp-method: " + snmpMethod + " (" + node.getName() + ")");
            }

            NodeInfo nodeinfo = new NodeInfo(node.getName());
            if (ipAddress != null) {
                InetAddress inetAddress = InetAddress.getByAddress(
                        VossMiscUtility.getByteFormIpAddress(ipAddress));
                nodeinfo.addIpAddress(inetAddress);
            }
            nodeinfo.setCommunityStringRO(snmpRO);
            nodeinfo.setUserAccount(userName);
            nodeinfo.setUserPassword(userPass);
            nodeinfo.setAdminAccount(adminName);
            nodeinfo.setAdminPassword(adminPass);
            Protocol p = Protocol.getByCaption(snmpMethod);
            ProtocolPort pp = new ProtocolPort(p);
            nodeinfo.addSupportedProtocol(pp);
            nodeinfo.addSupportedProtocol(ProtocolPort.TELNET);
            return nodeinfo;
        } catch (UnknownHostException e) {
            log.warn("failed to create nodeinfo: " + node.getName(), e);
            return null;
        }
    }

}