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
import voss.multilayernms.inventory.nmscore.inventory.accessor.L2LinkHandler;
import voss.multilayernms.inventory.nmscore.model.converter.LinkModelDisplayNameConverter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class L2LinkListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(L2LinkListViewMaker.class);

    public L2LinkListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new LinkModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws AuthenticationException, RemoteException, MalformedURLException, NotBoundException, IOException, InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException {
        List<? extends IModel> linkList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    break;
                case port:
                    break;
                case link:
                    linkList = L2LinkHandler.get(inventoryId);
                    break;
                case lsp:
                    break;
                case pseudoWire:
                    break;
                default:
                    break;
            }
        } else {
            linkList = L2LinkHandler.getList(getQuery());
        }

        return getConverter().convertList(linkList);
    }

}