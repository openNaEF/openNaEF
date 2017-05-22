package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import jp.iiga.nmt.core.model.portedit.VlanPort;
import naef.dto.PortDto;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.portedit.delete.LagPortDelete;
import voss.multilayernms.inventory.nmscore.portedit.delete.VlanIfDelete;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;

public class InventoryPortDeleteState extends UnificUIViewState {

    public InventoryPortDeleteState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            PortEditModel model = (PortEditModel) Operation.getTargets(context);
            PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
            String user = context.getUser();

            if (model instanceof LagPort) {
                String version = DtoUtil.getMvoVersionString(port);
                if (!version.equals(model.getVersion())) {
                    throw new InventoryException("Version mismatch.");
                }
                new LagPortDelete(model, user).delete();
            } else if (model instanceof VlanPort) {
                String version = DtoUtil.getMvoVersionString(port);
                if (!version.equals(model.getVersion())) {
                    throw new InventoryException("Version mismatch.");
                }
                new VlanIfDelete(model, user).delete();
            } else {
                throw new IllegalArgumentException("Target is unknown.");
            }

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }
}