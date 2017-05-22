package voss.multilayernms.inventory.nmscore.fwlb;

import jp.iiga.nmt.core.model.Device;
import naef.dto.NodeDto;
import naef.ui.NaefDtoFacade;
import net.phalanx.fwlb.model.LB;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.model.creator.NodeModelCreator;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DeviceController implements FWLBController {

    @Override
    public boolean canHandle(String action) {
        return action.equals("device");
    }

    @Override
    public Object handleGet(FlowContext context) throws ServletException {
        Collection<LB> lbs = new ArrayList<LB>();
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            NaefDtoFacade facade;
            facade = conn.getDtoFacade();

            Collection<NodeDto> dtos = new ArrayList<NodeDto>();
            dtos.addAll(facade.getNodes());
            LB lb0 = new LB();
            lb0.setId("0");
            lb0.setName("NONE");
            lbs.add(lb0);

            for (NodeDto dto : dtos) {
                Device device = NodeModelCreator.createModel(
                        dto, DtoUtil.getMvoId(dto).toString());

                String osType = (String) device.getMetaData().getPropertyValue("OS Type");
                if (osType.equals("BIG-IP")) {
                    LB lb = new LB();
                    lb.setId(device.getId());
                    lb.setName(device.getName());
                    lbs.add(lb);
                }
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        }

        return lbs;
    }

    @Override
    public Object handlePut(FlowContext context, Object data)
            throws ServletException {
        return null;
    }
}