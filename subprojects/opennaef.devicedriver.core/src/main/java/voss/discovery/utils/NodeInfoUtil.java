package voss.discovery.utils;

import voss.model.NodeInfo;
import voss.model.Protocol;
import voss.model.ProtocolPort;
import voss.util.VossMiscUtility;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NodeInfoUtil {

    public static NodeInfo createNodeInfo(String[] parameters) {
        if (parameters.length < 2) {
            throw new IllegalArgumentException();
        } else if (parameters[0].startsWith("#")) {
            return null;
        }

        try {
            NodeInfo nodeinfo = new NodeInfo();
            InetAddress inetAddress = InetAddress.getByAddress(VossMiscUtility.getByteFormIpAddress(parameters[0]));
            nodeinfo.addIpAddress(inetAddress);
            nodeinfo.setCommunityStringRO(parameters[1]);

            if (parameters.length >= 3) {
                nodeinfo.setUserAccount(parameters[2]);
            }
            if (parameters.length >= 4) {
                nodeinfo.setUserPassword(parameters[3]);
            }
            if (parameters.length >= 5) {
                nodeinfo.setAdminAccount(parameters[4]);
            }
            if (parameters.length >= 6) {
                nodeinfo.setAdminPassword(parameters[5]);
            }
            boolean snmp = false;
            if (parameters.length >= 7) {
                String snmpVersion = parameters[6];
                if (snmpVersion.toLowerCase().equals("v1")) {
                    nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V1);
                } else if (snmpVersion.toLowerCase().equals("v2next")) {
                    nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETNEXT);
                } else if (snmpVersion.toLowerCase().equals("v2walk")) {
                    nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETNEXT);
                } else {
                    nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETBULK);
                }
                snmp = true;
            }
            if (!snmp) {
                nodeinfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETBULK);
            }
            ProtocolPort pp = ProtocolPort.TELNET;
            if (parameters.length >= 8) {
                String cli = parameters[7];
                if (cli == null || cli.length() == 0) {
                    pp = ProtocolPort.TELNET;
                } else {
                    Protocol p = Protocol.valueOf(cli);
                    int port;
                    if (parameters[8] == null || parameters[8].isEmpty()) {
                        switch (p) {
                            case TELNET:
                                port = 23;
                                break;
                            case SSH2:
                                port = 22;
                                break;
                            default:
                                throw new IllegalStateException("unexpected console protocol type: " + p);
                        }
                    } else {
                        port = Integer.parseInt(parameters[8]);
                    }
                    pp = new ProtocolPort(p, port);
                }
            }
            nodeinfo.addSupportedProtocol(pp);
            if (parameters.length >= 10) {
                for (int i = 9; i < parameters.length; i++) {
                    String option = parameters[i];
                    if (option.indexOf('=') > -1) {
                        String[] arr = option.split("=");
                        String key = arr[0];
                        String value = arr[1];
                        nodeinfo.addOption(key, value);
                    } else {
                        nodeinfo.addOption(option, "true");
                    }
                }
            }
            return nodeinfo;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}