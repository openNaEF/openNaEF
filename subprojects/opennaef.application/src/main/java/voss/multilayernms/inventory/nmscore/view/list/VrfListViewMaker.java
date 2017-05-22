package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.CommonHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VrfHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VrfModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class VrfListViewMaker extends ListViewMaker {

    public VrfListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new VrfModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException, ExternalServiceException, IOException,
            NotBoundException, InstantiationException, IllegalAccessException, ParseException {
        List<? extends IModel> vrfList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    vrfList = VrfHandler.getListOnNode(inventoryId);
                    break;
                case port:
                    vrfList = VrfHandler.getListOnPort(inventoryId);
                    break;
                case vrf:
                    vrfList = VrfHandler.get(inventoryId);
                    break;
                default:
                    break;
            }
        } else {
            vrfList = VrfHandler.getList(getQuery());
        }

        return getConverter().convertList(vrfList);
    }

}