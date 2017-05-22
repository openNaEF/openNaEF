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
import voss.multilayernms.inventory.nmscore.inventory.accessor.PseudoWireHandler;
import voss.multilayernms.inventory.nmscore.model.converter.PseudoWireModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;

public class PseudoWireListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(PseudoWireListViewMaker.class);

    public PseudoWireListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new PseudoWireModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws NotBoundException, InstantiationException, IllegalAccessException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<? extends IModel> pwList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    pwList = PseudoWireHandler.getListOnNode(inventoryId);
                    break;
                case port:
                    pwList = PseudoWireHandler.getListOnPort(inventoryId);
                    break;
                case link:
                    pwList = PseudoWireHandler.getListOnLink(inventoryId);
                    break;
                case lsp:
                    pwList = PseudoWireHandler.getListUpperLsp(inventoryId);
                    break;
                case pseudoWire:
                    pwList = PseudoWireHandler.get(inventoryId);
                    break;
                default:
                    break;
            }
        } else {
            pwList = PseudoWireHandler.getList(getQuery());
        }

        return getConverter().convertList(pwList);
    }

}