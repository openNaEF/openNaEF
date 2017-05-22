package voss.multilayernms.inventory.nmscore.fwlb;

import net.phalanx.core.models.Vlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VlanHandler;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

public class VLANsController implements FWLBController {

    private static final Logger log = LoggerFactory.getLogger(VLANsController.class);

    @Override
    public boolean canHandle(String action) {
        return action.equals("vlan");
    }

    @Override
    public Object handleGet(FlowContext context) throws ServletException {
        List<Vlan> vlans;
        try {
            vlans = VlanHandler.getList(context.getParameter("customerId"));
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        }

        return vlans;
    }

    @Override
    public Object handlePut(FlowContext context, Object data)
            throws ServletException {
        return null;
    }
}