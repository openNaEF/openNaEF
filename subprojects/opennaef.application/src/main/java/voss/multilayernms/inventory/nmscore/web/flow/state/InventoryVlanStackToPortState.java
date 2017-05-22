package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.portedit.TaggedVlanStackToPort;
import voss.multilayernms.inventory.nmscore.portedit.UnTaggedVlanStackToPort;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanStackToPortState extends UnificUIViewState {

    public InventoryVlanStackToPortState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            PortEditModel model = (PortEditModel) Operation.getTargets(context);
            String user = context.getUser();
            PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
            if (model instanceof HardPort) {
                String version = model.getVersion();
                if (version != null && !version.equals(DtoUtil.getMvoVersionString(port))) {
                    throw new InventoryException("The versions do not match.\nRefreshes the list view.");
                }
                if (((HardPort) model).getTagMode().matches("UnTagged")) {
                    new UnTaggedVlanStackToPort(port, model, user).update();
                } else if (((HardPort) model).getTagMode().matches("Tagged")) {
                    new TaggedVlanStackToPort(port, model, user).update();
                }
            } else if (model instanceof LagPort) {
                String version = model.getVersion();
                if (version != null && !version.equals(DtoUtil.getMvoVersionString(port))) {
                    throw new InventoryException("The versions do not match.\nRefreshes the list view.");
                }
                if (((LagPort) model).getTagMode().matches("UnTagged")) {
                    new UnTaggedVlanStackToPort(port, model, user).update();
                } else if (((LagPort) model).getTagMode().matches("Tagged")) {
                    new TaggedVlanStackToPort(port, model, user).update();
                }
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