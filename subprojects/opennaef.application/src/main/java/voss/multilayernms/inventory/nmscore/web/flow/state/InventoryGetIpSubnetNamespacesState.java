package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.vlan.VlanDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.view.portedit.GetIpSubnetNamespaces;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryGetIpSubnetNamespacesState extends UnificUIViewState {

    private final static Logger log = LoggerFactory.getLogger(InventoryGetIpSubnetNamespacesState.class);

    public InventoryGetIpSubnetNamespacesState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanEditModel model = (VlanEditModel) Operation.getTargets(context);

            if (model instanceof Vlan) {
                model.setAllVlanPoolName(VlanRenderer.getVlanIdPoolsName());
                VlanDto vlan = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), VlanDto.class);
                model.setVlanPoolName(VlanRenderer.getVlanIdPoolName(vlan));

                setXmlObject(new GetIpSubnetNamespaces(model).getIpSubnetNamespaces());

            } else {
                throw new IllegalArgumentException("Target is wrong.");
            }
            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }

}