package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RsvpLspFacilityStatusUpdater extends FacilityStatusUpdater {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(RsvpLspFacilityStatusUpdater.class);

    public RsvpLspFacilityStatusUpdater(Collection<? extends IModel> targets, String userName) throws IOException {
        super(targets, userName, new RsvpLspModelDisplayNameConverter());
    }

    @Override
    public List<? extends IModel> update() throws AuthenticationException, RemoteException, IOException,
            InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException {
        List<LabelSwitchedPath> result = new ArrayList<LabelSwitchedPath>();

        Map<String, FacilityStatus> newStatus = getNewStatus(getTargets());

        List<String> updated = new ArrayList<String>();
        for (Entry<String, FacilityStatus> entry : newStatus.entrySet()) {
            FacilityStatus fs = entry.getValue();

            String inventoryId = entry.getKey();
            if (updated.contains(inventoryId)) {
                continue;
            }

            RsvpLspDto lsp = RsvpLspHandler.getRsvpLspDto(inventoryId);
            RsvpLspDto lsp_opposite = RsvpLspExtUtil.getOppositLsp(lsp);
            boolean hasOpposite = (lsp_opposite != null ? true : false);

            if (lsp != null && !FacilityStatusUtil.isAllowableChangeByGUI(lsp, fs)) {
                throw new IllegalStateException("cannot change: LSP=[" + lsp.getName()
                        + "] " + fs.getDisplayString());
            }
            if (lsp_opposite != null && !FacilityStatusUtil.isAllowableChangeByGUI(lsp_opposite, fs)) {
                throw new IllegalStateException("cannot change: LSP=[" + lsp_opposite.getName()
                        + "] " + fs.getDisplayString());
            }

            RsvpLspHandler.executeSetFacilityStatus(RsvpLspHandler.getRsvpLspDto(inventoryId), fs, getUserName());
            updated.add(inventoryId);

            LabelSwitchedPath lspModel = RsvpLspModelCreator.createNullModel(inventoryId);
            result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lspModel));

            if (hasOpposite) {
                String inventoryId_opposite = InventoryIdUtil.getInventoryId(lsp_opposite);
                if (updated.contains(inventoryId_opposite)) {
                    continue;
                }

                updated.add(inventoryId_opposite);

                LabelSwitchedPath lspModel_opposite = RsvpLspModelCreator.createNullModel(inventoryId_opposite);
                result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lspModel_opposite));
            }
        }

        return result;
    }
}