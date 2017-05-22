package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.models.Vrf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VrfHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VplsModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.VrfModelCreator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VrfRefresher extends Refresher {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(VrfRefresher.class);

    public VrfRefresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    @Override
    public List<? extends IModel> refresh() throws RemoteException,
            ExternalServiceException, IOException, AuthenticationException,
            NotBoundException, InstantiationException, IllegalAccessException {
        List<Vrf> result = new ArrayList<Vrf>();

        List<String> inventoryIds = new ArrayList<String>();
        for (IModel target : getTargets()) {
            inventoryIds.add(target.getId());
        }
        for (String inventoryId : inventoryIds) {
            Vrf vrfModel = VrfModelCreator.createModel(VrfHandler.getVrfIfDto(inventoryId), inventoryId);
            result.add((Vrf) new VplsModelDisplayNameConverter().convertModel(vrfModel));
        }

        return result;
    }

}