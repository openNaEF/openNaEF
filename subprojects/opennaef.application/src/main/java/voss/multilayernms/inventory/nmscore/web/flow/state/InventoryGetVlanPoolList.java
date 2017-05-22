package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import javax.servlet.ServletException;


public class InventoryGetVlanPoolList extends UnificUIViewState {

    public InventoryGetVlanPoolList(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            VlanEditModel model = new Vlan();
            model.setAllVlanPoolName(VlanRenderer.getVlanIdPoolsName());
            setXmlObject(model);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }
}