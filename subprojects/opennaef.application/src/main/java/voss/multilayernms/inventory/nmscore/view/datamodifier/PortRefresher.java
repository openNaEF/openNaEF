package voss.multilayernms.inventory.nmscore.view.datamodifier;


import jp.iiga.nmt.core.model.IModel;
import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PortHandler;
import voss.multilayernms.inventory.nmscore.model.converter.PortModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.PortModelCreator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PortRefresher extends Refresher {

    public PortRefresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    @Override
    public List<? extends IModel> refresh() throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        List<PhysicalEthernetPort> result = new ArrayList<PhysicalEthernetPort>();

        List<String> inventoryIds = new ArrayList<String>();
        for (IModel target : getTargets()) {
            inventoryIds.add(target.getId());
        }
        for (String inventoryId : inventoryIds) {
            PhysicalEthernetPort portModel = PortModelCreator.createModel(PortHandler.getPortDto(inventoryId), inventoryId);
            result.add((PhysicalEthernetPort) new PortModelDisplayNameConverter().convertModel(portModel));
        }

        return result;
    }

}