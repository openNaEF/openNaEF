package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.RsvpLspHandler;
import voss.multilayernms.inventory.nmscore.model.converter.RsvpLspModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.RsvpLspModelCreator;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class RsvpLspGroupingModifier extends Modifier {

    private final Logger log = LoggerFactory.getLogger(FacilityStatusUpdater.class);

    public RsvpLspGroupingModifier(List<? extends IModel> targets, String userName) {
        super(targets, userName);

        for (IModel target : targets) {
            for (String key : target.getMetaData().getProperties().keySet()) {
                log.debug(key + "=" + target.getMetaData().getProperties().get(key));
            }
        }
    }

    public List<LabelSwitchedPath> grouping() throws AuthenticationException, RemoteException, IOException, InventoryException,
            ExternalServiceException, InstantiationException, IllegalAccessException {
        List<LabelSwitchedPath> result = new ArrayList<LabelSwitchedPath>();

        if (getTargets().size() != 2) {
            throw new IllegalArgumentException("can't couple lsps : lsps.size() is not 2.");
        }

        Iterator<? extends IModel> i = getTargets().iterator();

        String lsp1InventoryId = i.next().getId();
        String lsp2InventoryId = i.next().getId();
        log.debug("id1:" + lsp1InventoryId);
        log.debug("id2:" + lsp2InventoryId);

        RsvpLspDto lsp1 = RsvpLspHandler.getRsvpLspDto(lsp1InventoryId);
        RsvpLspDto lsp2 = RsvpLspHandler.getRsvpLspDto(lsp2InventoryId);

        checkLspsStatus(lsp1, lsp2);

        RsvpLspHandler.executeGrouping(lsp1, lsp2, getUserName());

        RsvpLspDto newLsp1 = RsvpLspHandler.getRsvpLspDto(lsp1InventoryId);
        RsvpLspDto newLsp2 = RsvpLspHandler.getRsvpLspDto(lsp2InventoryId);
        LabelSwitchedPath lsp1Model = RsvpLspModelCreator.createModel(newLsp1, lsp1InventoryId);
        LabelSwitchedPath lsp2Model = RsvpLspModelCreator.createModel(newLsp2, lsp2InventoryId);

        result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lsp1Model));
        result.add((LabelSwitchedPath) new RsvpLspModelDisplayNameConverter().convertModel(lsp2Model));

        return result;
    }

    private void checkLspsStatus(RsvpLspDto lsp1, RsvpLspDto lsp2) {

        if (RsvpLspRenderer.getOppositLspName(lsp1) != null) {
            throw new IllegalArgumentException("can't couple lsps : lsp1 has opposite lsp.");
        }

        if (RsvpLspRenderer.getOppositLspName(lsp2) != null) {
            throw new IllegalArgumentException("can't couple lsps : lsp2 has opposite lsp.");
        }

        if (!RsvpLspRenderer.getOperStatus(lsp1).equals(RsvpLspRenderer.getOperStatus(lsp2))) {
            throw new IllegalArgumentException("can't couple lsps : differ operation status. lsp1 != lsp2.");
        }

        if (!RsvpLspRenderer.getFacilityStatus(lsp1).equals(RsvpLspRenderer.getFacilityStatus(lsp2))) {
            throw new IllegalArgumentException("can't couple lsps : differ equipment status. lsp1 != lsp2.");
        }
    }

}