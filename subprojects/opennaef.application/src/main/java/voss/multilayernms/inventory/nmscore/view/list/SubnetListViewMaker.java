package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.SubnetHandler;
import voss.multilayernms.inventory.nmscore.model.converter.SubnetModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SubnetListViewMaker extends ListViewMaker {

    public SubnetListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new SubnetModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException,
            ExternalServiceException, IOException, NotBoundException,
            InstantiationException, IllegalAccessException, ParseException {

        List<? extends IModel> subnetList = new ArrayList<IModel>();
        if (hasInventoryIdAsKeyInQuery()) {
            subnetList = SubnetHandler.getList(getMvoIdFromQuery());
        } else {
            subnetList = SubnetHandler.getList(getQuery());
        }

        return getConverter().convertList(subnetList);
    }

}