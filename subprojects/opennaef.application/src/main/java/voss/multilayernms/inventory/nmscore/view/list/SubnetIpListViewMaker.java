package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.SubnetIpHandler;
import voss.multilayernms.inventory.nmscore.model.converter.SubnetIpModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SubnetIpListViewMaker extends ListViewMaker {

    private final Logger log = LoggerFactory.getLogger(SubnetIpListViewMaker.class);

    public SubnetIpListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new SubnetIpModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException,
            ExternalServiceException, IOException, NotBoundException,
            InstantiationException, IllegalAccessException, ParseException {

        List<? extends IModel> subnetList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String mvoId = getInventoryIdFromQuery();
            subnetList = SubnetIpHandler.getList(getQuery(), mvoId);
        } else {
            subnetList = SubnetIpHandler.getList(getQuery());
        }
        return getConverter().convertList(subnetList);
    }

}