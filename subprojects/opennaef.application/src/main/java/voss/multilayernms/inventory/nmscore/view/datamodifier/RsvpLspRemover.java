package voss.multilayernms.inventory.nmscore.view.datamodifier;

import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.multilayernms.inventory.util.RsvpLspExtUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class RsvpLspRemover {

    private final Logger log = LoggerFactory.getLogger(RsvpLspRemover.class);

    private final LabelSwitchedPath target;
    private final String userName;

    public RsvpLspRemover(LabelSwitchedPath target, String userName) {
        this.target = target;
        this.userName = userName;

        log.debug("id:" + target.getId());

        for (String key : target.getMetaData().getProperties().keySet()) {
            log.debug(key + "/" + target.getMetaData().getProperties().get(key));
        }
    }

    public LabelSwitchedPath getTarget() {
        return target;
    }

    public String getUserName() {
        return (userName == null ? "null" : userName);
    }

    public List<LabelSwitchedPath> remove() throws AuthenticationException, RemoteException, IOException, InventoryException, ExternalServiceException, InstantiationException, IllegalAccessException {
        if (getTarget() == null) {
            throw new IllegalArgumentException("can't remove lsp : lsps.size() is not 1.");
        }

        String inventoryId = getTarget().getId();
        RsvpLspDto lsp = RsvpLspHandler.getRsvpLspDto(inventoryId);
        RsvpLspDto lsp_opposite = RsvpLspExtUtil.getOppositLsp(lsp);
        boolean hasOpposite = (lsp_opposite != null ? true : false);

        checkLspStatus(lsp);
        if (hasOpposite) {
            checkLspStatus(lsp_opposite);
        }

        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        builders.addAll(RsvpLspHandler.buildRsvpLspRemover(lsp, getUserName()));
        if (hasOpposite) {
            builders.addAll(RsvpLspHandler.buildRsvpLspRemover(lsp_opposite, getUserName()));
        }
        ShellConnector.getInstance().executes(builders);
        List<LabelSwitchedPath> result = new ArrayList<LabelSwitchedPath>();
        LabelSwitchedPath lspModel = RsvpLspModelCreator.createNullModel(inventoryId);
        result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lspModel));
        return result;
    }

    @SuppressWarnings("unchecked")
    public void checkLspStatus(RsvpLspDto lsp) {
        FacilityStatus fs = FacilityStatusUtil.getStatus(lsp);
        log.debug("RsvpLspRenderer.getFacilityStatus(lsp)[" + fs + "]");
        if (!Util.isOneOf(fs, FacilityStatus.REVOKED, FacilityStatus.LOST)) {
            throw new IllegalArgumentException(
                    "can't remove lsp[" + RsvpLspRenderer.getLspName(lsp) + "] : FacilityStatus is not [" + FacilityStatus.REVOKED + "]");
        }
    }


}