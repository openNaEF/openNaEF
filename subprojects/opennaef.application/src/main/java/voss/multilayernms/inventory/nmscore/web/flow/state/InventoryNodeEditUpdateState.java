package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.nodeedit.NodeEditModel;
import naef.dto.NodeDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.nms.inventory.builder.NodeCommandBuilder;
import voss.nms.inventory.diff.DiffCategory;

import javax.servlet.ServletException;

public class InventoryNodeEditUpdateState extends UnificUIViewState {

    public InventoryNodeEditUpdateState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            NodeEditModel model = (NodeEditModel) Operation.getTargets(context);
            String editorName = context.getUser();
            NodeDto node = null;
            String version = model.getVersion();
            if (version != null) {
                node = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), NodeDto.class);
                if (!version.equals(DtoUtil.getMvoVersionString(node))) {
                    throw new InventoryException("Version mismatch.");
                }
            }

            NodeCommandBuilder builder = new NodeCommandBuilder(node, editorName);
            builder.setNodeName(model.getNodeName());
            builder.setIpAddress(model.getIpAddress());
            builder.setSnmpMode(model.getSnmpMode());
            builder.setSnmpCommunityRO(model.getSnmpCommunityRO());
            builder.setPurpose(model.getPurpose());
            builder.setCliMode(model.getCliMode());
            builder.setAdminPassword(model.getAdminPassword());
            builder.setAdminUser(model.getAdminUser());
            builder.setLoginUser(model.getLoginUser());
            builder.setLoginPassword(model.getLoginPassword());
            builder.setNote(model.getNote());
            builder.setOsType(model.getOsType());
            builder.setOsVersion(model.getOsVersion());
            builder.setPurpose(model.getPurpose());
            builder.setLocation("location;" + model.getLocation());
            if (!model.getVendor().isEmpty() && !model.getNodeType().isEmpty()) {
                builder.setMetadata(model.getVendor(), model.getNodeType());
            }
            if (model.getVirtualizedHostingEnabled()) {
                builder.setVmHostingEnabled(model.getVirtualizedHostingEnabled());
            }
            if (node == null) {
                builder.setSource(DiffCategory.INVENTORY.name());
            }
            BuildResult result = builder.buildCommand();
            if (BuildResult.NO_CHANGES == result) {
                return;
            } else if (BuildResult.FAIL == result) {
                throw new IllegalStateException("Update Fail.");
            }
            ShellConnector.getInstance().execute(builder);

        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }
}