package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.model.Device;
import naef.dto.NaefDto;
import naef.dto.NodeDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.model.NodeInfo;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.NodeModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.NodeRenderingUtil;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.diff.network.NodeInfoFactory;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class NodeHandler {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NodeHandler.class);

    public static List<Device> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<Device> nodeList = new ArrayList<Device>();
        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            if (InventoryIdUtil.getInventoryId(node).equals(inventoryId)) {
                nodeList.add(NodeModelCreator.createModel(node, InventoryIdUtil.getInventoryId(node)));
                break;
            }
        }
        return nodeList;

    }

    public static NodeDto getNodeDto(String inventoryId) throws InventoryException, ExternalServiceException, IOException, ParseException {
        NaefDto dto = InventoryIdDecoder.getDto(inventoryId);
        if (dto instanceof NodeDto) {
            return (NodeDto) dto;
        } else {
            throw new IllegalArgumentException("Id[" + inventoryId + "] is not Node");
        }
    }

    public static NodeInfo getNodeInfo(String inventoryId) throws RemoteException, InventoryException, ExternalServiceException, IOException {
        try {
            NodeInfoFactory factory = InventoryConfiguration.getInstance().getNodeInfoFactory();
            NodeDto node = NodeUtil.getNode(CommonHandler.getNodeName(inventoryId));
            return factory.createNodeInfo(node);
        } catch (InstantiationException e) {
            throw new IOException(e);
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public static List<Device> getList(ObjectFilterQuery query) throws AuthenticationException, RemoteException, NotBoundException, IOException, ExternalServiceException {
        List<Device> nodeList = new ArrayList<Device>();
        for (NodeDto node : filterNodes(query, MplsNmsInventoryConnector.getInstance().getActiveNodes())) {
            nodeList.add(NodeModelCreator.createModel(node, InventoryIdUtil.getInventoryId(node)));
        }
        return nodeList;
    }

    private static List<NodeDto> filterNodes(ObjectFilterQuery query, Collection<NodeDto> nodes) {
        List<NodeDto> result = new ArrayList<NodeDto>();

        for (NodeDto node : nodes) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = NodeRenderingUtil.rendering(node, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(node);
            }
        }
        return result;
    }


}