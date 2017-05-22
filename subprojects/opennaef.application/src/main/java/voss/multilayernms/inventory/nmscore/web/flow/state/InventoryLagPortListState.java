package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.LagPort;
import naef.dto.HardPortDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryLagPortListState extends UnificUIViewState {

    public InventoryLagPortListState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            String inventoryId = getInventoryIdFromQuery(Operation.getQuery(context));
            PortDto port = MplsNmsInventoryConnector.getInstance().getPortByInventoryID(inventoryId);

            Map<String, Map<String, String>> portList = new HashMap<String, Map<String, String>>();

            NodeDto node = port.getNode();
            for (PortDto p : node.getPorts()) {
                if (p instanceof HardPortDto) {
                    if (p.getContainer() == null) {
                        portList.put(p.getIfname(), new HashMap<String, String>());
                        portList.get(p.getIfname()).put(InventoryIdCalculator.getId(p), p.getAbsoluteName());
                    }
                }
            }

            LagPort model = new LagPort();
            model.setNodeName(node.getName());
            model.setNodeMvoId(DtoUtil.getMvoId(node).toString());
            model.setPortList(portList);
            model.setMemberPorts(new HashMap<String, Map<String, String>>());
            setXmlObject(model);

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static String getInventoryIdFromQuery(ObjectFilterQuery query) {
        String inventoryId = null;

        Object target = query.get("ID").getPattern();
        if (target instanceof String) {
            inventoryId = (String) target;
        } else if (target instanceof List) {
            inventoryId = (String) ((List<String>) target).get(0);
        }

        log.debug("inventoryId[" + inventoryId + "]");
        return inventoryId;

    }

}