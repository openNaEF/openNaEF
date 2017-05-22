package voss.core.server.naming.inventory;

import voss.core.server.naming.naef.AbsoluteNameFactory;

public class InventoryIdBuilder {

    public static String getLayer1LinkID(String port1, String port2) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.L1LINK.name())
                .append(":")
                .append(escape(port1))
                .append(":")
                .append(escape(port2))
        ;
        return sb.toString();
    }

    public static String getLayer2LinkID(String port1, String port2) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.L2LINK.name())
                .append(":")
                .append(escape(port1))
                .append(":")
                .append(escape(port2))
        ;
        return sb.toString();
    }

    public static String getLayer2LinkID(String type, String port1, String port2) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.L1LINK.name())
                .append(":")
                .append(escape(type))
                .append(":")
                .append(escape(port1))
                .append(":")
                .append(escape(port2))
        ;
        return sb.toString();
    }

    public static String getPipeID(String nodeName, String pipeName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.PIPE.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(pipeName))
        ;
        return sb.toString();
    }

    public static String getPipeID(String nodeName, String pipeName, String acName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.PIPE.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(pipeName))
                .append(":")
                .append(escape(acName))
        ;
        return sb.toString();
    }

    public static String getPseudoWireID(String nodeName, String pseudoWireID) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.PW.name())
                .append(":")
                .append(escape(pseudoWireID))
                .append(":")
                .append(escape(nodeName))
        ;
        return sb.toString();
    }

    public static String getPseudoWireID(String pseudoWireID) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.PW.name())
                .append(":")
                .append(escape(pseudoWireID))
        ;
        return sb.toString();
    }

    public static String getPseudoWireID(String nodeName, Long pseudoWireID) {
        return getPseudoWireID(pseudoWireID.toString(), nodeName);
    }

    public static String getRsvpLspID(String ingressNodeName, String lspName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.LSP.name())
                .append(":")
                .append(escape(ingressNodeName))
                .append(":")
                .append(escape(lspName))
        ;
        return sb.toString();
    }

    public static String getPathID(String ingressNodeName, String lspName, String pathName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.LSPPATH.name())
                .append(":")
                .append(escape(ingressNodeName))
                .append(":")
                .append(escape(lspName))
                .append(":")
                .append(escape(pathName));
        return sb.toString();
    }

    public static String getVirtualNodeID(String host, String guest) {
        String nodeName = AbsoluteNameFactory.getVirtualNodeName(host, guest);
        return getNodeID(nodeName);
    }

    public static String getVrfID(String nodeName, String vrfName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.VRF.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(vrfName));
        return sb.toString();
    }

    public static String getVplsID(String nodeName, String vplsName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.VPLS.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(vplsName));
        return sb.toString();
    }

    public static String getNodeID(String nodeName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.NODE.name())
                .append(":")
                .append(escape(nodeName));
        return sb.toString();
    }

    public static String getNodeElementID(String nodeName, String elementID) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.NE.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(elementID))
        ;
        return sb.toString();
    }

    public static String getPortID(String nodeName, String ifName) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.PORT.name())
                .append(":")
                .append(escape(nodeName))
                .append(":")
                .append(escape(ifName));
        return sb.toString();
    }

    public static String getAliasID(String nodeName, String source) {
        StringBuilder sb = new StringBuilder();
        sb.append(InventoryIdType.ALIAS.name())
                .append(":")
                .append(nodeName)
                .append(":")
                .append(source);
        return sb.toString();
    }

    private static String escape(String s) {
        return InventoryIdCalculator.escape(s);
    }
}