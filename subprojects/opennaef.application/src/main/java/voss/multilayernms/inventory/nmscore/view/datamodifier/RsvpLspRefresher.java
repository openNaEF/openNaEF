package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RsvpLspRefresher extends Refresher {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(RsvpLspRefresher.class);

    public RsvpLspRefresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    @Override
    public List<? extends IModel> refresh() throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        List<LabelSwitchedPath> result = new ArrayList<LabelSwitchedPath>();

        List<String> inventoryIds = new ArrayList<String>();
        for (IModel target : getTargets()) {
            inventoryIds.add(target.getId());
        }
        for (String inventoryId : inventoryIds) {
            LabelSwitchedPath lspModel = RsvpLspModelCreator.createModel(RsvpLspHandler.getRsvpLspDto(inventoryId), inventoryId);
            result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lspModel));
        }

        return result;
    }

}
