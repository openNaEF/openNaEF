package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;
import naef.dto.PortDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import net.phalanx.core.models.PseudoWire;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.PseudoWireHandler;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.nmscore.model.converter.PseudoWireModelDisplayNameConverter;
import voss.multilayernms.inventory.nmscore.model.creator.PseudoWireModelCreator;
import voss.nms.inventory.util.RsvpLspUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PseudoWireRefresher extends Refresher {

    public PseudoWireRefresher(Collection<? extends IModel> targets, String userName) {
        super(targets, userName);
    }

    @Override
    public List<? extends IModel> refresh() throws RemoteException, ExternalServiceException, IOException, AuthenticationException, NotBoundException, InstantiationException, IllegalAccessException {
        List<PseudoWire> result = new ArrayList<PseudoWire>();

        List<String> inventoryIds = new ArrayList<String>();
        for (IModel target : getTargets()) {
            inventoryIds.add(target.getId());
        }
        for (String inventoryId : inventoryIds) {
            FakePseudoWire fpw = PseudoWireHandler.getPseudowireDto(inventoryId);
            if (fpw.isPipe()) {
                PortDto ac1 = fpw.getAc1();
                PortDto ac2 = fpw.getAc2();
                PseudoWire pwModel1 = PseudoWireModelCreator.createModel(fpw, null, inventoryId, ac1, ac2);
                result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel1));
                PseudoWire pwModel2 = PseudoWireModelCreator.createModel(fpw, null, inventoryId, ac2, ac1);
                result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel2));
            } else {
                PseudowireDto pw = fpw.getPseudowireDto();
                for (RsvpLspDto lsp : RsvpLspUtil.getLspsUnder(pw)) {
                    if (PseudoWireHandler.getInventoryIdOnLsp(pw, lsp).equals(inventoryId)) {
                        PortDto acOnIngress = PseudoWireHandler.getAcOnIngress(pw, lsp);
                        PortDto acOnEgress = PseudoWireHandler.getAcOnEgress(pw, lsp);
                        PseudoWire pwModel = PseudoWireModelCreator.createModel(fpw, lsp, inventoryId, acOnIngress, acOnEgress);
                        result.add((PseudoWire) new PseudoWireModelDisplayNameConverter().convertModel(pwModel));
                    }
                }
            }
        }

        return result;
    }

}