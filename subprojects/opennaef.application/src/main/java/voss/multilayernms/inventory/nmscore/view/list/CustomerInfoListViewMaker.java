package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.CustomerInfoHandler;
import voss.multilayernms.inventory.nmscore.model.converter.CustomerInfoModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class CustomerInfoListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(CustomerInfoListViewMaker.class);

    public CustomerInfoListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new CustomerInfoModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException,
            ExternalServiceException, IOException, NotBoundException,
            InstantiationException, IllegalAccessException, ParseException {

        List<? extends IModel> result = new ArrayList<IModel>();

        if (hasMvoIdAsKeyInQuery()) {
            result = CustomerInfoHandler.getList(getMvoIdFromQuery());
        } else {
            result = CustomerInfoHandler.getList(getQuery());
        }
        return getConverter().convertList(result);
    }

}