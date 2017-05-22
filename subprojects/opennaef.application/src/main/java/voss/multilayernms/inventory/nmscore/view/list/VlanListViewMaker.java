package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VlanHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VlanModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class VlanListViewMaker extends ListViewMaker {

    public VlanListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new VlanModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException,
            ExternalServiceException, IOException, NotBoundException,
            InstantiationException, IllegalAccessException, ParseException {
        List<? extends IModel> vlanList = new ArrayList<IModel>();
        if (hasMvoIdAsKeyInQuery()) {
            vlanList = VlanHandler.getList(getMvoIdFromQuery());
        } else {
            vlanList = VlanHandler.getList(getQuery());
        }

        return getConverter().convertList(vlanList);
    }

}