package voss.core.server.naming.inventory;

import naef.dto.*;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.NamingUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class InventoryIdDecoder {

    private static void checkArg(String inventoryID, InventoryIdType... types) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException();
        }
        if (inventoryID == null) {
            throw new IllegalArgumentException();
        }
        InventoryIdType type_ = getType(inventoryID);
        boolean match = false;
        for (InventoryIdType type : types) {
            if (type_ == type) {
                match = true;
                break;
            }
        }
        if (!match) {
            throw new IllegalStateException("type mismatch: [" + Arrays.toString(types) + "], [" + inventoryID + "]");
        }
    }

    public static NaefDto getDto(String inventoryID) throws InventoryException, ExternalServiceException, IOException, ParseException {
        if (inventoryID == null) {
            throw new IllegalArgumentException();
        }
        NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
        InventoryIdType type = getType(inventoryID);
        switch (type) {
            case L1LINK:
                PortDto l1Port1 = getPort1FromLinkID(inventoryID);
                return NodeUtil.getLayer1Link(l1Port1);
            case L2LINK:
                PortDto l2Port1 = getPort1FromLinkID(inventoryID);
                return NodeUtil.getLayer2Link(l2Port1);
            case L2LINK_CUSTOM:
                PortDto l2EP = getPort1FromLinkID(inventoryID);
                String linkType = getCustomLinkType(inventoryID);
                return NodeUtil.getL2LinkOf(l2EP, linkType);
            case L3LINK:
                PortDto port1 = getPort1FromLinkID(inventoryID);
                return NodeUtil.getLayer3Link(port1);
            case LSP:
                String nodeName = getNode(inventoryID);
                String lspName = getLspName(inventoryID);
                NodeDto node = NodeUtil.getNode(nodeName);
                Set<RsvpLspDto> lsps = facade.getRsvpLsps(node);
                for (RsvpLspDto lsp : lsps) {
                    if (DtoUtil.hasStringValue(lsp, ATTR.LSP_NAME, lspName)) {
                        return lsp;
                    }
                }
                return null;
            case LSPPATH:
                break;
            case NE:
                return getNodeElementDto(inventoryID);
            case NODE:
                String name = getNode(inventoryID);
                NodeDto nodeDto = NodeUtil.getNode(name);
                return nodeDto;
            case PORT:
                String portNodeName = getNode(inventoryID);
                String portIfName = getPortIfName(inventoryID);
                NodeDto node_ = NodeUtil.getNode(portNodeName);
                if (node_ == null) {
                    return null;
                }
                Set<PortDto> ports = facade.selectNodeElements(node_, PortDto.class,
                        SearchMethod.EXACT_MATCH, NamingUtil.getIfNameAttributeName(), portIfName);
                if (ports.size() == 1) {
                    return ports.iterator().next();
                } else if (ports.size() > 1) {
                    throw new IllegalArgumentException("duplicated ifname found:" + inventoryID);
                }
                return null;
            case ALIAS:
                String aliasNodeName = getNode(inventoryID);
                String sourceName = getAliasSourceName(inventoryID);
                PortDto source = (PortDto) getDto(sourceName);
                if (source == null) {
                    return null;
                }
                for (PortDto alias : source.getAliases()) {
                    if (alias.getNode() == null) {
                        continue;
                    } else if (alias.getNode().getName().equals(aliasNodeName)) {
                        return alias;
                    }
                }
                return null;
            case PIPE:
                String pipeIfName = getPipeName(inventoryID);
                String pipeNodeName = getNode(inventoryID);
                NodeDto pipeNode = NodeUtil.getNode(pipeNodeName);
                if (pipeNode == null) {
                    return null;
                }
                Set<InterconnectionIfDto> pipes = facade.selectNodeElements(pipeNode,
                        InterconnectionIfDto.class, SearchMethod.EXACT_MATCH, NamingUtil.getIfNameAttributeName(), pipeIfName);
                if (pipes.size() == 0) {
                    return null;
                } else if (pipes.size() == 1) {
                    return pipes.iterator().next();
                } else {
                    throw new IllegalStateException("ambiguous pipe if-name: " + inventoryID);
                }
            case PW:
                String acNodeName = getNode(inventoryID);
                NodeDto acNode = NodeUtil.getNode(acNodeName);
                String pwID = getPseudoWireID(inventoryID);
                Collection<PseudowireDto> pws;
                if (acNodeName == null) {
                    pws = facade.getPseudowires();
                } else {
                    pws = facade.getPseudowires(acNode);
                }
                for (PseudowireDto pw : pws) {
                    if (pw.getLongId() != null && pw.getLongId().toString().equals(pwID)) {
                        return pw;
                    } else if (pw.getStringId() != null && pw.getStringId().equals(pwID)) {
                        return pw;
                    }
                }
                return null;
            case VLAN:
                break;
            case VPLS:
                break;
            case VRF:
                break;
        }
        throw new IllegalArgumentException("unknown id: " + inventoryID);
    }

    private static PortDto getPort1FromLinkID(String inventoryID) throws InventoryException, ExternalServiceException, IOException, ParseException {
        String port1Name = getLinkPort1(inventoryID);
        PortDto port1 = (PortDto) getDto(port1Name);
        if (port1 == null) {
            throw new IllegalStateException("no port:" + inventoryID);
        }
        return port1;
    }

    private static String getCustomLinkType(String inventoryID) {
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 3) {
            throw new IllegalArgumentException("illegal inventory-id: " + inventoryID);
        }
        InventoryIdType t = InventoryIdType.valueOf(arr.get(0));
        if (InventoryIdType.L2LINK_CUSTOM == t) {
            return arr.get(1);
        }
        return null;
    }

    public static String getNode(String inventoryID) throws ParseException {
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() == 1) {
            return inventoryID;
        }
        InventoryIdType type = getType(inventoryID);
        String result = null;
        switch (type) {
            case NODE:
                result = arr.get(1);
                break;
            case LSP:
                if (arr.size() > 2) {
                    result = arr.get(1);
                }
                break;
            case LSPPATH:
                if (arr.size() > 3) {
                    result = arr.get(1);
                }
                break;
            case NE:
                result = arr.get(1);
                break;
            case PIPE:
                result = arr.get(1);
                break;
            case PORT:
                result = arr.get(1);
                break;
            case ALIAS:
                result = arr.get(1);
                break;
            case PW:
                if (arr.size() > 2) {
                    result = arr.get(2);
                }
                break;
            case VLAN:
                break;
            case VPLS:
                break;
            case VRF:
                break;
        }
        return unescape(result);
    }

    public static NodeElementDto getNodeElementDto(String inventoryID) throws ParseException {
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        try {
            NodeDto node = NodeUtil.getNode(arr.get(1));
            String nodeElementName = arr.get(2);
            List<String> elemNames = splitElementName(nodeElementName);
            int level = 0;
            NodeElementDto result = getNodeElement(node.getSubElements(), elemNames, level);
            return result;
        } catch (Exception e) {
            ParseException ex = new ParseException(inventoryID, 0);
            ex.initCause(e);
            throw ex;
        }
    }

    private static List<String> splitElementName(String elementName) {
        List<String> result = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < elementName.length(); i++) {
            int ch = elementName.codePointAt(i);
            if (ch == '/') {
                if (first) {
                    result.add(sb.toString());
                    first = false;
                    sb = new StringBuilder();
                } else {
                    result.add(sb.toString());
                    result.add("");
                    sb = new StringBuilder();
                }
            } else {
                sb.append((char) (0xffff & ch));
            }
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }

    public static String getNodeElement(String inventoryID) throws ParseException {
        NodeElementDto dto = getNodeElementDto(inventoryID);
        if (dto == null) {
            return null;
        }
        return dto.getName();
    }

    private static NodeElementDto getNodeElement(Collection<NodeElementDto> elements, List<String> names, int current) {
        String targetName = Util.s2n(names.get(current));
        System.err.println(current + ":" + targetName);
        NodeElementDto target = null;
        for (NodeElementDto element : elements) {
            if (Util.s2n(element.getName()) == null && targetName == null) {
                target = element;
                break;
            } else if (element.getName().equals(targetName)) {
                target = element;
                break;
            }
        }
        if (target == null) {
            System.err.println("not found. " + targetName);
            return null;
        }
        if (current == (names.size() - 1)) {
            return target;
        } else {
            return getNodeElement(target.getSubElements(), names, current + 1);
        }
    }

    public static String getPortIfName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.PORT);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 2) {
            throw new IllegalStateException("no port part:" + inventoryID);
        }
        return unescape(arr.get(2));
    }

    public static String getAliasSourceName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.ALIAS);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 4) {
            throw new IllegalStateException("no port part:" + inventoryID);
        }
        return InventoryIdBuilder.getPortID(arr.get(3), arr.get(4));
    }

    public static String getLspName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.LSP, InventoryIdType.LSPPATH);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 2) {
            throw new ParseException(inventoryID, 0);
        }
        return unescape(arr.get(2));
    }

    public static String getPathName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.LSPPATH);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 3) {
            throw new ParseException(inventoryID, 0);
        }
        return unescape(arr.get(3));
    }

    public static String getPseudoWireID(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.PW);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 2) {
            throw new ParseException("no PseudoWireID: " + inventoryID, 0);
        }
        return arr.get(1);
    }

    public static String getPipeName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.PIPE);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 3) {
            throw new ParseException("no PipeName: " + inventoryID, 0);
        }
        return arr.get(2);
    }

    public static String getPipeAcName(String inventoryID) throws ParseException {
        checkArg(inventoryID, InventoryIdType.PIPE);
        List<String> arr = Util.splitWithEscape(inventoryID, ":");
        if (arr.size() < 3) {
            throw new ParseException("no Pipe AcName: " + inventoryID, 0);
        }
        return arr.get(3);
    }

    public static String getLinkPort1(String inventoryID) {
        List<String> portNames = Util.splitWithEscape(inventoryID, ":");
        if (portNames.size() == 0) {
            throw new IllegalStateException("no port-name:" + inventoryID);
        }
        int index = 1;
        if (portNames.get(0).equals(InventoryIdType.L2LINK_CUSTOM.name())) {
            index++;
        }
        return portNames.get(index);
    }

    public static String getLinkPort2(String inventoryID) {
        List<String> portNames = Util.splitWithEscape(inventoryID, ":");
        if (portNames.size() == 0) {
            throw new IllegalStateException("no name:" + inventoryID);
        }
        int index = 2;
        if (portNames.get(0).equals(InventoryIdType.L2LINK_CUSTOM.name())) {
            index++;
        }
        if (portNames.size() <= index) {
            throw new IllegalStateException("dangling link id: " + inventoryID);
        }
        return portNames.get(index);
    }

    public static InventoryIdType getType(String id) {
        if (id == null) {
            throw new IllegalArgumentException("arg is null.");
        } else if (id.indexOf(':') == -1) {
            throw new IllegalArgumentException("no type id:" + id);
        }
        String typeName = id.substring(0, id.indexOf(':'));
        return InventoryIdType.valueOf(typeName);
    }

    private static String unescape(String s) {
        return InventoryIdCalculator.unEscape(s);
    }

}