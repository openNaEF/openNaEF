package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RsvpLspReserveCanceller extends Modifier {

    private final Logger log = LoggerFactory.getLogger(RsvpLspReserveCanceller.class);

    public RsvpLspReserveCanceller(List<? extends IModel> targets, String userName) {
        super(targets, userName);

    }

    public List<LabelSwitchedPath> cancel() throws AuthenticationException, RemoteException, IOException, InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException {
        List<String> checked = checkStatus(getTargets());
        executeRemove(checked);

        List<LabelSwitchedPath> result = new ArrayList<LabelSwitchedPath>();
        for (IModel target : getTargets()) {
            LabelSwitchedPath lspModel = RsvpLspModelCreator.createNullModel(target.getId());
            result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lspModel));
        }
        return result;
    }

    private List<String> checkStatus(Collection<? extends IModel> targets) throws AuthenticationException, RemoteException, IOException, ExternalServiceException {
        List<String> checked = new ArrayList<String>();
        for (IModel target : targets) {
            String inventoryId = target.getId();
            if (checked.contains(inventoryId)) {
                continue;
            }

            RsvpLspDto lsp = RsvpLspHandler.getRsvpLspDto(inventoryId);
            RsvpLspDto lsp_opposite = RsvpLspExtUtil.getOppositLsp(lsp);
            boolean hasOpposite = (lsp_opposite != null ? true : false);

            checkLspStatus(lsp);
            checked.add(inventoryId);
            if (hasOpposite) {
                try {
                    String inventoryId_opposite = InventoryIdUtil.getInventoryId(lsp_opposite);
                    checkLspStatus(lsp_opposite);
                    checked.add(inventoryId_opposite);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(RsvpLspRenderer.getLspName(lsp) + "'s opposite: " + e.getMessage());
                }
            }
        }
        return checked;
    }

    private void executeRemove(List<String> checked) throws AuthenticationException, RemoteException, IOException, InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException {
        List<String> removed = new ArrayList<String>();
        for (String inventoryId : checked) {
            if (removed.contains(inventoryId)) {
                continue;
            }
            RsvpLspDto lsp = RsvpLspHandler.getRsvpLspDto(inventoryId);
            RsvpLspDto lsp_opposite = RsvpLspExtUtil.getOppositLsp(lsp);
            boolean hasOpposite = (lsp_opposite != null ? true : false);
            List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
            builders.addAll(RsvpLspHandler.buildRsvpLspRemover(lsp, getUserName()));
            removed.add(inventoryId);
            if (hasOpposite) {
                String inventoryId_opposite = InventoryIdUtil.getInventoryId(lsp_opposite);
                builders.addAll(RsvpLspHandler.buildRsvpLspRemover(lsp_opposite, getUserName()));
                removed.add(inventoryId_opposite);
            }
            ShellConnector.getInstance().executes(builders);
        }
    }

    private void checkLspStatus(RsvpLspDto lsp) {
        log.debug("RsvpLspRenderer.getFacilityStatus(lsp)[" + RsvpLspRenderer.getFacilityStatus(lsp) + "]");

        if (!RsvpLspRenderer.getFacilityStatus(lsp).equals(FacilityStatus.RESERVED.getDisplayString())) {
            throw new IllegalArgumentException(
                    "can't cancel reservation lsp[" + RsvpLspRenderer.getLspName(lsp) + "]: FacilityStatus is not [" + FacilityStatus.RESERVED + "]");
        }

    }

}