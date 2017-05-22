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
import voss.multilayernms.inventory.nmscore.inventory.accessor.PortHandler;
import voss.multilayernms.inventory.nmscore.model.converter.PortModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;

public class PortListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(PortListViewMaker.class);

    public PortListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new PortModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws NotBoundException, InstantiationException, IllegalAccessException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<? extends IModel> portList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    portList = PortHandler.getListOwnedByNode(inventoryId);
                    break;
                case port:
                    portList = PortHandler.get(inventoryId);
                    break;
                case link:
                    break;
                case lsp:
                    break;
                case pseudoWire:
                    break;
                default:
                    break;
            }
        } else {
            portList = PortHandler.getList(getQuery());
        }

        return getConverter().convertList(portList);
    }

}