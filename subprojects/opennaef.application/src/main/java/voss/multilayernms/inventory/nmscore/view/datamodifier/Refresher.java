package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

public abstract class Refresher extends Modifier {

    public Refresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    public abstract List<? extends IModel> refresh() throws RemoteException, InventoryException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException;

}