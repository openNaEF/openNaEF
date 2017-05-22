package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.nodeedit.NodeBatchModel;
import jp.iiga.nmt.core.model.nodeedit.NodeEditModel;
import naef.dto.NodeDto;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.NodeRenderer;

import javax.servlet.ServletException;


public class InventoryGetNodeEditListState extends UnificUIViewState {

    public InventoryGetNodeEditListState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            NodeBatchModel models = new NodeBatchModel();

            for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
                models.addModel(node2NodeEditModel(node));
            }
            setXmlObject(models);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    public NodeEditModel node2NodeEditModel(NodeDto node) {
        NodeEditModel model = new NodeEditModel();
        model.setInventoryId(InventoryIdCalculator.getId(node));
        model.setMvoId(DtoUtil.getMvoId(node).toString());
        model.setVersion(DtoUtil.getMvoVersionString(node));
        model.setNodeName(NodeRenderer.getNodeName(node));
        return model;
    }
}