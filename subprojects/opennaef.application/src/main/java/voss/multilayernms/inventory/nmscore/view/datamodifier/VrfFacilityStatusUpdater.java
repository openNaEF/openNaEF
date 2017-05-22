package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.vrf.VrfIfDto;
import net.phalanx.core.models.Vrf;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VrfHandler;
import voss.multilayernms.inventory.nmscore.model.converter.VrfModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.VrfModelCreator;
import voss.multilayernms.inventory.renderer.VrfRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class VrfFacilityStatusUpdater extends FacilityStatusUpdater {

    public VrfFacilityStatusUpdater(Collection<? extends IModel> targets, String userName) throws IOException {
        super(targets, userName, new VrfModelDisplayNameConverter());
    }

    @Override
    public List<? extends IModel> update() throws AuthenticationException,
            RemoteException, IOException, InventoryException,
            ExternalServiceException, InstantiationException,
            IllegalAccessException, NotBoundException {
        List<Vrf> result = new ArrayList<Vrf>();

        Map<String, FacilityStatus> newStatus = getNewStatus(getTargets());

        List<String> updated = new ArrayList<String>();
        for (Entry<String, FacilityStatus> entry : newStatus.entrySet()) {
            FacilityStatus fs = entry.getValue();

            String inventoryId = entry.getKey();
            if (updated.contains(inventoryId)) {
                continue;
            }

            VrfIfDto vrf = VrfHandler.getVrfIfDto(inventoryId);

            if (vrf != null && !FacilityStatusUtil.isAllowableChangeByGUI(vrf, fs)) {
                throw new IllegalStateException("cannot change: VRF=[" + VrfRenderer.getVpnId(vrf)
                        + "] " + fs.getDisplayString());
            }

            VrfHandler.executeSetFacilityStatus(VrfHandler.getVrfIfDto(inventoryId), fs, getUserName());
            updated.add(inventoryId);

            Vrf vrfModel = VrfModelCreator.createModel(vrf, inventoryId);
            result.add((Vrf) new VrfModelDisplayNameConverter().convertModel(vrfModel));
        }

        return result;
    }

}