package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.IObjectFilterQuery;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VlanLinkHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VlanLinkModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class VlanLinkListViewMaker extends ListViewMaker {
    private final static Logger log = LoggerFactory.getLogger(VlanLinkListViewMaker.class);

    public VlanLinkListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new VlanLinkModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws RemoteException,
            AuthenticationException, InventoryException,
            ExternalServiceException, IOException, NotBoundException,
            InstantiationException, IllegalAccessException, ParseException {

        List<? extends IModel> vlanLinkList = new ArrayList<IModel>();

        for (String str : getQuery().keySet()) {
            log.debug(str + " : " + getQuery().getMatcher(str).hashCode());
        }

        if (hasMvoIdAsKeyInQuery()) {
            String mvoId = getMvoIdFromQuery();
            vlanLinkList = VlanLinkHandler.getList(mvoId);
        }
        return getConverter().convertList(vlanLinkList);
    }

    protected boolean hasMvoIdAsKeyInQuery() {
        return getQuery().containsKey(IObjectFilterQuery.MATCH_MVO_ID);
    }

    @SuppressWarnings("unchecked")
    protected String getMvoIdFromQuery() {
        String mvoId = null;

        Object target = getQuery().get(IObjectFilterQuery.MATCH_MVO_ID).getPattern();
        if (target instanceof String) {
            mvoId = (String) target;
        } else if (target instanceof List) {
            mvoId = (String) ((List<String>) target).get(0);
        }

        return mvoId;
    }


}