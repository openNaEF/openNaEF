package voss.multilayernms.inventory.nmscore.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.fwlb.*;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.util.ArrayList;


public class InventoryFWLBState extends UnificUIViewState {

    static Logger log = LoggerFactory.getLogger(InventoryFWLBState.class);
    private ArrayList<FWLBController> controllers;

    public InventoryFWLBState(StateId stateId) {
        super(stateId);

        this.controllers = new ArrayList<FWLBController>();
        controllers.add(new CustomerInfoController());
        controllers.add(new DeviceController());
        controllers.add(new VLANController());
        controllers.add(new AssignedIpController());
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            String reqMethod = context.getHttpServletRequest().getMethod();
            String action = context.getParameter("action");
            Object res = new Object();
            for (FWLBController controller : this.controllers) {
                if (controller.canHandle(action)) {
                    if (reqMethod.equals("GET")) {
                        res = controller.handleGet(context);
                    } else if (reqMethod.equals("PUT")) {
                        Object data = Operation.getTargets(context);
                        res = controller.handlePut(context, data);
                    }
                }
            }
            super.setXmlObject(res);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }
}