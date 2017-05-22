package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.vpls.VplsIfDto;
import net.phalanx.core.models.Vpls;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VplsHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VplsModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.VplsModelCreator;
import voss.multilayernms.inventory.renderer.VplsRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class VplsFacilityStatusUpdater extends FacilityStatusUpdater {

    public VplsFacilityStatusUpdater(Collection<? extends IModel> targets, String userName) throws IOException {
        super(targets, userName, new VplsModelDisplayNameConverter());
    }

    @Override
    public List<? extends IModel> update() throws AuthenticationException,
            RemoteException, IOException, InventoryException,
            ExternalServiceException, InstantiationException,
            IllegalAccessException, NotBoundException {
        List<Vpls> result = new ArrayList<Vpls>();

        Map<String, FacilityStatus> newStatus = getNewStatus(getTargets());

        List<String> updated = new ArrayList<String>();
        for (Entry<String, FacilityStatus> entry : newStatus.entrySet()) {
            FacilityStatus fs = entry.getValue();

            String inventoryId = entry.getKey();
            if (updated.contains(inventoryId)) {
                continue;
            }

            VplsIfDto vpls = VplsHandler.getVplsIfDto(inventoryId);

            if (vpls != null && !FacilityStatusUtil.isAllowableChangeByGUI(vpls, fs)) {
                throw new IllegalStateException("cannot change: VPLS=[" + VplsRenderer.getVplsId(vpls)
                        + "] " + fs.getDisplayString());
            }

            VplsHandler.executeSetFacilityStatus(VplsHandler.getVplsIfDto(inventoryId), fs, getUserName());
            updated.add(inventoryId);

            Vpls vplsModel = VplsModelCreator.createModel(vpls, inventoryId);
            result.add((Vpls) new VplsModelDisplayNameConverter().convertModel(vplsModel));
        }

        return result;
    }

}