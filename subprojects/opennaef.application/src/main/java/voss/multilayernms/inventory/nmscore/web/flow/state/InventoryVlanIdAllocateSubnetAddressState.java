package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.portedit.VlanIdAllocateSubnetAddress;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanIdAllocateSubnetAddressState extends UnificUIViewState {

    public InventoryVlanIdAllocateSubnetAddressState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanEditModel model = (VlanEditModel) Operation.getTargets(context);
            String user = context.getUser();
            if (model instanceof Vlan) {
                Integer vlanId = model.getVlanId();
                if (vlanId == null) {
                    throw new IllegalArgumentException("VLAN ID is null.");
                }
                new VlanIdAllocateSubnetAddress(model, user).update();
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