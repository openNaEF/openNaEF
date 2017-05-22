package voss.multilayernms.inventory.nmscore.view.list;

import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.model.converter.DisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.List;

public abstract class ListViewMaker {

    private final Logger log = LoggerFactory.getLogger(ListViewMaker.class);

    private final ObjectFilterQuery query;
    private final DisplayNameConverter converter;

    public ListViewMaker(ObjectFilterQuery query, DisplayNameConverter converter) {
        this.query = query;
        this.converter = converter;
    }

    public ObjectFilterQuery getQuery() {
        return query;
    }

    public DisplayNameConverter getConverter() {
        return converter;
    }

    public abstract TableInput makeListView() throws RemoteException, AuthenticationException, InventoryException, ExternalServiceException, IOException, NotBoundException, InstantiationException, IllegalAccessException, ParseException;

    protected boolean hasInventoryIdAsKeyInQuery() {
        return getQuery().containsKey("ID");
    }

    @SuppressWarnings("unchecked")
    protected String getInventoryIdFromQuery() {
        String inventoryId = null;

        Object target = getQuery().get("ID").getPattern();
        if (target instanceof String) {
            inventoryId = (String) target;
        } else if (target instanceof List) {
            inventoryId = (String) ((List<String>) target).get(0);
        }

        log.debug("inventoryId[" + inventoryId + "]");
        return inventoryId;

    }

    protected boolean hasMvoIdAsKeyInQuery() {
        return getQuery().containsKey("MVO_ID");
    }

    @SuppressWarnings("unchecked")
    protected String getMvoIdFromQuery() {
        String inventoryId = null;

        Object target = getQuery().get("MVO_ID").getPattern();
        if (target instanceof String) {
            inventoryId = (String) target;
        } else if (target instanceof List) {
            inventoryId = (String) ((List<String>) target).get(0);
        }

        log.debug("mvoId[" + inventoryId + "]");
        return inventoryId;

    }

}