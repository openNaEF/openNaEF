package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.CommonHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VplsHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VplsModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class VplsListViewMaker extends ListViewMaker {

    public VplsListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new VplsModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException, ExternalServiceException, IOException,
            NotBoundException, InstantiationException, IllegalAccessException, ParseException {
        List<? extends IModel> vplsList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    vplsList = VplsHandler.getListOnNode(inventoryId);
                    break;
                case port:
                    vplsList = VplsHandler.getListOnPort(inventoryId);
                    break;
                case vrf:
                    vplsList = VplsHandler.get(inventoryId);
                    break;
                default:
                    break;
            }
        } else {
            vplsList = VplsHandler.getList(getQuery());
        }

        return getConverter().convertList(vplsList);
    }

}