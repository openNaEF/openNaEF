package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.SubnetIp;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.PortRenderer;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class InventoryGetPortList extends UnificUIViewState {


    public InventoryGetPortList(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            SubnetIp model = (SubnetIp) Operation.getTargets(context);
            Map<String, Map<String, String>> map = new TreeMap<String, Map<String, String>>();
            for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
                if (PortEditConstraints.isPortSetIpEnabled(node)) {
                    String nodeName = node.getName();
                    map.put(nodeName, new HashMap<String, String>());
                    for (PortDto port : node.getPorts()) {
                        if (port != null) {
                            if (PortEditConstraints.isPortSetIpEnabled(port)) {
                                map.get(nodeName).put(PortRenderer.getIfName(port), DtoUtil.getMvoId(port).toString());
                            }
                        }
                    }
                }
            }
            model.setPortList(map);
            IpSubnetDto subnet = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getSubnetMvoId(), IpSubnetDto.class);
            model.setVersion(DtoUtil.getMvoVersionString(subnet));
            setXmlObject(model);

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }
}