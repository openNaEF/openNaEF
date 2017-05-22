package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.PseudoWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PseudoWireHandler;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.nmscore.model.converter.PseudoWireModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.PseudoWireModelCreator;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PseudoWireFacilityStatusUpdater extends FacilityStatusUpdater {

    private final Logger log = LoggerFactory.getLogger(PseudoWireFacilityStatusUpdater.class);

    public PseudoWireFacilityStatusUpdater(Collection<? extends IModel> targets, String userName) throws IOException {
        super(targets, userName, new PseudoWireModelDisplayNameConverter());
    }

    @Override
    public List<? extends IModel> update() throws RemoteException, InventoryException, ExternalServiceException,
            IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        List<PseudoWire> result = new ArrayList<PseudoWire>();

        Map<String, FacilityStatus> newStatus = getNewStatus(getTargets());
        for (Entry<String, FacilityStatus> entry : newStatus.entrySet()) {
            String inventoryId = entry.getKey();
            FacilityStatus fs = entry.getValue();
            FakePseudoWire fpw = PseudoWireHandler.getPseudowireDto(inventoryId);

            if (!FacilityStatusUtil.isAllowableChangeByGUI(fpw, fs)) {
                throw new IllegalStateException("cannot change: PW=[" + fpw.getPseudoWireId()
                        + ":" + fpw.getPseudoWireName() + "] " + fs.getDisplayString());
            }

            PseudoWireHandler.executeSetFacilityStatus(fpw, fs, getUserName());

            fpw.renew();
            if (fpw.isPipe()) {
                PortDto ac1 = fpw.getAc1();
                PortDto ac2 = fpw.getAc2();
                PseudoWire pwModel1 = PseudoWireModelCreator.createModel(fpw, null, inventoryId, ac1, ac2);
                result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel1));
                PseudoWire pwModel2 = PseudoWireModelCreator.createModel(fpw, null, inventoryId, ac2, ac1);
                result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel2));
            } else {
                PseudowireDto pw = fpw.getPseudowireDto();
                for (RsvpLspDto lsp : pw.getRsvpLsps()) {
                    log.debug("result.add(: id[" + inventoryId + "/" + RsvpLspRenderer.getLspName(lsp) + "]");
                    PortDto acOnIngress = PseudoWireHandler.getAcOnIngress(pw, lsp);
                    PortDto acOnEgress = PseudoWireHandler.getAcOnEgress(pw, lsp);
                    PseudoWire pwModel = PseudoWireModelCreator.createModel(fpw, lsp, inventoryId, acOnIngress, acOnEgress);
                    result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel));
                }
            }
        }

        return result;
    }
}