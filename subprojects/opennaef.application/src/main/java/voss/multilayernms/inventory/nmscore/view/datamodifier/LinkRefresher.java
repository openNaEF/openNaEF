package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import jp.iiga.nmt.core.model.PhysicalLink;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.L2LinkHandler;
import voss.multilayernms.inventory.nmscore.model.converter.LinkModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LinkRefresher extends Refresher {

    public LinkRefresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    @Override
    public List<? extends IModel> refresh() throws RemoteException, InventoryException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        List<PhysicalLink> result = new ArrayList<PhysicalLink>();

        List<String> inventoryIds = new ArrayList<String>();
        for (IModel target : getTargets()) {
            inventoryIds.add(target.getId());
        }
        for (String inventoryId : inventoryIds) {
            PhysicalLink linkModel = LinkModelCreator.createModel(L2LinkHandler.getLinkDto(inventoryId), inventoryId);
            result.add((PhysicalLink) new LinkModelDisplayNameConverter().convertModel(linkModel));
        }

        return result;
    }

}