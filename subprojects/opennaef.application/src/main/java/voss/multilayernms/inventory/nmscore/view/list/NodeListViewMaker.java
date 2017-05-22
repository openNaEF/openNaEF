package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.CommonHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.NodeHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PseudoWireHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.NodeModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class NodeListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(NodeListViewMaker.class);

    public NodeListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new NodeModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException, AuthenticationException, InventoryException, ExternalServiceException, IOException, NotBoundException, InstantiationException, IllegalAccessException {
        List<? extends IModel> nodeList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    nodeList = NodeHandler.get(inventoryId);
                    break;
                case port:
                    break;
                case link:
                    break;
                case lsp:
                    nodeList = NodeHandler.get(RsvpLspHandler.parseNodeId(inventoryId));
                    break;
                case pseudoWire:
                    nodeList = NodeHandler.get(PseudoWireHandler.parseNodeId(inventoryId));
                    break;
                default:
                    break;
            }
        } else {
            nodeList = NodeHandler.getList(getQuery());
        }

        return getConverter().convertList(nodeList);
    }


}