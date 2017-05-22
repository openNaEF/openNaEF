package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.nodeedit.NodeEditModel;
import naef.dto.LocationDto;
import naef.dto.NodeDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.nms.inventory.database.MetadataManager;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryNodeEditDialogState extends UnificUIViewState {

    public InventoryNodeEditDialogState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            NodeEditModel model = (NodeEditModel) Operation.getTargets(context);
            if (model.getMvoId() != null) {
                NodeDto node = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), NodeDto.class);
                populateModel(model, node);
            } else {
                setLists(model);
            }
            setXmlObject(model);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    private void populateModel(NodeEditModel model, NodeDto node) throws IOException, ExternalServiceException, InventoryException {
        model.setVersion(DtoUtil.getMvoVersionString(node));
        model.setNodeName(NodeRenderer.getNodeName(node));
        model.setIpAddress(NodeRenderer.getManagementIpAddress(node));
        model.setVendor(NodeRenderer.getVendorName(node));
        model.setNodeType(NodeRenderer.getNodeType(node));
        model.setOsType(NodeRenderer.getOsType(node));
        model.setOsVersion(NodeRenderer.getOsVersion(node));
        model.setLocation(NodeRenderer.getLocationName(node));
        model.setSnmpMode(NodeRenderer.getSnmpMode(node));
        model.setSnmpCommunityRO(NodeRenderer.getSnmpCommunity(node));
        model.setAdminPassword(NodeRenderer.getPrivilegedLoginPassword(node));
        model.setAdminUser(NodeRenderer.getPrivilegedUserName(node));
        model.setLoginPassword(NodeRenderer.getConsoleLoginPassword(node));
        model.setLoginUser(NodeRenderer.getConsoleLoginUserName(node));
        model.setPurpose(NodeRenderer.getPurpose(node));
        model.setCliMode(NodeRenderer.getCliMode(node));
        model.setNote(NodeRenderer.getNote(node));
        model.setVirtualizedHostingEnabled(NodeRenderer.isVmHostingEnable(node));
        setLists(model);
    }

    public void setLists(NodeEditModel model) throws ExternalServiceException, IOException, InventoryException {

        Map<String, String> locationList = new HashMap<String, String>();
        for (LocationDto location : LocationUtil.getAreas()) {
            locationList.put(LocationUtil.getCaption(location), location.getName());
        }
        model.setLocationList(locationList);

        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        model.setSnmpTypeList(conn.getSnmpMethodList());
        model.setConsoleTypes(conn.getConsoleTypeList());

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        MetadataManager mm = MetadataManager.getInstance();
        List<String> vendors = mm.getVendorList();
        if (vendors == null) {
            vendors = Collections.emptyList();
        }
        for (String vendor : vendors) {
            map.put(vendor, mm.getNodeTypeList(vendor));
        }
        model.setMaker_nodeTypeList(map);
    }
}