package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.VlanPort;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.nmscore.view.portedit.VlanIfEditDialogMaker;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import javax.servlet.ServletException;

public class InventoryGetGatewayVlanIf extends UnificUIViewState {


    public InventoryGetGatewayVlanIf(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            VlanPort model = (VlanPort) Operation.getTargets(context);
            String targetVlanId = model.getVlanId().toString();
            String targetVlanPool = model.getVlanPoolName();
            VlanDto vlan = getTargetVlan(targetVlanId, targetVlanPool);
            VlanIfDto targetVlanIf = null;
            if (vlan != null) {
                for (VlanIfDto vlanIf : vlan.getMemberVlanifs()) {
                    if (isTarget(vlanIf)) {
                        targetVlanIf = vlanIf;
                        break;
                    }
                }
            }
            if (vlan != null && targetVlanIf != null) {
                VlanIfEditDialogMaker maker = new VlanIfEditDialogMaker(InventoryIdCalculator.getId(targetVlanIf), targetVlanIf);
                model = (VlanPort) maker.makeDialog();
            } else {
                model = null;
            }
            setXmlObject(model);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    private boolean isTarget(VlanIfDto vlanIf) {
        log.debug(NodeRenderer.getNodeType(vlanIf.getNode()) + " # " + vlanIf.getAbsoluteName() + " " + DtoUtil.getMvoIdString(vlanIf));
        if (NodeRenderer.getNodeType(vlanIf.getNode()).equals("FGT_1240B")
                || NodeRenderer.getNodeType(vlanIf.getNode()).equals("FGT_1000C")
                || NodeRenderer.getNodeType(vlanIf.getNode()).equals("FGT_310B")
                || NodeRenderer.getNodeType(vlanIf.getNode()).equals("FGT_300C")) {
            if (PortEditConstraints.isPortDirectLine(vlanIf)) {
                String ipAddress = PortRenderer.getIpAddress(vlanIf);
                if (ipAddress == null || ipAddress.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private VlanDto getTargetVlan(String targetVlanId, String targetVlanPool) throws ExternalServiceException {
        for (VlanIdPoolDto pool : VlanRenderer.getVlanIdPools()) {
            if (!pool.getName().equals(targetVlanPool)) continue;
            for (VlanDto vlan : pool.getUsers()) {
                String vlanId = VlanRenderer.getVlanId(vlan);
                if (targetVlanId.equals(vlanId)) {
                    return vlan;
                }
            }
        }
        return null;
    }
}