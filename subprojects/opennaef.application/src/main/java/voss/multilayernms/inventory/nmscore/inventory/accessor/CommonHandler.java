package voss.multilayernms.inventory.nmscore.inventory.accessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdBuilder;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.core.server.naming.inventory.InventoryIdType;
import voss.multilayernms.inventory.nmscore.inventory.constants.OBJECT_TYPE;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;


public class CommonHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonHandler.class);

    public static OBJECT_TYPE getObjectType(String inventoryId) throws RemoteException, InventoryException, IOException {
        InventoryIdType type = InventoryIdDecoder.getType(inventoryId);
        if (type == null) {
            throw new IllegalArgumentException("unexpected type: " + inventoryId);
        }
        switch (type) {
            case VRF:
                return OBJECT_TYPE.vrf;
            case VPLS:
                return OBJECT_TYPE.vpls;
            case PW:
                return OBJECT_TYPE.pseudoWire;
            case LSP:
                return OBJECT_TYPE.lsp;
            case LSPPATH:
                return OBJECT_TYPE.lsppath;
            case L3LINK:
                return OBJECT_TYPE.link;
            case L2LINK:
                return OBJECT_TYPE.link;
            case PORT:
                return OBJECT_TYPE.port;
            case NODE:
                return OBJECT_TYPE.node;
            case PIPE:
                return OBJECT_TYPE.other;
        }
        throw new IllegalArgumentException("can't identify object type: " + type);
    }

    public static String getNodeName(String inventoryId) throws RemoteException, InventoryException, IOException {
        String nodeID = getNodeId(inventoryId, getObjectType(inventoryId));
        try {
            return InventoryIdDecoder.getNode(nodeID);
        } catch (ParseException e) {
            throw new InventoryException(e);
        }
    }

    public static String getNodeId(String inventoryId, OBJECT_TYPE objectType) throws RemoteException, InventoryException, IOException {

        switch (objectType) {
            case node:
                try {
                    String nodeName = InventoryIdDecoder.getNode(inventoryId);
                    return InventoryIdBuilder.getNodeID(nodeName);
                } catch (ParseException e) {
                    throw new InventoryException("parse failed: " + inventoryId, e);
                }
            case port:
                return PortHandler.parseNodeName(inventoryId);
            case link:
                return PortHandler.parseNodeName(LinkHandler.parseInventoryIdOfPort1(inventoryId));
            case lsp:
                return RsvpLspHandler.parseNodeId(inventoryId);
            case pseudoWire:
                return PseudoWireHandler.parseNodeId(inventoryId);
            default:
                break;
        }
        throw new IllegalArgumentException("can't identify object type ");
    }
}