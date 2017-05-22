package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.nodeedit.NodeBatchModel;
import jp.iiga.nmt.core.model.nodeedit.NodeBatchModel.BatchTarget;
import jp.iiga.nmt.core.model.nodeedit.NodeEditModel;
import naef.dto.NodeDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.nms.inventory.builder.NodeCommandBuilder;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;


public class InventoryNodeBatchProcessState extends UnificUIViewState {

    public InventoryNodeBatchProcessState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            NodeBatchModel models = (NodeBatchModel) Operation.getTargets(context);
            String value = models.getValue();
            BatchTarget target = models.getTarget();
            if (target == null) {
                throw new IllegalArgumentException("target is null");
            }
            String editorName = context.getUser();
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            NodeDto node;
            for (NodeEditModel model : models.getModels()) {
                node = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), NodeDto.class);
                if (!DtoUtil.getMvoVersionString(node).equals(model.getVersion())) {
                    throw new InventoryException("Version mismatch.:" + NodeRenderer.getNodeName(node));
                }
                NodeCommandBuilder builder = new NodeCommandBuilder(node, editorName);
                setParameter(builder, target, value);
                BuildResult result = builder.buildCommand();
                if (BuildResult.NO_CHANGES == result) {
                    return;
                } else if (BuildResult.FAIL == result) {
                    throw new IllegalStateException("Update Fail.");
                }
                commandBuilderList.add(builder);
            }
            ShellConnector.getInstance().executes(commandBuilderList);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    public void setParameter(NodeCommandBuilder builder, BatchTarget target, String value) {
        switch (target) {
            case SnmpComunity:
                builder.setSnmpCommunityRO(value);
                break;
            case LoginAccount:
                builder.setLoginUser(value);
                break;
            case LoginPassword:
                builder.setLoginPassword(value);
                break;
            case AdminAccount:
                builder.setAdminUser(value);
                break;
            case AdminPassword:
                builder.setAdminPassword(value);
                break;
        }
    }
}