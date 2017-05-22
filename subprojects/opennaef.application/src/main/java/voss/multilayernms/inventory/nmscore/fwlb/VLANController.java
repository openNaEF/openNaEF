package voss.multilayernms.inventory.nmscore.fwlb;

import naef.dto.CustomerInfoDto;
import naef.dto.vlan.VlanDto;
import net.phalanx.core.models.Vlan;
import net.phalanx.fwlb.model.VLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.accessor.VlanHandler;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.nms.inventory.builder.AttributeUpdateCommandBuilder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VLANController implements FWLBController {

    private static final Logger log = LoggerFactory.getLogger(VLANController.class);

    @Override
    public boolean canHandle(String action) {
        return action.equals("vlan");
    }

    @Override
    public Object handleGet(FlowContext context) throws ServletException {
        List<VLAN> vlans = new ArrayList<VLAN>();
        try {
            List<Vlan> dtoVlans = VlanHandler.getList(context.getParameter("customerId"));

            for (Vlan dtoVlan : dtoVlans) {
                VLAN vlan = new VLAN();
                vlan.setId(Integer.decode(dtoVlan.getId()));
                vlan.setAreaCode((String) dtoVlan.getMetaData().getPropertyValue("AreaCode"));
                vlan.setType((String) dtoVlan.getMetaData().getPropertyValue("Purpose"));
                vlan.setRouteDomain((String) dtoVlan.getMetaData().getPropertyValue("ROUTE_DOMAIN"));
                vlans.add(vlan);
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        }

        return vlans;
    }

    @Override
    public Object handlePut(FlowContext context, Object data) throws ServletException {
        @SuppressWarnings("unchecked")
        Map<Integer, String> routeDomains = (Map<Integer, String>) data;

        try {
            String customerId = context.getParameter("customerId");

            for (Integer vlanId : routeDomains.keySet()) {
                VlanDto vlanDto = getVlanDto(customerId, vlanId);
                setRouteDomainAttributeToDto(
                        vlanDto, routeDomains.get(vlanId), context.getUser());
            }
        } catch (ExternalServiceException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (InventoryException e) {
            throw new ServletException(e);
        }

        return new Object();
    }

    private void setRouteDomainAttributeToDto(VlanDto vlanDto, String routeDomain, String editor) throws IOException, ExternalServiceException, InventoryException {
        if (!routeDomain.isEmpty()) {
            AttributeUpdateCommandBuilder builder = new AttributeUpdateCommandBuilder(vlanDto, editor);
            builder.setValue(FWLBATTR.ROUTE_DOMAIN, routeDomain);
            builder.buildCommand();
            ShellConnector.getInstance().execute(builder);
        }

    }

    private VlanDto getVlanDto(String customerId, Integer vlanId) throws IOException, ExternalServiceException, InventoryException {
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        CustomerInfoDto customerInfoDto = conn.getMvoDto(customerId, CustomerInfoDto.class);
        List<VlanDto> vlanDtos = CustomerInfoRenderer.getMemberVlans(customerInfoDto);

        for (VlanDto vlanDto : vlanDtos) {
            if (vlanDto.getVlanId().equals(vlanId)) {
                return vlanDto;
            }
        }
        return null;
    }
}