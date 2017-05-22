package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.resistvlansubnet.IpSubnetNamespaceModel;
import voss.multilayernms.inventory.nmscore.inventory.ipsubnet.IpSubnetNamespaceUpdaterFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryIpSubnetNamespaceUpdateState extends UnificUIViewState {

    public InventoryIpSubnetNamespaceUpdateState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            IpSubnetNamespaceModel model = getTarget(context);
            String user = context.getUser();
            IpSubnetNamespaceUpdaterFactory.getUpdater(model, user).commit();
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    private IpSubnetNamespaceModel getTarget(FlowContext context) throws IOException {
        return (IpSubnetNamespaceModel) Operation.getTargets(context);
    }
}