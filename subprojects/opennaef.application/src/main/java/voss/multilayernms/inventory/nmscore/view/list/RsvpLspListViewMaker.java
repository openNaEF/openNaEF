package voss.multilayernms.inventory.nmscore.view.list;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.TableInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.CommonHandler;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;

public class RsvpLspListViewMaker extends ListViewMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(RsvpLspListViewMaker.class);

    public RsvpLspListViewMaker(ObjectFilterQuery query) throws IOException {
        super(query, new RsvpLspModelDisplayNameConverter());
    }

    @Override
    public TableInput makeListView() throws NotBoundException, InstantiationException, IllegalAccessException,
            AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<? extends IModel> lspList = new ArrayList<IModel>();

        if (hasInventoryIdAsKeyInQuery()) {
            String inventoryId = getInventoryIdFromQuery();
            switch (CommonHandler.getObjectType(inventoryId)) {
                case node:
                    lspList = RsvpLspHandler.getListOnNode(inventoryId);
                    break;
                case port:
                    lspList = RsvpLspHandler.getListOnPort(inventoryId);
                    break;
                case link:
                    lspList = RsvpLspHandler.getListOnLink(inventoryId);
                    break;
                case lsp:
                    lspList = RsvpLspHandler.get(inventoryId);
                    break;
                case lsppath:
                    lspList = RsvpLspHandler.get(InventoryIdUtil.normalizeLspId(inventoryId));
                    break;
                case pseudoWire:
                    lspList = RsvpLspHandler.getListUnderPseudoWire(inventoryId);
                    break;
                default:
                    break;
            }
        } else {
            lspList = RsvpLspHandler.getList(getQuery());
        }

        return getConverter().convertList(lspList);
    }
}